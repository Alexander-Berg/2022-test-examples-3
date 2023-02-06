package ru.yandex.market.marketpromo.core.application.properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(locations = "classpath:properties/logbroker-assortment-topic.properties")
public class LogbrokerAssortmentTopicPropertiesTest {

    @Autowired
    private LogbrokerAssortmentTopicProperties assortmentTopicProperties;

    @Test
    void propertiesShouldBeFilled() {
        assertThat(assortmentTopicProperties.getTopic(), is("assortment topic"));
        assertThat(assortmentTopicProperties.getConsumer(), is("assortment consumer"));
        assertThat(assortmentTopicProperties.getInstallationEndPoint(), is("write endpoint"));
        assertThat(assortmentTopicProperties.getClusterEndPoints(), hasItems("read endpoint 1", "read endpoint 2"));
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(LogbrokerAssortmentTopicProperties.class)
    public static class Config {

    }

}
