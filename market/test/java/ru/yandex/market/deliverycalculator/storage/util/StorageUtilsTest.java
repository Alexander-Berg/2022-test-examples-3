package ru.yandex.market.deliverycalculator.storage.util;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.collection.internal.PersistentSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class StorageUtilsTest extends FunctionalTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Тест для {@link StorageUtils#loadRuleTree(long, int, EntityManager)}.
     * Case: пытаемся вычитать дерево определенной глубины, притом запрашиваемая глубины меньше реальной высоты дерева.
     */
    @DbUnitDataSet(before = "database/ruleTree.before.csv")
    @Test
    public void testLoadRuleTreeWithHeightLessThanTreeHeight() {
        DeliveryRuleEntity deliveryRule =
                StorageUtils.loadRuleTree(16299301L, 2, entityManagerFactory.createEntityManager());

        assertFalse((deliveryRule.getChildren() instanceof PersistentSet));
        assertEquals(1, deliveryRule.getChildren().size());
        DeliveryRuleEntity firstLevelChild = deliveryRule.getChildren().stream().findFirst().orElseThrow();
        assertEquals(16299302L, firstLevelChild.getId());

        assertFalse((firstLevelChild.getChildren() instanceof PersistentSet));
        assertEquals(1, firstLevelChild.getChildren().size());
        DeliveryRuleEntity secondLevelChild = firstLevelChild.getChildren().stream().findFirst().orElseThrow();
        assertEquals(16299303L, secondLevelChild.getId());

        assertTrue((secondLevelChild.getChildren() instanceof PersistentSet));
        assertFalse(((PersistentSet) secondLevelChild.getChildren()).wasInitialized());
    }

    /**
     * Тест для {@link StorageUtils#loadRuleTree(long, int, EntityManager)}.
     * Case: пытаемся вычитать дерево определенной глубины, притом запрашиваемая глубины больше реальной высоты дерева.
     */
    @DbUnitDataSet(before = "database/ruleTree.before.csv")
    @Test
    public void testLoadRuleTreeWithHeightMoreThanTreeHeight() {
        DeliveryRuleEntity deliveryRule = StorageUtils.loadRuleTree(16299303L, 4, entityManagerFactory.createEntityManager());

        assertFalse((deliveryRule.getChildren() instanceof PersistentSet));
        assertEquals(3, deliveryRule.getChildren().size());

        List<DeliveryRuleEntity> firstLayerChildren = deliveryRule.getChildren().stream()
                .sorted(Comparator.comparing(DeliveryRuleEntity::getId))
                .collect(Collectors.toList());

        //validating first child
        assertEquals(16299305L, firstLayerChildren.get(0).getId());
        assertFalse((firstLayerChildren.get(0).getChildren() instanceof PersistentSet));
        assertEquals(2, firstLayerChildren.get(0).getChildren().size());

        List<DeliveryRuleEntity> secondLayerChildren = firstLayerChildren.get(0).getChildren().stream()
                .sorted(Comparator.comparing(DeliveryRuleEntity::getId))
                .collect(Collectors.toList());

        assertEquals(16299334L, secondLayerChildren.get(0).getId());
        assertFalse((secondLayerChildren.get(0).getChildren() instanceof PersistentSet));
        assertEquals(0, secondLayerChildren.get(0).getChildren().size());

        assertEquals(16299335L, secondLayerChildren.get(1).getId());
        assertFalse((secondLayerChildren.get(1).getChildren() instanceof PersistentSet));
        assertEquals(0, secondLayerChildren.get(1).getChildren().size());


        //validate second child
        assertEquals(16299340L, firstLayerChildren.get(1).getId());
        assertFalse((firstLayerChildren.get(1).getChildren() instanceof PersistentSet));
        assertEquals(0, firstLayerChildren.get(1).getChildren().size());

        //validate third child
        assertEquals(16299341L, firstLayerChildren.get(2).getId());
        assertFalse((firstLayerChildren.get(2).getChildren() instanceof PersistentSet));
        assertEquals(0, firstLayerChildren.get(2).getChildren().size());
    }
}
