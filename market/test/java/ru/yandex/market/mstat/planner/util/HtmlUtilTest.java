package ru.yandex.market.mstat.planner.util;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HtmlUtilTest {

    @Test
    public void testSumColors() {
        assertEquals(ImmutableMap.of(1L, BigDecimal.valueOf(1.0)),
            HtmlUtil.rowSums(ImmutableMap.of(1L, ImmutableMap.of(
            "R", BigDecimal.valueOf(0.5),
            "W", BigDecimal.valueOf(0.5)
            ))));

    }
}
