package ru.yandex.direct.core.entity.abt.service;

import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UaasConditionEvaluatorTest {

    @Test
    public void evaluateNullParamsTest() {
        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        var result = uaasConditionEvaluator.evaluate("interfaceLang != 'en'", null);
        assertThat(result).isFalse();
    }

    @Test
    public void evaluateInvalidConditionTest() {
        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        var result = uaasConditionEvaluator.evaluate("invalidCondition",
                new UaasConditionParams().withClientCreateDate("2020-01-01").withInterfaceLang("ru"));
        assertThat(result).isFalse();
    }

    @Test
    public void evaluateGoodConditionsTest() {
        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        Boolean result;
        result = uaasConditionEvaluator.evaluate("clientCreateDate > '2020-01-01'",
                new UaasConditionParams().withClientCreateDate("2020-01-02").withInterfaceLang("ru"));
        assertThat(result).isTrue();

        result = uaasConditionEvaluator.evaluate("clientCreateDate > '2020-01-01'",
                new UaasConditionParams().withClientCreateDate("2020-01-02"));
        assertThat(result).isTrue();

        result = uaasConditionEvaluator.evaluate("clientCreateDate > '2020-01-01'",
                new UaasConditionParams().withClientCreateDate("2020-01-02").withInterfaceLang(null));
        assertThat(result).isTrue();

        result = uaasConditionEvaluator.evaluate("clientCreateDate >= '2020-01-01'",
                new UaasConditionParams().withClientCreateDate("2020-01-01").withInterfaceLang("ru"));
        assertThat(result).isTrue();

        result = uaasConditionEvaluator.evaluate("clientCreateDate < '2020-01-01'",
                new UaasConditionParams().withClientCreateDate("2020-01-02").withInterfaceLang("ru"));
        assertThat(result).isFalse();

        result = uaasConditionEvaluator.evaluate("clientCreateDate > '2020-01-01' && interfaceLang == 'ru'",
                new UaasConditionParams().withClientCreateDate("2020-01-02").withInterfaceLang("ru"));
        assertThat(result).isTrue();

        result = uaasConditionEvaluator.evaluate("clientCreateDate > '2020-01-01' && interfaceLang == 'ru'",
                new UaasConditionParams().withClientCreateDate("2020-01-02"));
        assertThat(result).isFalse();

        result = uaasConditionEvaluator.evaluate("clientCreateDate > '2020-01-01' || interfaceLang == 'ru'",
                new UaasConditionParams().withClientCreateDate("2020-01-02"));
        assertThat(result).isTrue();

        result = uaasConditionEvaluator.evaluate("clientCreateDate > '2020-01-01' && interfaceLang != 'ru'",
                new UaasConditionParams().withClientCreateDate("2020-01-02").withInterfaceLang("ru"));
        assertThat(result).isFalse();

        result = uaasConditionEvaluator.evaluate("isAgencyClient", new UaasConditionParams().withIsAgencyClient(true));
        assertThat(result).isTrue();

        result = uaasConditionEvaluator.evaluate("isAgencyClient", new UaasConditionParams().withIsAgencyClient(false));
        assertThat(result).isFalse();
    }

    @Test
    public void evaluate_trySetPropertyTest() {
        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        Boolean result;
        UaasConditionParams uaasConditionParams = new UaasConditionParams().withInterfaceLang("en");
        result = uaasConditionEvaluator.evaluate("interfaceLang = 'ru'", uaasConditionParams);
        assertThat(result).isFalse();
        assertThat(uaasConditionParams.getInterfaceLang()).isEqualTo("en");
    }

    @Test
    public void evaluate_trySetContainsMethodWithEnabledFeature_expressionReturnsTrue() {
        String enabledFeature = "enabled_feature";
        String espression = String.format("manuallyEnabledFeatures.contains(\"%s\")", enabledFeature);

        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        Boolean result;
        UaasConditionParams uaasConditionParams = new UaasConditionParams()
                .withManuallyEnabledFeatures(Set.of(enabledFeature));
        result = uaasConditionEvaluator.evaluate(espression, uaasConditionParams);
        assertThat(result).isTrue();
    }

    @Test
    public void evaluate_trySetContainsMethodWithNotEnabledFeature_expressionReturnsFalse() {
        String enabledFeature = "enabled_feature";
        String notEnabledFeature = "not_enabled_feature";
        String espression = String.format("manuallyEnabledFeatures.contains(\"%s\")", notEnabledFeature);

        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        Boolean result;
        UaasConditionParams uaasConditionParams = new UaasConditionParams()
                .withManuallyEnabledFeatures(Set.of(enabledFeature));
        result = uaasConditionEvaluator.evaluate(espression, uaasConditionParams);
        assertThat(result).isFalse();
    }

    @Test
    public void evaluate_trySetContainsMethodWithNullSet_expressionReturnsFalse() {
        String enabledFeature = "enabled_feature";
        String espression = String.format("manuallyEnabledFeatures.contains(\"%s\")", enabledFeature);

        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        Boolean result;
        UaasConditionParams uaasConditionParams = new UaasConditionParams()
                .withManuallyEnabledFeatures(null);
        result = uaasConditionEvaluator.evaluate(espression, uaasConditionParams);
        assertThat(result).isFalse();
    }

    @Test
    public void evaluate_tryWrongMethodWithSet_expressionReturnsFalse() {
        String enabledFeature = "enabled_feature";
        String espression = String.format("manuallyEnabledFeatures.CONTAIN(\"%s\")", enabledFeature);

        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        Boolean result;
        UaasConditionParams uaasConditionParams = new UaasConditionParams()
                .withManuallyEnabledFeatures(Set.of());
        result = uaasConditionEvaluator.evaluate(espression, uaasConditionParams);
        assertThat(result).isFalse();
    }

    @Test
    public void evaluate_tryForbiddenMethodWithSet_expressionReturnsFalse() {
        String enabledFeature = "enabled_feature";
        String espression = "manuallyEnabledFeatures.size() == 1";

        UaasConditionEvaluator uaasConditionEvaluator = new UaasConditionEvaluator();
        Boolean result;
        UaasConditionParams uaasConditionParams = new UaasConditionParams()
                .withManuallyEnabledFeatures(Set.of(enabledFeature));
        result = uaasConditionEvaluator.evaluate(espression, uaasConditionParams);
        assertThat(result).isFalse();
    }
}
