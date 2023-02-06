package ru.yandex.market.tsup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource(value = "classpath:application-local.properties", ignoreResourceNotFound = true)
@Configuration
public class LocalPropertiesConfig {

    // Класс нужен, чтобы подгружать application-local.properties
    // только при локальном старте и не требовать его наличия в CI
}
