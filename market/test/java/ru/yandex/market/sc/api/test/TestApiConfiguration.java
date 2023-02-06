package ru.yandex.market.sc.api.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.sc.api.util.ScApiControllerCaller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Configuration
@ComponentScan(basePackages = "ru.yandex.market.sc.api.util.flow.xdoc")
public class TestApiConfiguration {

    /**
     * Корректное отображение кириллицы в ответах
     */
    @Bean
    public MockMvc getMockMvc(WebApplicationContext webApplicationContext) {
        return MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .addFilter((request, response, chain) -> {
                    response.setCharacterEncoding("UTF-8");
                    chain.doFilter(request, response);
                }, "/*")
                .build();
    }

    @Bean
    public ScApiControllerCaller getControllerCaller(MockMvc mockMvc) {
        return ScApiControllerCaller.createCaller(mockMvc);
    }
}
