package ru.yandex.market.fps.ticket.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.fps.module.axapta.test.ModuleAxaptaTestConfiguration;
import ru.yandex.market.fps.module.supplier1p.test.ModuleSupplier1pTestConfiguration;
import ru.yandex.market.fps.ticket.ModuleFpsTicketConfiguration;
import ru.yandex.market.jmf.module.ou.security.ModuleOuSecurityTestConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;

@Configuration
@Import({
        ModuleFpsTicketConfiguration.class,
        ModuleAxaptaTestConfiguration.class,
        ModuleTicketTestConfiguration.class,
        ModuleOuSecurityTestConfiguration.class,
        ModuleSupplier1pTestConfiguration.class,
})
@PropertySource(
        name = "testModuleFpsTicketProperties",
        value = "classpath:module/fps/ticket/test/module_fps_ticket.properties"
)
public class ModuleFpsTicketTestConfiguration {
}
