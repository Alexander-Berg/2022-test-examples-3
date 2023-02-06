package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerEnvironment;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerPipelineConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.WmsBalancerLocationConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.WmsBalancerLocationsConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.WmsBalancerServiceConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.WmsBalancerServicesConfig;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WmsBalancerConfigParserTest {
    private final TestJobContext context = new TestJobContext();

    @Spy
    private StartrekTicket startrekTicket = new StartrekTicket("TEST-1");

    @Spy
    @InjectMocks
    private WmsBalancerConfigParser job = new WmsBalancerConfigParser();

    private final WmsBalancerServicesConfig servicesConfig = new WmsBalancerServicesConfig(Arrays.asList(
        WmsBalancerServiceConfig.builder()
            .withName("")
            .withPing("/ping")
            .withHaproxyPort(8080)
            .build(),
        WmsBalancerServiceConfig.builder()
            .withName("api")
            .withPing("/ping")
            .withHaproxyPort(8081)
            .build(),
        WmsBalancerServiceConfig.builder()
            .withName("core")
            .withPing("/ping")
            .withHaproxyPort(8082)
            .build(),
        WmsBalancerServiceConfig.builder()
            .withName("picking")
            .withPing("/ping")
            .withHaproxyPort(8083)
            .build()
    ));

    private final WmsBalancerLocationsConfig locationsConfig = new WmsBalancerLocationsConfig(Arrays.asList(
        WmsBalancerLocationConfig.builder()
            .withKey("ekb")
            .build(),
        WmsBalancerLocationConfig.builder()
            .withKey("nsk")
            .build(),
        WmsBalancerLocationConfig.builder()
            .withKey("dc-tom")
            .withAlternativeDomain("wms-tom.vs.market.yandex.net")
            .build()
    ));

    private final BalancerPipelineConfig balancerPipelineConfig = new BalancerPipelineConfig("TESTING", "TEST-1");

    public WmsBalancerConfigParserTest() {
        balancerPipelineConfig.setBalancerFlavour("MSLB");
        balancerPipelineConfig.setBalancerType("Внутренний");
        balancerPipelineConfig.setIpVersion("IPv6-only");
        balancerPipelineConfig.setHttpPort("80");
        balancerPipelineConfig.setHttpsPort("443");
        balancerPipelineConfig.setRedirectToHttps("Нет");
        balancerPipelineConfig.setSslBackends("Нет");
        balancerPipelineConfig.setSslExternalCa("Нет");
        balancerPipelineConfig.setTypeOfBackends("CONDUCTOR_GROUP");
        balancerPipelineConfig.setHealthCheckType("200-ый код ответа");
        balancerPipelineConfig.setJugglerMonitor("Нет");
        job.setBalancerPipelineConfig(balancerPipelineConfig);
        job.setWmsBalancerServicesConfig(servicesConfig);
        job.setWmsBalancerLocationsConfig(locationsConfig);
    }

    @Test
    public void testGetWmsBalancerResources() {
        Assert.assertEquals(
            servicesConfig.getServices().size() * locationsConfig.getLocations().size(),
            job.getWmsBalancerResources(locationsConfig, servicesConfig).size()
        );
    }

    @Test
    public void testCalculateServiceName() {
        Assert.assertEquals(
            "wms-ekb",
            job.calculateServiceName(null, "ekb")
        );
        Assert.assertEquals(
            "wms-ekb",
            job.calculateServiceName("", "ekb")
        );
        Assert.assertEquals(
            "api.wms-ekb",
            job.calculateServiceName("api", "ekb")
        );
    }

    @Test
    public void testCalculateDomainName() {
        Assert.assertEquals(
            ".tst.vs.market.yandex.net",
            job.calculateServiceDomain(BalancerEnvironment.TESTING)
        );
        Assert.assertEquals(
            ".vs.market.yandex.net",
            job.calculateServiceDomain(BalancerEnvironment.PRODUCTION)
        );
        Assert.assertEquals(
            ".vs.market.yandex.net",
            job.calculateServiceDomain(BalancerEnvironment.PRESTABLE)
        );
    }

    @Ignore("Integration test")
    @Test
    public void testJob() throws Exception {
        doNothing().when(job).commentTicket(anyString());

        List<WmsBalancerLocationConfig> locations = new ArrayList<>(locationsConfig.getLocations());
        WmsBalancerLocationsConfig newLocationsConfig = new WmsBalancerLocationsConfig(locations);
        newLocationsConfig.addLocation(WmsBalancerLocationConfig.builder()
            .withKey("new")
            .withAlternativeDomain("wms-alt.vs.market.yandex.net")
            .withEnvironment(BalancerEnvironment.PRODUCTION)
            .build());
        job.setWmsBalancerLocationsConfig(newLocationsConfig);

        job.execute(context);
        List<Resource> producedResources = context.getProducedResourcesList();
        Assert.assertEquals(
            servicesConfig.getServices().size() + 1,
            producedResources.size()
        );
    }
}
