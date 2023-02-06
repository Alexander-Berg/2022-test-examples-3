package ru.yandex.market.jmf.module.entity.snapshot;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.logic.wf.test.LogicWfTestConfiguration;

@Import({
        ModuleEntitySnapshotConfiguration.class,
        LogicWfTestConfiguration.class
})
@Configuration
public class ModuleEntitySnapshotTestConfiguration {
}
