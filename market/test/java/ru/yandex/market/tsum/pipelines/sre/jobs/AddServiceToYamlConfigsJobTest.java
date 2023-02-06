package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.pipelines.sre.resources.CloneNannyServiceConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.YamlConfigs;
import ru.yandex.market.tsum.pipelines.sre.resources.YamlConfigsType;

@RunWith(Parameterized.class)
public class AddServiceToYamlConfigsJobTest {
    private final AddServiceToYamlConfigsJob job = new AddServiceToYamlConfigsJob();

    @Parameterized.Parameter
    public String nannyServiceName;

    @Parameterized.Parameter(1)
    public YamlConfigs yamlConfigs;

    @Parameterized.Parameter(2)
    public String expectedPath;

    @Parameterized.Parameters(name = "{index}: Service: {0}, Config Type: {1}, Path: \"{2}\"")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"testing_market_super_service_vla",
                YamlConfigs.MARKET_ALERTS_CONFIGS, "market/sre/conf/market-alerts-configs/configs"},
            {"testing_market_super_service_vla",
                YamlConfigs.SLB_HAPROXY, "market/sre/conf/slb-haproxy/test/etc/haproxy/values-available"},
            {"production_market_super_service_vla",
                YamlConfigs.SLB_HAPROXY, "market/sre/conf/slb-haproxy/prod/etc/haproxy/values-available"},
        });
    }

    @Before
    public void setup() {
        job.yamlConfigsType = new YamlConfigsType();
        job.yamlConfigsType.setYamlConfigs(yamlConfigs);
        job.cloneNannyServiceConfig = new CloneNannyServiceConfig();
        job.cloneNannyServiceConfig.setNannyServiceName(nannyServiceName);
    }

    @Test
    public void testGetConfigsPath() {
        Environment env = job.getEnv();
        Assert.assertEquals(expectedPath, job.yamlConfigsType.getYamlConfigs().getConfigInfo(env).getConfigsPath());
    }
}
