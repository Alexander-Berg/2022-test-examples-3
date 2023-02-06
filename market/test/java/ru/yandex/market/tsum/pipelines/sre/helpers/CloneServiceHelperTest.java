package ru.yandex.market.tsum.pipelines.sre.helpers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.sre.resources.CloneNannyServiceConfig;

public class CloneServiceHelperTest {
    private CloneNannyServiceConfig config;

    @Before
    public void setConfig() {
        config = new CloneNannyServiceConfig();
        config.setNannyServiceName("testing_market_super_service_vla");
        config.setNewDc("SAS");
    }

    @Test
    public void TestGetNewNannyServiceName() {
        String expected = "testing_market_super_service_sas";
        String result = CloneServiceHelper.getNewNannyServiceName(config.getNannyServiceName(), config.getNewDc());
        Assert.assertEquals(expected, result);
    }

    @Test
    public void TestGetNewGenCfgGroupName() {
        String expected = "SAS_MARKET_TEST_SUPER_SERVICE";
        String result = CloneServiceHelper.getNewGenCfgGroupName(
            "VLA_MARKET_TEST_SUPER_SERVICE", config.getNewDc());
        Assert.assertEquals(expected, result);
    }

    @Test
    public void TestGetRtcServiceName() {
        String expected = "super_service";
        String result = CloneServiceHelper.getRtcServiceName(config.getNannyServiceName());
        Assert.assertEquals(expected, result);
        Assert.assertEquals(
            "report_market", CloneServiceHelper.getRtcServiceName("prod_report_market_man"));
    }
}
