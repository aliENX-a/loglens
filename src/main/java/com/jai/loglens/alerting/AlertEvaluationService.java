package com.jai.loglens.alerting;

import com.jai.loglens.domain.Alert;
import com.jai.loglens.domain.AlertRule;
import com.jai.loglens.domain.AlertSeverity;
import com.jai.loglens.domain.AlertStatus;
import com.jai.loglens.query.QueryService;
import com.jai.loglens.repository.AlertRepository;
import com.jai.loglens.repository.AlertRuleRepository;
import com.jai.loglens.repository.LogEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlertEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationService.class);

    private final AlertRuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final LogEventRepository logRepository;
    private final QueryService queryService;
    private final AnomalyDetector detector;

    public AlertEvaluationService(AlertRuleRepository ruleRepository,
                                  AlertRepository alertRepository,
                                  LogEventRepository logRepository,
                                  QueryService queryService,
                                  AnomalyDetector detector) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
        this.logRepository = logRepository;
        this.queryService = queryService;
        this.detector = detector;
    }

    @Scheduled(fixedDelayString = "${loglens.alerts.scan-interval-ms:15000}")
    public void scan() {
        Instant now = Instant.now();
        for (AlertRule rule : ruleRepository.findByEnabledTrue()) {
            try {
                evaluate(rule, now);
            } catch (RuntimeException e) {
                log.warn("rule {} failed to evaluate: {}", rule.getName(), e.getMessage());
            }
        }
        resolveStale(now);
    }

    public void evaluate(AlertRule rule, Instant now) {
        Evaluation result = evaluateCondition(rule, now);
        if (!result.breached()) {
            return;
        }

        String key = dedupKey(rule, now);
        if (alertRepository.findByDedupKey(key).isPresent()) {
            return;
        }
        if (inCooldown(rule, now)) {
            return;
        }

        Alert alert = new Alert(
                rule.getId(),
                rule.getName(),
                rule.getServiceName(),
                severityFor(rule),
                describe(rule, result),
                result.observed(),
                result.expected(),
                now,
                key
        );
        alertRepository.save(alert);
        log.warn("ALERT {}: {}", alert.getSeverity(), alert.getMessage());
    }

    public Evaluation evaluateCondition(AlertRule rule, Instant now) {
        switch (rule.getRuleType()) {
            case THRESHOLD: {
                Instant from = now.minus(Duration.ofMinutes(Math.max(rule.getWindowMinutes(), 1)));
                long observed = count(rule, from, now);
                return new Evaluation(observed > rule.getThreshold(), observed, rule.getThreshold());
            }
            case ANOMALY: {
                int window = Math.max(rule.getWindowMinutes(), 1);
                Instant from = now.minus(Duration.ofMinutes(window));
                double current = count(rule, from, now);

                List<Double> baseline = new ArrayList<>();
                for (int i = 1; i <= Math.max(rule.getBaselineWindows(), 1); i++) {
                    Instant s = now.minus(Duration.ofMinutes((long) window * (i + 1)));
                    Instant e = now.minus(Duration.ofMinutes((long) window * i));
                    baseline.add((double) count(rule, s, e));
                }

                double z = detector.zscore(current, baseline);
                return new Evaluation(z > rule.getThreshold(), current, detector.mean(baseline), z);
            }
            case ABSENCE: {
                String service = rule.getServiceName();
                if (service == null || service.isBlank()) {
                    return new Evaluation(false, 0, 0);
                }
                Instant since = now.minus(Duration.ofMinutes(Math.max(rule.getAbsenceMinutes(), 1)));
                long observed = logRepository.countByServiceNameAndEventTimeAfter(service, since);
                return new Evaluation(observed == 0, observed, 1);
            }
            default:
                return new Evaluation(false, 0, 0);
        }
    }

    private void resolveStale(Instant now) {
        for (Alert alert : alertRepository.findByStatusOrderByTriggeredAtDesc(AlertStatus.FIRING)) {
            Optional<AlertRule> rule = ruleRepository.findById(alert.getRuleId());
            if (rule.isEmpty() || !rule.get().isEnabled()) {
                alert.resolve(now);
                alertRepository.save(alert);
                continue;
            }
            try {
                if (!evaluateCondition(rule.get(), now).breached()) {
                    alert.resolve(now);
                    alertRepository.save(alert);
                }
            } catch (RuntimeException e) {
                log.warn("could not re-check alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    private long count(AlertRule rule, Instant from, Instant to) {
        return queryService.countMatching(rule.getServiceName(), rule.getLevel(), rule.getKeyword(), from, to);
    }

    private boolean inCooldown(AlertRule rule, Instant now) {
        Instant since = now.minus(Duration.ofMinutes(Math.max(rule.getCooldownMinutes(), 0)));
        return !alertRepository.findByRuleIdAndTriggeredAtAfter(rule.getId(), since).isEmpty();
    }

    private String dedupKey(AlertRule rule, Instant now) {
        long stepMinutes = Math.max(rule.getWindowMinutes(), 1);
        long stepMillis = Duration.ofMinutes(stepMinutes).toMillis();
        long bucket = (now.toEpochMilli() / stepMillis) * stepMillis;
        return rule.getId() + "|" + rule.getRuleType() + "|" + bucket;
    }

    private static AlertSeverity severityFor(AlertRule rule) {
        return rule.getRuleType() == com.jai.loglens.domain.RuleType.ABSENCE
                ? AlertSeverity.CRITICAL
                : AlertSeverity.WARNING;
    }

    private String describe(AlertRule rule, Evaluation e) {
        String target = rule.getServiceName() == null ? "all services" : rule.getServiceName();
        return switch (rule.getRuleType()) {
            case THRESHOLD -> String.format("%s: %d matching logs in last %d min (threshold %.0f)",
                    target, (long) e.observed(), Math.max(rule.getWindowMinutes(), 1), rule.getThreshold());
            case ANOMALY -> String.format("%s: %.0f logs in last %d min vs baseline %.1f (z=%.2f)",
                    target, e.observed(), Math.max(rule.getWindowMinutes(), 1), e.expected(), e.score());
            case ABSENCE -> String.format("%s: no logs received in the last %d min",
                    target, Math.max(rule.getAbsenceMinutes(), 1));
        };
    }

    public record Evaluation(boolean breached, double observed, double expected, double score) {

        public Evaluation(boolean breached, double observed, double expected) {
            this(breached, observed, expected, observed);
        }
    }
}
