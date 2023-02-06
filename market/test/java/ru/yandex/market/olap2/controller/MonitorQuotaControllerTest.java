package ru.yandex.market.olap2.controller;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MonitorQuotaControllerTest {
    @Test
    public void testCheckQuota() throws Exception {
        long size10GB = 10L * 1024 * 1024 * 1024;
        assertTrue(MonitorQuotaController.checkQuota(size10GB).startsWith(JugglerConstants.OK));

        long size800GB = 800L * 1024 * 1024 * 1024;
        assertTrue(MonitorQuotaController.checkQuota(size800GB).startsWith(JugglerConstants.WARN));

        long size2TB = 2L * 1024 * 1024 * 1024 * 1024;
        assertTrue(MonitorQuotaController.checkQuota(size2TB).startsWith(JugglerConstants.CRIT));
    }


    @Test
    public void testPercent() throws Exception {
        assertThat(MonitorQuotaController.percent(0.712345), is("71%"));
        assertThat(MonitorQuotaController.percent(0.001), is("0%"));
        assertThat(MonitorQuotaController.percent(1.0), is("100%"));
    }
}
