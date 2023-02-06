package ru.yandex.market.jmf.module.automation.test;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;

public class ResponsibleGroupAutomationRuleTest extends AbstractAutomationRuleTest {

    @Inject
    private EmployeeTestUtils employeeTestUtils;
    @Inject
    private MockSecurityDataService mockSecurityDataService;

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        mockSecurityDataService.reset();
    }

    @Test
    void shouldThrowValidationExceptionWhileResponsibleGroupNotSpecified() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        Assertions.assertThrows(ValidationException.class, () -> {
            createApprovedActionRule(
                    "test", FQN_1,
                    "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                    Set.of(),
                    Set.of(),
                    initValue,
                    value1
            );
        });
    }

    @Test
    void shouldCreateRuleOnlyWithResponsibleOu() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));
        var ou = ouTestUtils.createOu();

        var rule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                Set.of(),
                Set.of(ou),
                initValue,
                value1
        );
        Assertions.assertTrue(rule.getResponsibleOus().contains(ou));
    }

    @Test
    void currentEmployeeShouldNotOverlapEntered() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));
        var ou = ouTestUtils.createOu();
        var enteredEmployee = employeeTestUtils.createEmployee(ou);
        var currentEmployee = employeeTestUtils.createEmployee(ou);

        mockSecurityDataService.setInitialEmployee(currentEmployee);

        var rule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                Set.of(enteredEmployee),
                Set.of(),
                initValue,
                value1
        );
        Assertions.assertEquals(1, rule.getResponsibleEmployees().size());
        Assertions.assertTrue(rule.getResponsibleEmployees().contains(enteredEmployee));
    }


    @Test
    void shouldSetCurrentEmployee() {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));
        var ou = ouTestUtils.createOu();
        var currentEmployee = employeeTestUtils.createEmployee(ou);

        mockSecurityDataService.setInitialEmployee(currentEmployee);

        var rule = createApprovedActionRule(
                "test", FQN_1,
                "/test/jmf/module/automation/rules/action/changeAttributeValue.json",
                Set.of(),
                Set.of(),
                initValue,
                value1
        );
        Assertions.assertEquals(1, rule.getResponsibleEmployees().size());
        Assertions.assertTrue(rule.getResponsibleEmployees().contains(currentEmployee));
    }
}
