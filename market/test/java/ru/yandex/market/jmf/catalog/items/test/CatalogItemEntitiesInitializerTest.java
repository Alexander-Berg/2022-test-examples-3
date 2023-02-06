package ru.yandex.market.jmf.catalog.items.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.catalog.items.CatalogItemsEntityInitializationProviderFactory;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entities.initialization.EntityInitializationProvider;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;

@Transactional
@SpringJUnitConfig(CatalogItemEntitiesInitializerTest.Config.class)
public class CatalogItemEntitiesInitializerTest {

    private static final Fqn FQN = Fqn.of("enumCatalog");
    @Inject
    DbService dbService;

    /**
     * Проверяем правильность инициализации элементов справочника по значениям enum-а
     */
    @Test
    public void checkItems() {
        List<CatalogItem> list = dbService.list(Query.of(FQN));
        Map<String, CatalogItem> index = Maps.uniqueIndex(list, CatalogItem::getCode);
        Assertions.assertEquals(3, list.size(), "Должно совпадать с кол-вом элементов TestEnum");
        CatalogItem a = index.get(TestEnum.A.name());
        Assertions.assertEquals(TestEnum.A.name(), a.getTitle());
        CatalogItem c = index.get(TestEnum.C.name());
        Assertions.assertEquals("custom name of C item", c.getTitle());
    }

    public enum TestEnum {
        A,
        B,
        @ApiModelProperty(value = "custom name of C item")
        C
    }

    @Import(InternalCatalogItemsTestConfiguration.class)
    public static class Config {
        @Bean
        public EntityInitializationProvider testEnumEntityInitializationProvider(
                CatalogItemsEntityInitializationProviderFactory factory
        ) {
            return factory
                    .enumCatalogItem(FQN, TestEnum.class);
        }
    }
}
