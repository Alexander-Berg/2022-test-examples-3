package ru.yandex.market.fps.ticket.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleFpsTicketTestConfiguration.class)
@PropertySource(
        name = "testModuleFpsTicketProperties",
        value = "classpath:module/fps/ticket/test/module_fps_ticket.properties"
)
public class InternalModuleFpsTicketTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleFpsTicketTestConfiguration() {
        super("module/fps/ticket/test");
    }

}
