package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;


public class GenerateAlertsConfiguratorConfigJobTest {
    @InjectMocks
    private GenerateAlertsConfiguratorConfigJob job = new GenerateAlertsConfiguratorConfigJob();

    @Mock
    private BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfo();

    public GenerateAlertsConfiguratorConfigJobTest() throws IOException {
    }

    @Test
    public void testGetPath() {
        String expected = "12345-test_service_name.yaml";
        String result = job.getPath(balancerInfo);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetBody() throws Exception {
        String templateName = "templates/marketAlertsConfigsTemplate.jinja";
        String expected = IOUtils.toString(this.getClass().getResourceAsStream(
            "/GenerateAlertsConfiguratorConfigJobTest/balancer_alerts_config.yml"), StandardCharsets.UTF_8.name());
        String result = job.getBody(templateName, balancerInfo);
        Assert.assertEquals(expected, result);
    }
}
