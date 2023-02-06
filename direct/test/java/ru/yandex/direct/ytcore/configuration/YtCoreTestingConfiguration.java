package ru.yandex.direct.ytcore.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.communication.config.CommunicationTestingConfiguration;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.ytcore.spring.YtCoreConfiguration;

/**
 *
 */
@Configuration
@Import({YtCoreConfiguration.class, CoreTestingConfiguration.class, CommunicationTestingConfiguration.class})
@ComponentScan(
        basePackages = "ru.yandex.direct.ytcore",
        excludeFilters = {
                @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION),
        }
)
public class YtCoreTestingConfiguration {
}



