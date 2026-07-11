package com.jai.loglens.alerting;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Compares the current window against a rolling baseline of earlier windows using a
 * z-score. Deliberately conservative: with too little history or a flat baseline there
 * is nothing to compare against, so it reports 0 instead of screaming.
 */
@Component
public class AnomalyDetector {

    private static final double EPSILON = 1e-9;

    public double mean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    public double stddev(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double m = mean(values);
        double acc = 0.0;
        for (double v : values) {
            double d = v - m;
            acc += d * d;
        }
        return Math.sqrt(acc / (values.size() - 1));
    }

    public double zscore(double current, List<Double> baseline) {
        if (baseline == null || baseline.size() < 3) {
            return 0.0;
        }
        double sd = stddev(baseline);
        if (sd < EPSILON) {
            return 0.0;
        }
        return (current - mean(baseline)) / sd;
    }
}
