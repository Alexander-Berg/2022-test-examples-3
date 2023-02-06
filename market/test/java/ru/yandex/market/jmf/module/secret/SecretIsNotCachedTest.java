package ru.yandex.market.jmf.module.secret;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.db.hibernate.impl.LogicCachedHelper;
import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.test.MetadataTestConfiguration;

@SpringJUnitConfig({
        ModuleSecretTestConfiguration.class,
        MetadataTestConfiguration.class,
        LogicDefaultTestConfiguration.class
})
@TestPropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class SecretIsNotCachedTest {
    @Inject
    private MetadataService metadataService;

    /**
     * Секреты нельзя хранить в памяти
     */
    @Test
    public void test() {
        Assertions.assertFalse(metadataService.getMetaclassOrError(Secret.FQN).hasLogic(LogicCachedHelper.CODE));
    }
}
