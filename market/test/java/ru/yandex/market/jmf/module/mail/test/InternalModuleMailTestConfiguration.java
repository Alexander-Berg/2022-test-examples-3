package ru.yandex.market.jmf.module.mail.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleMailTestConfiguration.class)
public class InternalModuleMailTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleMailTestConfiguration() {
        super("module/mail/test");
    }
}
