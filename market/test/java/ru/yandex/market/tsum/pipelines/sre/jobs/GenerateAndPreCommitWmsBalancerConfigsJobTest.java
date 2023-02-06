package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.sre.resources.BalancerEnvironment;
import ru.yandex.market.tsum.pipelines.sre.resources.WmsBalancerResource;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;

import static org.junit.Assert.assertEquals;

public class GenerateAndPreCommitWmsBalancerConfigsJobTest {
    private final GenerateAndPreCommitWmsBalancerConfigsJob job = new GenerateAndPreCommitWmsBalancerConfigsJob();

    private final BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoBuilder()
        .withFqdn("wms-ekb.vs.market.yandex.net")
        .withHttpPort(80)
        .withHttpsPort(443)
        .withRedirectToHttps(false)
        .withRealPort(80)
        .build();

    private final WmsBalancerResource.Builder wmsBalancerResourceBuilder = WmsBalancerResource.builder()
        .withPing(balancerInfo.getHealthCheckUrl())
        .withRealPort(0)
        .withHaproxyPort(8080)
        .withLocationKey("ekb")
        .withLocationHumanName("Екатеринбург")
        .withConductorGroup("market_wms-app-stable-dc-ekb")
        .withL3Balancer(balancerInfo.getFqdn());

    private void assertTemplate(String resultConfigPath, String config) throws IOException {
        assertEquals(
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(resultConfigPath)), StandardCharsets.UTF_8
            ),
            config
        );
    }

    @Test
    public void testGenerateConfigsWms() throws IOException {
        WmsBalancerResource wmsBalancerResource = wmsBalancerResourceBuilder
            .withName("wms-ekb")
            .withWebsockets(true)
            .withActivePassive(true)
            .withFqdn(balancerInfo.getFqdn())
            .withEnvironment(BalancerEnvironment.TESTING)
            .build();
        GenerateAndPreCommitWmsBalancerConfigsJob.WmsBalancerConfigs wmsBalancerConfigs = job.generateConfigs(
            balancerInfo, wmsBalancerResource, 1);
        Assert.assertEquals(
            "test/123451-wms-ekb.market_slb_search-testing.yaml",
            wmsBalancerConfigs.getName()
        );
        assertTemplate(
            "/GenerateAndCommitWmsBalancerConfigsJobTest/testGenerateConfigsWmsHaproxy.yaml",
            wmsBalancerConfigs.getHaproxyConfig()
        );
        assertTemplate(
            "/GenerateAndCommitWmsBalancerConfigsJobTest/testGenerateConfigsWmsNginx.yaml",
            wmsBalancerConfigs.getNginxConfig()
        );
    }

    @Test
    public void testGenerateConfigsApi() throws IOException {
        WmsBalancerResource wmsBalancerResource = wmsBalancerResourceBuilder
            .withName("api.wms-ekb")
            .withWebsockets(false)
            .withActivePassive(false)
            .withFqdn("api." + balancerInfo.getFqdn())
            .withAlternativeName("api.wms-ekat.vs.market.yandex.net")
            .withEnvironment(BalancerEnvironment.PRODUCTION)
            .build();
        GenerateAndPreCommitWmsBalancerConfigsJob.WmsBalancerConfigs wmsBalancerConfigs = job.generateConfigs(
            balancerInfo, wmsBalancerResource, 2);
        Assert.assertEquals(
            "prod/123452-api.wms-ekb.market_slb_search-testing.yaml",
            wmsBalancerConfigs.getName()
        );
        assertTemplate(
            "/GenerateAndCommitWmsBalancerConfigsJobTest/testGenerateConfigsApiHaproxy.yaml",
            wmsBalancerConfigs.getHaproxyConfig()
        );
        assertTemplate(
            "/GenerateAndCommitWmsBalancerConfigsJobTest/testGenerateConfigsApiNginx.yaml",
            wmsBalancerConfigs.getNginxConfig()
        );
    }
}
