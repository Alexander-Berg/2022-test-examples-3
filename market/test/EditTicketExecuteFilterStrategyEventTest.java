package ru.yandex.market.jmf.module.ticket.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.automation.EventAutomationRule;
import ru.yandex.market.jmf.module.automation.impl.AutomationRuleContext;
import ru.yandex.market.jmf.module.automation.impl.AutomationRuleContextConfiguration;
import ru.yandex.market.jmf.module.automation.impl.AutomationRuleContextProvider;
import ru.yandex.market.jmf.module.automation.impl.config.AutomationRuleItem;
import ru.yandex.market.jmf.module.automation.impl.config.condition.AutomationRuleCondition;
import ru.yandex.market.jmf.module.automation.test.AbstractAutomationRuleTest;
import ru.yandex.market.jmf.module.ticket.impl.EditTicketExecuteFilterStrategyEvent;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@ContextConfiguration(classes = EditTicketExecuteFilterStrategyEventTest.TestConfiguration.class)
public class EditTicketExecuteFilterStrategyEventTest extends AbstractAutomationRuleTest {
    private final AutomationRuleContextConfiguration defaultConfiguration =
            AutomationRuleContextConfiguration.defaultConfiguration();

    @Inject
    EditTicketExecuteFilterStrategyEvent checkerStrategy;
    @Inject
    AutomationRuleContextProvider automationRuleContextProvider;

    @Test
    void shouldAllowRule() {
        Entity entity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_2, Randoms.string()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeStringEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity),
                defaultConfiguration);

        Assertions.assertTrue(checkerStrategy.shouldAllowRule(rule, context));
    }

    @Test
    void shouldAllowRuleCondition_stringAttr_newEntity() {
        Entity entity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_2, Randoms.string()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeStringEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertTrue(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    @Test
    void shouldAllowRuleCondition_stringAttr_changed_skipOtherFilter() {
        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, Randoms.string()
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_2, Randoms.string()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributesStringEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        Assertions.assertFalse(checkerStrategy.shouldAllowRuleCondition(rule, getConditions(rule).get(0), context));
        Assertions.assertTrue(checkerStrategy.shouldAllowRuleCondition(rule, getConditions(rule).get(1), context));
    }

    @Test
    void shouldAllowRuleCondition_stringAttr_notChanged_skipOtherFilter() {
        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, Randoms.string(),
                INT_ATTR, Randoms.intValue()
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, oldEntity.getAttribute(RULE_CONDITION_ATTR_1),
                INT_ATTR, Randoms.intValue()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeStringEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertFalse(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    @Test
    void shouldAllowRuleCondition_stringAttr_changed() {
        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, Randoms.string()
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_2, Randoms.string()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeStringEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertTrue(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    @Test
    void shouldAllowRuleCondition_stringAttr_notChanged() {
        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, Randoms.string(),
                INT_ATTR, Randoms.intValue()
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, oldEntity.getAttribute(RULE_CONDITION_ATTR_1),
                INT_ATTR, Randoms.intValue()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeStringEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertFalse(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    @Test
    void shouldAllowRuleCondition_objectAttr_changed() {
        Entity oldObject = bcpService.create(FQN_1, Map.of());
        Entity newObject = bcpService.create(FQN_1, Map.of());
        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, oldObject
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, newObject
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeObjectEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertTrue(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    @Test
    void shouldAllowRuleCondition_objectAttr_notChanged() {
        Entity object = bcpService.create(FQN_1, Map.of());
        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, object
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, object,
                INT_ATTR, Randoms.intValue()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeObjectEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertFalse(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    @Test
    void shouldAllowRuleCondition_objectAttrOfObjectAttr_changed() {
        Entity oldObject = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, Randoms.string()
        ));
        Entity newObject = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, Randoms.string()
        ));
        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, oldObject
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, newObject
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeObjectAttributeEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertTrue(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    @Test
    void shouldAllowRuleCondition_objectAttrOfObjectAttr_notChanged() {
        Entity object = bcpService.create(FQN_1, Map.of(
                RULE_CONDITION_ATTR_1, Randoms.string()
        ));

        Entity oldEntity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, object
        ));
        Entity entity = bcpService.create(FQN_1, Map.of(
                OBJECT_ATTR, object,
                INT_ATTR, Randoms.intValue()
        ));

        EventAutomationRule rule = createApprovedEventRule(
                entity, "/automation_rules/attributeObjectAttributeEqCondition.json");

        AutomationRuleContext context = automationRuleContextProvider.get(
                Map.of("ticket", entity, "oldTicket", oldEntity),
                defaultConfiguration);

        for (AutomationRuleCondition condition : getConditions(rule)) {
            Assertions.assertFalse(checkerStrategy.shouldAllowRuleCondition(rule, condition, context));
        }
    }

    private List<AutomationRuleCondition> getConditions(EventAutomationRule rule) {
        List<AutomationRuleCondition> conditions = rule.getConfig().getRules().stream()
                .map(AutomationRuleItem::getConditions)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        Assertions.assertFalse(conditions.isEmpty());
        return conditions;
    }

    @Import(ModuleTicketTestConfiguration.class)
    @Configuration
    public static class TestConfiguration extends AbstractModuleConfiguration {
        protected TestConfiguration() {
            super("automation_rules");
        }
    }
}
