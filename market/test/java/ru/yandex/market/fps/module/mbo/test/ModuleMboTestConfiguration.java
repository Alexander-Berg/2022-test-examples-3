package ru.yandex.market.fps.module.mbo.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.mbo.ModuleMboConfiguration;
import ru.yandex.market.jmf.utils.UtilsTestConfiguration;
import ru.yandex.market.mboc.http.MboMappingsService;

@Configuration
@Import({
        ModuleMboConfiguration.class,
        UtilsTestConfiguration.class,
})
public class ModuleMboTestConfiguration {
    @Bean
    public MboMappingsService mockMboMappingsService() {
        return Mockito.mock(MboMappingsService.class);
    }
}
