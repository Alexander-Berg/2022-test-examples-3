package ru.yandex.market.jmf.ui.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.http.metaclass.test.ModuleHttpMetaclassTestConfiguration;
import ru.yandex.market.jmf.module.relation.test.ModuleRelationTestConfiguration;
import ru.yandex.market.jmf.ui.UiConfiguration;

@Configuration
@Import({
        UiConfiguration.class,
        ModuleRelationTestConfiguration.class,
        ModuleHttpMetaclassTestConfiguration.class,
})
public class UiTestConfiguration {
}
