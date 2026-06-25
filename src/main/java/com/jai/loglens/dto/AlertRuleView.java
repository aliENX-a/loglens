package com.jai.loglens.dto;

import com.jai.loglens.domain.AlertRule;
import com.jai.loglens.domain.RuleType;

import java.time.Instant;

public record AlertRuleView(
        Long id,
        String name,
        String service,
        RuleType ruleType,
        String level,
        String keyword,
        double threshold,
        int windowMinutes,
        int baselineWindows,
        int absenceMinutes,
        int cooldownMinutes,
        boolean enabled,
        Instant createdAt
) {

    public static AlertRuleView from(AlertRule r) {
        return new AlertRuleView(
                r.getId(),
                r.getName(),
                r.getServiceName(),
                r.getRuleType(),
                r.getLevel(),
                r.getKeyword(),
                r.getThreshold(),
                r.getWindowMinutes(),
                r.getBaselineWindows(),
                r.getAbsenceMinutes(),
                r.getCooldownMinutes(),
                r.isEnabled(),
                r.getCreatedAt()
        );
    }
}
