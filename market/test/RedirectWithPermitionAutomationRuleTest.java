package ru.yandex.market.jmf.module.automation.test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.automation.AutomationRuleGroup;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.misc.lang.StringUtils;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RedirectWithPermitionAutomationRuleTest extends AbstractAutomationRuleTest {

    @Inject
    private MockAuthRunnerService authRunnerService;

    @Inject
    private MockSecurityDataService securityDataService;

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

    @AfterEach
    public void tearDown() {
        setCurrentUserSuperUser(true);
    }

    static Stream<Arguments> data() {
        return Stream.of(
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithoutRedirect.json", "/test/jmf/module" +
                        "/automation/rules/event/bigRuleWithoutRedirect2.json"),
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json", "/test/jmf/module" +
                        "/automation/rules/event/bigRuleWithRedirect2.json"),
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json",
                        "/test/jmf/module/automation/rules/event/bigRuleWithoutRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithoutRedirect.json", "/test/jmf/module" +
                        "/automation/rules/event/bigRuleWithRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithRedirect.json", ""),
                arguments("/test/jmf/module/automation/rules/event/simpleRuleWithoutRedirect.json", "/test/jmf/module" +
                        "/automation/rules/event/simpleRuleWithoutRedirect2.json"),
                arguments("/test/jmf/module/automation/rules/event/simpleRuleWithRedirect.json", "/test/jmf/module" +
                        "/automation/rules/event/simpleRuleWithRedirect2.json"),
                arguments("/test/jmf/module/automation/rules/event/ruleWithNegativeConditionAndRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithNegativeConditionWithoutRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/ruleWithNegativeConditionWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithNegativeConditionAndRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/ruleWithPositiveConditionWithoutRedirect.json",
                        "/test/jmf/module/automation/rules/event/ruleWithPositiveConditionAndRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/ruleWithConditionAndRedirect.json", "/test/jmf" +
                        "/module/automation/rules/event/ruleWithConditionWithoutRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/ruleWithConditionWithoutRedirect.json", "/test/jmf" +
                        "/module/automation/rules/event/ruleWithConditionAndRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/simpleRuleWithRedirect.json",
                        "/test/jmf/module/automation/rules/event/simpleRuleWithoutRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/simpleRuleWithoutRedirect.json", "/test/jmf/module" +
                        "/automation/rules/event/simpleRuleWithRedirect.json"),
                arguments("/test/jmf/module/automation/rules/event/simpleRuleWithRedirect.json", ""),
                arguments("/test/jmf/module/automation/rules/event/emptyRuleWithRedirect.json", "")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRedirectInAutomationRule(String jsonPath1,  String jsonPath2) {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        var eventRule = createApprovedEventRule(
                entity1,
                jsonPath1,
                initValue,
                value1
        );

        if(StringUtils.isNotEmpty(jsonPath2)) {
            editEventRule(
                    eventRule,
                    jsonPath2,
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
        String THE_CREATOR_PROFILE_ID = "@admin";
    }
}
