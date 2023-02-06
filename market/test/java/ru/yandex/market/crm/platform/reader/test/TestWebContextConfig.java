package ru.yandex.market.crm.platform.reader.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.crm.platform.http.WebContextConfig;

/**
 * @author apershukov
 */
@Configuration
@ComponentScan("ru.yandex.market.crm.platform.reader.http.controllers")
@Import(WebContextConfig.class)
public class TestWebContextConfig {

    @Bean
    public MockMvc mockMvc(WebApplicationContext webApplicationContext) {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
}
