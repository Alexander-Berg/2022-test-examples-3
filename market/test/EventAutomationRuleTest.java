package ru.yandex.market.jmf.module.automation.test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.module.automation.AutomationRule;
import ru.yandex.market.jmf.module.automation.AutomationRuleHolder;
import ru.yandex.market.jmf.module.automation.EntityHistory;
import ru.yandex.market.jmf.module.automation.EventAutomationRule;
import ru.yandex.market.jmf.module.entity.snapshot.SnapshottedByStatusLogic;
import ru.yandex.market.jmf.trigger.TriggerServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EventAutomationRuleTest extends AbstractAutomationRuleTest {

    @MethodSource({"dataForTest", "attributePredicateOnNullObject"})
    @ParameterizedTest(name = "{0}")
    public void automationRule(String testTitle,
                               String jsonConfPath,
                               Map<String, Object> properties,
                               String expectedAttributeValue) {
        Entity entity1 = bcpService.create(FQN_1, properties);
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, jsonConfPath);
        startTrigger(entity1, entity2);

        assertEquals(expectedAttributeValue, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    private static Stream<Arguments> dataForTest() {
        return Stream.of(
                Arguments.of(
                        "actionOnly",
                        "/test/jmf/module/automation/rules/event/actionOnly.json",
                        Map.of(),
                        SUCCESS
                ),
                Arguments.of(
                        "andAttributePredicate",
                        "/test/jmf/module/automation/rules/event/andAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "tom and jerry"),
                        SUCCESS
                ),
                Arguments.of(
                        "eqAttributePredicate",
                        "/test/jmf/module/automation/rules/event/eqAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "cat"),
                        SUCCESS
                ),
                Arguments.of(
                        "eqEmptyValueAttributePredicate",
                        "/test/jmf/module/automation/rules/event/eqEmptyValueAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, ""),
                        SUCCESS
                ),
                Arguments.of(
                        "eqNullValueAttributePredicate",
                        "/test/jmf/module/automation/rules/event/eqNullValueAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, ""),
                        SUCCESS
                ),
                Arguments.of(
                        "negativeBranch",
                        "/test/jmf/module/automation/rules/event/negativeBranch.json",
                        Map.of(RULE_CONDITION_ATTR_1, "dog"),
                        SUCCESS
                ),
                Arguments.of(
                        "positiveBranchIsAbsence",
                        "/test/jmf/module/automation/rules/event/negativeBranch.json",
                        Map.of(RULE_CONDITION_ATTR_1, "mouse"),
                        null
                ),
                Arguments.of(
                        "orAttributePredicate",
                        "/test/jmf/module/automation/rules/event/orAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "only dogs"),
                        SUCCESS
                ),
                Arguments.of(
                        "notAttributePredicate",
                        "/test/jmf/module/automation/rules/event/notAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "only cats"),
                        SUCCESS
                ),
                Arguments.of(
                        "neAttributePredicate",
                        "/test/jmf/module/automation/rules/event/neAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "dog"),
                        SUCCESS
                ),
                Arguments.of(
                        "neEmptyValueAttributePredicate",
                        "/test/jmf/module/automation/rules/event/neEmptyValueAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "dog"),
                        SUCCESS
                ),
                Arguments.of(
                        "neNullValueAttributePredicate",
                        "/test/jmf/module/automation/rules/event/neNullValueAttributePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "dog"),
                        SUCCESS
                ),
                Arguments.of(
                        "andRulePredicate",
                        "/test/jmf/module/automation/rules/event/andRulePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "cat", RULE_CONDITION_ATTR_2, "dog"),
                        SUCCESS
                ),
                Arguments.of(
                        "scriptRulePredicate",
                        "/test/jmf/module/automation/rules/event/scriptRulePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "cat", RULE_CONDITION_ATTR_2, "dog"),
                        SUCCESS
                ),
                Arguments.of(
                        "orRulePredicate",
                        "/test/jmf/module/automation/rules/event/orRulePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "cat", RULE_CONDITION_ATTR_2, "mouse"),
                        SUCCESS
                ),
                Arguments.of(
                        "notRulePredicate",
                        "/test/jmf/module/automation/rules/event/notRulePredicate.json",
                        Map.of(RULE_CONDITION_ATTR_1, "only dogs"),
                        SUCCESS
                ),
                Arguments.of(
                        "ignoreActionOnNullObject",
                        "/test/jmf/module/automation/rules/event/ignoreActionOnNullObject.json",
                        Map.of(),
                        SUCCESS
                ),
                Arguments.of(
                        "emptyRules",
                        "/test/jmf/module/automation/rules/event/emptyRules.json",
                        Map.of(),
                        null
                ),
                Arguments.of(
                        "emptyCreateActionProperties",
                        "/test/jmf/module/automation/rules/event/emptyCreateActionProperties.json",
                        Map.of(),
                        null
                ),
                Arguments.of(
                        "emptyEditActionProperties",
                        "/test/jmf/module/automation/rules/event/emptyEditActionProperties.json",
                        Map.of(),
                        null
                ),
                Arguments.of(
                        "nullCreateActionProperties",
                        "/test/jmf/module/automation/rules/event/nullCreateActionProperties.json",
                        Map.of(),
                        null
                ),
                Arguments.of(
                        "nullEditActionProperties",
                        "/test/jmf/module/automation/rules/event/nullEditActionProperties.json",
                        Map.of(),
                        null
                ),
                Arguments.of(
                        "nullCreateActionPropertyValue",
                        "/test/jmf/module/automation/rules/event/nullCreateActionPropertyValue.json",
                        Map.of(),
                        null
                ),
                Arguments.of(
                        "nullEditActionPropertyValue",
                        "/test/jmf/module/automation/rules/event/nullEditActionPropertyValue.json",
                        Map.of(),
                        null
                )
        );
    }

    private static Stream<Arguments> attributePredicateOnNullObject() {
        return Stream.of(
                Arguments.of(
                        "eqAttributePredicateOnNullObject",
                        "/test/jmf/module/automation/rules/event/eqAttributePredicateOnNullObject.json",
                        Map.of(),
                        SUCCESS
                ),
                Arguments.of(
                        "neAttributePredicateOnNullObject",
                        "/test/jmf/module/automation/rules/event/neAttributePredicateOnNullObject.json",
                        Map.of(),
                        SUCCESS
                ),
                Arguments.of(
                        "containsAttributePredicateOnNullObject",
                        "/test/jmf/module/automation/rules/event/containsAttributePredicateOnNullObject.json",
                        Map.of(),
                        SUCCESS
                ),
                Arguments.of(
                        "isNullAttributePredicateOnNullObject",
                        "/test/jmf/module/automation/rules/event/isNullAttributePredicateOnNullObject.json",
                        Map.of(),
                        SUCCESS
                ),
                Arguments.of(
                        "isNotNullAttributePredicateOnNullObject",
                        "/test/jmf/module/automation/rules/event/isNotNullAttributePredicateOnNullObject.json",
                        Map.of(),
                        SUCCESS
                )
        );
    }

    @Test
    public void containsAll() {
        Entity value1 = bcpService.create(FQN_1, Map.of());
        Entity value2 = bcpService.create(FQN_1, Map.of());
        Entity value3 = bcpService.create(FQN_1, Map.of());

        Entity entity1 = bcpService.create(FQN_1, Map.of(OBJECTS_ATTR, List.of(value1, value2, value3)));
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/containsAllAttributePredicate.json",
                value1.getGid(), value2.getGid());
        startTrigger(entity1, entity2);

        assertEquals(SUCCESS, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    private static List<Arguments> containsAnyCases() {
        return List.of(
                Arguments.of(List.of(0, 1, 2), List.of(0, 1), SUCCESS),
                Arguments.of(List.of(0, 1, 2), List.of(1, 2), SUCCESS),
                Arguments.of(List.of(0, 2), List.of(0, 1), SUCCESS),
                Arguments.of(List.of(2), List.of(0, 2), SUCCESS),
                Arguments.of(List.of(0, 1), List.of(2, 3), null),
                Arguments.of(List.of(2), List.of(0, 3), null)
        );
    }

    @Test
    public void containsNotAll() {
        Entity value1 = bcpService.create(FQN_1, Map.of());
        Entity value2 = bcpService.create(FQN_1, Map.of());
        Entity value3 = bcpService.create(FQN_1, Map.of());

        Entity entity1 = bcpService.create(FQN_1, Map.of(OBJECTS_ATTR, List.of(value1, value2)));
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/containsAllAttributePredicate.json",
                value1.getGid(), value3.getGid());
        startTrigger(entity1, entity2);
        startTrigger(entity1, entity2);

        assertNull(entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @ParameterizedTest
    @MethodSource(value = "containsAnyCases")
    public void containsAny(List<Integer> attributeValueIndexes, List<Integer> filterValueIndexes, String result) {
        var values = List.<Entity>of(
                bcpService.create(FQN_1, Map.of()),
                bcpService.create(FQN_1, Map.of()),
                bcpService.create(FQN_1, Map.of()),
                bcpService.create(FQN_1, Map.of()));

        var attributeValues = attributeValueIndexes.stream().map(values::get).collect(Collectors.toList());
        Entity entity1 = bcpService.create(FQN_1, Map.of(OBJECTS_ATTR, attributeValues));
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        var filterValues = filterValueIndexes
                .stream()
                .map(index -> values.get(index).getGid())
                .toArray(String[]::new);

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/containsAnyAttributePredicate.json",
                filterValues);
        startTrigger(entity1, entity2);

        assertEquals(result, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @Test
    public void editRelatedObject() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of(RULE_CONDITION_ATTR_3, "cat"));

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/editRelatedObject.json");
        startTrigger(entity1, entity2);

        assertEquals(SUCCESS, entity2.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @Test
    public void multipleRule() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/multipleRule.json");
        startTrigger(entity1, entity2);

        assertEquals(SUCCESS, entity1.getAttribute(RULE_RESULT_ATTR));
        assertEquals(SUCCESS, entity1.getAttribute(RULE_RESULT_ATTR_2));
        checkErrors(0);
    }

    @Test
    public void skipErrorRulePredicate() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/errorRulePredicate.json");
        assertThrows(TriggerServiceException.class, () -> startTrigger(entity1, entity2));

        assertNull(entity1.getAttribute(RULE_RESULT_ATTR));
        assertEquals(SUCCESS, entity1.getAttribute(RULE_RESULT_ATTR_2));
        checkErrors(1);
    }

    @Test
    public void skipErrorRuleAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/errorRuleAction.json");
        startTrigger(entity1, entity2);

        assertEquals(SUCCESS, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @Test
    public void returnRuleAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/returnRuleAction.json");
        startTrigger(entity1, entity2);

        assertEquals(SUCCESS, entity1.getAttribute(RULE_RESULT_ATTR));
        assertNull(entity1.getAttribute(RULE_RESULT_ATTR_2));
        checkErrors(0);
    }

    @Test
    public void renderStringAttrOnEditAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/renderStringAttrOnEditAction.json");
        startTrigger(entity1, entity2);

        assertEquals(
                String.format("%s %s", SUCCESS, gidService.parse(entity1.getGid()).getId()),
                entity1.getAttribute(RULE_RESULT_ATTR)
        );
        checkErrors(0);
    }

    @Test
    public void renderStringAttrOnCreateAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/renderStringAttrOnCreateAction.json");
        startTrigger(entity1, entity2);

        List<Entity> entities = dbService.list(Query.of(FQN_2));
        entities.remove(entity2);
        assertEquals(1, entities.size());
        assertEquals(
                String.format("%s %s", SUCCESS, gidService.parse(entity1.getGid()).getId()),
                entities.get(0).getAttribute(RULE_RESULT_ATTR)
        );
        checkErrors(0);
    }

    @Test
    public void doNotRenderRawStringAttrOnEditAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event" +
                "/doNotRenderRawStringAttrOnEditAction.json");
        startTrigger(entity1, entity2);

        assertEquals(
                String.format("%s ${obj.id}", SUCCESS),
                entity1.getAttribute(RULE_RESULT_ATTR)
        );
        checkErrors(0);
    }

    @Test
    public void doNotRenderRawStringAttrOnCreateAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event" +
                "/doNotRenderRawStringAttrOnCreateAction.json");
        startTrigger(entity1, entity2);

        List<Entity> entities = dbService.list(Query.of(FQN_2));
        entities.remove(entity2);
        assertEquals(1, entities.size());
        assertEquals(
                String.format("%s ${obj.id}", SUCCESS),
                entities.get(0).getAttribute(RULE_RESULT_ATTR)
        );
        checkErrors(0);
    }

    @Test
    public void entityHistory() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        String initValue = "value0";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        AutomationRule rule1 = createEntityHistoryRule(entity2, initValue, value1);
        AutomationRule rule2 = createEntityHistoryRule(entity2, value1, value2);
        AutomationRule rule3 = createEntityHistoryRule(entity2, value2, value3);

        bcpService.edit(entity1, Map.of(
                LINKED_ATTR, entity2,
                RULE_RESULT_ATTR, initValue,
                TRIGGER_CONDITION_ATTR, "START_HISTORY"
        ));

        assertEquals(value3, entity1.getAttribute(RULE_RESULT_ATTR));
        List<EntityHistory> history = getHistory(entity1);

        assertEventHistory(history, initValue, value1, rule1);
        assertEventHistory(history, value1, value2, rule2);
        assertEventHistory(history, value2, value3, rule3);

        assertNull(AutomationRuleHolder.getAutomationRule());
        checkErrors(0);
    }

    private void assertEventHistory(List<EntityHistory> history, String value1, String value2, AutomationRule rule) {
        EntityHistory historyItem = history.stream()
                .filter(x -> null != x.getDescription()
                        && x.getDescription().contains(value1)
                        && x.getDescription().contains(value2))
                .findFirst()
                .orElse(null);
        assertNotNull(historyItem);
        assertEquals(rule, historyItem.getAutomationRule());
    }

    @Test
    public void editObjectAttr() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/editObjectAttribute.json",
                entity1.getGid());
        startTrigger(entity1, entity2);

        assertEquals(entity1, entity1.getAttribute(OBJECT_ATTR));
        checkErrors(0);
    }

    @Test
    public void conditionOnObjectAttr() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_1, Map.of(OBJECT_ATTR, entity1));
        Entity entity3 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity3, "/test/jmf/module/automation/rules/event/conditionOnObjectAttr.json",
                entity1.getGid());
        startTrigger(entity2, entity3);

        assertEquals(SUCCESS, entity2.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @Test
    public void filterRule() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_1, Map.of(OBJECT_ATTR, entity1));
        Entity entity3 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity3, "/test/jmf/module/automation/rules/event/conditionOnObjectAttr.json",
                entity1.getGid());
        startTrigger(entity2, entity3);

        assertEquals(SUCCESS, entity2.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @Test
    public void testSnapshotRuleExecution() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        String value1 = Randoms.string();
        EventAutomationRule rule = createApprovedEventRule(entity2,
                "/test/jmf/module/automation/rules/event/setRuleResult.json", value1);

        String value2 = Randoms.string();
        editEventRule(rule, "/test/jmf/module/automation/rules/event/setRuleResult.json", value2);

        assertEquals(SnapshottedByStatusLogic.Statuses.DRAFT, rule.getStatus());

        startTrigger(entity1, entity2);
        assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);

        bcpService.edit(rule, Map.of(HasWorkflow.STATUS, SnapshottedByStatusLogic.Statuses.REVIEW));
        restartTrigger(entity1, entity2);
        assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);

        bcpService.edit(rule, Map.of(HasWorkflow.STATUS, SnapshottedByStatusLogic.Statuses.APPROVED));
        restartTrigger(entity1, entity2);
        assertEquals(value2, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @Test
    public void testActiveRuleExecution() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        String value1 = Randoms.string();
        EventAutomationRule rule = createDraftEventRule(entity2,
                "/test/jmf/module/automation/rules/event/setRuleResult.json", value1);

        startTrigger(entity1, entity2);
        assertNull(entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);

        bcpService.edit(rule,
                Map.of(AutomationRule.STATUS, AutomationRule.Statuses.ACTIVE),
                Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
        );

        restartTrigger(entity1, entity2);
        assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    @Test
    public void testSnapshotActiveRuleExecution() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        String value1 = Randoms.string();
        EventAutomationRule rule = createDraftEventRule(entity2,
                "/test/jmf/module/automation/rules/event/setRuleResult.json", value1);

        bcpService.edit(rule,
                Map.of(AutomationRule.STATUS, AutomationRule.Statuses.ACTIVE),
                Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
        );

        String value2 = Randoms.string();
        editEventRule(rule, "/test/jmf/module/automation/rules/event/setRuleResult.json", value2);

        startTrigger(entity1, entity2);
        assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));
        checkErrors(0);
    }

    private AutomationRule createEntityHistoryRule(Entity entity, String value1, String value2) {
        return createApprovedEventRule(entity,
                "/test/jmf/module/automation/rules/event/entityHistoryRule.json",
                value1, value2);
    }

    public List<EntityHistory> getHistory(Entity entity) {
        Query q = Query.of(EntityHistory.FQN)
                .withFilters(Filters.eq(EntityHistory.ENTITY, entity));
        return dbService.list(q);
    }
}
