package com.jai.loglens.metrics;

import com.jai.loglens.domain.LogLevel;
import com.jai.loglens.dto.BucketPoint;
import com.jai.loglens.dto.MetricsSummary;
import com.jai.loglens.query.QueryService;
import com.jai.loglens.repository.LogEventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private final LogEventRepository repository;
    private final QueryService queryService;

    public MetricsService(LogEventRepository repository, QueryService queryService) {
        this.repository = repository;
        this.queryService = queryService;
    }

    public MetricsSummary summary(Instant from, Instant to, int buckets) {
        long total = queryService.countMatching(null, null, null, from, to);
        long errors = queryService.countErrors(null, from, to);

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (LogLevel level : LogLevel.values()) {
            long count = queryService.countMatching(null, level.name(), null, from, to);
            if (count > 0) {
                byLevel.put(level.name(), count);
            }
        }

        Map<String, Long> byService = new LinkedHashMap<>();
        for (Object[] row : repository.countByServiceBetween(from, to)) {
            byService.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        double errorRate = total == 0 ? 0.0 : (double) errors / total;

        return new MetricsSummary(from, to, total, errors, errorRate, byLevel, byService,
                histogram(from, to, buckets));
    }

    private List<BucketPoint> histogram(Instant from, Instant to, int buckets) {
        int n = Math.min(Math.max(buckets, 1), 240);
        long spanMillis = Math.max(Duration.between(from, to).toMillis(), 1);
        long step = Math.max(spanMillis / n, 1);

        List<BucketPoint> points = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Instant start = from.plusMillis(step * i);
            Instant end = (i == n - 1) ? to : from.plusMillis(step * (i + 1));
            long count = queryService.countMatching(null, null, null, start, end);
            long errs = queryService.countErrors(null, start, end);
            points.add(new BucketPoint(start, count, errs));
        }
        return points;
    }
}
