package ru.yandex.market.tsum.pipelines.sre.resources;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;

public class ConfigForGenerateAlertsConfiguratorConfigTest {
    @InjectMocks
    private ConfigForGenerateAlertsConfiguratorConfig configJob = new ConfigForGenerateAlertsConfiguratorConfig();

    @Test
    public void testTemplateName() {
        String expected = "test_name";
        configJob.setTemplateName(expected);
        String result = configJob.getTemplateName();
        Assert.assertEquals(expected, result);
    }
}
