package ru.yandex.direct.screenshooter.client.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ScreenShooterConfiguration.class})
public class ScreenShooterTestingConfiguration {
}
