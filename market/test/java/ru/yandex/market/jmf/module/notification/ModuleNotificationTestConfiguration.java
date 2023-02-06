package ru.yandex.market.jmf.module.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.xiva.ModuleXivaTestConfiguration;

@Configuration
@Import({
        ModuleNotificationConfiguration.class,
        ModuleXivaTestConfiguration.class
})
public class ModuleNotificationTestConfiguration {
}
