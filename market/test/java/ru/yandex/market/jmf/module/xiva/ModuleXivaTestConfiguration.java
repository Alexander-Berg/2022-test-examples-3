package ru.yandex.market.jmf.module.xiva;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;

@Configuration
@Import({
        ModuleXivaConfiguration.class,
        ModuleOuTestConfiguration.class,
})
public class ModuleXivaTestConfiguration {

    @Bean
    @Primary
    public XivaPersonalClient testXivaClient() {
        return Mockito.mock(XivaPersonalClient.class);
    }

    @Bean
    @Primary
    public XivaTopicClient testXivaTopicClient() {
        return Mockito.mock(XivaTopicClient.class);
    }
}
