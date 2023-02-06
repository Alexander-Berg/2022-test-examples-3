package ru.yandex.market.crm.platform.api.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.crm.platform.http.WebContextConfig;

/**
 * @author apershukov
 */
@Configuration
@ComponentScan("ru.yandex.market.crm.platform.api.http.controllers")
@Import(WebContextConfig.class)
@EnableWebMvc
public class TestWebContextConfig {
}
