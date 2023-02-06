package ru.yandex.market.partner.content.common.service;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.dao.ApplicationPropertyDao;

@ContextConfiguration(classes = {
        ApplicationPropertyServiceTest.TestConfig.class,
})
public class ApplicationPropertyServiceTest extends BaseDbCommonTest {

    @Autowired
    ApplicationPropertyService applicationPropertyService;

    @Test
    public void test() {
        Boolean resultBoolean = applicationPropertyService.getProperty("gutgin.test_boolean", Boolean.class);
        String resultString = applicationPropertyService.getProperty("gutgin.test_string", String.class);
        Integer resultInteger = applicationPropertyService.getProperty("gutgin.test_int", Integer.class);
        Double resultDouble = applicationPropertyService.getProperty("gutgin.test_double", Double.class);

        Assertions.assertThat(resultBoolean).isFalse();
        Assertions.assertThat(resultString).isEqualTo("string");
        Assertions.assertThat(resultInteger).isEqualTo(1);
        Assertions.assertThat(resultDouble).isEqualTo(1.0);

        applicationPropertyService.updateProperty("gutgin.test_boolean", true);
        applicationPropertyService.updateProperty("gutgin.test_string", "overridden_string");
        applicationPropertyService.updateProperty("gutgin.test_int", 2);
        applicationPropertyService.updateProperty("gutgin.test_double", 2.0);

        resultBoolean = applicationPropertyService.getProperty("gutgin.test_boolean", Boolean.class);
        resultString = applicationPropertyService.getProperty("gutgin.test_string", String.class);
        resultInteger = applicationPropertyService.getProperty("gutgin.test_int", Integer.class);
        resultDouble = applicationPropertyService.getProperty("gutgin.test_double", Double.class);

        Assertions.assertThat(resultBoolean).isTrue();
        Assertions.assertThat(resultString).isEqualTo("overridden_string");
        Assertions.assertThat(resultInteger).isEqualTo(2);
        Assertions.assertThat(resultDouble).isEqualTo(2.0);
    }

    @Configuration
    public static class TestConfig {

        @Autowired
        ResourceLoader resourceLoader;

        @Bean
        ApplicationPropertyService applicationPropertyService(ApplicationPropertyDao applicationPropertyDao) {
            return new ApplicationPropertyService(
                    () -> new Resource[]{resourceLoader.getResource("test.properties")},
                    applicationPropertyDao);
        }
    }
}
