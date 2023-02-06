package ru.yandex.market.mbo.db.params;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.db.modelstorage.IndexedModelQueryService;
import ru.yandex.market.mbo.db.modelstorage.YtSaasIndexesWrapper;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.ModelIndexPayload;
import ru.yandex.market.mbo.db.params.validators.ParameterValidationService;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.db.recommendations.dao.RecommendationServiceDAO;
import ru.yandex.market.mbo.db.rules.ModelRuleService;
import ru.yandex.market.mbo.db.rules.ModelRuleTaskService;
import ru.yandex.market.mbo.db.templates.OutputTemplateService;
import ru.yandex.market.mbo.gwt.exceptions.ValidationError;
import ru.yandex.market.mbo.gwt.models.ParamValueSearch;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.recommendation.Recommendation;
import ru.yandex.market.mbo.gwt.models.recommendation.RecommendationBuilder;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplates;
import ru.yandex.market.mbo.validator.OptionPropertyDuplicationValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gwt.exceptions.ValidateMessages.PARAM_REMOVE;

/**
 * @author york
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ParameterValidationServiceTest {

    // Constants for parameter validation tests
    public static final long PARAM_CATEGORY_HID = 1;
    public static final long BLOCKING_RECOMENDATION_MAIN_CATEGORY_HID = 2;
    public static final long BLOCKING_RECOMENDATION_LINKED_CATEGORY_HID = 3;
    public static final long SAFE_RECOMENDATION_MAIN_CATEGORY_HID = 4;
    public static final long SAFE_RECOMENDATION_LINKED_CATEGORY_HID = 5;
    public static final long BLOCKING_RECOMENDATION_ID = 1;
    public static final long SAFE_RECOMENDATION_ID = 2;
    public static final long PARAM_SAFE_TO_DELETE_ID = 1;
    public static final long PARAM_UNSAFE_TO_DELETE_ID = 2;
    public static final long PARAM_USED_IN_SKU_ID = 3;

    public static final Parameter PARAM_USED_IN_SKU = CategoryParamBuilder.newBuilder(PARAM_USED_IN_SKU_ID,
            "xsl-" + PARAM_USED_IN_SKU_ID)
        .setCategoryHid(PARAM_CATEGORY_HID)
        .setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
        .build();

    public static final Parameter PARAM_SAFE_TO_DELETE = CategoryParamBuilder.newBuilder(PARAM_SAFE_TO_DELETE_ID,
            "xsl-" + PARAM_SAFE_TO_DELETE_ID)
        .setCategoryHid(PARAM_CATEGORY_HID)
        .build();

    public static final Parameter PARAM_UNSAFE_TO_DELETE = CategoryParamBuilder.newBuilder(PARAM_UNSAFE_TO_DELETE_ID,
            "xsl-" + PARAM_UNSAFE_TO_DELETE_ID)
        .setCategoryHid(PARAM_CATEGORY_HID)
        .build();

    public static final Recommendation BLOCKING_RECOMENDATION = RecommendationBuilder.newBuilder()
        .setId(BLOCKING_RECOMENDATION_ID)
        .setMainCategoryId(BLOCKING_RECOMENDATION_MAIN_CATEGORY_HID)
        .setLinkedCategoryId(BLOCKING_RECOMENDATION_LINKED_CATEGORY_HID)
        .build();

    public static final Recommendation SAFE_RECOMENDATION = RecommendationBuilder.newBuilder()
        .setId(SAFE_RECOMENDATION_ID)
        .setMainCategoryId(SAFE_RECOMENDATION_MAIN_CATEGORY_HID)
        .setLinkedCategoryId(SAFE_RECOMENDATION_LINKED_CATEGORY_HID)
        .build();

    // Constants for option validation tests
    // Category tree
    public static final long HID_1 = 1;
    public static final long HID_1_1 = 2;
    public static final long HID_1_2 = 3;
    public static final long HID_1_1_1 = 4;
    public static final long HID_1_1_2 = 5;
    public static final long HID_1_2_1 = 6;
    public static final long HID_1_2_2 = 7;

    public static final long OPTION_ID = 1;
    public static final Option OPTION = OptionBuilder.newBuilder()
        .setId(OPTION_ID)
        .setFilterValue(false)
        .setPublished(true)
        .build();

    public static final long PARAM_ID = 11;
    public static final Parameter PARAM = CategoryParamBuilder.newBuilder()
        .setId(PARAM_ID)
        .setCategoryHid(HID_1_2)
        .build();

    public static final long OPTION_RECOMENDATION_MAIN_HID_1_1_2_ID = 1;
    public static final Recommendation OPTION_RECOMENDATION_MAIN_HID_1_1_2 =
        RecommendationBuilder.newBuilder()
            .setId(OPTION_RECOMENDATION_MAIN_HID_1_1_2_ID)
            .setMainCategoryId(HID_1_1_2)
            .setLinkedCategoryId(HID_1_1_1)
            .build();

    public static final long OPTION_RECOMENDATION_MAIN_HID_1_2_2_ID = 2;
    public static final Recommendation OPTION_RECOMENDATION_MAIN_HID_1_2_2 =
        RecommendationBuilder.newBuilder()
            .setId(OPTION_RECOMENDATION_MAIN_HID_1_2_2_ID)
            .setMainCategoryId(HID_1_2_2)
            .setLinkedCategoryId(HID_1_1)
            .build();

    public static final long OPTION_RECOMENDATION_LINKED_HID_1_2_ID = 3;
    public static final Recommendation OPTION_RECOMENDATION_LINKED_HID_1_2 =
        RecommendationBuilder.newBuilder()
            .setId(OPTION_RECOMENDATION_LINKED_HID_1_2_ID)
            .setMainCategoryId(HID_1_1)
            .setLinkedCategoryId(HID_1_2)
            .build();

    public static final long OPTION_RECOMENDATION_LINKED_HID_1_1_2_ID = 4;
    public static final Recommendation OPTION_RECOMENDATION_LINKED_HID_1_1_2 =
        RecommendationBuilder.newBuilder()
            .setId(OPTION_RECOMENDATION_LINKED_HID_1_1_2_ID)
            .setMainCategoryId(HID_1_2_1)
            .setLinkedCategoryId(HID_1_1_2)
            .build();

    private Map<Long, Set<Long>> categoriesByParam;

    @Mock
    private OptionDuplicationRecoverService optionDuplicationRecoverService;

    @Mock
    private OptionPropertyDuplicationValidator optionAliasValidator;

    @Mock
    private OptionPropertyDuplicationValidator optionNameValidator;

    @Mock
    private IndexedModelQueryService modelQueryService;

    @Mock
    private ParameterLinkService parameterLinkService;

    @Mock
    private RecommendationServiceDAO recommendationServiceDAO;

    @Mock
    private ModelRuleService modelRuleService;

    @Mock
    private ModelFormService modelFormService;

    @Mock
    private RecipeService recipeService;

    @Mock
    OutputTemplateService outputTemplateService;

    @Mock
    YtSaasIndexesWrapper ytSaasIndexesWrapper;

    @Mock
    ModelRuleTaskService modelRuleTaskService;

    @Mock
    GLRulesService glRulesService;

    @Mock
    TitlemakerTemplateDao titlemakerTemplateDao;

    @Mock
    PatternService patternService;

    @Mock
    IParameterLoaderService parameterLoaderService;

    @InjectMocks
    private ParameterValidationService parameterValidationService;

    @Before
    public void setUp() {
        when(outputTemplateService.getTemplates(PARAM_CATEGORY_HID)).thenReturn(new OutputTemplates());
        Set<Long> subtree = new HashSet<>();
        subtree.add(PARAM_CATEGORY_HID);
        subtree.add(BLOCKING_RECOMENDATION_LINKED_CATEGORY_HID);
        categoriesByParam = new HashMap<>();
        categoriesByParam.put(PARAM_SAFE_TO_DELETE.getId(), subtree);
        categoriesByParam.put(PARAM_UNSAFE_TO_DELETE.getId(), subtree);
        categoriesByParam.put(PARAM_USED_IN_SKU.getId(), subtree);
        categoriesByParam.put(PARAM_ID, new HashSet<>(Arrays.asList(HID_1_2, HID_1_2_1, HID_1_2_2)));
    }

    private Collection<Long> prepareParametersForValidationTest() {
        Mockito.doReturn(singletonList(SAFE_RECOMENDATION))
            .when(recommendationServiceDAO)
            .getRawRecommendationsByParam(Mockito.eq(PARAM_SAFE_TO_DELETE.getId()));

        Mockito.doReturn(singletonList(BLOCKING_RECOMENDATION))
            .when(recommendationServiceDAO)
            .getRawRecommendationsByParam(Mockito.eq(PARAM_UNSAFE_TO_DELETE.getId()));

        return Arrays.asList(PARAM_CATEGORY_HID, BLOCKING_RECOMENDATION_LINKED_CATEGORY_HID);
    }

    private void mockGetModelDocuments(int resultSize) {
        List<ModelIndexPayload> result = new ArrayList<>();
        for (int i = 0; i < resultSize; i++) {
            result.add(new ModelIndexPayload(i, 0, null));
        }
        Mockito.doReturn(result).when(ytSaasIndexesWrapper).getModelDocuments(any(MboIndexesFilter.class),
            any(ReadStats.class));
    }

    @Test
    public void testSkuUsagesNotCalled() {
        parameterValidationService.validateParameter(singletonList(PARAM_SAFE_TO_DELETE),
            categoriesByParam, PARAM_REMOVE, new ArrayList<>());
        Mockito.verifyZeroInteractions(modelQueryService);
    }

    @Test
    public void testSkuUsagesZeroModels() {
        mockGetModelDocuments(0);
        parameterValidationService.validateParameter(singletonList(PARAM_USED_IN_SKU),
            categoriesByParam, PARAM_REMOVE, new ArrayList<>());
        Mockito.verify(ytSaasIndexesWrapper).getModelDocuments(any(MboIndexesFilter.class), any(ReadStats.class));
    }

    @Test(expected = OperationException.class)
    public void testSkuUsages() {
        mockGetModelDocuments(ParamValueSearch.MAX_MODEL_EXAMPLES / 2);
        parameterValidationService.validateParameter(singletonList(PARAM_USED_IN_SKU),
            categoriesByParam, PARAM_REMOVE, new ArrayList<>());
    }

    @Test
    public void parametersDeleteionValidationByRecomendationSucceed() {
        prepareParametersForValidationTest();
        List<ValidationError> errors = new ArrayList<>();
        parameterValidationService.validateParamsByRecommendations(
            singletonList(PARAM_SAFE_TO_DELETE), categoriesByParam, PARAM_REMOVE, errors);
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void parametersDeleteionValidationByRecomendationFailed() {
        prepareParametersForValidationTest();
        List<ValidationError> errors = new ArrayList<>();
        parameterValidationService.validateParamsByRecommendations(
            Arrays.asList(PARAM_SAFE_TO_DELETE, PARAM_UNSAFE_TO_DELETE), categoriesByParam, PARAM_REMOVE, errors);
        Assertions.assertThat(errors).isNotEmpty();
    }

    private ParameterValuesChanges createValuesChanges(List<Option> updates, List<Option> deletes) {
        ParameterValuesChanges result = new ParameterValuesChanges();
        updates.forEach(result::valueUpdated);
        deletes.forEach(result::valueDeleted);
        return result;
    }

    private void prepareOptionValidationSucceedTest() {
        Mockito.doReturn(emptyList())
            .when(recommendationServiceDAO)
            .getRawRecommendationsByOptions(
                Mockito.eq(Collections.singletonList(OPTION.getValueId())));
    }

    private void prepareOptionValidationFailTest() {
        Mockito.doReturn(singletonList(OPTION_RECOMENDATION_MAIN_HID_1_2_2))
            .when(recommendationServiceDAO)
            .getRawRecommendationsByOptions(
                Mockito.eq(Collections.singletonList(OPTION.getValueId())));
    }

    @Test
    public void optionUpdateValidationSucceed() {
        prepareOptionValidationSucceedTest();
        ParameterValuesChanges validatedChanges = createValuesChanges(singletonList(OPTION), emptyList());
        parameterValidationService.validateOption(PARAM, validatedChanges, categoriesByParam, new ArrayList<>());
    }

    @Test
    public void optionUpdateValidationByOnOptionNotInFilterSucceed() {
        prepareOptionValidationFailTest();
        OPTION.setFilterValue(true);
        ParameterValuesChanges validatedChanges = createValuesChanges(singletonList(OPTION), emptyList());
        parameterValidationService.validateOption(PARAM, validatedChanges, categoriesByParam, new ArrayList<>());
    }

    @Test
    public void optionDeleteValidationSucceed() {
        prepareOptionValidationSucceedTest();
        ParameterValuesChanges validatedChanges = createValuesChanges(emptyList(), singletonList(OPTION));
        parameterValidationService.validateOption(PARAM, validatedChanges, categoriesByParam, new ArrayList<>());
    }

    @Test(expected = OperationException.class)
    public void optionUpdateValidationFail() {
        prepareOptionValidationFailTest();
        ParameterValuesChanges validatedChanges = createValuesChanges(singletonList(OPTION), emptyList());
        parameterValidationService.validateOption(PARAM, validatedChanges, categoriesByParam, new ArrayList<>());
    }

    @Test(expected = OperationException.class)
    public void optionUpdateValidationOnOptionNotInFilterFail() {
        prepareOptionValidationFailTest();
        OPTION.setFilterValue(false);
        ParameterValuesChanges validatedChanges = createValuesChanges(singletonList(OPTION), emptyList());
        parameterValidationService.validateOption(PARAM, validatedChanges, categoriesByParam, new ArrayList<>());
    }

    @Test(expected = OperationException.class)
    public void optionDeleteValidationFail() {
        prepareOptionValidationFailTest();
        ParameterValuesChanges validatedChanges = createValuesChanges(emptyList(), singletonList(OPTION));
        parameterValidationService.validateOption(PARAM, validatedChanges, categoriesByParam, new ArrayList<>());
    }

    //Отключенный параметр не может быть обязательным
    @Test(expected = OperationException.class)
    public void validateDisabledButExtractable() {
        CategoryParam p = generateParameter(CategoryParam.Level.OFFER, SkuParameterMode.SKU_NONE, false, true);
        parameterValidationService.validateSkuDependentFields(p);
    }

    //Отключенный параметр не может быть извлекаемым в SKUBD
    @Test(expected = OperationException.class)
    public void validateDisabledButMandatory() {
        CategoryParam p = generateParameter(CategoryParam.Level.OFFER, SkuParameterMode.SKU_NONE, true, false);
        parameterValidationService.validateSkuDependentFields(p);
    }

    //Информационный параметр не может быть извлекаемым в SKUBD
    @Test(expected = OperationException.class)
    public void validateInformationalButExtractable() {
        CategoryParam p = generateParameter(CategoryParam.Level.OFFER, SkuParameterMode.SKU_INFORMATIONAL, true, false);
        parameterValidationService.validateSkuDependentFields(p);
    }

    //Определяющий параметр может быть не обязательным
    @Test
    public void validateDefiningButExtractable() {
        CategoryParam p = generateParameter(CategoryParam.Level.OFFER, SkuParameterMode.SKU_DEFINING, true, false);
        parameterValidationService.validateSkuDependentFields(p);
    }

    private CategoryParam generateParameter(CategoryParam.Level level, SkuParameterMode skuParameterMode,
                                            boolean extractInSku, boolean mandatory) {
        CategoryParam p = new Parameter();
        p.setLevel(level);
        p.setSkuParameterMode(skuParameterMode);
        p.setExtractInSkubd(extractInSku);
        p.setMandatory(mandatory);
        return p;
    }
}
