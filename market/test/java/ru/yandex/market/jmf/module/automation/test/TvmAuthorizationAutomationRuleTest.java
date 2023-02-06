package ru.yandex.market.jmf.module.automation.test;

import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.automation.AutomationRuleGroup;
import ru.yandex.market.jmf.module.def.AllowedOutgoingTvmService;
import ru.yandex.misc.lang.StringUtils;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TvmAuthorizationAutomationRuleTest extends AbstractAutomationRuleTest {

    private Entity entity;

    static Stream<Arguments> data() {
        return Stream.of(
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect1.json", true),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect2.json", true),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect3.json", true),
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithTvmAuthorizationCorrect.json", true),
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithTvmAuthorizationIncorrectInFirstLevel" +
                        ".json", false),
                arguments("/test/jmf/module/automation/rules/event" +
                        "/bigRuleWithTvmAuthorizationIncorrectOnlyPositiveBranch.json", false),
                arguments("/test/jmf/module/automation/rules/event/bigRuleWithTvmAuthorizationIncorrectInBranch" +
                        ".json", false),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationIncorrectUrl.json", false),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationIncorrectClientId.json",
                        false)
        );
    }

    static Stream<Arguments> data2() {
        return Stream.of(
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect1.json",
                        "/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect2.json", true),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect1.json",
                        "/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect3.json", true),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect2.json",
                        "/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect3.json", true),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect1.json",
                        "/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationIncorrectUrl.json", false),
                arguments("/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationCorrect1.json",
                        "/test/jmf/module/automation/rules/event/ruleWithTvmAuthorizationIncorrectClientId.json",
                        false)
        );
    }

    @BeforeEach
    public void setUp() {
        ouTestUtils.createOu();
        entity = bcpService.create(AutomationRuleGroup.FQN, Map.of(
                AutomationRuleGroup.TITLE, Randoms.string(),
                AutomationRuleGroup.CODE, TEST_GROUP
        ));
        createAllowedOutgoingTvmService("YANDEX", "http://yandexlocal999.ru", 987L);
        createAllowedOutgoingTvmService("LH", "http://127.0.0.1", 101L);
        createAllowedOutgoingTvmService("OTHER", "http://127.0.0.1", 123L);
        createAllowedOutgoingTvmService("LOCALHOST", "http://localhost", 1234L);
    }

    private void createAllowedOutgoingTvmService(String title, String url, Long clientId) {
        bcpService.create(AllowedOutgoingTvmService.FQN, ImmutableMap.of(
                "title", title,
                "url", url,
                "clientId", clientId));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void createAutomationRule(String jsonPath1, boolean expectSuccess) {
        String initValue = "value0";
        String value1 = "value1";
        Entity entity1 = bcpService.create(FQN_1, Map.of(
                RULE_RESULT_ATTR, initValue
        ));

        if (expectSuccess) {
            createApprovedEventRule(entity1,
                    jsonPath1,
                    initValue,
                    value1
            );
        } else {
            Assertions.assertThrows(ValidationException.class, () -> {
                createApprovedEventRule(entity1,
                        jsonPath1,
                        initValue,
                        value1
                );
            });
        }
    }

    @ParameterizedTest
    @MethodSource("data2")
    public void editAutomationRule(String jsonPath1, String jsonPath2, boolean expectSuccess) {
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

        if (expectSuccess) {
            if (StringUtils.isNotEmpty(jsonPath2)) {
                editEventRule(
                        eventRule,
                        jsonPath2,
                        initValue,
                        value1
                );
            }
        } else {
            Assertions.assertThrows(ValidationException.class, () -> {
                editEventRule(
                        eventRule,
                        jsonPath2,
                        initValue,
                        value1
                );
            });
        }
    }

}
