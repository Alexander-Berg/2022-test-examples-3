package ru.yandex.market.jmf.timings.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.timings.TimingConfiguration;
import ru.yandex.market.jmf.timings.geo.Geobase;

@Configuration
@Import({
        TimingConfiguration.class,
        ModuleDefaultTestConfiguration.class
})
@ComponentScan("ru.yandex.market.jmf.timings.test.impl")
public class TimingTestConfiguration {

    @Bean
    public Geobase geobase() {
        return Mockito.mock(Geobase.class);
    }
}
