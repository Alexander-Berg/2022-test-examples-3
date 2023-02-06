package ru.yandex.market.mbo.gwt.models.rules.conditions;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.rules.JavaModelRuleValidator;
import ru.yandex.market.mbo.db.rules.NashornJsExecutor;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.JavascriptExecutor;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleExecutor;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCaseWith;

public class TestMultiValueCondition extends TestConditionBase {

    public static final String MULTI_STR_PARAM = "str";
    public static final String MULTI_STR_PARAM_NEW = "str_new";
    public static final String MULTI_ENUM_PARAM = "enum";
    public static final String MULTI_ENUM_PARAM_NEW = "enum_new";
    public static final String MULTI_NUMERIC_ENUM_PARAM = "numeric_enum";
    public static final String MULTI_NUMERIC_ENUM_PARAM_NEW = "numeric_enum_new";

    public static final String RULE_NAME = "Rule1";
    public static final String RULE_GROUP = "Test";

    public static final long OPTION_1_1 = 11L;
    public static final long OPTION_1_2 = 12L;
    public static final long OPTION_1_3 = 13L;
    public static final long OPTION_2_1 = 21L;
    public static final long OPTION_2_2 = 22L;
    public static final long OPTION_2_3 = 23L;

    public static final String STR_VAL_1 = "strVal1";
    public static final String STR_VAL_2 = "strVal2";
    public static final String ENUM_VAL_1 = "Opt1";
    public static final String ENUM_VAL_2 = "Opt2";
    public static final String ENUM_VAL_3 = "Opt3";
    public static final long NUMERIC_ENUM_VAL_1 = 111;
    public static final long NUMERIC_ENUM_VAL_2 = 222;
    public static final long NUMERIC_ENUM_VAL_3 = 333;

    private JavascriptExecutor javascriptExecutor;

    private ModelRuleTester tester;

    @Before
    public void setUp() {
        javascriptExecutor = spy(new NashornJsExecutor());
        ModelRuleExecutor executor = new ModelRuleExecutor(JavaModelRuleValidator.INSTANCE, javascriptExecutor);

        // @formatter:off
        tester = testCaseWith(executor)
            .startParameters()
                .startParameter()
                    .xsl(MULTI_STR_PARAM).type(Param.Type.STRING)
                .endParameter()
                .startParameter()
                    .xsl(MULTI_STR_PARAM_NEW).type(Param.Type.STRING)
                .endParameter()
                .startParameter()
                    .xsl(MULTI_ENUM_PARAM).type(Param.Type.ENUM)
                    .multifield(true)
                    .option(OPTION_1_1, ENUM_VAL_1)
                    .option(OPTION_1_2, ENUM_VAL_2)
                    .option(OPTION_1_3, ENUM_VAL_3)
                .endParameter()
                .startParameter()
                    .xsl(MULTI_ENUM_PARAM_NEW).type(Param.Type.ENUM)
                    .multifield(true)
                    .option(OPTION_2_1, ENUM_VAL_1)
                    .option(OPTION_2_2, ENUM_VAL_2)
                    .option(OPTION_2_3, ENUM_VAL_3)
                .endParameter()
                .startParameter()
                    .xsl(MULTI_NUMERIC_ENUM_PARAM).type(Param.Type.NUMERIC_ENUM)
                    .multifield(true)
                    .option(OPTION_1_1, NUMERIC_ENUM_VAL_1)
                    .option(OPTION_1_2, NUMERIC_ENUM_VAL_2)
                    .option(OPTION_1_3, NUMERIC_ENUM_VAL_3)
                .endParameter()
                .startParameter()
                    .xsl(MULTI_NUMERIC_ENUM_PARAM_NEW).type(Param.Type.NUMERIC_ENUM)
                    .multifield(true)
                    .option(OPTION_2_1, NUMERIC_ENUM_VAL_1)
                    .option(OPTION_2_2, NUMERIC_ENUM_VAL_2)
                    .option(OPTION_2_3, NUMERIC_ENUM_VAL_3)
                .endParameter()
            .endParameters();
        // @formatter:on
    }

    @Test
    public void multiStringValueTest() {
        // @formatter:off
        tester
            .startModel()
                .id(1).category(1)
                .param(MULTI_STR_PARAM).setString(STR_VAL_1, STR_VAL_2)
                .param(MULTI_STR_PARAM_NEW).setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name(RULE_NAME).group(RULE_GROUP)
                    ._if()
                        .param(MULTI_STR_PARAM).isNotEmpty()
                    .then()
                        .param(MULTI_STR_PARAM_NEW).javascript("return val('" + MULTI_STR_PARAM + "');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .param(MULTI_STR_PARAM_NEW).valid().modified().string(STR_VAL_1, STR_VAL_2)
                    .stringDomain()
                        .match(STR_VAL_1, STR_VAL_2)
                    .endDomain()
                .endParam()
            .endResults();
        // @formatter:on
        verify(javascriptExecutor, times(1)).init(any());
    }

    @Test
    public void multiEnumValueTest() {
        // @formatter:off
        tester
            .startModel()
                .id(1).category(1)
                .param(MULTI_ENUM_PARAM).setOption(OPTION_1_1)
                .param(MULTI_ENUM_PARAM).setOption(OPTION_1_3)
                .param(MULTI_ENUM_PARAM_NEW).setOption(OPTION_1_2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name(RULE_NAME).group(RULE_GROUP)
                    ._if()
                        .param(MULTI_ENUM_PARAM).isNotEmpty()
                    .then()
                        .param(MULTI_ENUM_PARAM_NEW).javascript("return val('"  + MULTI_ENUM_PARAM + "');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .param(MULTI_ENUM_PARAM_NEW)
                    .enumDomain()
                        .options(OPTION_2_1, OPTION_2_3)
                    .endDomain()
                .endParam()
            .endResults();
        // @formatter:on
        verify(javascriptExecutor, times(1)).init(any());
    }

    @Test
    public void multiNumericEnumValueTest() {
        // @formatter:off
        tester
            .startModel()
                .id(1).category(1)
                .param(MULTI_NUMERIC_ENUM_PARAM).setOption(OPTION_1_1)
                .param(MULTI_NUMERIC_ENUM_PARAM).setOption(OPTION_1_3)
                .param(MULTI_NUMERIC_ENUM_PARAM_NEW).setOption(OPTION_1_2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name(RULE_NAME).group(RULE_GROUP)
                    ._if()
                        .param(MULTI_NUMERIC_ENUM_PARAM).isNotEmpty()
                    .then()
                        .param(MULTI_NUMERIC_ENUM_PARAM_NEW)
                            .javascript("return val('" + MULTI_NUMERIC_ENUM_PARAM + "');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .param(MULTI_NUMERIC_ENUM_PARAM_NEW)
                    .enumDomain()
                        .options(OPTION_2_1, OPTION_2_3)
                    .endDomain()
                .endParam()
            .endResults();
        // @formatter:on
        verify(javascriptExecutor, times(1)).init(any());
    }
}
