package ru.yandex.market.jmf.catalog.items.test;

import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.RequiredAttributesValidationException;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.catalog.items.CatalogItemService;
import ru.yandex.market.jmf.catalog.items.HierarchicalCatalog;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.logic.def.HasTitle;
import ru.yandex.market.jmf.metadata.Fqn;

@Transactional
@SpringJUnitConfig(InternalCatalogItemsTestConfiguration.class)
public class Catalog2Test {

    private static final Fqn FQN_1 = Fqn.parse("c1");
    private static final Fqn FQN_2 = Fqn.parse("c2");
    private static final String ATTR_0 = "attr0";

    @Inject
    BcpService bcpService;
    @Inject
    CatalogItemService catalogItemService;

    @Test
    public void create() {
        String code = Randoms.string();
        String title = Randoms.string();
        String attr0 = Randoms.string();
        Entity result = bcpService.create(FQN_1, ImmutableMap.of(HasTitle.TITLE, title, "code", code, ATTR_0, attr0));

        // проверяем правильность заполнение как атрибутов определенных в Типе, так и в Классе
        Assertions.assertEquals(title, result.getAttribute(HasTitle.TITLE));
        Assertions.assertEquals(code, result.getAttribute("code"));
        Assertions.assertEquals(attr0, result.getAttribute(ATTR_0));
    }

    @Test
    public void create_withoutCode() {
        Assertions.assertThrows(RequiredAttributesValidationException.class, () -> {
            String title = Randoms.string();
            String attr0 = Randoms.string();
            bcpService.create(FQN_1, ImmutableMap.of(HasTitle.TITLE, title, ATTR_0, attr0));
        });
    }

    @Test
    public void create_withoutTitle() {
        Assertions.assertThrows(RequiredAttributesValidationException.class, () -> {
            String code = Randoms.string();
            String attr0 = Randoms.string();
            bcpService.create(FQN_1, ImmutableMap.of("code", code, ATTR_0, attr0));
        });
    }

    @Test
    public void create_duplicateCode() {
        Assertions.assertThrows(ValidationException.class, () -> {
            String code = Randoms.string();

            create(code, Randoms.string());
            create(code, Randoms.string());
        });
    }

    private Entity create(String code, String title) {
        return bcpService.create(FQN_1, ImmutableMap.of(HasTitle.TITLE, title, "code", code));
    }

    @Test
    public void catalogItemType() {
        Entity result = create(Randoms.string(), Randoms.string());
        Assertions.assertTrue(result instanceof CatalogItem);
    }

    @Test
    public void catalogService_get_code() {
        String code = Randoms.string();
        create(code, Randoms.string());
        // вызов системы
        CatalogItem item = catalogItemService.get(FQN_1, code);
        Assertions.assertNotNull(item);
        Assertions.assertEquals(code, item.getCode());
    }

    @Test
    public void catalogService_get_gid() {
        String code = Randoms.string();
        Entity entity = create(code, Randoms.string());

        CatalogItem item = catalogItemService.get(FQN_1, entity.getGid());
        Assertions.assertNotNull(item);
        Assertions.assertEquals(code, item.getCode());
    }

    @Test
    public void hierarchicalCatalogItem() {
        var root = bcpService.create(FQN_2, Map.of("code", Randoms.string(), HasTitle.TITLE,
                Randoms.string(), ATTR_0,
                Randoms.string(), "isSelectable", false));
        var leaf = bcpService.create(FQN_2, Map.of("code", Randoms.string(), HasTitle.TITLE,
                Randoms.string(), ATTR_0, Randoms.string(),
                "parent", root, "isSelectable", true));

        HierarchicalCatalog leafCatalogItem = catalogItemService.get(FQN_2, leaf.getGid());

        Assertions.assertNotNull(leafCatalogItem);
        Assertions.assertTrue(leafCatalogItem.isSelectable());
        Assertions.assertEquals(root, leafCatalogItem.getParent());
    }
}
