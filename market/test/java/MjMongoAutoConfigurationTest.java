package ru.yandex.market.starter.mongo;

import com.mongodb.MongoClientOptions;
import de.flapdoodle.embed.mongo.MongodExecutable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import ru.yandex.market.starter.mongo.config.MjMongoAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.starter.mongo.processors.MongoTypeSelectorEnvironmentPostProcessor.MONGO_EMBEDDED_PROP_NAME;

public class MjMongoAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
                MongoAutoConfiguration.class, MjMongoAutoConfiguration.class
            )
        );

    @Test
    void networkMongoTest() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(MongoClientOptions.class);
                assertThat(context).doesNotHaveBean(MongodExecutable.class);
            });
    }

    @Test
    @Disabled
    void embeddedMongoTest() {
        contextRunner
            .withPropertyValues(MONGO_EMBEDDED_PROP_NAME + "=true")
            .withConfiguration(AutoConfigurations.of(EmbeddedMongoAutoConfiguration.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean(MongoClientOptions.class);
                assertThat(context).hasSingleBean(MongodExecutable.class);
            });
    }

}
