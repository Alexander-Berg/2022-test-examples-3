package ru.yandex.market.jmf.module.ou.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.dataimport.test.DataImportTestConfiguration;
import ru.yandex.market.jmf.module.entity.snapshot.ModuleEntitySnapshotTestConfiguration;
import ru.yandex.market.jmf.module.ou.ModuleOuConfiguration;

@Configuration
@Import({
        ModuleOuConfiguration.class,
        ModuleEntitySnapshotTestConfiguration.class,
        DataImportTestConfiguration.class,
})
public class ModuleOuTestConfiguration {
}
