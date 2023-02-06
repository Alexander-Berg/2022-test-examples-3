package ru.yandex.market.jmf.module.automation.test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.automation.AutomationRuleGroup;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RedirectNoPermitionAutomationRuleTest extends AbstractAutomationRuleTest {

    @Inject
    private MockAuthRunnerService authRunnerService;

    @Inject
    private MockSecurityDataService securityDataService;

    static Stream<Arguments> data() {
        return Stream.of(
                arguments(false, false, "/test/jmf/module/automation/rules/event/bigRuleWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/bigRuleWithoutRedirect2.json"),
                arguments(true, false, "/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json",
                        "/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json"),
                arguments(true, true, "/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json",
                        "/test/jmf/module/automation/rules/event/bigRuleWithRedirect2.json"),
                arguments(true, true, "/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json",
                        "/test/jmf/module/automation/rules/event/bigRuleWithoutRedirect.json"),
                arguments(false, true, "/test/jmf/module/automation/rules/event/bigRuleWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json"),
                arguments(false, false, "/test/jmf/module/automation/rules/event/simpleRuleWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/simpleRuleWithoutRedirect2.json"),
                arguments(true, true, "/test/jmf/module/automation/rules/event/simpleRuleWithRedirect.json", "/test" +
                        "/jmf/module/automation/rules/event/simpleRuleWithRedirect2.json"),
                arguments(true, true,
                        "/test/jmf/module/automation/rules/event/ruleWithNegativeConditionAndRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithNegativeConditionWithoutRedirect.json"),
                arguments(false, true,
                        "/test/jmf/module/automation/rules/event/ruleWithNegativeConditionWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithNegativeConditionAndRedirect.json"),
                arguments(false, true,
                        "/test/jmf/module/automation/rules/event/ruleWithPositiveConditionWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithPositiveConditionAndRedirect.json"),
                arguments(true, true, "/test/jmf/module/automation/rules/event/ruleWithConditionAndRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithConditionWithoutRedirect.json"),
                arguments(false, true,
                        "/test/jmf/module/automation/rules/event/ruleWithConditionWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithConditionAndRedirect.json"),
                arguments(true, true, "/test/jmf/module/automation/rules/event/simpleRuleWithRedirect.json",
                        "/test/jmf/module/automation/rules/event/simpleRuleWithoutRedirect.json"),
                arguments(false, true, "/test/jmf/module/automation/rules/event/simpleRuleWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/simpleRuleWithRedirect.json")
        );
    }

    static Stream<Arguments> data2() {
        return Stream.of(
                arguments(true, "/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json", ""),
                arguments(true, "/test/jmf/module/automation/rules/event/simpleRuleWithRedirect.json", ""),
                arguments(false, "/test/jmf/module/automation/rules/event/emptyRuleWithRedirect.json", "")
        );
    }

    @AfterEach
    public void tearDown() {
        setCurrentUserSuperUser(true);
    }

    @BeforeEach
    public void setUp() {
        ouTestUtils.createOu();
        bcpService.create(AutomationRuleGroup.FQN, Map.of(
                AutomationRuleGroup.TITLE, Randoms.string(),
                AutomationRuleGroup.CODE, TEST_GROUP
        ));

        setCurrentUserProfiles(List.of(Profiles.THE_CREATOR_PROFILE_ID));
        setCurrentUserSuperUser(false);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRedirectInAutomationRule(boolean isSuperUser, boolean isAcessDenide, String jsonPath1,
                                             String jsonPath2) {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));
        if (isSuperUser) {
            setCurrentUserSuperUser(true);
        }
        var eventRule = createApprovedEventRule(
                entity1,
                jsonPath1,
                initValue,
                value1
        );

        if (isSuperUser) {
            setCurrentUserSuperUser(false);
        }
        if (isAcessDenide) {
            Assertions.assertThrows(SecurityException.class, () -> {
                editEventRule(
                        eventRule,
                        jsonPath2,
                        initValue,
                        value1
                );
            });
        } else {
            editEventRule(
                    eventRule,
                    jsonPath2,
                    initValue,
                    value1
            );
        }
    }

    @ParameterizedTest
    @MethodSource("data2")
    public void testCreateAutomationRule(boolean isAcessDenide, String jsonPath) {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));
        if (isAcessDenide) {
            Assertions.assertThrows(SecurityException.class, () -> {
                createApprovedEventRule(
                        entity1,
                        jsonPath,
                        initValue,
                        value1
                );
            });
        } else {
            createApprovedEventRule(
                    entity1,
                    jsonPath,
                    initValue,
                    value1
            );
        }
    }

    private void setCurrentUserProfiles(List<String> employeeRoles) {
        securityDataService.setCurrentUserProfiles(employeeRoles);
    }

    private void setCurrentUserSuperUser(boolean isCurrentUserSuperUser) {
        authRunnerService.setCurrentUserSuperUser(isCurrentUserSuperUser);
    }

    interface Profiles {
        String THE_CREATOR_PROFILE_ID = "@user";
    }
}
