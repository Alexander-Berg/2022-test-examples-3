package ru.yandex.market.jmf.script.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.metainfo.test.MetaInfoTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ScriptStorageConfiguration.class,
        MetaInfoTestConfiguration.class,
})
public class ScriptStorageTestConfiguration extends AbstractModuleConfiguration {
    protected ScriptStorageTestConfiguration() {
        super("script/storage/test");
    }
}
