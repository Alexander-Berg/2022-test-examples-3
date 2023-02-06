package ru.yandex.market.checkout.checkouter.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.market.checkout.checkouter.config.web.ErrorsConfig;
import ru.yandex.market.checkout.checkouter.config.web.ViewsConfig;

@Import({ViewsConfig.class, ErrorsConfig.class})
public abstract class AbstractControllerContext {

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
