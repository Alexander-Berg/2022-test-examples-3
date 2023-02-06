package ru.yandex.market.ir.classifier.data.confidence;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ConfidencePoolTest {

    @Test
    public void parseFromStream() {
        List<String> statistics = Arrays.asList(
            "-10.0\t1.0\t0.746\t1.0",
            "-0.5\t0.75\t0.8035555555555556\t0.8078641644325291",
            "0.0\t0.5\t0.8786666666666667\t0.5889186773905273",
            "50.5\t0.25\t0.9413333333333334\t0.3154602323503128"
        );
        ConfidencePool pool = ConfidencePool.parseFromStream(statistics.stream());
        assertEquals(pool.findBoundWithMinimalCoverage(0.7).getThreshold(), -0.5, 1e-9);
    }

    @Test
    public void findTopPrecisionTest1() {
        List<String> statistics = Arrays.asList(
            "-10.0\t1.0\t0.746\t1.0",
            "-0.5\t0.75\t0.8035555555555556\t0.8078641644325291",
            "0.0\t0.5\t0.8786666666666667\t0.5889186773905273",
            "50.5\t0.25\t0.9413333333333334\t0.3154602323503128",
            "55.5\t0.20\t0.9513333333333334\t0.2154602323503128"
        );
        ConfidencePool pool = ConfidencePool.parseFromStream(statistics.stream());
        assertEquals(pool.findSmoothedTopPrecisionByScore(1.0), 0.8786666666666667, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByScore(0.0), 0.8786666666666667, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByScore(100.0), 0.9513333333333334, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByScore(-1000), 0.746, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByScore(-0.3), 0.8035555555555556, 1e-9);
    }

    @Test
    public void findTopPrecisionTest2() {
        List<String> statistics = Arrays.asList(
            "-10.0\t1.0\t0.746\t1.0",
            "-0.5\t0.75\t0.8035555555555556\t0.8078641644325291",
            "0.0\t0.5\t0.8786666666666667\t0.5889186773905273",
            "50.5\t0.25\t0.9413333333333334\t0.3154602323503128",
            "55.5\t0.20\t0.9513333333333334\t0.2154602323503128"
        );
        ConfidencePool pool = ConfidencePool.parseFromStream(statistics.stream());
        assertEquals(pool.findSmoothedTopPrecisionByProbability(sigm(1.0)), 0.8786666666666667, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByProbability(sigm(0.0)), 0.8786666666666667, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByProbability(sigm(100.0)), 0.9513333333333334, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByProbability(sigm(-1000)), 0.746, 1e-9);
        assertEquals(pool.findSmoothedTopPrecisionByProbability(sigm(-0.3)), 0.8035555555555556, 1e-9);
    }

    private static double sigm(double x) {
        return 1 / (1.0 + Math.exp(-x));
    }
}