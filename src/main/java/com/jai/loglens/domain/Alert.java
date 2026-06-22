package com.jai.loglens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_triggered", columnList = "triggeredAt")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false, length = 64)
    private String ruleName;

    @Column(length = 64)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Column(length = 512)
    private String message;

    private double observedValue;

    private double expectedValue;

    @Column(nullable = false)
    private Instant triggeredAt;

    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status;

    @Column(nullable = false, unique = true, length = 160)
    private String dedupKey;

    protected Alert() {
    }

    public Alert(Long ruleId, String ruleName, String serviceName, AlertSeverity severity, String message,
                 double observedValue, double expectedValue, Instant triggeredAt, String dedupKey) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.serviceName = serviceName;
        this.severity = severity;
        this.message = message;
        this.observedValue = observedValue;
        this.expectedValue = expectedValue;
        this.triggeredAt = triggeredAt;
        this.dedupKey = dedupKey;
        this.status = AlertStatus.FIRING;
    }

    public void resolve(Instant at) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = at;
    }

    public Long getId() {
        return id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public double getObservedValue() {
        return observedValue;
    }

    public double getExpectedValue() {
        return expectedValue;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public String getDedupKey() {
        return dedupKey;
    }
}
