package ru.yandex.direct.oneshot.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;

@Configuration
@Import({AppConfiguration.class, CoreTestingConfiguration.class})
public class OneshotTestingConfiguration {
}
