package ru.yandex.market.jmf.background.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(BackgroundTestConfiguration.class)
public class InternalBackgroundTestConfiguration {
}
