package com.jai.loglens.alerting;

import com.jai.loglens.domain.AlertRule;
import com.jai.loglens.domain.RuleType;
import com.jai.loglens.dto.AlertRuleRequest;
import com.jai.loglens.repository.AlertRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlertRuleService {

    private final AlertRuleRepository repository;

    public AlertRuleService(AlertRuleRepository repository) {
        this.repository = repository;
    }

    public List<AlertRule> list() {
        return repository.findAll();
    }

    public Optional<AlertRule> get(Long id) {
        return repository.findById(id);
    }

    public AlertRule create(AlertRuleRequest request) {
        AlertRule rule = new AlertRule(
                request.name(),
                blankToNull(request.service()),
                request.ruleType(),
                blankToNull(request.level()),
                blankToNull(request.keyword()),
                request.threshold() == null ? 10.0 : request.threshold(),
                request.windowMinutes() == null ? 5 : request.windowMinutes(),
                request.baselineWindows() == null ? 12 : request.baselineWindows(),
                request.absenceMinutes() == null ? 10 : request.absenceMinutes(),
                request.cooldownMinutes() == null ? 10 : request.cooldownMinutes(),
                request.enabled() == null || request.enabled()
        );
        return repository.save(rule);
    }

    public Optional<AlertRule> update(Long id, AlertRuleRequest request) {
        Optional<AlertRule> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        AlertRule rule = existing.get();
        if (request.name() != null) {
            rule.setName(request.name());
        }
        if (request.service() != null) {
            rule.setServiceName(blankToNull(request.service()));
        }
        if (request.ruleType() != null) {
            rule.setRuleType(request.ruleType());
        }
        if (request.level() != null) {
            rule.setLevel(blankToNull(request.level()));
        }
        if (request.keyword() != null) {
            rule.setKeyword(blankToNull(request.keyword()));
        }
        if (request.threshold() != null) {
            rule.setThreshold(request.threshold());
        }
        if (request.windowMinutes() != null) {
            rule.setWindowMinutes(request.windowMinutes());
        }
        if (request.baselineWindows() != null) {
            rule.setBaselineWindows(request.baselineWindows());
        }
        if (request.absenceMinutes() != null) {
            rule.setAbsenceMinutes(request.absenceMinutes());
        }
        if (request.cooldownMinutes() != null) {
            rule.setCooldownMinutes(request.cooldownMinutes());
        }
        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }
        return Optional.of(repository.save(rule));
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    public void seedDefaultsIfEmpty() {
        if (repository.count() > 0) {
            return;
        }
        repository.save(new AlertRule("payment-error-spike", "payment-service", RuleType.THRESHOLD,
                "ERROR", null, 15, 5, 12, 10, 10, true));
        repository.save(new AlertRule("checkout-volume-anomaly", "checkout-service", RuleType.ANOMALY,
                null, null, 2.0, 5, 12, 10, 10, true));
        repository.save(new AlertRule("inventory-heartbeat", "inventory-service", RuleType.ABSENCE,
                null, null, 0, 5, 12, 8, 10, true));
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }
}
