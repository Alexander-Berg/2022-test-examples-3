package ru.yandex.market.mbo.gwt.models.rules;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.mbo.db.rules.JavaModelRuleValidator;
import ru.yandex.market.mbo.db.rules.NashornJsExecutor;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.SubType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCaseWith;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 27.08.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelRuleExecutorTest {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    private JavascriptExecutor javascriptExecutor;

    private ModelRuleTester tester;
    private ModelRuleExecutor executor;

    @Before
    public void setUp() {
        javascriptExecutor = spy(new NashornJsExecutor());
        executor = new ModelRuleExecutor(spy(JavaModelRuleValidator.INSTANCE), javascriptExecutor);

        // @formatter:off
        tester = testCaseWith(executor)
            .startParameters()
            .startParameter()
            .xsl("mdm-parameter").type(Param.Type.STRING)
            .mdmParameter(true)
            .endParameter()
            .startParameter()
            .xsl("description").type(Param.Type.STRING)
            .endParameter()
            .startParameter()
            .xsl("size").type(Param.Type.NUMERIC)
            .endParameter()
            .startParameter()
            .xsl("count").type(Param.Type.NUMERIC)
            .endParameter()
            .startParameter()
            .xsl("valid").type(Param.Type.BOOLEAN)
            .option(1L, "true")
            .option(2L, "false")
            .endParameter()
            .startParameter()
            .xslAndName("enum")
            .type(Param.Type.ENUM)
            .subType(SubType.NOT_DEFINED)
            .level(CategoryParam.Level.MODEL)
            .endParameter()
            .endParameters();
        // @formatter:on
    }

    @Test
    public void dontCallInitIfNoJavascript() {
        // @formatter:off
        tester.startModel()
            .id(1).category(1)
            .param("description").setString("cool")
            .param("size").setEmpty()
            .endModel()
            .startRuleSet()
            .id(1)
            .startRule()
            .name("First match").group("Test")
            ._if()
            .param("description").matchesString("cool")
            .then()
            .param("size").matchesNumeric(1008)
            .endRule()
            .endRuleSet()
            .doInference()
            .results()
            .param("size").valid().modified().numeric(1008).numericDomain().single(1008).endDomain().endParam()
            .endResults();
        // @formatter:on
        verify(javascriptExecutor, never()).init(any());
    }

    @Test
    public void setParamIfItDoesnotExist() {
        // @formatter:off
        tester.startModel()
            .id(1).category(1)
            .param("description").setString("cool")
            .endModel()
            .startRuleSet()
            .id(1)
            .startRule()
            .name("First match").group("Test")
            ._if()
            .param("description").matchesString("cool")
            .then()
            .param("size").matchesNumeric(1008)
            .endRule()
            .endRuleSet()
            .doInference()
            .results()
            .param("size").valid().modified().numeric(1008).numericDomain().single(1008).endDomain().endParam()
            .endResults();
        // @formatter:on
    }


    @Test
    public void dontSetMdmParam() {
        // @formatter:off
        tester.startModel()
            .id(1).category(1)
            .param("description").setString("cool")
            .param("mdm-parameter").setString("cool")
            .endModel()
            .startRuleSet()
            .id(1)
            .startRule()
            .name("First match").group("Test")
            ._if()
            .param("description").matchesString("cool")
            .then()
            .param("mdm-parameter").matchesString("bad")
            .endRule()
            .endRuleSet()
            .doInference()
            .results()
            .param("mdm-parameter").invalid().endParam()
            .endResults();
        // @formatter:on
    }

    @Test(expected = ru.yandex.market.mbo.gwt.models.rules.ModelRuleException.class)
    public void validateMatchesEnumParam() {
        // @formatter:off
        tester.startModel()
            .id(1).category(1)
            .param("description").setString("cool")
            .param("enum").setOption(1L)
            .endModel()
            .startRuleSet()
            .id(1)
            .startRule()
            .name("First match").group("Test")
            ._if()
            .param("description").matchesString("cool")
            .then()
            .param("enum").matchesEnum(2L, 3L)
            .endRule()
            .endRuleSet()
            .doInference();
        // @formatter:on
    }


    @Test
    public void validateMatchesEnumParamPartnerModel() {
        // @formatter:off
        tester.startModel()
            .id(1).category(1)
            .param("description").setString("cool")
            .param("count").setNumeric(111)
            .param("enum").setOption(1L)
            .param("valid").setBoolean(false)
            .source(CommonModel.Source.PARTNER)
            .currentType(CommonModel.Source.GURU)
            .endModel()
            .startRuleSet()
            .id(1)
            .startRule()
            .name("First match").group("Test")
            .allowedApplyToPSKU(true)
            ._if()
            .param("description").matchesString("cool")
            .then()
            .param("enum").matchesEnum(2L, 3L)
            .param("valid").javascriptValidation("return true;")
            .param("count").matchesNumeric(222)
            .endRule()
            .endRuleSet()
            .doInference()
            .results()
            .param("enum").valid().modified().isEmpty().endParam()
            .noParam("valid")
            .param("count").valid().modified().numeric(222).numericDomain().single(222).endDomain().endParam()
            .endResults();
        // @formatter:on
    }


    @Test
    public void callJavascriptInitOnce() {
        // @formatter:off
        tester.startModel()
            .id(1).category(1)
            .param("description").setString("cool")
            .param("size").setEmpty()
            .endModel()
            .startRuleSet()
            .id(1)
            .startRule()
            .name("First match").group("Test")
            ._if()
            .param("description").javascript("return val('description')[0] === 'cool';")
            .then()
            .param("size").javascript("return 1003;")
            .endRule()
            .endRuleSet()
            .doInference()
            .results()
            .param("size").valid().modified().numeric(1003).numericDomain().single(1003).endDomain().endParam()
            .endResults();
        // @formatter:on
        verify(javascriptExecutor, times(1)).init(any());
    }

    @Test
    public void callJavaModelRuleValidatorOnce() {
        // @formatter:off
        tester.startModel()
            .id(1).category(1)
            .param("description").setString("cool")
            .param("size").setEmpty()
            .endModel()
            .startModel()
            .id(2).category(1)
            .param("description").setString("cool2")
            .param("size").setEmpty()
            .endModel()
            .startModel()
            .id(3).category(1)
            .param("description").setString("cool3")
            .param("size").setEmpty()
            .endModel()
            .startRuleSet()
            .id(1)
            .startRule()
            .name("First match").group("Test")
            ._if()
            .param("description").javascript("return val('description')[0] === 'cool';")
            .then()
            .param("size").javascript("return 1003;")
            .endRule()
            .endRuleSet()
            .doInference()
            .selectModel(2)
            .doInference()
            .selectModel(3)
            .doInference()
            .results()
            .endResults();
        // @formatter:on
        verify(executor.getValidator(), times(1)).fullRuleValidation(any(), any());
    }

}

