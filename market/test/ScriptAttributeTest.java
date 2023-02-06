package ru.yandex.market.jmf.module.def.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.script.storage.ScriptsProvider;
import ru.yandex.market.jmf.script.storage.ScriptsProviders;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = ScriptAttributeTest.Configuration.class)
public class ScriptAttributeTest {

    @Inject
    BcpService bcpService;

    @Test
    public void cachedAttr() {
        Entity entity = createEntity();

        String initialValue = entity.getAttribute("attr0");
        String scriptValue = entity.getAttribute("attr0scriptCached");

        Assertions.assertEquals(initialValue, scriptValue,
                "Вычислимый атрибут должен возвращать значение атрибута attr0");

        bcpService.edit(entity, Maps.of(
                "attr0", Randoms.string()
        ));

        String result = entity.getAttribute("attr0scriptCached");

        Assertions.assertEquals(initialValue, result, "Значение вычислимого атрибута не должно измениться т.к. оно " +
                "кешируется в рамках " +
                "транзакции (см. настроики атрибута)");
    }

    @Test
    public void attr() {
        Entity entity = createEntity();

        String initialValue = entity.getAttribute("attr0");
        String scriptValue = entity.getAttribute("attr0script");

        Assertions.assertEquals(initialValue, scriptValue, "Вычислимый атрибут должен возвращать значение атрибута " +
                "attr0");

        bcpService.edit(entity, Maps.of(
                "attr0", Randoms.string()
        ));

        String currentValue = entity.getAttribute("attr0");
        String result = entity.getAttribute("attr0script");

        Assertions.assertEquals(currentValue, result, "Значение вычислимого атрибута должно измениться т.к. оно не " +
                "кешируется в рамках " +
                "транзакции  (см. настроики атрибута)");
    }

    private Entity createEntity() {
        return bcpService.create(Fqn.of("simple$type1"), Maps.of(
                "attr0", Randoms.string()
        ));
    }


    @Import(ModuleDefaultTestConfiguration.class)
    public static class Configuration {
        @Bean
        public MetadataProvider testMetaclassesProvider(MetadataProviders providers) {
            return providers.of("classpath:module/default/test/entity_metadata.xml");
        }

        @Bean
        public ScriptsProvider testScriptsProvider(ScriptsProviders providers) {
            return providers.ofXml("classpath:module/default/test/entity_scripts.xml");
        }
    }
}
