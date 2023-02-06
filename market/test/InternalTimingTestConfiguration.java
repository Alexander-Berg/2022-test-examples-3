package ru.yandex.market.jmf.timings.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(TimingTestConfiguration.class)
public class InternalTimingTestConfiguration extends AbstractModuleConfiguration {
    public InternalTimingTestConfiguration() {
        super("timing/engine/test");
    }

}
