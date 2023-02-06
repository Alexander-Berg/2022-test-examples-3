package ru.yandex.market.jmf.metadata.test.unique_group;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.metadata.ConfigurationError;
import ru.yandex.market.jmf.metadata.impl.MetadataInitializer;
import ru.yandex.market.jmf.metainfo.ReloadAttributes;
import ru.yandex.market.jmf.metainfo.ReloadType;

public abstract class AbstractConfigurationErrorTest {

    @Inject
    MetadataInitializer initializer;

    @Test
    public void check() {
        Assertions.assertThrows(ConfigurationError.class, () -> initializer.build(null,
                ReloadType.INSTANCE_MODIFICATION, ReloadAttributes.EMPTY));
    }
}
