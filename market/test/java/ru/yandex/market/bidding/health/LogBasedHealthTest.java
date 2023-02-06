package ru.yandex.market.bidding.health;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogBasedHealthTest {
    @Test
    public void test2xx() {
        LogBasedHealth health = mock(LogBasedHealth.class);
        when(health.is2xx(anyString())).thenCallRealMethod();

        assertFalse(health.is2xx(null));
        assertFalse(health.is2xx(""));
        assertFalse(health.is2xx("20"));
        assertFalse(health.is2xx("2000"));
        assertFalse(health.is2xx("301"));
        assertFalse(health.is2xx("402"));
        assertFalse(health.is2xx("100"));
        assertFalse(health.is2xx("500"));
        assertFalse(health.is2xx("500"));
        assertFalse(health.is2xx("2ab"));
        assertFalse(health.is2xx("2xx"));

        assertTrue(health.is2xx("200"));
        assertTrue(health.is2xx("201"));
    }
}