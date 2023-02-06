package ru.yandex.market.psku.postprocessor.service.deduplication;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.daos.TaskPropertiesDao;

@ContextConfiguration(classes = {
        TaskPropertiesServiceTest.Config.class,
})
public class TaskPropertiesServiceTest extends BaseDBTest {

    @Autowired
    public TaskPropertiesService taskPropertiesService;

    @Before
    public void setUp() throws Exception {
        System.setProperty("configs.path",
                getClass().getClassLoader().getResource("task_properties_service_test.properties").getFile());
    }

    @Ignore
    @Test
    public void test() {
        Boolean resultBoolean = taskPropertiesService.getProperty("ppp.test_boolean", Boolean.class);
        String resultString = taskPropertiesService.getProperty("ppp.test_string", String.class);
        Integer resultInteger = taskPropertiesService.getProperty("ppp.test_int", Integer.class);
        Double resultDouble = taskPropertiesService.getProperty("ppp.test_double", Double.class);

        Assertions.assertThat(resultBoolean).isFalse();
        Assertions.assertThat(resultString).isEqualTo("string");
        Assertions.assertThat(resultInteger).isEqualTo(1);
        Assertions.assertThat(resultDouble).isEqualTo(1.0);

        taskPropertiesService.updateProperty("ppp.test_boolean", true);
        taskPropertiesService.updateProperty("ppp.test_string", "overridden_string");
        taskPropertiesService.updateProperty("ppp.test_int", 2);
        taskPropertiesService.updateProperty("ppp.test_double", 2.0);

        resultBoolean = taskPropertiesService.getProperty("ppp.test_boolean", Boolean.class);
        resultString = taskPropertiesService.getProperty("ppp.test_string", String.class);
        resultInteger = taskPropertiesService.getProperty("ppp.test_int", Integer.class);
        resultDouble = taskPropertiesService.getProperty("ppp.test_double", Double.class);

        Assertions.assertThat(resultBoolean).isTrue();
        Assertions.assertThat(resultString).isEqualTo("overridden_string");
        Assertions.assertThat(resultInteger).isEqualTo(2);
        Assertions.assertThat(resultDouble).isEqualTo(2.0);
    }

    @Configuration
    static class Config {
        @Bean
        TaskPropertiesService taskPropertiesService(TaskPropertiesDao taskPropertiesDao) {
            return new TaskPropertiesService(taskPropertiesDao);
        }
    }
}
