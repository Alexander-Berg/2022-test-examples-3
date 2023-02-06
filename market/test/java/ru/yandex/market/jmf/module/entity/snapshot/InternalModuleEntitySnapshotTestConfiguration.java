package ru.yandex.market.jmf.module.entity.snapshot;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Import({ModuleEntitySnapshotTestConfiguration.class})
@Configuration
public class InternalModuleEntitySnapshotTestConfiguration extends AbstractModuleConfiguration {

    protected InternalModuleEntitySnapshotTestConfiguration() {
        super("test/module/entity/snapshot");
    }
}
