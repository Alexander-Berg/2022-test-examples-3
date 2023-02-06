package ru.yandex.market.jmf.module.def.test;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.script.ScriptService;

@Transactional
@SpringJUnitConfig(classes = ScriptTest.Configuration.class)
public class ScriptTest {
    public static final Fqn FQN = Fqn.of("simple$type1");

    @Inject
    ScriptService scriptService;
    @Inject
    DbService dbService;

    /**
     * В этом тесте опираемся на системный справчник "дни недели"
     */
    @Test
    public void apiDbOfGetByNaturalId() {
        // вызов системы
        String script = "api.db.of('dayOfWeek').get('monday')";
        HasGid result = scriptService.execute(script, ImmutableMap.of());

        // проверка утверждений
        Assertions.assertNotNull(result);
        CatalogItem catalogItem = dbService.get(result.getGid());
        Assertions.assertEquals("monday", catalogItem.getCode());
    }

    @Test
    public void apiDbOfGetNotExists() {
        // вызов системы
        String script = "api.db.of('dayOfWeek').get('valueNotExists')";
        HasGid result = scriptService.execute(script, ImmutableMap.of());
        // проверка утверждений
        Assertions.assertNull(result);
    }

    @Import(ModuleDefaultTestConfiguration.class)
    public static class Configuration {
        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:module/default/test/entity_metadata.xml");
        }
    }
}
