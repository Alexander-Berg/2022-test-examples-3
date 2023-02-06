package ru.yandex.direct.logicprocessor.processors.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.logicprocessor.configuration.EssLogicProcessorConfiguration;

@Configuration
@Import({EssLogicProcessorConfiguration.class, CoreTestingConfiguration.class})
public class EssLogicProcessorTestConfiguration {
}
