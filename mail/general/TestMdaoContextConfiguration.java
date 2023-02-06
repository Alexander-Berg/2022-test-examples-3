package ru.yandex.reminders.mongodb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.version.AppName;
import ru.yandex.reminders.boot.InitContextConfiguration;

/**
 * @author Eugene Voytitsky
 */
@Configuration
@Import({
        InitContextConfiguration.class,
        MongoDbContextConfiguration.class,
        MongoDaoContextConfiguration.class
})
public class TestMdaoContextConfiguration {

    @Bean
    public AppName appName() {
        return new AppName() {
            @Override
            public String serviceName() {
                return "reminders";
            }

            @Override
            public String appName() {
                return "unit-tests";
            }
        };
    }

    @Bean
    public EnvironmentType environmentType() {
        return EnvironmentType.TESTS;
    }
}
