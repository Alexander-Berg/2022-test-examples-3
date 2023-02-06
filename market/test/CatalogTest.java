package ru.yandex.market.jmf.catalog.items.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Iterables;
import org.assertj.core.api.IterableAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.catalog.items.Catalog;
import ru.yandex.market.jmf.catalog.items.CatalogCategory;
import ru.yandex.market.jmf.catalog.items.conf.ui.CatalogListContentConf;
import ru.yandex.market.jmf.catalog.items.impl.catalog.CatalogListContentStrategy;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;

@Transactional
@SpringJUnitConfig(InternalCatalogItemsTestConfiguration.class)
public class CatalogTest {

    @Inject
    EntityStorageService entityStorageService;
    @Inject
    CatalogListContentStrategy catalogListContentStrategy;

    @Test
    public void getExists() {
        Catalog result = entityStorageService.get(Catalog.FQN.gidOf("c1"));

        Assertions.assertNotNull(result);
        Assertions.assertEquals("catalog@c1", result.getGid());
        Assertions.assertEquals("c1", result.getCode());
        Assertions.assertEquals("Простой каталог", result.getTitle());
        Assertions.assertEquals("Описание простого каталога", result.getDescription());
        Assertions.assertEquals("common", result.getCategory().getCode());
    }

    @Test
    public void getNotExists() {
        Catalog result = entityStorageService.get(Catalog.FQN.gidOf("notExistsRandomStringNfnckhfcnld"));

        Assertions.assertNull(result);
    }

    @Test
    public void getByNaturalId() {
        Catalog result = entityStorageService.getByNaturalId(Catalog.FQN, "c1");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("c1", result.getCode());
    }

    /**
     * Проверяем, что метод list не разваливается и возвращает справочник дней недели
     */
    @Test
    public void list() {
        Query query = Query.of(Catalog.FQN)
                .withSortingOrder(
                        SortingOrder.asc(Catalog.TITLE),
                        SortingOrder.desc(Catalog.CODE),
                        SortingOrder.asc(Catalog.DESCRIPTION)
                );
        List<Catalog> result = entityStorageService.list(query);

        Assertions.assertNotNull(result);
        new IterableAssert<>(result)
                .anyMatch(c -> "c1".equals(c.getCode()));
    }

    @Test
    public void count() {
        long count = entityStorageService.count(Query.of(Catalog.FQN));
        Assertions.assertTrue(0 < count);
    }

    @Test
    public void content() {
        CatalogListContentStrategy.CatalogList result = catalogListContentStrategy.apply(
                null /* не используется */, new CatalogListContentConf());

        Assertions.assertNotNull(result);
        List<Map<String, Object>> data = result.getData();
        Assertions.assertNotNull(data);

        Map<String, Object> commonCategory = Iterables.find(data, i -> "common".equals(i.get(CatalogCategory.CODE)),
                null);
        Assertions.assertNotNull(
                commonCategory, "Должна быть категория common т.к. к ней относится справочник дней недели");
        Collection<Map<String, Object>> catalogs = (Collection<Map<String, Object>>) commonCategory.get("catalogs");
        Assertions.assertNotNull(catalogs, "вместе  категориями должны получить список справочников к ней " +
                "относящимися");
        boolean hasDayOfWeeks = Iterables.any(catalogs, c -> "c1".equals(c.get(Catalog.CODE)));
        Assertions.assertTrue(hasDayOfWeeks);
    }
}
