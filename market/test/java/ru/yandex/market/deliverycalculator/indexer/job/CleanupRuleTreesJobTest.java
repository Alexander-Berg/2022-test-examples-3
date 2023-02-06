package ru.yandex.market.deliverycalculator.indexer.job;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import javax.persistence.TypedQuery;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;
import ru.yandex.market.deliverycalculator.storage.model.MarketDeliveryTariff;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorStorageService;
import ru.yandex.market.deliverycalculator.storage.util.StorageUtils;

class CleanupRuleTreesJobTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private CleanupRuleTreesJob cleanupRuleTreesJob;

    @Autowired
    private DeliveryCalculatorStorageService storageService;

    @Test
    void basicTest() {
        MarketDeliveryTariff tariff = new MarketDeliveryTariff();
        tariff.setCurrency("BTC");
        tariff.setRule(new DeliveryRuleEntity());
        tariff.getRule().setChildren(Sets.newHashSet(new DeliveryRuleEntity(), new DeliveryRuleEntity(), new DeliveryRuleEntity()));
        DeliveryShop shop = new DeliveryShop();
        shop.setId(774L);
        shop.setMarketTariffs(Sets.newHashSet(tariff));
        storageService.insertShop(shop);

        //Make orphan rule with child tree
        DeliveryRuleEntity rule = new DeliveryRuleEntity();
        addChildrenTree(rule, 4, 2);
        insertRule(rule);

        cleanupRuleTreesJob.doJob(null);

        List<Long> expected = Lists.newArrayList(tariff.getRule().getId());
        tariff.getRule().getChildren().stream()
                .map(DeliveryRuleEntity::getId)
                .forEach(expected::add);
        Collections.sort(expected);

        List<Long> actual = getRuleIds();

        Assertions.assertIterableEquals(expected, actual);
    }

    private void addChildrenTree(DeliveryRuleEntity root, int childrenCount, int deep) {
        if (deep <= 0) {
            return;
        }

        Set<DeliveryRuleEntity> children = new HashSet<>();
        IntStream.range(0, childrenCount).forEach(i -> children.add(new DeliveryRuleEntity()));
        root.setChildren(children);
        children.forEach(c -> {
                    c.setParent(root);
                    addChildrenTree(c, childrenCount, deep - 1);
                }
        );
    }

    private void insertRule(DeliveryRuleEntity rule) {
        setParents(rule);
        StorageUtils.doInEntityManager(transactionTemplate, entityManager -> {
            entityManager.persist(rule);
        });
    }

    private void setParents(DeliveryRuleEntity rule) {
        if (CollectionUtils.isNotEmpty(rule.getChildren())) {
            rule.getChildren().forEach(this::setParents);
        }
    }

    private List<Long> getRuleIds() {
        return StorageUtils.doInEntityManager(transactionTemplate, entityManager -> {
            String hql = "select rule.id from DeliveryRuleEntity rule order by rule.id";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            return query.getResultList();
        });
    }
}
