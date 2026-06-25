package com.jai.loglens.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MetricsSummary(
        Instant from,
        Instant to,
        long total,
        long errorCount,
        double errorRate,
        Map<String, Long> byLevel,
        Map<String, Long> byService,
        List<BucketPoint> volume
) {
}
