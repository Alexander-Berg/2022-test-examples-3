package ru.yandex.market.gutgin.tms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration("taskConfiguration")
@Import({
    TestServiceConfig.class
})
public class TestTaskConfig {

}
