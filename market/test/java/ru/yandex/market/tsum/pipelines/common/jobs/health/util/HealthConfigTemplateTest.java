package ru.yandex.market.tsum.pipelines.common.jobs.health.util;

import org.junit.Assert;
import org.junit.Test;


public class HealthConfigTemplateTest {

    @Test
    public void escapeInvalidCharactersInApplicationName_escapeHyphens() {
        HealthConfigTemplate template = new HealthConfigTemplate("someTemplate");
        String applicationName = "my-beautiful-service";
        String validApplicationName = template.createValidTableName(applicationName);
        Assert.assertEquals("my_beautiful_service", validApplicationName);
    }

}
