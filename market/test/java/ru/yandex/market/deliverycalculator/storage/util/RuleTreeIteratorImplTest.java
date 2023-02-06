package ru.yandex.market.deliverycalculator.storage.util;

import java.util.function.Consumer;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleEntity;

class RuleTreeIteratorImplTest extends FunctionalTest {
    private static final Logger log = LoggerFactory.getLogger(RuleTreeIteratorImplTest.class);

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void basicTest() {
        DeliveryRuleEntity tree = new DeliveryRuleEntity();
        StorageTestUtils.createOption(tree, 1, 2, 3, 4);

        DeliveryRuleEntity subTree1 = new DeliveryRuleEntity();
        StorageTestUtils.createOption(subTree1, 2, 3, 4, 5);
        StorageTestUtils.addChildRule(tree, subTree1);

        DeliveryRuleEntity subTree2 = new DeliveryRuleEntity();
        StorageTestUtils.createOption(subTree2, 3, 4, 5, 6);
        StorageTestUtils.addChildRule(tree, subTree2);

        DeliveryRuleEntity subTree11 = new DeliveryRuleEntity();
        StorageTestUtils.createOption(subTree11, 4, 5, 6, 7);
        StorageTestUtils.createPickpoint(subTree11, 234, null);
        StorageTestUtils.addChildRule(subTree1, subTree11);

        DeliveryRuleEntity subTree12 = new DeliveryRuleEntity();
        StorageTestUtils.createOption(subTree12, 5, 6, 7, 8);
        StorageTestUtils.addChildRule(subTree1, subTree12);

        DeliveryRuleEntity subTree111 = new DeliveryRuleEntity();
        StorageTestUtils.createOption(subTree111, 4, 5, 6, 7);
        StorageTestUtils.addChildRule(subTree11, subTree111);

        insertTree(tree);

        StorageUtils.doInEntityManager(transactionTemplate, entityManager -> {
            StorageUtils.visitRuleTree(tree.getId(), entityManager, 2,
                    rule -> log.info("rule: {}", rule.getId()));
        });

    }

    private void insertTree(DeliveryRuleEntity tree) {
        StorageUtils.doInEntityManager(transactionTemplate, (Consumer<EntityManager>) entityManager ->
                entityManager.persist(tree));
    }

}
