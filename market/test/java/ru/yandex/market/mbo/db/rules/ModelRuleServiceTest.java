package ru.yandex.market.mbo.db.rules;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleException;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.EMPTY;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.NOT_EMPTY;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType.IF;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType.THEN;

/**
 * @author ayratgdl
 * @since 25.10.18
 */
public class ModelRuleServiceTest {
    private static final long RULE_SET_ID_1 = 101;
    private static final long PARAMETER_ID_1 = 201;
    private static final long CATEGORY_ID_1 = 301;
    private static final String PARAMETER_XSL_NAME = "PARAMETER_XSL_NAME";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ModelRuleService ruleService;

    private ParameterLoaderServiceStub parameterLoaderService = new ParameterLoaderServiceStub();

    @Before
    public void setUp() throws Exception {
        ruleService = new ModelRuleService(new ModelRuleDAOStub(), parameterLoaderService);

        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(PARAMETER_ID_1, PARAMETER_XSL_NAME)
            .setCategoryHid(CATEGORY_ID_1)
            .setName("My-parameter in category 1")
            .setSkuParameterMode(SkuParameterMode.SKU_NONE)
            .build());
    }

    // region ===== Tests for getModelRuleSet and saveModelRuleSet methods =====
    @Test
    public void ifRuleContainsWrongRuleSetIdThenThrowException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("ModelRule.ruleSetId doesn't match corresponding ModelRuleSet.id");

        ModelRuleSet ruleSet = new ModelRuleSet();
        ModelRule rule = new ModelRule();
        rule.setRuleSetId(RULE_SET_ID_1);
        ruleSet.getRules().add(rule);

        ruleService.saveModelRuleSet(ruleSet);

    }

    @Test
    public void ifRuleDoesNotContainNameThenThrowException() {
        thrown.expect(ModelRuleException.class);
        thrown.expectMessage("Необходимо указать название правила");

        ModelRuleSet ruleSet = new ModelRuleSet();
        ModelRule rule = new ModelRule();
        ruleSet.getRules().add(rule);

        ruleService.saveModelRuleSet(ruleSet);
    }

    @Test
    public void ifRuleSetIsNotEditableThenThrowException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Unable to modify read-only model rule set");

        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(CATEGORY_ID_1);
        ruleSet.setEditable(false);
        ruleSet.getRules().add(buildExampleRule());

        ruleService.saveModelRuleSet(ruleSet);
        ruleService.saveModelRuleSet(ruleSet);
    }

    @Test
    public void saveAndGetRuleSet() {
        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(CATEGORY_ID_1);
        ruleSet.getRules().add(buildExampleRule());
        ruleService.saveModelRuleSet(ruleSet);

        ModelRuleSet savedRuleSet = ruleService.getModelRuleSet(ruleSet.getId());
        Assert.assertEquals(ruleSet, savedRuleSet);
    }
    // endregion

    // region ===== Tests for getCategoryRuleSet methods =====
    @Test
    public void getCategoryRuleSetForCategoryWithoutRuleSet() {
        ModelRuleSet expectedRuleSet = new ModelRuleSet();
        expectedRuleSet.setCategoryId(CATEGORY_ID_1);

        ModelRuleSet ruleSet = ruleService.getCategoryRuleSet(CATEGORY_ID_1);
        Assert.assertEquals(expectedRuleSet, eraseIds(ruleSet));
    }

    @Test
    public void getCategoryRuleSetForCategoryWithRuleSet() {
        ModelRuleSet ruleSet = ruleService.getCategoryRuleSet(CATEGORY_ID_1);
        ruleSet.getRules().add(buildExampleRule());
        ruleService.saveModelRuleSet(ruleSet);

        ModelRuleSet expectedRuleSet = new ModelRuleSet(ruleSet);

        ModelRuleSet categoryRuleSet = ruleService.getCategoryRuleSet(CATEGORY_ID_1);
        Assert.assertEquals(expectedRuleSet, categoryRuleSet);
    }
    // endregion

    // region ===== Tests for saveModelRule and deleteModelRule methods =====
    @Test
    public void saveModelRule() {
        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(CATEGORY_ID_1);
        ModelRule rule = buildExampleRule();

        ModelRuleSet expectedRuleSet = new ModelRuleSet(ruleSet);
        expectedRuleSet.getRules().add(new ModelRule(rule));

        ruleService.saveModelRuleSet(ruleSet);
        rule.setRuleSetId(ruleSet.getId());
        ruleService.saveModelRule(rule, CATEGORY_ID_1);
        ModelRuleSet actualRuleSet = ruleService.getModelRuleSet(ruleSet.getId());
        Assert.assertEquals(expectedRuleSet, eraseIds(actualRuleSet));
    }

    @Test
    public void saveModelRuleIfRuleSetIsNotEditableThenThrowException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Unable to modify read-only model rule set");

        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(CATEGORY_ID_1);
        ruleSet.setEditable(false);
        ruleService.saveModelRuleSet(ruleSet);

        ModelRule rule = buildExampleRule();
        rule.setRuleSetId(ruleSet.getId());
        ruleService.saveModelRule(rule, CATEGORY_ID_1);
    }

    @Test
    public void deleteModelRule() {
        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(CATEGORY_ID_1);
        ModelRule rule = buildExampleRule();
        ruleSet.getRules().add(rule);
        ruleService.saveModelRuleSet(ruleSet);

        ruleService.deleteModelRule(rule);

        ModelRuleSet expectedRuleSet = new ModelRuleSet();
        expectedRuleSet.setCategoryId(CATEGORY_ID_1);

        ModelRuleSet actualRuleSet = ruleService.getModelRuleSet(ruleSet.getId());
        Assert.assertEquals(expectedRuleSet, eraseIds(actualRuleSet));
    }
    //endregion

    private ModelRule buildExampleRule() {
        ModelRule rule = new ModelRule();
        rule.setName("Rule name");

        Parameter parameter = new Parameter();
        parameter.setId(PARAMETER_ID_1);
        parameter.setXslName(PARAMETER_XSL_NAME);
        parameter.setType(Param.Type.STRING);
        parameter.setUseForGuru(true);


        ModelRulePredicate ifPredicate = new ModelRulePredicate(parameter.getId(), IF, NOT_EMPTY);
        rule.getIfs().add(ifPredicate);

        ModelRulePredicate thenPredicate = new ModelRulePredicate(parameter.getId(), THEN, EMPTY);
        rule.getThens().add(thenPredicate);

        return rule;
    }

    private ModelRuleSet eraseIds(ModelRuleSet ruleSet) {
        ruleSet.setId(ModelRuleSet.NO_ID);
        for (ModelRule rule : ruleSet.getRules()) {
            rule.setId(ModelRule.NO_ID);
            rule.setRuleSetId(ModelRuleSet.NO_ID);
        }
        return ruleSet;
    }
}
