package ru.yandex.direct.api.v5.entity.dynamictextadtargets.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.dynamictextadtargets.StringConditionOperatorEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class DynamicTextAdTargetRuleOperatorConverterTest {

    @Parameterized.Parameter
    public WebpageRuleKind ruleOperator;

    @Parameterized.Parameter(1)
    public StringConditionOperatorEnum operator;

    @Autowired
    private TranslationService translationService;

    @Parameterized.Parameters(name = "from {0} to {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {WebpageRuleKind.EXACT, StringConditionOperatorEnum.CONTAINS_ANY},
                {WebpageRuleKind.NOT_EXACT, StringConditionOperatorEnum.NOT_CONTAINS_ALL},
                {WebpageRuleKind.EQUALS, StringConditionOperatorEnum.EQUALS_ANY},
                {WebpageRuleKind.NOT_EQUALS, StringConditionOperatorEnum.NOT_EQUALS_ALL}
        };
    }

    @Test
    public void test() {
        assertThat(GetResponseWebpageRuleConverter.convertRuleOperator(ruleOperator)).isEqualTo(operator);
    }
}
