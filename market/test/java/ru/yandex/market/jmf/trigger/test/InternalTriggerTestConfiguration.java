package ru.yandex.market.jmf.trigger.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.market.jmf.entity.AccessorInitializer;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.entity.EntityInstanceStrategy;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(TriggerTestConfiguration.class)
public class InternalTriggerTestConfiguration extends AbstractModuleConfiguration {
    public InternalTriggerTestConfiguration() {
        super("module/trigger/test");
    }

    @Bean
    public AccessorInitializer transientEntityAccessorInitializer(@Lazy AttributeTypeService attributeTypeService) {
        return new TransientEntityAccessorInitializer(attributeTypeService);
    }

    @Bean
    public EntityInstanceStrategy transientEntityInstanceStrategy(@Lazy EntityAdapterService entityAdapterService) {
        return new TransientEntityInstanceStrategy(entityAdapterService);
    }
}
