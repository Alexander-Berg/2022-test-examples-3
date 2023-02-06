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
@ContextConfiguration(classes = TypeInExplicitlyNotTypedTest.Configuration.class)
public class TypeInExplicitlyNotTypedTest {

    @Inject
    MetadataInitializer initializer;

    /**
     * Должны получить ошибку инициализации т.к. Тип вложен в класс, не поддерживающий типизацию.
     */
    @Test
    public void check() {
        Assertions.assertThrows(ConfigurationError.class, () -> initializer.build(null,
                ReloadType.INSTANCE_MODIFICATION, ReloadAttributes.EMPTY));
    }

    @org.springframework.context.annotation.Configuration
    static class Configuration extends SimpleConfiguration {
        public Configuration() {
            super("classpath:type_in_explicitlyNotTyped_metadata.xml");
        }
    }
}
