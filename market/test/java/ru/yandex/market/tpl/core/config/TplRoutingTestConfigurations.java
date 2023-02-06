package ru.yandex.market.tpl.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.MvrpRequestMapper;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.HardcodedVrpSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.VrpSettingsProvider;

import static org.mockito.Mockito.mock;

@Configuration
@ComponentScan(basePackageClasses = MvrpRequestMapper.class)
public class TplRoutingTestConfigurations {
    @Bean
    public VrpSettingsProvider vrpSettingsProvider() {
        return new HardcodedVrpSettingsProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public VrpClient vrpClient() {
        VrpClient mock = mock(VrpClient.class);
        return mock;
    }
}
