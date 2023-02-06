package ru.yandex.market.starter.mongo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.mock.env.MockEnvironment;

import ru.yandex.market.starter.mongo.processors.MongoTypeSelectorEnvironmentPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.starter.mongo.processors.MongoTypeSelectorEnvironmentPostProcessor.AUTOCONFIGURE_EXCLUDE_PROP_NAME;
import static ru.yandex.market.starter.mongo.processors.MongoTypeSelectorEnvironmentPostProcessor.MONGO_EMBEDDED_PROP_NAME;

public class MongoTypeSelectorTest {

    private final MongoTypeSelectorEnvironmentPostProcessor postProcessor =
        new MongoTypeSelectorEnvironmentPostProcessor();

    @Test
    void embeddedEnabledTest() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(MONGO_EMBEDDED_PROP_NAME, "true");
        postProcessor.postProcessEnvironment(environment, mock(SpringApplication.class));
        assertThat(environment.getProperty(AUTOCONFIGURE_EXCLUDE_PROP_NAME)).isNull();
    }

    @Test
    void embeddedEnabledWithOtherDisabledAutoConfigsTest() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(MONGO_EMBEDDED_PROP_NAME, "true");
        environment.setProperty(AUTOCONFIGURE_EXCLUDE_PROP_NAME, DataSourceAutoConfiguration.class.getName());
        postProcessor.postProcessEnvironment(environment, mock(SpringApplication.class));
        assertThat(environment.getProperty(AUTOCONFIGURE_EXCLUDE_PROP_NAME))
            .contains(DataSourceAutoConfiguration.class.getName())
            .doesNotContain(EmbeddedMongoAutoConfiguration.class.getName());
    }

    @Test
    void embeddedNotSpecifiedTest() {
        MockEnvironment environment = new MockEnvironment();
        postProcessor.postProcessEnvironment(environment, mock(SpringApplication.class));
        assertThat(environment.getProperty(AUTOCONFIGURE_EXCLUDE_PROP_NAME))
            .contains(EmbeddedMongoAutoConfiguration.class.getName());
    }

    @Test
    void embeddedNotSpecifiedWithOtherDisabledAutoConfigsTest() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(AUTOCONFIGURE_EXCLUDE_PROP_NAME, DataSourceAutoConfiguration.class.getName());
        postProcessor.postProcessEnvironment(environment, mock(SpringApplication.class));
        assertThat(environment.getProperty(AUTOCONFIGURE_EXCLUDE_PROP_NAME))
            .contains(DataSourceAutoConfiguration.class.getName())
            .contains(EmbeddedMongoAutoConfiguration.class.getName());
    }
}
