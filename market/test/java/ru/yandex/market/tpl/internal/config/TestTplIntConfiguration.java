package ru.yandex.market.tpl.internal.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.internal.service.manual",
})
public class TestTplIntConfiguration {
}
