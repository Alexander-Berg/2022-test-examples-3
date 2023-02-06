package ru.yandex.market.jmf.metadata.test.unique_group;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.metadata.impl.MetadataInitializer;
import ru.yandex.market.jmf.metainfo.ReloadAttributes;
import ru.yandex.market.jmf.metainfo.ReloadType;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UniqueAttributeGroupTest.Configuration.class)
public class UniqueAttributeGroupTest {

    @Inject
    MetadataInitializer initializer;

    @Test
    public void check() {
        initializer.build(null, ReloadType.INSTANCE_MODIFICATION, ReloadAttributes.EMPTY);
    }

    @org.springframework.context.annotation.Configuration
    static class Configuration extends UniqueAttributeGroupConfiguration {
        public Configuration() {
            super("classpath:unique_group/unique_group_metadata.xml");
        }
    }
}
