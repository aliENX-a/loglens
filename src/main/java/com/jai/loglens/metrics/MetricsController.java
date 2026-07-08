package com.jai.loglens.metrics;

import com.jai.loglens.dto.MetricsSummary;
import com.jai.loglens.util.TimeParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/summary")
    public MetricsSummary summary(
            @RequestParam(defaultValue = "60") int minutes,
            @RequestParam(defaultValue = "30") int buckets,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        Instant now = Instant.now();
        Instant fromInstant = TimeParser.parseOrNull(from);
        Instant toInstant = TimeParser.parseOrNull(to);

        if (fromInstant == null) {
            fromInstant = now.minus(Duration.ofMinutes(Math.max(minutes, 1)));
        }
        if (toInstant == null) {
            toInstant = now;
        }

        return metricsService.summary(fromInstant, toInstant, buckets);
    }
}
