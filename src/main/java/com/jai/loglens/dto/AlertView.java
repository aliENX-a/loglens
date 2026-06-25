package com.jai.loglens.dto;

import com.jai.loglens.domain.Alert;
import com.jai.loglens.domain.AlertSeverity;
import com.jai.loglens.domain.AlertStatus;

import java.time.Instant;

public record AlertView(
        Long id,
        Long ruleId,
        String ruleName,
        String service,
        AlertSeverity severity,
        String message,
        double observedValue,
        double expectedValue,
        Instant triggeredAt,
        Instant resolvedAt,
        AlertStatus status
) {

    public static AlertView from(Alert a) {
        return new AlertView(
                a.getId(),
                a.getRuleId(),
                a.getRuleName(),
                a.getServiceName(),
                a.getSeverity(),
                a.getMessage(),
                a.getObservedValue(),
                a.getExpectedValue(),
                a.getTriggeredAt(),
                a.getResolvedAt(),
                a.getStatus()
        );
    }
}
