package ru.yandex.market.pers.service.common.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributionUtilsTest {

    @Test
    public void testDistribution() {
        Map<Long, BigDecimal> source = Map.of(2L, BigDecimal.valueOf(15.3846), 3L, BigDecimal.valueOf(38.4615), 5L, BigDecimal.valueOf(46.1538));
        checkSum(DistributionUtils.makeDistribution(source).values());
    }

    @Test
    public void testDistribution2() {
        Map<Long, BigDecimal> source = Map.of(2L, BigDecimal.valueOf(24.4), 3L, BigDecimal.valueOf(24.3), 5L, BigDecimal.valueOf(24.4), 6L, BigDecimal.valueOf(26.9));
        checkSum(DistributionUtils.makeDistribution(source).values());
    }

    @Test
    public void testDistribution3() {
        Map<Long, BigDecimal> source = new HashMap<>();
        for (long i = 0; i < 50; i++) {
            source.put(i, BigDecimal.valueOf(1.1));
        }
        source.put(51L, BigDecimal.valueOf(45));
        checkSum(DistributionUtils.makeDistribution(source).values());
    }

    private void checkSum(Collection<Long> values) {
        assertEquals(100, values.stream().mapToLong(e -> e).sum());
    }
}
