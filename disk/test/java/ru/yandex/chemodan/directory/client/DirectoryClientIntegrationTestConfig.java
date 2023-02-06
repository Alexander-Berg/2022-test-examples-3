package ru.yandex.chemodan.directory.client;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.misc.spring.ServicesStarter;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

@Import(
        DirectoryConfig.class
)
public class DirectoryClientIntegrationTestConfig extends ChemodanTestBaseContextConfiguration {
    @Override
    protected AppName appName() {
        return new SimpleAppName("disk", "ps-billing-worker");
    }

    @Bean
    public Starter starter(ServicesStarter servicesStarter) {
        return new Starter(servicesStarter);
    }

    @RequiredArgsConstructor
    public static class Starter {
        private final ServicesStarter servicesStarter;
        @PostConstruct
        public void setup() {
            servicesStarter.start();
        }

        @PreDestroy
        public void teardown() {
            servicesStarter.stop();
        }
    }
}
