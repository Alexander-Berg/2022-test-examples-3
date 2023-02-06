package ru.yandex.direct.bsexport.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;

@Configuration
@Import({BsExportConfiguration.class, CoreTestingConfiguration.class})
public class BsExportTestingConfiguration {
}
