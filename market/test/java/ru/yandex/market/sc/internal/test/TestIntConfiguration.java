package ru.yandex.market.sc.internal.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

/**
 * @author valter
 */
@Configuration
@ComponentScan(basePackages = "ru.yandex.market.sc.internal.util.flow.xdoc")
public class TestIntConfiguration {

    @Bean
    LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public ScIntControllerCaller getControllerCaller(MockMvc mockMvc) {
        return ScIntControllerCaller.createCaller(mockMvc);
    }

    @Bean
    public CargoUnitTestFactory cargoUnitTestFactory() {
        return new CargoUnitTestFactory();
    }

    @Bean
    public WwClient wwClient() {
        return Mockito.mock(WwClient.class, Mockito.RETURNS_DEEP_STUBS);
    }

}
