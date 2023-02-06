package ru.yandex.direct.api.v5.entity.dynamictextadtargets.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.dynamictextadtargets.WebpageConditionOperandEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class DynamicTextAdTargetRuleOperandConverterTest {

    @Parameterized.Parameter
    public WebpageRuleType ruleOperand;

    @Parameterized.Parameter(1)
    public WebpageConditionOperandEnum operand;

    @Parameterized.Parameters(name = "from {0} to {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {WebpageRuleType.URL, WebpageConditionOperandEnum.URL},
                {WebpageRuleType.URL_PRODLIST, WebpageConditionOperandEnum.OFFERS_LIST_URL},
                {WebpageRuleType.CONTENT, WebpageConditionOperandEnum.PAGE_CONTENT},
                {WebpageRuleType.DOMAIN, WebpageConditionOperandEnum.DOMAIN},
                {WebpageRuleType.TITLE, WebpageConditionOperandEnum.PAGE_TITLE}
        };
    }

    @Test
    public void test() {
        assertThat(GetResponseWebpageRuleConverter.convertRuleOperand(ruleOperand)).isEqualTo(operand);
    }
}
