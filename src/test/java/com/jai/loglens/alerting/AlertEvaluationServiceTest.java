package com.jai.loglens.alerting;

import com.jai.loglens.domain.Alert;
import com.jai.loglens.domain.AlertRule;
import com.jai.loglens.domain.RuleType;
import com.jai.loglens.query.QueryService;
import com.jai.loglens.repository.AlertRepository;
import com.jai.loglens.repository.AlertRuleRepository;
import com.jai.loglens.repository.LogEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEvaluationServiceTest {

    @Mock
    private AlertRuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private LogEventRepository logRepository;

    @Mock
    private QueryService queryService;

    private AlertEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new AlertEvaluationService(ruleRepository, alertRepository, logRepository,
                queryService, new AnomalyDetector());
    }

    @Test
    void firesWhenCountCrossesThreshold() {
        AlertRule rule = rule(RuleType.THRESHOLD, 5.0);
        when(queryService.countMatching(eq("payment-service"), eq("ERROR"), isNull(), any(), any()))
                .thenReturn(10L);
        allowSave();

        service.evaluate(rule, Instant.now());

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void staysQuietBelowThreshold() {
        AlertRule rule = rule(RuleType.THRESHOLD, 5.0);
        when(queryService.countMatching(eq("payment-service"), eq("ERROR"), isNull(), any(), any()))
                .thenReturn(2L);

        service.evaluate(rule, Instant.now());

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void firesWhenServiceGoesSilent() {
        AlertRule rule = rule(RuleType.ABSENCE, 0.0);
        when(logRepository.countByServiceNameAndEventTimeAfter(eq("payment-service"), any()))
                .thenReturn(0L);
        allowSave();

        service.evaluate(rule, Instant.now());

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void doesNotRefireInsideCooldownWindow() {
        AlertRule rule = rule(RuleType.THRESHOLD, 5.0);
        when(queryService.countMatching(eq("payment-service"), eq("ERROR"), isNull(), any(), any()))
                .thenReturn(50L);
        when(alertRepository.findByDedupKey(anyString())).thenReturn(Optional.empty());
        when(alertRepository.findByRuleIdAndTriggeredAtAfter(any(), any()))
                .thenReturn(List.of(existingAlert()));

        service.evaluate(rule, Instant.now());

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void duplicateKeySuppressesSecondAlert() {
        AlertRule rule = rule(RuleType.THRESHOLD, 5.0);
        when(queryService.countMatching(eq("payment-service"), eq("ERROR"), isNull(), any(), any()))
                .thenReturn(50L);
        when(alertRepository.findByDedupKey(anyString())).thenReturn(Optional.of(existingAlert()));

        service.evaluate(rule, Instant.now());

        verify(alertRepository, never()).save(any(Alert.class));
    }

    private void allowSave() {
        when(alertRepository.findByDedupKey(anyString())).thenReturn(Optional.empty());
        when(alertRepository.findByRuleIdAndTriggeredAtAfter(any(), any())).thenReturn(List.of());
    }

    private Alert existingAlert() {
        return new Alert(1L, "payment-error-spike", "payment-service",
                com.jai.loglens.domain.AlertSeverity.WARNING, "stale", 10, 5,
                Instant.now().minusSeconds(30), "1|THRESHOLD|1");
    }

    private static AlertRule rule(RuleType type, double threshold) {
        return new AlertRule("payment-error-spike", "payment-service", type, "ERROR", null,
                threshold, 5, 12, 10, 10, true);
    }
}
