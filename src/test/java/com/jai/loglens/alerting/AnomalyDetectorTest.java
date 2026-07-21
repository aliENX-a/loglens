package com.jai.loglens.alerting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectorTest {

    private final AnomalyDetector detector = new AnomalyDetector();

    @Test
    void computesMeanAndSampleStddev() {
        List<Double> values = List.of(10.0, 12.0, 11.0, 10.0, 13.0);

        assertThat(detector.mean(values)).isEqualTo(11.2);
        assertThat(detector.stddev(values)).isCloseTo(1.3038, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void spikeProducesHighZScore() {
        double z = detector.zscore(40.0, List.of(10.0, 12.0, 11.0, 10.0, 13.0));
        assertThat(z).isGreaterThan(2.0);
    }

    @Test
    void flatBaselineNeverFires() {
        assertThat(detector.zscore(500.0, List.of(5.0, 5.0, 5.0, 5.0, 5.0))).isZero();
    }

    @Test
    void tooLittleHistoryNeverFires() {
        assertThat(detector.zscore(100.0, List.of())).isZero();
        assertThat(detector.zscore(100.0, List.of(3.0))).isZero();
        assertThat(detector.zscore(100.0, List.of(3.0, 4.0))).isZero();
    }

    @Test
    void normalValueStaysBelowThreshold() {
        double z = detector.zscore(11.0, List.of(10.0, 12.0, 11.0, 10.0, 13.0));
        assertThat(Math.abs(z)).isLessThan(2.0);
    }
}
