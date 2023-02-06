package ru.yandex.market.abo.core.hiding.rules.common.export;

import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason;
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRule;
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRuleService;
import ru.yandex.market.abo.core.hiding.rules.common.OfferHidingRuleAction;
import ru.yandex.market.abo.core.hiding.rules.common.OfferHidingRulesAbstractTest;
import ru.yandex.market.abo.core.hiding.rules.common.dao.ExclusiveRulesDao;
import ru.yandex.market.abo.core.hiding.rules.common.dao.InclusiveRulesDao;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.Include;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.IndexerRules;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.Model;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.ModelExceptShopRule;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.Offer;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.ShopRule;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.StopWord;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.Vendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OfferHidingRulesExportServiceTest extends OfferHidingRulesAbstractTest {

    private static final OfferHidingRuleAction RECENT_ACTION = new OfferHidingRuleAction(99L, "Some comment", Instant.now());
    private static final OfferHidingRuleAction OLD_ACTION = new OfferHidingRuleAction(88L, "Some comment", Instant.now().minus(Period.ofDays(30)));

    @Inject
    private OfferHidingRulesExportService exportService;
    @Autowired
    private BlueOfferHidingRuleService blueOfferHidingRuleService;

    @Test
    public void recentRulesShouldBeExportedForIndexer() throws Exception {
        checkRulesAreExportedForIndexer(RECENT_ACTION);
    }

    @Test
    public void oldRulesShouldBeExportedForIndexer() throws Exception {
        checkRulesAreExportedForIndexer(OLD_ACTION);
    }

    @Test
    public void testExclusiveRuleWithEmptyExclusion() throws Exception {
        service.updateExclusionRules(Collections.singleton(MODEL_ID), Collections.emptySet(), ACTION_1, ExclusiveRulesDao.MODEL_EXCEPT_SHOP);
        IndexerRules rules = exportService.getRulesForIndexer();
        List<ModelExceptShopRule> modelRules = new ArrayList<>(rules.getModelRules());
        assertEquals(1, modelRules.size());
        assertEquals(MODEL_ID, modelRules.get(0).getModelId());
        assertEquals(Collections.emptyList(), modelRules.get(0).getExcludes());
    }

    @Test
    public void modelIdsFromBlueOfferHidingRule() throws Exception {
        var blueModelRule = new BlueOfferHidingRule();
        blueModelRule.setModelId(12345L);
        blueModelRule.setDeleted(false);
        blueModelRule.setHidingReason(BlueOfferHidingReason.OTHER);
        blueOfferHidingRuleService.saveBatch(List.of(blueModelRule), 1L);

        IndexerRules rules = exportService.getRulesForIndexer();
        assertEquals(blueModelRule.getModelId(), rules.getModelRules().stream().findFirst().get().getModelId());
    }

    private void checkRulesAreExportedForIndexer(OfferHidingRuleAction action) {
        createRules(action);

        IndexerRules rules = exportService.getRulesForIndexer();

        assertEquals(2, rules.getModelRules().size());
        checkModelRule(rules, MODEL_ID_2, Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID));
        checkModelRule(rules, MODEL_ID_3, Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID));

        assertEquals(2, rules.getShopRules().size());
        checkShopRule(rules, SHOP_ID);
        checkShopRule(rules, ANOTHER_SHOP_ID);
    }

    private void checkShopRule(IndexerRules rules, long shopId) {
        assertTrue(getShopRule(rules, shopId).isPresent());
        ShopRule shopRule = getShopRule(rules, SHOP_ID).get();
        assertEquals(8, shopRule.getIncludes().size());
        checkInclude(shopRule, new Model(MODEL_ID_1));
        checkInclude(shopRule, new Model(MODEL_ID_2));
        checkInclude(shopRule, new Vendor(VENDOR_ID_1));
        checkInclude(shopRule, new Vendor(VENDOR_ID_2));
        checkInclude(shopRule, new Offer(OFFER_1));
        checkInclude(shopRule, new Offer(OFFER_2));
        checkInclude(shopRule, new StopWord(WORD_1));
        checkInclude(shopRule, new StopWord(WORD_2));
    }

    private Optional<ShopRule> getShopRule(IndexerRules indexerRules, long shopId) {
        return indexerRules.getShopRules().stream().filter(rule -> rule.getShopId() == shopId).findAny();
    }

    private void checkInclude(ShopRule shopRule, Include include) {
        assertTrue(shopRule.getIncludes().contains(include));
    }


    private void createRules(OfferHidingRuleAction action) {
        service.updateExclusionRules(new HashSet<>(Arrays.asList(MODEL_ID_2, MODEL_ID_3)),
                new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), action, ExclusiveRulesDao.MODEL_EXCEPT_SHOP);

        service.addInclusionRules(new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), new HashSet<>(Arrays.asList(MODEL_ID_1, MODEL_ID_2)),
                action, InclusiveRulesDao.SHOP_MODEL);
        service.addInclusionRules(new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), new HashSet<>(Arrays.asList(OFFER_1, OFFER_2)),
                action, InclusiveRulesDao.SHOP_OFFER);
        service.addInclusionRules(new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), new HashSet<>(Arrays.asList(VENDOR_ID_1, VENDOR_ID_2)),
                action, InclusiveRulesDao.SHOP_VENDOR);
        service.addInclusionRules(new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), new HashSet<>(Arrays.asList(WORD_1, WORD_2)),
                action, InclusiveRulesDao.SHOP_WORD);
    }

    private void checkModelRule(IndexerRules rules, long modelId, List<Long> expectedShopIds) {
        Optional<ModelExceptShopRule> modelRule = rules.getModelRules().stream().filter(rule -> rule.getModelId() == modelId).findAny();
        assertTrue(modelRule.isPresent());
        assertEquals(expectedShopIds.size(), modelRule.get().getExcludes().size());
        expectedShopIds.forEach(shopId -> assertTrue(modelRule.get().getExcludes().stream().anyMatch(rule -> shopId == rule.getShopId())));
    }
}
