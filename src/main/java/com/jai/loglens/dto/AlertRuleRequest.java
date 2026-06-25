package com.jai.loglens.dto;

import com.jai.loglens.domain.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertRuleRequest(
        @NotBlank String name,
        String service,
        @NotNull RuleType ruleType,
        String level,
        String keyword,
        Double threshold,
        Integer windowMinutes,
        Integer baselineWindows,
        Integer absenceMinutes,
        Integer cooldownMinutes,
        Boolean enabled
) {
}
