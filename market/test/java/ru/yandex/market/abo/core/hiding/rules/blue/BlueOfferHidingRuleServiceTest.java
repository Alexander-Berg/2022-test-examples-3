package ru.yandex.market.abo.core.hiding.rules.blue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason;
import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 25.07.18.
 */
public class BlueOfferHidingRuleServiceTest extends DeletableEntityServiceTest<BlueOfferHidingRule, Long> {

    private static final long SUPPLIER_ID = 123L;
    private static final String SHOP_SKU = "BDR3235";

    @Autowired
    private BlueOfferHidingRuleService blueOfferHidingRuleService;

    @Override
    protected DeletableEntityService<BlueOfferHidingRule, Long> service() {
        return blueOfferHidingRuleService;
    }

    @Override
    protected Long extractId(BlueOfferHidingRule entity) {
        return entity.getId();
    }

    @Override
    protected BlueOfferHidingRule newEntity() {
        BlueOfferHidingRule hidingRule = new BlueOfferHidingRule();
        hidingRule.setMarketSku(1L);
        hidingRule.setComment("foobar");
        hidingRule.setHidingReason(BlueOfferHidingReason.FAULTY);
        hidingRule.setDeleted(false);
        return hidingRule;
    }

    @Override
    protected BlueOfferHidingRule example() {
        return new BlueOfferHidingRule();
    }

    @Test
    void saveBatch() {
        BlueOfferHidingRule rule1 = newEntity();
        BlueOfferHidingRule rule2 = newEntity();
        rule2.setMarketSku(2L);
        List<BlueOfferHidingRule> batchToSave = List.of(rule1, rule2);

        blueOfferHidingRuleService.saveBatch(batchToSave, 324L);

        List<BlueOfferHidingRule> saved = blueOfferHidingRuleService.findAllNotDeleted();
        assertEquals(batchToSave, saved);

        batchToSave.forEach(r -> r.setDeleted(true));
        blueOfferHidingRuleService.saveBatch(batchToSave, 43242L);
        flushAndClear();

        List<BlueOfferHidingRule> markDeleted = batchToSave.stream()
                .map(blueOfferHidingRuleService::findAlike)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        markDeleted.forEach(r -> assertTrue(r.getDeleted()));
        assertEquals(batchToSave, markDeleted);
    }

    @Test
    void deleteBySupplierAndShopSkuTest() {
        var rule = newEntity();
        rule.setSupplierId(SUPPLIER_ID);
        rule.setShopSku(SHOP_SKU);
        rule.setMarketSku(null);

        blueOfferHidingRuleService.addIfNotExistsOrDeleted(rule, 324L);
        flushAndClear();

        blueOfferHidingRuleService.deleteBySupplierAndShopSku(SUPPLIER_ID, SHOP_SKU, 31325L);

        assertTrue(blueOfferHidingRuleService.findAllNotDeleted().isEmpty());
    }

    @Test
    void saveWithSskuExistingFilterTest() {
        var rule = newEntity();
        rule.setSupplierId(SUPPLIER_ID);
        rule.setShopSku(SHOP_SKU);
        rule.setHidingReason(BlueOfferHidingReason.WRONG_SKU_MAPPING);
        rule.setMarketSku(null);

        blueOfferHidingRuleService.addIfNotExistsOrDeleted(rule, 324L);
        flushAndClear();

        var newRule = newEntity();
        newRule.setSupplierId(SUPPLIER_ID);
        newRule.setShopSku(SHOP_SKU);
        newRule.setMarketSku(null);
        newRule.setHidingReason(BlueOfferHidingReason.MISSING_ITEM);

        blueOfferHidingRuleService.saveWithSskuExistingFilter(List.of(newRule), 1245L);

        var savedRules = blueOfferHidingRuleService.findAllNotDeleted();

        assertEquals(1, savedRules.size());
        assertEquals(rule, savedRules.get(0));
        assertNotEquals(newRule, savedRules.get(0));
    }

    @Test
    void saveModel() {
        var rule1 = new BlueOfferHidingRule();
        rule1.setModelId(1L);
        rule1.setHidingReason(BlueOfferHidingReason.OTHER);
        rule1.setDeleted(false);
        blueOfferHidingRuleService.addIfNotExistsOrDeleted(rule1, 1001L);

        var rule2 = new BlueOfferHidingRule();
        rule2.setModelId(2L);
        rule2.setHidingReason(BlueOfferHidingReason.OTHER);
        rule2.setDeleted(false);
        blueOfferHidingRuleService.addIfNotExistsOrDeleted(rule2, 1001L);
    }

    @Test
    void getActualOrDeletedAfterTest() {
        Date actualDate = new Date();
        Date oldDate = DateUtil.addDay(actualDate, -2);

        saveHidingRule(oldDate, 1, true, 1);
        var expected = Set.of(
                saveHidingRule(oldDate, 2, false, 2),
                saveHidingRule(actualDate, 3, true, 3),
                saveHidingRule(actualDate, 4, false, 4)
        );

        List<BlueOfferHidingRule> found =
                blueOfferHidingRuleService.getActualOrDeletedAfter(DateUtil.addDay(actualDate, -1));
        assertEquals(expected, Set.copyOf(found));
    }

    @Test
    void getActualWithModelIdTest() {
        Date date = new Date();
        saveHidingRule(null, false, 1L);
        saveHidingRule(date, true, 2L);
        flushAndClear();

        var actualRules = blueOfferHidingRuleService.getActualWithModelId();
        assertEquals(1, actualRules.size());
        assertEquals(1L, actualRules.stream().findFirst().get().getModelId());
    }

    BlueOfferHidingRule saveHidingRule(Date deletionTime, long userId, boolean deleted, Long msku, Long modelId) {
        var rule = newEntity();
        rule.setCreationTime(new Date());
        rule.setDeletionTime(deletionTime);
        rule.setCreatedUserId(userId);
        rule.setDeleted(deleted);
        rule.setMarketSku(msku);
        rule.setModelId(modelId);
        blueOfferHidingRuleService.repo().save(rule);
        return rule;
    }

    BlueOfferHidingRule saveHidingRule(Date deletionTime, long userId, boolean deleted, long msku) {
        return saveHidingRule(deletionTime, userId, deleted, msku, null);
    }

    BlueOfferHidingRule saveHidingRule(Date deletionTime, boolean deleted, long modelId) {
        return saveHidingRule(deletionTime, 1L, deleted, null, modelId);
    }
}
