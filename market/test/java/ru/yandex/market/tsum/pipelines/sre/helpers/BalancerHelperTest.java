package ru.yandex.market.tsum.pipelines.sre.helpers;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.sre.jobs.BalancerPipelineTestFactory;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;

public class BalancerHelperTest {

    private final BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfo();
    private final BalancerInfo balancerInfoFailed = BalancerPipelineTestFactory.getBalancerInfoFailed();

    public BalancerHelperTest() throws IOException {
    }

    @Test
    public void generateHaproxyConfigName() throws Exception {
        Assert.assertEquals(
            "12345-test-service-name.market_slb_search-testing.yaml",
            BalancerHelper.generateHaproxyConfigValues(balancerInfo)
        );
    }

    @Test(expected = IllegalStateException.class)
    public void generateHaproxyConfigNameFailed() throws Exception {
        Assert.assertEquals(
            "12345-test-service-name.market_slb_search-testing.yaml",
            BalancerHelper.generateHaproxyConfigValues(balancerInfoFailed)
        );
    }

    @Test
    public void generateNginxConfigName() throws Exception {
        Assert.assertEquals(
            "12345-test-service-name.market_slb_search-testing.yaml",
            BalancerHelper.generateNginxConfigValues(balancerInfo)
        );
    }

    @Test(expected = IllegalStateException.class)
    public void generateNginxConfigNameFailed() throws Exception {
        Assert.assertEquals(
            "12345-test-service-name.market_slb_search-testing.yaml",
            BalancerHelper.generateNginxConfigValues(balancerInfoFailed)
        );
    }

    @Test
    public void generateAnsibleJugglerConfigTest() {
        Assert.assertEquals(
            "12345.bp.test-service.name.tst.vs.market.yandex.net.yml",
            BalancerHelper.generateAnsibleJugglerConfigName(balancerInfo)
        );
    }
}
