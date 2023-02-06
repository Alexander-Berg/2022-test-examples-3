package ru.yandex.market.tpl.dora.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tpl.dora.test.factory.TestCourseFactory;
import ru.yandex.market.tpl.dora.test.factory.TestPlatformFactory;

@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.dora.test.factory"
})
@Configuration
public class TestInternalConfiguration {

    @Bean
    TestCourseFactory testCourseFactory() {
        return new TestCourseFactory();
    }

    @Bean
    TestPlatformFactory testPlatformFactory() {
        return new TestPlatformFactory();
    }

}
