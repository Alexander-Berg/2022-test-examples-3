package ru.yandex.market.checkout.checkouter.test.config.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import ru.yandex.market.checkout.checkouter.config.SwaggerConfiguration;
import ru.yandex.market.checkout.checkouter.mocks.MockMvcFactory;
import ru.yandex.market.checkout.checkouter.test.config.services.IntTestCommonConfig;
import ru.yandex.market.checkout.checkouter.web.ErrorHandlingTestController;
import ru.yandex.market.checkout.common.WebTestHelper;

@Import({IntTestCommonConfig.class, SwaggerConfiguration.class})
@ConditionalOnWebApplication
@ComponentScan(
        value = "ru.yandex.market.checkout.helpers",
        includeFilters = {
                @ComponentScan.Filter(WebTestHelper.class)
        }
)
@Configuration
public class IntTestWebConfig {

    @Bean
    public MockMvcFactory mockMvcFactory() {
        return new MockMvcFactory();
    }

    @Bean
    public MockMvc mockMvc() {
        return mockMvcFactory().getMockMvc();
    }

    @Bean
    public ErrorHandlingTestController errorHandlingTestController() {
        return new ErrorHandlingTestController();
    }

    @Bean
    public FixedLocaleResolver localeResolver() {
        return new FixedLocaleResolver();
    }
}
