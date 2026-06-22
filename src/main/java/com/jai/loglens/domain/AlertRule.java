package com.jai.loglens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(length = 64)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RuleType ruleType;

    @Column(length = 16)
    private String level;

    @Column(length = 128)
    private String keyword;

    private double threshold;

    private int windowMinutes = 5;

    private int baselineWindows = 12;

    private int absenceMinutes = 10;

    private int cooldownMinutes = 10;

    private boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt;

    protected AlertRule() {
    }

    public AlertRule(String name, String serviceName, RuleType ruleType, String level, String keyword,
                     double threshold, int windowMinutes, int baselineWindows, int absenceMinutes,
                     int cooldownMinutes, boolean enabled) {
        this.name = name;
        this.serviceName = serviceName;
        this.ruleType = ruleType;
        this.level = level;
        this.keyword = keyword;
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
        this.baselineWindows = baselineWindows;
        this.absenceMinutes = absenceMinutes;
        this.cooldownMinutes = cooldownMinutes;
        this.enabled = enabled;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getServiceName() {
        return serviceName;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public String getLevel() {
        return level;
    }

    public String getKeyword() {
        return keyword;
    }

    public double getThreshold() {
        return threshold;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public int getBaselineWindows() {
        return baselineWindows;
    }

    public int getAbsenceMinutes() {
        return absenceMinutes;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public void setBaselineWindows(int baselineWindows) {
        this.baselineWindows = baselineWindows;
    }

    public void setAbsenceMinutes(int absenceMinutes) {
        this.absenceMinutes = absenceMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
