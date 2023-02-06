package ru.yandex.market.abo.core.hiding.rules.common;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.hiding.rules.common.dao.ExclusiveRulesDao;
import ru.yandex.market.abo.core.hiding.rules.common.dao.InclusiveRulesDao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class OfferHidingRulesServiceTest extends OfferHidingRulesAbstractTest {


    @Test
    public void testAddShopModelRules() {
        service.addInclusionRules(Collections.singleton(SHOP_ID), Stream.of(MODEL_ID_1, MODEL_ID_2, MODEL_ID_3).collect(Collectors.toSet()), ACTION_1, InclusiveRulesDao.SHOP_MODEL);
        service.addInclusionRules(Collections.singleton(ANOTHER_SHOP_ID), Collections.singleton(MODEL_ID), ACTION_1, InclusiveRulesDao.SHOP_MODEL);
        Set<Long> modelIds = service.findInclusions(SHOP_ID, InclusiveRulesDao.SHOP_MODEL);
        assertEquals(3, modelIds.size());
        Stream.of(MODEL_ID_1, MODEL_ID_2, MODEL_ID_3)
                .forEach(modelId -> assertTrue(modelIds.contains(modelId), modelId + " is missing"));
        assertEquals(1, service.findInclusions(ANOTHER_SHOP_ID, InclusiveRulesDao.SHOP_MODEL).size());

        service.addInclusionRules(Collections.singleton(SHOP_ID), Stream.of(MODEL_ID_1, MODEL_ID_4).collect(Collectors.toSet()), ACTION_1, InclusiveRulesDao.SHOP_MODEL);
        Set<Long> modelIdsAfterUpdate = service.findInclusions(SHOP_ID, InclusiveRulesDao.SHOP_MODEL);
        assertEquals(4, modelIdsAfterUpdate.size());
        Stream.of(MODEL_ID_1, MODEL_ID_2, MODEL_ID_3, MODEL_ID_4)
                .forEach(modelId -> assertTrue(modelIdsAfterUpdate.contains(modelId), modelId + " modelId is missing"));
        assertEquals(1, service.findInclusions(ANOTHER_SHOP_ID, InclusiveRulesDao.SHOP_MODEL).size());

        assertEquals(0, service.findInclusions(ANOTHER_SHOP_ID, InclusiveRulesDao.SHOP_OFFER).size());
    }

    @Test
    public void testAddShopOfferRules() {
        service.addInclusionRules(Collections.singleton(SHOP_ID), Stream.of(OFFER_1, OFFER_2).collect(Collectors.toSet()), ACTION_1, InclusiveRulesDao.SHOP_OFFER);
        assertEquals(2, service.findInclusions(SHOP_ID, InclusiveRulesDao.SHOP_OFFER).size());
        assertEquals(0, service.findInclusions(SHOP_ID, InclusiveRulesDao.SHOP_MODEL).size());
    }

    @Test
    public void testInclusionRulesHistory() throws Exception {
        service.addInclusionRules(new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), new HashSet<>(Arrays.asList(MODEL_ID_1, MODEL_ID_2, MODEL_ID_4)), ACTION_1, InclusiveRulesDao.SHOP_MODEL);
        assertEquals(1, getActionsCount());
        long model1ruleId = InclusiveRulesDao.SHOP_MODEL.getRuleId(SHOP_ID, MODEL_ID_1, jdbcTemplate).get();
        service.addInclusionRules(Collections.singleton(SHOP_ID), Collections.singleton(MODEL_ID_3), ACTION_2, InclusiveRulesDao.SHOP_MODEL);
        assertEquals(2, getActionsCount());
        long model3ruleId = InclusiveRulesDao.SHOP_MODEL.getRuleId(SHOP_ID, MODEL_ID_3, jdbcTemplate).get();

        assertEquals(ACTION_1, getRuleCreationAction(model1ruleId));
        assertEquals(ACTION_2, getRuleCreationAction(model3ruleId));
    }

    @Test
    public void addCompositeRulesShouldNotCreateEmptyActions() {
        service.addInclusionRules(Collections.singleton(SHOP_ID), new HashSet<>(Arrays.asList(MODEL_ID_1, MODEL_ID_2)), ACTION_1, InclusiveRulesDao.SHOP_MODEL);
        assertEquals(1, getActionsCount());
        service.addInclusionRules(Collections.singleton(SHOP_ID), Collections.singleton(MODEL_ID_1), ACTION_2, InclusiveRulesDao.SHOP_MODEL);
        assertEquals(1, getActionsCount());
    }


    @Test
    public void updateExclusionRulesShouldCreateNewRules() throws Exception {
        service.updateExclusionRules(new HashSet<>(Arrays.asList(MODEL_ID_1, MODEL_ID_2)),
                new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), ACTION_1, ExclusiveRulesDao.MODEL_EXCEPT_SHOP);
        assertEquals(2, ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getAllActiveRules(jdbcTemplate).size());
        assertTrue(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_1, SHOP_ID, jdbcTemplate).isPresent());
        assertTrue(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_1, ANOTHER_SHOP_ID, jdbcTemplate).isPresent());
        assertTrue(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_2, SHOP_ID, jdbcTemplate).isPresent());
        assertTrue(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_2, ANOTHER_SHOP_ID, jdbcTemplate).isPresent());
    }

    @Test
    public void updateExclusionRulesShouldRecreateChangedRules() throws Exception {
        service.updateExclusionRules(Collections.singleton(MODEL_ID_1),
                new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), ACTION_1, ExclusiveRulesDao.MODEL_EXCEPT_SHOP);
        service.updateExclusionRules(new HashSet<>(Arrays.asList(MODEL_ID_1, MODEL_ID_2)),
                Collections.singleton(ANOTHER_SHOP_ID), ACTION_2, ExclusiveRulesDao.MODEL_EXCEPT_SHOP);

        assertEquals(2, ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getAllActiveRules(jdbcTemplate).size());

        assertFalse(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_1, SHOP_ID, jdbcTemplate).isPresent());
        assertTrue(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_1, ANOTHER_SHOP_ID, jdbcTemplate).isPresent());
        assertFalse(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_2, SHOP_ID, jdbcTemplate).isPresent());
        assertTrue(ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_2, ANOTHER_SHOP_ID, jdbcTemplate).isPresent());
    }

    @Test
    public void testExclusionRulesHistory() {
        service.updateExclusionRules(Collections.singleton(MODEL_ID_1),
                new HashSet<>(Arrays.asList(SHOP_ID, ANOTHER_SHOP_ID)), ACTION_1, ExclusiveRulesDao.MODEL_EXCEPT_SHOP);
        long oldModel1RuleId = ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_1, SHOP_ID, jdbcTemplate).get();
        assertEquals(oldModel1RuleId, ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_1, ANOTHER_SHOP_ID, jdbcTemplate).get().longValue());

        service.updateExclusionRules(new HashSet<>(Arrays.asList(MODEL_ID_1, MODEL_ID_2)),
                Collections.singleton(ANOTHER_SHOP_ID), ACTION_2, ExclusiveRulesDao.MODEL_EXCEPT_SHOP);
        long newModel1RuleId = ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_1, ANOTHER_SHOP_ID, jdbcTemplate).get();
        long model2RuleId = ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID_2, ANOTHER_SHOP_ID, jdbcTemplate).get();
        assertFalse(oldModel1RuleId == newModel1RuleId);
        assertEquals(2, getActionsCount());
        assertEquals(ACTION_1, getRuleCreationAction(oldModel1RuleId));
        assertEquals(ACTION_2, getRuleCreationAction(newModel1RuleId));
        assertEquals(ACTION_2, getRuleCreationAction(model2RuleId));
        assertEquals(getRuleCreationActionId(newModel1RuleId), getRuleCreationActionId(model2RuleId));
        assertEquals(getRuleDeletionActionId(oldModel1RuleId).get(), getRuleCreationActionId(newModel1RuleId));
        assertEquals(ACTION_2, getRuleDeletionAction(oldModel1RuleId).get());
    }

    @Test
    public void testDeleteRulesByIds() {
        service.addInclusionRules(Collections.singleton(SHOP_ID), Collections.singleton(OFFER_1), ACTION_1,
                InclusiveRulesDao.SHOP_OFFER);
        service.addInclusionRules(Collections.singleton(SHOP_ID), Collections.singleton(MODEL_ID_1), ACTION_1,
                InclusiveRulesDao.SHOP_MODEL);
        service.updateExclusionRules(Collections.singleton(MODEL_ID), Collections.singleton(ANOTHER_SHOP_ID), ACTION_1,
                ExclusiveRulesDao.MODEL_EXCEPT_SHOP);
        assertEquals(3, getActionsCount());
        long shopOfferRuleId = InclusiveRulesDao.SHOP_OFFER.getRuleId(SHOP_ID, OFFER_1, jdbcTemplate).get();
        long modelRuleId = ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getRuleId(MODEL_ID, ANOTHER_SHOP_ID, jdbcTemplate).get();
        service.deleteRulesByIds(Arrays.asList(shopOfferRuleId, modelRuleId), ACTION_2);
        assertEquals(4, getActionsCount());
        assertEquals(0, InclusiveRulesDao.SHOP_OFFER.getAllActiveRules(jdbcTemplate).size());
        assertEquals(0, ExclusiveRulesDao.MODEL_EXCEPT_SHOP.getAllActiveRules(jdbcTemplate).size());
        assertEquals(1, InclusiveRulesDao.SHOP_MODEL.getAllActiveRules(jdbcTemplate).size());
        assertEquals(getRuleDeletionActionId(shopOfferRuleId), getRuleDeletionActionId(modelRuleId));
        assertEquals(ACTION_2, getRuleDeletionAction(modelRuleId).get());
        assertEquals(ACTION_2, getRuleDeletionAction(shopOfferRuleId).get());
    }

    @Test
    void testDeleteRules() {
        service.addInclusionRules(Collections.singleton(SHOP_ID), Collections.singleton(WORD_1), ACTION_1,
                InclusiveRulesDao.SHOP_WORD);
        service.addInclusionRules(Collections.singleton(SHOP_ID), Collections.singleton(WORD_2), ACTION_1,
                InclusiveRulesDao.SHOP_WORD);
        assertEquals(2, getActionsCount());

        long firstRuleId = InclusiveRulesDao.SHOP_WORD.getRuleId(SHOP_ID, WORD_1, jdbcTemplate).get();
        long secondRuleId = InclusiveRulesDao.SHOP_WORD.getRuleId(SHOP_ID, WORD_1, jdbcTemplate).get();

        service.deleteRules(Collections.singleton(SHOP_ID), Arrays.asList(WORD_1, WORD_2), ACTION_2,
                InclusiveRulesDao.SHOP_WORD);
        assertEquals(3, getActionsCount());

        assertTrue(InclusiveRulesDao.SHOP_WORD.getAllActiveRules(jdbcTemplate).isEmpty());
        assertEquals(getRuleDeletionActionId(firstRuleId), getRuleDeletionActionId(secondRuleId));
        assertEquals(ACTION_2, getRuleDeletionAction(firstRuleId).get());
        assertEquals(ACTION_2, getRuleDeletionAction(secondRuleId).get());
    }

    @Test
    void testDeleteRulesEmpty() {
        service.deleteRules(Collections.emptyList(), Collections.singleton(WORD_1), ACTION_1,
                InclusiveRulesDao.SHOP_WORD);
        service.deleteRules(Collections.singleton(SHOP_ID), Collections.emptyList(), ACTION_1,
                InclusiveRulesDao.SHOP_WORD);
        assertEquals(0, getActionsCount());
    }

    private OfferHidingRuleAction getRuleCreationAction(long ruleId) {
        long creationActionId = getRuleCreationActionId(ruleId);
        return getRuleAction(creationActionId);
    }

    private Long getRuleCreationActionId(long ruleId) {
        return jdbcTemplate.queryForObject("SELECT created_in_action_id FROM offer_hiding_rule WHERE rule_id = ?",
                Long.class, ruleId);
    }

    private Optional<OfferHidingRuleAction> getRuleDeletionAction(long ruleId) {
        Optional<Long> deletionActionId = getRuleDeletionActionId(ruleId);
        return deletionActionId.map(this::getRuleAction);
    }

    private Optional<Long> getRuleDeletionActionId(long ruleId) {
        return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT deleted_in_action_id FROM offer_hiding_rule WHERE rule_id = ?",
                Long.class, ruleId));
    }

    private int getActionsCount() {
        return jdbcTemplate.queryForList("SELECT count(*) FROM offer_hiding_rule_action", Integer.class).stream()
                .findFirst().orElseThrow(() -> new RuntimeException("Should never been thrown"));
    }

    private OfferHidingRuleAction getRuleAction(long actionId) {
        return jdbcTemplate.query(
                "SELECT action_time, user_id, user_comment " +
                        "FROM offer_hiding_rule_action " +
                        "WHERE action_id = ?",
                (rs, rowNum) -> new OfferHidingRuleAction(rs.getLong("user_id"), rs.getString("user_comment"),
                        rs.getTimestamp("action_time").toInstant()),
                actionId
        ).stream().findAny().orElse(null);
    }

}
