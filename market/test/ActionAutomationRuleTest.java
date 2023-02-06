package ru.yandex.market.jmf.module.automation.test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.module.automation.ActionAutomationRule;
import ru.yandex.market.jmf.module.automation.AutomationRule;
import ru.yandex.market.jmf.module.automation.AutomationRulesService;
import ru.yandex.market.jmf.module.entity.snapshot.SnapshottedByStatusLogic;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.security.SecurityProfileService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActionAutomationRuleTest extends AbstractAutomationRuleTest {
    @Inject
    private AutomationRulesService automationRulesService;

    @Inject
    private EntityStorageService entityStorageService;

    @Inject
    private EmployeeTestUtils employeeTestUtils;

    @Inject
    private SecurityProfileService securityProfileService;


    @Test
    public void testAvailableRulesAreVisible() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        var actionRule1 = createDraftActionRule(
                "testDraft", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );

        var actionRule2 = createApprovedActionRule(
                "testApproved", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );

        var actionRule3 = createDraftActionRule(
                "testActive", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );
        bcpService.edit(
                actionRule3,
                Map.of(HasWorkflow.STATUS, AutomationRule.Statuses.ACTIVE),
                Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
        );

        var availableRules = automationRulesService.getAvailableActionAutomationRules(entity1)
                .map(ActionAutomationRule::getActionTitle)
                .collect(Collectors.toSet());

        Assertions.assertEquals(2, availableRules.size());
        Assertions.assertEquals(Set.of("testApproved", "testActive"), availableRules);
    }

    @Test
    public void testUnavailableRulesAreNotVisible_byCondition() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of());

        var actionRule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );

        var availableRules = automationRulesService.getAvailableActionAutomationRules(entity1)
                .collect(Collectors.toList());

        Assertions.assertEquals(0, availableRules.size());
    }

    @Test
    public void testAvailableRulesFromSnapshot() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        var actionRule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );
        actionRule = editActionRuleCondition(actionRule,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.condition.json",
                Randoms.string());

        var availableActions = automationRulesService.getAvailableActionAutomationRules(entity1)
                .map(ActionAutomationRule::getActionTitle)
                .collect(Collectors.toList());

        Assertions.assertEquals(1, availableActions.size());
        Assertions.assertEquals("test", availableActions.get(0));

        actionRule = bcpService.edit(actionRule, Map.of(HasWorkflow.STATUS, SnapshottedByStatusLogic.Statuses.REVIEW));
        availableActions = automationRulesService.getAvailableActionAutomationRules(entity1)
                .map(ActionAutomationRule::getActionTitle)
                .collect(Collectors.toList());

        Assertions.assertEquals(1, availableActions.size());
        Assertions.assertEquals("test", availableActions.get(0));

        actionRule = bcpService.edit(actionRule, Map.of(HasWorkflow.STATUS,
                SnapshottedByStatusLogic.Statuses.APPROVED));
        var availableRules = automationRulesService.getAvailableActionAutomationRules(entity1)
                .collect(Collectors.toList());

        Assertions.assertEquals(0, availableRules.size());
    }

    @Test
    public void testUnavailableRulesAreNotVisible_byAvailableMetaclass() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_2, Map.of());

        var actionRule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );

        var availableRules = automationRulesService.getAvailableActionAutomationRules(entity1)
                .collect(Collectors.toList());

        Assertions.assertEquals(0, availableRules.size());
    }

    @Test
    public void testRuleExecution() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        var actionRule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );

        bcpService.edit(entity1, "@executeActionAutomationRule", Map.of(
                "actionTitle", actionRule.getActionTitle()
        ));

        Assertions.assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));
    }

    @ParameterizedTest(name = "user with hardcoded profile {0} must be {2}")
    @CsvSource({
            "@user,value0,AdminWithRelativeRole",
            "@admin,value1,AdminWithRelativeRole",
            "@user,value1,AdminOrActiveEmployee",
            "@admin,value1,AdminOrActiveEmployee"
    })
    public void testRuleExecutionWithProfileRulePredicate(String profileCode, String expected, String ruleSemantic) {
        // Устанавливаем сотрудника, который будет выполнять правило автоматизации:
        var ou = entityStorageService.list(Query.of(Ou.FQN)).get(0);
        securityDataService.setCurrentEmployee(employeeTestUtils.createEmployee("test %s".formatted(profileCode), ou));
        securityDataService.setCurrentUserProfile(profileCode);
        var currentEmployeeFqn = securityDataService.getCurrentEmployee().getFqn();
        Assertions.assertTrue(securityProfileService.hasProfile(currentEmployeeFqn, profileCode));

        String initValue = "value0";
        String finishValue = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));
        var actionRule = createApprovedActionRule(
                "test_for_%s".formatted(profileCode), FQN_1,
                "/test/jmf/module/automation/rules/action/%s.json".formatted("changeAttributeValueFor" + ruleSemantic),
                initValue,
                finishValue
        );
        bcpService.edit(entity1, "@executeActionAutomationRule", Map.of(
                "actionTitle", actionRule.getActionTitle()
        ));

        Assertions.assertEquals(expected, entity1.getAttribute(RULE_RESULT_ATTR));
    }

    @Test
    public void testSnapshotRuleExecution() {
        String initValue = "value0";
        String value1 = "value1";
        String value2 = "value2";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        var actionRule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );
        editActionRule(actionRule, "/test/jmf/module/automation/rules/action/changeAttributeValue.json", initValue,
                value2);

        bcpService.edit(entity1, Map.of(RULE_RESULT_ATTR, initValue));
        bcpService.edit(entity1, "@executeActionAutomationRule", Map.of(
                "actionTitle", actionRule.getActionTitle()
        ));

        Assertions.assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));

        actionRule = bcpService.edit(actionRule, Map.of(HasWorkflow.STATUS, SnapshottedByStatusLogic.Statuses.REVIEW));
        bcpService.edit(entity1, Map.of(RULE_RESULT_ATTR, initValue));
        bcpService.edit(entity1, "@executeActionAutomationRule", Map.of(
                "actionTitle", actionRule.getActionTitle()
        ));

        Assertions.assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));

        actionRule = bcpService.edit(actionRule, Map.of(HasWorkflow.STATUS,
                SnapshottedByStatusLogic.Statuses.APPROVED));
        bcpService.edit(entity1, Map.of(RULE_RESULT_ATTR, initValue));
        bcpService.edit(entity1, "@executeActionAutomationRule", Map.of(
                "actionTitle", actionRule.getActionTitle()
        ));

        Assertions.assertEquals(value2, entity1.getAttribute(RULE_RESULT_ATTR));
    }

    @Test
    public void testActiveRuleExecution() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        var actionRule = createDraftActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );
        bcpService.edit(actionRule,
                Map.of(AutomationRule.STATUS, AutomationRule.Statuses.ACTIVE),
                Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
        );

        bcpService.edit(entity1, "@executeActionAutomationRule", Map.of(
                "actionTitle", actionRule.getActionTitle()
        ));

        Assertions.assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));
    }

    @Test
    public void testSnapshotActiveRuleExecution() {
        String initValue = "value0";
        String value1 = "value1";
        String value2 = "value2";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        var actionRule = createDraftActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                initValue,
                value1
        );
        bcpService.edit(actionRule,
                Map.of(AutomationRule.STATUS, AutomationRule.Statuses.ACTIVE),
                Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
        );
        editActionRule(actionRule, "/test/jmf/module/automation/rules/action/changeAttributeValue.json", initValue,
                value2);

        assertEquals(SnapshottedByStatusLogic.Statuses.DRAFT, actionRule.getStatus());

        bcpService.edit(entity1, "@executeActionAutomationRule", Map.of(
                "actionTitle", actionRule.getActionTitle()
        ));

        Assertions.assertEquals(value1, entity1.getAttribute(RULE_RESULT_ATTR));
    }
}

