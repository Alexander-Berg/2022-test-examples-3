package ru.yandex.direct.core.entity.dynamictextadtarget.repository;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultRules;

@CoreTest
public class DynamicTextAdTargetRulesMappingTest {
    private final String defaultJsonCondition = "[{\"type\":\"any\"}]";
    private final String jsonCondition =
            "[{\"kind\":\"equals\",\"type\":\"URL_prodlist\",\"value\":[\"http://ya.ru/contact/\",\"http://ya.ru/\"]}]";

    @Test
    public void defaultRulesToJson() {
        List<WebpageRule> rules = defaultRules();
        String jsonString = DynamicTextAdTargetMapping.webpageRulesToJson(rules);
        assertThat(jsonString, equalTo(defaultJsonCondition));
    }

    @Test
    public void customRulesToJson() {
        WebpageRule rule = new WebpageRule()
                .withKind(WebpageRuleKind.EQUALS)
                .withType(WebpageRuleType.URL_PRODLIST)
                .withValue(ImmutableList.of("http://ya.ru/contact/", "http://ya.ru/"));

        List<WebpageRule> rules = Collections.singletonList(rule);
        String jsonString = DynamicTextAdTargetMapping.webpageRulesToJson(rules);
        assertThat(jsonString, equalTo(jsonCondition));
    }

    @Test
    public void defaultRulesFromJson() {
        List<WebpageRule> rules = defaultRules();

        List<WebpageRule> actualRules = DynamicTextAdTargetMapping.webpageRulesFromJson(defaultJsonCondition);
        assertThat("rule соответствует ожиданиям", rules, beanDiffer(actualRules).useCompareStrategy(
                DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void customRulesFromJson() {
        WebpageRule rule = new WebpageRule()
                .withKind(WebpageRuleKind.EQUALS)
                .withType(WebpageRuleType.URL_PRODLIST)
                .withValue(ImmutableList.of("http://ya.ru/contact/", "http://ya.ru/"));

        List<WebpageRule> rules = Collections.singletonList(rule);

        List<WebpageRule> actualRules = DynamicTextAdTargetMapping.webpageRulesFromJson(jsonCondition);
        assertThat("rule соответствует ожиданиям", rules, beanDiffer(actualRules).useCompareStrategy(
                DefaultCompareStrategies.onlyExpectedFields()));
    }
}
