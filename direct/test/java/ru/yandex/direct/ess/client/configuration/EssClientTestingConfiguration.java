package ru.yandex.direct.ess.client.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.common.testing.CommonTestingConfiguration;
import ru.yandex.direct.ess.client.EssClientConfiguration;

@Configuration
@Import({EssClientConfiguration.class, CommonTestingConfiguration.class})
public class EssClientTestingConfiguration {

}
