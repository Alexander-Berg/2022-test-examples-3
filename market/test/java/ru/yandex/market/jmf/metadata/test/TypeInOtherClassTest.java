package ru.yandex.market.jmf.metadata.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.metadata.ConfigurationError;
import ru.yandex.market.jmf.metadata.impl.MetadataInitializer;
import ru.yandex.market.jmf.metainfo.ReloadAttributes;
import ru.yandex.market.jmf.metainfo.ReloadType;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TypeInOtherClassTest.Configuration.class)
public class TypeInOtherClassTest {

    @Inject
    MetadataInitializer initializer;

    /**
     * Должны получить ошибку инициализации т.к. Тип вложен в Класс с другим кодом.
     */
    @Test
    public void check() {
        Assertions.assertThrows(ConfigurationError.class, () -> initializer.build(null,
                ReloadType.INSTANCE_MODIFICATION, ReloadAttributes.EMPTY));
    }

    @org.springframework.context.annotation.Configuration
    static class Configuration extends SimpleConfiguration {
        public Configuration() {
            super("classpath:type_in_otherClass_metadata.xml");
        }
    }
}
