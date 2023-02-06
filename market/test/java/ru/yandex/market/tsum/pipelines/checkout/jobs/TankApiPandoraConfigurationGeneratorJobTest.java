package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.checkout.jobs.sandbox.MarketOrdersForecastSandboxResource;
import ru.yandex.market.tsum.pipelines.common.jobs.tank.TankApiJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxResource;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JobTesterConfig.class)
public class TankApiPandoraConfigurationGeneratorJobTest {

    private final SandboxResource sandboxResource = mockSandboxResource();
    private final MarketOrdersForecastSandboxResource ordersForecastResource = mockForecast();
    private final StartrekTicket startrekTicket = mockStartrekTicket();

    @Autowired
    private JobTester jobTester;

    private static SandboxResource mockSandboxResource() {
        SandboxResource sandboxResource = mock(SandboxResource.class);
        when(sandboxResource.getId()).thenReturn(1L);
        return sandboxResource;
    }

    private static MarketOrdersForecastSandboxResource mockForecast() {
        MarketOrdersForecastSandboxResource res = mock(MarketOrdersForecastSandboxResource.class);
        when(res.getForecast()).thenReturn(Collections.emptyList());
        when(res.getForecastMap()).thenReturn(Collections.emptyMap());
        return res;
    }

    private static StartrekTicket mockStartrekTicket() {
        StartrekTicket ticket = mock(StartrekTicket.class);
        when(ticket.getKey()).thenReturn("TEST-123456");
        return ticket;
    }

    @Test
    public void execute() throws Exception {

        TankApiPandoraConfigurationGeneratorJob job = jobTester
            .jobInstanceBuilder(TankApiPandoraConfigurationGeneratorJobSimplifiedConfig.class)
            .withResources(checkouterConfig(), tankConfig(), mutableConfig(), shootingConfig(), logisticsConfig(),
                generatorTemplateConfig(), jobConfig(), sandboxResource, loyaltyShootingOptions(),
                ordersForecastResource, startrekTicket)
            .create();
        TestJobContext context = new TestJobContext();
        job.execute(context);
        TankApiJobConfig tankApiJobConfig = context.resources().getProducedResource(TankApiJobConfig.class);
        Assert.assertTrue(tankApiJobConfig.getConfiguration().contains("stocksRequiredRate: 1.00"));
        Assert.assertTrue(tankApiJobConfig.getConfiguration().contains("totalOrders: 120"));
        ExpectedShootingStatisticsOrders expectedOrders =
            context.resources().getProducedResource(ExpectedShootingStatisticsOrders.class);
        Assert.assertEquals(new HashSet<>(Arrays.asList(
            new OrdersResource.DistributionCountTuple(Arrays.asList(1L), 20),
            new OrdersResource.DistributionCountTuple(Arrays.asList(2L), 20),
            new OrdersResource.DistributionCountTuple(Arrays.asList(1L, 1L), 20),
            new OrdersResource.DistributionCountTuple(Arrays.asList(2L, 2L), 20)
        )), expectedOrders.getOrders().getDistributionToCount());

    }


    private PandoraCheckouterConfig checkouterConfig() {
        PandoraCheckouterConfig config = mock(PandoraCheckouterConfigImpl.class);
        when(config.getCartsDistribution()).thenReturn("[{'internalCarts':1, 'ordersDistribution': 0.33}, " +
            "{'internalCarts':2, 'ordersDistribution': 0.66}]");
        when(config.getOffersDistribution()).thenReturn("[{'offersCount':1, 'ordersDistribution': 0.5}, " +
            "{'offersCount':2, 'ordersDistribution': 0.5}]");
        when(config.getBalancer()).thenReturn("checkouter");
        return config;
    }

    private PandoraTankConfig tankConfig() {
        PandoraTankConfig config = mock(PandoraTankConfigImpl.class);
        when(config.getTankBaseUrl()).thenReturn("http://tiger01h.market.yandex.net:8083");
        return config;
    }

    private PandoraMutableConfig mutableConfig() {
        PandoraMutableConfig config = mock(PandoraMutableConfigImpl.class);
        when(config.getDuration()).thenReturn(120);
        when(config.getOrdersPerHour()).thenReturn(3600);
        when(config.getStocksRequiredRate()).thenReturn("1.0");
        return config;
    }

    private PandoraShootingConfig shootingConfig() {
        PandoraShootingConfig config = mock(PandoraShootingConfigImpl.class);
        return config;
    }

   private PandoraRegionSpecificConfig logisticsConfig() {
        PandoraRegionSpecificConfig config = mock(PandoraRegionSpecificConfigImpl.class);
        when(config.getDeliveryServices()).thenReturn("");
        when(config.getDeliveryType()).thenReturn("");
        when(config.getFlashShopPromoID()).thenReturn("");
        when(config.getWarehouseId()).thenReturn("154");
        when(config.getRegionId()).thenReturn("213");
        return config;
    }

    private PandoraGeneratorTemplateConfig generatorTemplateConfig() {
        PandoraGeneratorTemplateConfig templateConfig = mock(PandoraGeneratorTemplateConfig.class);
        when(templateConfig.getTemplate()).thenReturn(PandoraGeneratorTemplateConfig.DEFAULT_TEMPLATE);
        return templateConfig;
    }

    private TankApiPandoraConfigurationGeneratorJobConfig jobConfig() {
        TankApiPandoraConfigurationGeneratorJobConfig jobConfig =
            mock(TankApiPandoraConfigurationGeneratorJobConfig.class);
        when(jobConfig.getCheckouterBaseUrl()).thenReturn("");
        when(jobConfig.getEnvironment()).thenReturn("");
        return jobConfig;
    }

    private LoyaltyShootingOptions loyaltyShootingOptions() {
        return mock(LoyaltyShootingOptions.class);
    }
}
