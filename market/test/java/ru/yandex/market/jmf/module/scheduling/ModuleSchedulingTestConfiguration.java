package ru.yandex.market.jmf.module.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;

@Configuration
@Import({
        ModuleSchedulingConfiguration.class,
        ModuleDefaultTestConfiguration.class,
})
public class ModuleSchedulingTestConfiguration {
}
