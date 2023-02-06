package ru.yandex.market.mbo.db.modelstorage;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.params.guru.BaseGuruServiceImpl;
import ru.yandex.market.mbo.db.rules.ModelRuleDAO;
import ru.yandex.market.mbo.db.rules.ModelRuleDAOStub;
import ru.yandex.market.mbo.db.rules.ModelRuleService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.modelstorage.FullModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleException;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.visual.Word;

/**
 * @author ayratgdl
 * @since 08.10.18
 */
public class ModelRulesExecutorServiceImplTest {
    private static final long CATEGORY_ID = 101;
    private static final long GURU_CATEGORY_ID = 201;
    private static final long PARAMETER_ID = 301;
    private static final long SKU_PARAMETER_ID = 401;
    private static final long PARENT_MODEL_ID = 1L;
    private static final String PARAMETER_XSL_NAME = "xsl_name_" + PARAMETER_ID;
    private static final String SKU_PARAMETER_XSL_NAME = "xsl_name_" + SKU_PARAMETER_ID;

    private ModelRulesExecutorServiceImpl rulesExecutor;
    private ModelRuleService ruleService;
    private ModelRuleDAO ruleDAO;

    private Parameter parameter;
    private Parameter skuParameter;

    @Before
    public void setUp() throws Exception {

        BaseGuruServiceImpl guruService = new BaseGuruServiceImpl();
        guruService.addCategory(CATEGORY_ID, GURU_CATEGORY_ID, false);
        guruService.addCategoryEntities(buildSampleCategory());

        ruleDAO = new ModelRuleDAOStub();
        ruleService = Mockito.spy(new ModelRuleService(ruleDAO, new ParameterLoaderServiceStub()));


        rulesExecutor = new ModelRulesExecutorServiceImpl(guruService, ruleService);
        addSampleRule(false, false, false);
    }

    // region ===== Tests applyRules for various types models (CLUSTER, GURU, ...) =====
    @Test(expected = ModelRuleException.class)
    public void ifApplyRulesToClustersThenRulesHasNotApplied() {
        addSampleRule(true, true, true);
        FullModel model = buildSampleModel(Source.CLUSTER);
        //Если правило затрагивает параметры, для кластеров оно будет падать на валидации
        rulesExecutor.applyRules(model);
    }

    @Test
    public void ifApplyRulesToGuruThenRulesHasApplied() {
        FullModel model = buildSampleModel(Source.GURU);
        Assert.assertTrue(rulesExecutor.applyRules(model));
    }

    @Test
    public void ifApplyNoSkuRulesToPskuRulesNotApplied() {
        FullModel model = buildSampleSkuModel(Source.SKU, Source.PARTNER_SKU, Source.GURU, Source.PARTNER);
        Assert.assertFalse(rulesExecutor.applyRules(model));
    }

    @Test
    public void ifApplyNoPskuRulesToPskuRulesNotApplied() {
        addSampleRule(true, false, false);
        FullModel model = buildSampleSkuModel(Source.SKU, Source.PARTNER_SKU, Source.GURU, Source.PARTNER);
        Assert.assertFalse(rulesExecutor.applyRules(model));
    }

    @Test
    public void ifApplyPskuRulesToPskuRulesHasApplied() {
        addSampleRule(true, true, false);
        FullModel model = buildSampleSkuModel(Source.SKU, Source.PARTNER_SKU, Source.GURU, Source.PARTNER);
        Assert.assertTrue(rulesExecutor.applyRules(model));
    }
    // endregion

    private CategoryEntities buildSampleCategory() {
        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setHid(CATEGORY_ID);
        parameter = new Parameter();
        parameter.setId(PARAMETER_ID);
        parameter.setXslName(PARAMETER_XSL_NAME);
        parameter.setType(Param.Type.STRING);
        parameter.setUseForGuru(true);
        categoryEntities.addParameter(parameter);

        skuParameter = new Parameter();
        skuParameter.setId(SKU_PARAMETER_ID);
        skuParameter.setXslName(SKU_PARAMETER_XSL_NAME);
        skuParameter.setType(Param.Type.STRING);
        skuParameter.setUseForGuru(true);
        skuParameter.setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL);
        categoryEntities.addParameter(skuParameter);

        return categoryEntities;
    }

    private FullModel buildSampleModel(Source modelType) {
        CommonModel commonModel = buildCommonModel(modelType, modelType);
        return new FullModel(commonModel);
    }

    private ModelRuleSet buildSampleRule(boolean applyToSku, boolean applyToPsku, boolean applyToClusters) {
        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(CATEGORY_ID);
        ruleSet.getRules().add(buildEraseRule(applyToSku, applyToPsku, applyToClusters));
        return ruleSet;
    }

    private void addSampleRule(boolean applyToSku, boolean applyToPsku, boolean applyToClusters) {
        ModelRuleSet ruleSet = buildSampleRule(applyToSku, applyToPsku, applyToClusters);

        ruleDAO.saveModelRuleSet(ruleSet);
        ruleDAO.setRuleSetIdToCategory(CATEGORY_ID, ruleSet.getId());
    }

    private ModelRule buildEraseRule(boolean applyToSku, boolean applyToPsku, boolean applyToClusters) {
        ModelRule rule = new ModelRule();
        rule.setActive(true);
        rule.setApplyToGuru(!applyToSku);
        rule.setApplyToSKU(applyToSku);
        rule.setAllowedApplyToPSKU(applyToPsku);
        rule.setApplyToClusters(applyToClusters);

        Parameter param = applyToSku ? skuParameter : parameter;
        rule.setName("Erase rule for parameter " + param.getId());

        ModelRulePredicate ifPredicate =
            new ModelRulePredicate(param.getId(), PredicateType.IF, PredicateOperation.NOT_EMPTY);
        rule.setIfs(Collections.singletonList(ifPredicate));
        ModelRulePredicate thenPredicate =
            new ModelRulePredicate(param.getId(), PredicateType.THEN, PredicateOperation.EMPTY);
        rule.setThens(Collections.singletonList(thenPredicate));
        return rule;
    }

    private FullModel buildSampleSkuModel(Source modelType, Source sourceType,
                                          Source parentModelType, Source parentSource) {
        CommonModel parentModel = buildCommonModel(parentModelType, parentSource);
        parentModel.setId(PARENT_MODEL_ID);
        CommonModel commonModel = buildCommonModel(modelType, sourceType);
        commonModel.addRelation(new ModelRelation(parentModel.getId(), parentModel.getCategoryId(),
            ModelRelation.RelationType.SKU_PARENT_MODEL));
        return new FullModel(commonModel, ImmutableMap.of(parentModel.getId(), parentModel));
    }

    private CommonModel buildCommonModel(Source modelType, Source sourceType) {
        Long paramId = modelType == Source.SKU ? SKU_PARAMETER_ID : PARAMETER_ID;
        String paramName = modelType == Source.SKU ?
            SKU_PARAMETER_XSL_NAME :
            PARAMETER_XSL_NAME;
        return CommonModelBuilder.newBuilder()
            .category(CATEGORY_ID)
            .currentType(modelType)
            .source(sourceType)
            .startParameterValue()
                .paramId(paramId)
                .xslName(paramName)
                .type(Param.Type.STRING)
                .words(new Word(Language.RUSSIAN.getId(), "string value"))
            .endParameterValue()
            .endModel();
    }
}
