package ru.yandex.market.mbo.db.params;

import com.google.common.collect.ImmutableSet;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.catalogue.CategoryFavoriteVendorServiceMock;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.processing.ConcurrentUpdateException;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.core.category.VendorGoodContentExclusionService;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotServiceMock;
import ru.yandex.market.mbo.core.utils.TransactionTemplateMock;
import ru.yandex.market.mbo.db.IdGeneratorStub;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.db.TovarTreeForVisualServiceMock;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkService;
import ru.yandex.market.mbo.db.modelstorage.IndexedModelQueryService;
import ru.yandex.market.mbo.db.modelstorage.YtSaasIndexesWrapper;
import ru.yandex.market.mbo.db.params.audit.ParameterAuditService;
import ru.yandex.market.mbo.db.params.validators.ParameterValidationService;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.db.recipes.RecipeServiceDaoMock;
import ru.yandex.market.mbo.db.recommendations.dao.RecommendationServiceDAOMock;
import ru.yandex.market.mbo.db.rules.ModelRuleDAOStub;
import ru.yandex.market.mbo.db.rules.ModelRuleService;
import ru.yandex.market.mbo.db.rules.ModelRuleTaskService;
import ru.yandex.market.mbo.db.rules.ModelRuleTaskServiceStub;
import ru.yandex.market.mbo.db.templates.OutputTemplateService;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryPatterns;
import ru.yandex.market.mbo.gwt.models.params.CategoryRules;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.GuruParamFilter;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.ParameterOverride;
import ru.yandex.market.mbo.gwt.models.rules.ModelProperty;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.rules.ValueHolder;
import ru.yandex.market.mbo.gwt.models.rules.ValueSource;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplates;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.tt.TaskTrackerMock;
import ru.yandex.market.mbo.utils.RandomTestUtils;
import ru.yandex.market.mbo.validator.OptionAliasDuplicationValidator;
import ru.yandex.market.mbo.validator.OptionNameDuplicationValidator;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.ADD_VALUE;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.ASSIGN;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.MATCHES;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.NOT_EMPTY;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType.IF;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType.THEN;

/**
 * Tests of {@link ParameterService}.
 */
public class ParameterServiceTest {
    private static final long SEED = 1L;

    private static final long HID1 = 1L;
    private static final long HID2 = 2L;
    private static final long HID3 = 3L;

    private static final int TEST_THREAD_COUNT = 5;

    private static final Logger log = Logger.getLogger(ParameterServiceTest.class);

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @Mock
    WordDBService wordDBService;
    @Mock
    ParameterAuditService parameterAuditService;
    @Mock
    ParameterLinkService parameterLinkService;
    @Mock
    ValueLinkService valueLinkService;
    @Mock
    GLRulesService glRulesService;
    @Mock
    PatternService patternService;
    @Mock
    OutputTemplateService outputTemplateService;
    @Mock
    IndexedModelQueryService indexedModelQueryService;
    @Mock
    ModelFormService modelFormService;
    @Mock
    YtSaasIndexesWrapper ytSaasIndexesWrapper;
    @Mock
    TitlemakerTemplateDao titlemakerTemplateDao;
    @Mock
    ParameterDAO parameterDAO;
    @Mock
    VendorGoodContentExclusionService vendorGoodContentExclusionService;

    private final ParameterSaveContext parameterSaveContext = new ParameterSaveContext(0L);
    private final IdGenerator kdepotIdGenerator = new IdGeneratorStub();
    private final ParameterTestStorage parameterTestStorage = new ParameterTestStorage();
    private final ParameterLoaderServiceStub parameterLoaderServiceStub = new ParameterLoaderServiceStub();
    private final ParameterDAOMock parameterDAOMock = new ParameterDAOMock(kdepotIdGenerator, parameterTestStorage);
    private final RecipeService recipeService = new RecipeService(null, new RecipeServiceDaoMock());

    private ParameterService parameterService;
    private ModelRuleTaskService modelRuleTaskService;
    private ModelRuleService modelRuleService;
    private EnhancedRandom newRandom;

    @Before
    public void setUp() {
        newRandom = RandomTestUtils.createNewRandom(SEED);

        when(parameterLinkService.getLinkedParameters(any(CategoryParam.class), any()))
            .thenReturn(new MultiMap<>());
        when(glRulesService.loadCategoryRulesByHid(anyLong()))
            .thenReturn(new CategoryRules());
        when(patternService.loadCategoryPatternsByHid(any()))
            .thenReturn(new CategoryPatterns());
        when(outputTemplateService.getTemplates(HID1)).thenReturn(new OutputTemplates());
        when(outputTemplateService.getTemplates(HID2)).thenReturn(new OutputTemplates());
        KnowledgeDepotServiceMock kd = new KnowledgeDepotServiceMock();

        TovarTreeDaoMock tovarTreeDao = new TovarTreeDaoMock();
        tovarTreeDao
            .addCategory(new TovarCategory("Root", KnownIds.GLOBAL_CATEGORY_ID, 0))
            .addCategory(new TovarCategory("Category1", HID1, KnownIds.GLOBAL_CATEGORY_ID))
            .addCategory(HID2, HID1)
            .addCategory(HID3, HID2);

        modelRuleService = new ModelRuleService(new ModelRuleDAOStub(), parameterLoaderServiceStub);
        modelRuleTaskService = new ModelRuleTaskServiceStub();

        OptionAliasDuplicationValidator optionAliasValidator = new OptionAliasDuplicationValidator();
        optionAliasValidator.setValueLinkService(valueLinkService);
        optionAliasValidator.setParameterLoaderService(new ParameterLoaderServiceStub());

        OptionNameDuplicationValidator optionNameValidator = new OptionNameDuplicationValidator();
        optionNameValidator.setValueLinkService(valueLinkService);
        optionNameValidator.setParameterLoaderService(new ParameterLoaderServiceStub());

        ParameterValidationService parameterValidationService = new ParameterValidationService(
            new RecommendationServiceDAOMock(),
            titlemakerTemplateDao,
            new OptionDuplicationRecoverService(),
            optionAliasValidator,
            optionNameValidator,
            parameterLinkService,
            indexedModelQueryService,
            modelRuleService,
            modelRuleTaskService,
            glRulesService,
            recipeService, modelFormService, outputTemplateService,
            ytSaasIndexesWrapper, parameterDAOMock, patternService, parameterLoaderServiceStub);

        ParameterServiceReader  parameterServiceReader = new ParameterServiceReader(
                new TovarTreeForVisualServiceMock(tovarTreeDao),
                tovarTreeDao);
        parameterServiceReader.setParameterDAO(parameterDAOMock);
        parameterServiceReader.setParameterValidationService(parameterValidationService);
        parameterServiceReader.setParameterLoader(parameterLoaderServiceStub);

        ParameterServiceFast  parameterServiceFast = new ParameterServiceFast(
                new TransactionTemplateMock());
        parameterServiceFast.setParameterAuditService(parameterAuditService);
        parameterServiceFast.setParameterLoader(parameterLoaderServiceStub);
        parameterServiceFast.setParameterDAO(parameterDAOMock);

        ParameterServiceOriginal  parameterServiceOriginal = new ParameterServiceOriginal(
                new TransactionTemplateMock(),
                null,
                kd,
                new TovarTreeForVisualServiceMock(tovarTreeDao),
                new TaskTrackerMock(),
                new CategoryFavoriteVendorServiceMock(),
                null,
                glRulesService,
                patternService,
                tovarTreeDao,
                vendorGoodContentExclusionService);
        parameterServiceOriginal.setParameterDAO(parameterDAOMock);
        parameterServiceOriginal.setParameterAuditService(parameterAuditService);
        parameterServiceOriginal.setParameterValidationService(parameterValidationService);
        parameterServiceOriginal.setParameterLoader(parameterLoaderServiceStub);
        parameterServiceOriginal.setWordService(wordDBService);
        parameterServiceOriginal.setParameterLinkService(parameterLinkService);
        parameterServiceOriginal.setValueLinkService(valueLinkService);
        parameterServiceOriginal.setOutputTemplateService(outputTemplateService);
        parameterServiceOriginal.setParameterServiceReader(parameterServiceReader);

        parameterService = new ParameterService(
                parameterServiceOriginal,
                parameterServiceFast,
                parameterServiceReader);
        parameterSaveContext.setForceAddOptionPermissions(true);
        parameterSaveContext.setFindGlobalParentForNewOption(true);
    }

    @Test
    public void testAddNewLocalOptionWithoutSuggest() {
        CategoryParam glParameter = createAndSaveParameter(KnownIds.GLOBAL_CATEGORY_ID, XslNames.VENDOR,
            Param.Type.ENUM, CategoryParam.LocalValueInheritanceStrategy.DIRECT_INCLUDE);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(glParameter);

        int glOptSize = parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId()).getOptions().size();

        InheritedParameter localParameter = createInheritedParameterWithOverride(HID1, glParameter);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(localParameter);

        Option option = createNewOption(localParameter);

        Option savedOption = parameterService.addValue(parameterSaveContext, localParameter, option);

        assertThat(savedOption.getInheritanceStrategy()).isEqualTo(Option.InheritanceStrategy.INHERIT);

        List<Option> glOptions = ((InheritedParameter) parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId())).getParent().getOptions();

        assertThat(glOptions.size()).isEqualTo(glOptSize + 1);
        assertThat(glOptions.stream().map(Option::getName).collect(Collectors.toSet())).contains("option");

        Set<Long> diOptions = parameterTestStorage.getDirectInclude().get(localParameter.getRealParamId());
        assertThat(diOptions).contains(option.getId());
    }

    @Test
    public void testAddNewLocalOptionWithSuggest() {
        CategoryParam glParameter = createAndSaveParameter(KnownIds.GLOBAL_CATEGORY_ID, XslNames.BAR_CODE,
            Param.Type.ENUM, CategoryParam.LocalValueInheritanceStrategy.DIRECT_INCLUDE);

        Option glOption = createDirectIncludeOption(glParameter);
        glParameter = parameterTestStorage.getRealIdToParam().get(glParameter.getRealParamId());
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(glParameter);

        List<Option> glOptions = parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId()).getOptions();

        int glOptSize = glOptions.size();

        InheritedParameter localParameter = createInheritedParameterWithOverride(HID1, glParameter);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(localParameter);

        Option copy = glOption.copy();
        copy.setId(glOption.getId());

        Option savedOption = parameterService.addValue(parameterSaveContext, localParameter, copy);

        assertThat(savedOption.getInheritanceStrategy()).isEqualTo(glOption.getInheritanceStrategy());
        assertThat(savedOption.getParamId()).isNotEqualTo(glOption.getParamId());

        glOptions = ((InheritedParameter) parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId())).getParent().getOptions();

        assertThat(glOptions.size()).isEqualTo(glOptSize);
        assertThat(glOptions.stream().map(Option::getName).collect(Collectors.toSet())).contains("option");

        final Set<Long> diOptions = parameterTestStorage.getDirectInclude().get(localParameter.getRealParamId());
        assertThat(diOptions).contains(savedOption.getId());
    }

    public void testAddNewLocalOptionWithSuggestError() {
        CategoryParam glParameter = createAndSaveParameter(KnownIds.GLOBAL_CATEGORY_ID, XslNames.VENDOR,
            Param.Type.ENUM, CategoryParam.LocalValueInheritanceStrategy.DIRECT_INCLUDE);

        Option glOption = createDirectIncludeOption(glParameter);
        glParameter = parameterTestStorage.getRealIdToParam().get(glParameter.getRealParamId());
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(glParameter);

        List<Option> glOptions = parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId()).getOptions();

        int glOptSize = glOptions.size();

        InheritedParameter localParameter = createInheritedParameterWithOverride(HID1, glParameter);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(localParameter);

        Option copy = glOption.copy();
        copy.setId(glOption.getId());

        parameterDAOMock.removeOption(glOption, glParameter);

        Option savedOption = parameterService.addValue(parameterSaveContext, localParameter, copy);

        assertThat(savedOption.getInheritanceStrategy()).isEqualTo(glOption.getInheritanceStrategy());
        assertThat(savedOption.getParamId()).isNotEqualTo(glOption.getParamId());

        glOptions = glOptions = ((InheritedParameter) parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId())).getParent().getOptions();

        assertThat(glOptions.size()).isEqualTo(glOptSize);
        assertThat(glOptions.stream().map(Option::getName).collect(Collectors.toSet())).contains("option");

        final Set<Long> diOptions = parameterTestStorage.getDirectInclude().get(localParameter.getRealParamId());
        assertThat(diOptions).contains(savedOption.getId());
    }

    @Test
    public void testAddNewLocalOptionWithoutSuggestWithOverriding() {
        CategoryParam glParameter = createAndSaveParameter(KnownIds.GLOBAL_CATEGORY_ID, XslNames.VENDOR,
            Param.Type.ENUM, CategoryParam.LocalValueInheritanceStrategy.DIRECT_INCLUDE);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(glParameter);

        int glOptSize = parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId()).getOptions().size();

        InheritedParameter localParameter = createInheritedParameterWithOverride(HID1, glParameter);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(localParameter);

        int lOptSize = parameterTestStorage.getRealIdToParam()
            .get(localParameter.getRealParamId()).getOptions().size();

        Option parentOption = createNewOption(localParameter);
        Option option = createNewOption(localParameter);
        option.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "option2")));
        option.setParent(parentOption);

        Option savedOption = parameterService.addValue(parameterSaveContext, localParameter, option);

        assertThat(savedOption.getInheritanceStrategy()).isEqualTo(Option.InheritanceStrategy.INHERIT);

        List<Option> glOptions = ((InheritedParameter) parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId())).getParent().getOptions();

        assertThat(glOptions.size()).isEqualTo(glOptSize + 1);
        assertThat(glOptions.stream().map(Option::getName).collect(Collectors.toSet())).contains("option");

        Set<Long> diOptions = parameterTestStorage.getDirectInclude().get(localParameter.getRealParamId());
        assertThat(diOptions).contains(parentOption.getId());

        List<Option> lOptions = parameterTestStorage.getRealIdToParam()
            .get(localParameter.getRealParamId()).getOptions();

        assertThat(lOptions.size()).isEqualTo(lOptSize + 1);
        assertThat(lOptions.stream().map(Option::getName).collect(Collectors.toSet())).contains("option2");
    }

    @Test
    public void testAddNewLocalOptionWithSuggestWithOverriding() {
        CategoryParam glParameter = createAndSaveParameter(KnownIds.GLOBAL_CATEGORY_ID, XslNames.VENDOR,
            Param.Type.ENUM, CategoryParam.LocalValueInheritanceStrategy.DIRECT_INCLUDE);

        Option glOption = createDirectIncludeOption(glParameter);
        glParameter = parameterTestStorage.getRealIdToParam().get(glParameter.getRealParamId());
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(glParameter);

        List<Option> glOptions = parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId()).getOptions();

        int glOptSize = glOptions.size();

        InheritedParameter localParameter = createInheritedParameterWithOverride(HID1, glParameter);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(localParameter);

        int lOptSize = parameterTestStorage.getRealIdToParam()
            .get(localParameter.getRealParamId()).getOptions().size();

        Option parentOption = glOption.copy();
        parentOption.setId(glOption.getId());

        Option option = createNewOption(localParameter);
        option.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "option2")));
        option.setParent(parentOption);

        Option savedOption = parameterService.addValue(parameterSaveContext, localParameter, option);

        assertThat(savedOption.getInheritanceStrategy()).isEqualTo(glOption.getInheritanceStrategy());
        assertThat(savedOption.getParamId()).isNotEqualTo(glOption.getParamId());

        glOptions = ((InheritedParameter) parameterTestStorage.getRealIdToParam()
            .get(glParameter.getRealParamId())).getParent().getOptions();

        assertThat(glOptions.size()).isEqualTo(glOptSize);
        assertThat(glOptions.stream().map(Option::getName).collect(Collectors.toSet())).contains("option");

        final Set<Long> diOptions = parameterTestStorage.getDirectInclude().get(localParameter.getRealParamId());
        assertThat(diOptions).contains(parentOption.getId());

        List<Option> lOptions = parameterTestStorage.getRealIdToParam()
            .get(localParameter.getRealParamId()).getOptions();

        assertThat(lOptions.size()).isEqualTo(lOptSize + 1);
        assertThat(lOptions.stream().map(Option::getName).collect(Collectors.toSet())).contains("option2");
    }

    @Test
    public void testAddOverridenOptionToOverridenParam() {
        Parameter parameter = createAndSaveParameter(HID1, XslNames.VENDOR, Param.Type.ENUM);
        Option option = createOption(parameter);

        InheritedParameter inheritedParameter = createInheritedParameterWithOverride(HID2, parameter);

        parameterLoaderServiceStub.addAllCategoryParams(inheritedParameter);

        OptionImpl overridenOption = new OptionImpl(Option.OptionType.VENDOR);
        overridenOption.setParent(option);

        parameterService.addLocalVendor(parameterSaveContext, HID2, inheritedParameter, overridenOption);

        CategoryParam savedOverride = parameterTestStorage.getRealIdToParam()
            .get(inheritedParameter.getRealParamId());

        assertThat(savedOverride.getOptions())
            .usingElementComparatorIgnoringFields("id")
            .containsExactly(overridenOption);
    }

    @Test
    public void testBooleanParameterCreation() {
        Parameter parameter = createAndSaveParameter(HID1, XslNames.IS_SKU, Param.Type.BOOLEAN, true);
        parameter.setFillDifficulty(BigDecimal.ONE);

        parameterService.saveParameter(parameterSaveContext, HID1, parameter, new ParameterValuesChanges());

        CategoryParam savedOverride = parameterTestStorage.getRealIdToParam().get(parameter.getId());

        // true and false
        assertThat(savedOverride.getOptions()).hasSize(2);
    }

    @Test
    public void testBooleanOptionAddFails() {
        Parameter parameter = createAndSaveParameter(HID1, XslNames.IS_SKU, Param.Type.BOOLEAN);
        parameter.setFillDifficulty(BigDecimal.ONE);
        parameterLoaderServiceStub.addCategoryParam(parameter);

        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getAdded().add(new OptionImpl("TRUE"));

        assertThatThrownBy(() -> {
            parameterService.saveParameter(parameterSaveContext, HID1, parameter, parameterValuesChanges);
        }).isInstanceOf(OperationException.class)
            .hasMessage("Нельзя добавить значение в параметр типа BOOLEAN");
    }

    @Test
    public void testAddOverridenOptionToNotOverridenParam() {
        Parameter parameter = createAndSaveParameter(HID1, XslNames.VENDOR, Param.Type.ENUM);
        Option option = createOption(parameter);

        InheritedParameter inheritedParameter = createInheritedParameterWithOverride(HID2, parameter);

        parameterLoaderServiceStub.addAllCategoryParams(inheritedParameter);

        OptionImpl overridenOption = new OptionImpl(Option.OptionType.VENDOR);
        overridenOption.setParent(option);

        parameterService.addLocalVendor(parameterSaveContext, HID2, inheritedParameter, overridenOption);

        CategoryParam savedOverride = parameterTestStorage.getRealIdToParam()
            .get(inheritedParameter.getRealParamId());

        assertThat(savedOverride.getOptions())
            .usingElementComparatorIgnoringFields("id")
            .containsExactly(overridenOption);
    }

    @Test
    public void testDeleteLocalVendor() {
        // assume
        Parameter parentParameter = createAndSaveParameter(HID1, XslNames.VENDOR, Param.Type.ENUM);
        Option option = createOption(parentParameter);

        InheritedParameter subParam = createInheritedParameterWithOverride(HID2, parentParameter);
        parameterLoaderServiceStub.addAllCategoryParams(subParam);

        OptionImpl subOption = new OptionImpl(Option.OptionType.VENDOR);
        subOption.setParent(option);
        parameterService.addLocalVendor(parameterSaveContext, HID2, subParam, subOption);

        // act
        parameterService.removeLocalGuruVendor(parameterSaveContext, subParam, subOption);

        // assert
        CategoryParam actualParam = parameterTestStorage.getRealIdToParam()
            .get(subParam.getRealParamId());

        assertThat(actualParam.getOptions()).isEmpty();
    }

    @Test
    public void testDeleteLocalVendorWillFailIfOptionIsUsedInRule() {
        // assume
        Parameter parentParameter = createAndSaveParameter(HID1, XslNames.VENDOR, Param.Type.ENUM);
        Option option = createOption(parentParameter);

        InheritedParameter subParam = createInheritedParameterWithOverride(HID2, parentParameter);
        parameterLoaderServiceStub.addAllCategoryParams(subParam);

        OptionImpl subOption = new OptionImpl(Option.OptionType.VENDOR);
        subOption.setParent(option);
        parameterService.addLocalVendor(parameterSaveContext, HID2, subParam, subOption);

        // create rules with option
        ModelRule modelRule1 = new ModelRule();
        modelRule1.setName("Rule 1");
        ModelRulePredicate if1 = new ModelRulePredicate(subParam.getId(), IF, MATCHES);
        if1.setValueIds(ImmutableSet.of(subOption.getValueId()));
        ModelRulePredicate then1 = new ModelRulePredicate(ModelProperty.PUBLISHED, THEN, ASSIGN);
        then1.setStringValue("true");
        modelRule1.setIfs(if1);
        modelRule1.setThens(then1);

        ModelRule modelRule2 = new ModelRule();
        modelRule2.setName("Rule 2");
        ModelRulePredicate if2 = new ModelRulePredicate(ModelProperty.PUBLISHED, IF, MATCHES);
        if2.setStringValue("false");
        ModelRulePredicate then2 = new ModelRulePredicate(subParam.getId(), THEN, ADD_VALUE);
        then2.setValueIds(ImmutableSet.of(subOption.getValueId()));
        modelRule2.setIfs(if2);
        modelRule2.setThens(then2);

        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(HID2);
        ruleSet.setRules(Arrays.asList(modelRule1, modelRule2));

        modelRuleService.saveModelRuleSet(ruleSet);

        // assert
        Assertions.assertThatThrownBy(() -> {
            parameterService.removeLocalGuruVendor(parameterSaveContext, subParam, subOption);
        }).isInstanceOf(OperationException.class);
    }

    @Test
    public void testDeleteLocalVendorWillFailIfOptionIsUsedInInheritedRule() {
        // assume
        Parameter parentParameter = createAndSaveParameter(HID1, XslNames.VENDOR, Param.Type.ENUM);
        Option option = createOption(parentParameter);

        InheritedParameter subParam = createInheritedParameterWithOverride(HID2, parentParameter);
        parameterLoaderServiceStub.addAllCategoryParams(subParam);

        OptionImpl subOption = new OptionImpl();
        subOption.setParent(option);
        parameterService.addLocalVendor(parameterSaveContext, HID2, subParam, subOption);

        // create rules with option
        ModelRule modelRule1 = new ModelRule();
        modelRule1.setName("Rule 1");
        ModelRulePredicate if1 = new ModelRulePredicate(subParam.getId(), IF, MATCHES);
        if1.setValueIds(ImmutableSet.of(subOption.getValueId()));
        ModelRulePredicate then1 = new ModelRulePredicate(ModelProperty.PUBLISHED, THEN, ASSIGN);
        then1.setStringValue("true");
        modelRule1.setIfs(if1);
        modelRule1.setThens(then1);

        ModelRule modelRule2 = new ModelRule();
        modelRule2.setName("Rule 2");
        ModelRulePredicate if2 = new ModelRulePredicate(ModelProperty.PUBLISHED, IF, MATCHES);
        if2.setStringValue("false");
        ModelRulePredicate then2 = new ModelRulePredicate(subParam.getId(), THEN, ADD_VALUE);
        then2.setValueIds(ImmutableSet.of(subOption.getValueId()));
        modelRule2.setIfs(if2);
        modelRule2.setThens(then2);

        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(HID2); // <- sub hid
        ruleSet.setRules(Arrays.asList(modelRule1, modelRule2));

        modelRuleService.saveModelRuleSet(ruleSet);

        // assert
        Assertions.assertThatThrownBy(() -> {
            parameterService.removeLocalGuruVendor(parameterSaveContext, subParam, subOption);
        }).isInstanceOf(OperationException.class);
    }

    @Test
    public void testRemoveParam() {
        // assume
        Parameter parentParameter = createAndSaveParameter(HID1, XslNames.LIFE_SHELF, Param.Type.ENUM);

        InheritedParameter subParam = createInheritedParameter(HID2, parentParameter);
        parameterLoaderServiceStub.addAllCategoryParams(subParam);

        // act
        parameterService.removeParameter(parameterSaveContext, subParam);

        // assert
        Set<Long> paramIds = parameterTestStorage.getBreakInheritance().get(HID2);
        assertThat(paramIds).contains(subParam.getId());
    }

    @Test
    public void testRemoveParamFailByLanding() {
        // assume
        createAndSaveParameter(HID1, XslNames.LIFE_SHELF, Param.Type.ENUM);
        Parameter parameter = createAndSaveParameter(HID1, XslNames.LIFE_SHELF, Param.Type.ENUM);
        parameterLoaderServiceStub.addAllCategoryParams(parameter);
        // act
        parameterService.removeParameter(parameterSaveContext, parameter);
    }

    @Test
    public void testLoadFilteredDeletedInheritanceParameters() {
        // assume
        Parameter parentParameter = createAndSaveParameter(HID1, XslNames.LIFE_SHELF, Param.Type.ENUM);

        InheritedParameter subParam = createInheritedParameter(HID2, parentParameter);
        parameterLoaderServiceStub.addAllCategoryParams(subParam);
        subParam.setDeleted(true);

        // act
        GuruParamFilter filter = new GuruParamFilter();
        filter.setDeleted(-1);

        List<CategoryParam> params = parameterLoaderServiceStub.loadFilteredParameters(
            subParam.getCategoryHid(), filter);

        // assert
        Set<Long> paramIds = params.stream().map(CategoryParam::getId).collect(Collectors.toSet());
        assertThat(paramIds).contains(subParam.getId());

        // act
        filter = new GuruParamFilter();
        filter.setDeleted(0);

        params = parameterLoaderServiceStub.loadFilteredParameters(subParam.getCategoryHid(), filter);

        // assert
        paramIds = params.stream().map(CategoryParam::getId).collect(Collectors.toSet());
        assertThat(paramIds).doesNotContain(subParam.getId());

        // act
        filter = new GuruParamFilter();
        filter.setDeleted(1);

        params = parameterLoaderServiceStub.loadFilteredParameters(subParam.getCategoryHid(), filter);

        // assert
        paramIds = params.stream().map(CategoryParam::getId).collect(Collectors.toSet());
        assertThat(paramIds).contains(subParam.getId());
    }

    @Test
    public void testRemoveParamWillFailIfParamIsUsedInRule() {
        // assume
        Parameter parentParameter = createAndSaveParameter(HID1, XslNames.LIFE_SHELF, Param.Type.ENUM);

        InheritedParameter subParam = createInheritedParameter(HID2, parentParameter);
        parameterLoaderServiceStub.addAllCategoryParams(subParam);

        // create rules
        ModelRule modelRule1 = new ModelRule();
        modelRule1.setName("Rule 1");
        ModelRulePredicate if1 = new ModelRulePredicate(subParam.getId(), IF, MATCHES);
        if1.setStringValue("false");
        if1.setValueHolder(new ValueHolder(ValueSource.MODEL_PARAMETER, subParam.getId()));
        ModelRulePredicate then1 = new ModelRulePredicate(ModelProperty.PUBLISHED, THEN, NOT_EMPTY);
        then1.setStringValue("true");
        modelRule1.setIfs(if1);
        modelRule1.setThens(then1);

        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(HID2);
        ruleSet.setRules(Arrays.asList(modelRule1));
        modelRuleService.saveModelRuleSet(ruleSet);

        // act
        // assert
        Assertions.assertThatThrownBy(() -> parameterService.removeParameter(parameterSaveContext, subParam))
            .isInstanceOf(OperationException.class);
    }

    @Test
    public void testRemoveParamWillFailIfParamIsUsedInInheritedRule() {
        // assume
        Parameter parentParameter = createAndSaveParameter(HID1, XslNames.LIFE_SHELF, Param.Type.ENUM);

        InheritedParameter subParam = createInheritedParameter(HID2, parentParameter);
        parameterLoaderServiceStub.addAllCategoryParams(subParam);

        // create rules
        ModelRule modelRule1 = new ModelRule();
        modelRule1.setName("Rule 1");
        ModelRulePredicate if1 = new ModelRulePredicate(subParam.getId(), IF, MATCHES);
        if1.setStringValue("false");
        if1.setValueHolder(new ValueHolder(ValueSource.MODEL_PARAMETER, subParam.getId()));
        ModelRulePredicate then1 = new ModelRulePredicate(ModelProperty.PUBLISHED, THEN, NOT_EMPTY);
        then1.setStringValue("true");
        modelRule1.setIfs(if1);
        modelRule1.setThens(then1);

        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(HID2); // <- sub hid
        ruleSet.setRules(Arrays.asList(modelRule1));
        modelRuleService.saveModelRuleSet(ruleSet);

        // act
        // assert
        Assertions.assertThatThrownBy(() -> parameterService.removeParameter(parameterSaveContext, subParam))
            .isInstanceOf(OperationException.class);
    }

    @Test
    public void testAddGlobalParameterToCategory() {
        Parameter global = new Parameter();
        global.setCategoryHid(KnownIds.GLOBAL_CATEGORY_ID);
        global.setType(Param.Type.NUMERIC);
        global.setXslName("Some global parameter");
        global.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "param name")));

        parameterDAOMock.insertParameter(global);
        parameterLoaderServiceStub.addCategoryParam(global);

        parameterService.addGlobalParameters(parameterSaveContext, HID1, singletonList(global.getId()));

        CategoryParam savedOverride = parameterTestStorage.getParameter(HID1, global.getId());
        assertThat(savedOverride.getTimestamp()).isNotNull();
    }

    @Test
    public void testAddOverrideForExistingParameter() {
        Parameter parent = new Parameter();
        parent.setCategoryHid(HID1);
        parent.setType(Param.Type.NUMERIC);
        parent.setXslName("Some parent parameter");
        parent.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "param name")));

        parameterDAOMock.insertParameter(parent);

        InheritedParameter localParam = new InheritedParameter(parent);
        localParam.setCategoryHid(HID2);
        localParam.setComment("some comment"); //to instantiate internal override object
        parameterService.saveParameter(
            parameterSaveContext, localParam.getCategoryHid(), localParam, new ParameterValuesChanges());

        CategoryParam savedOverride = parameterTestStorage.getParameter(localParam.getCategoryHid(), parent.getId());
        assertThat(savedOverride.getTimestamp()).isNotNull();
        assertThat(savedOverride.getComment()).isEqualTo(localParam.getComment());
    }

    @Test
    public void testXslNameDuplicationCheckWorks() {
        Parameter param1 = new Parameter();
        param1.setCategoryHid(HID1);
        param1.setType(Param.Type.NUMERIC);
        param1.setXslName("xslName1");
        param1.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "param name")));

        parameterDAOMock.insertParameter(param1);
        parameterLoaderServiceStub.addCategoryParam(param1);

        Parameter param2 = new Parameter();
        param2.setCategoryHid(HID1);
        param2.setType(Param.Type.NUMERIC);
        param2.setXslName("xslName1");
        param2.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "param name 2")));

        assertThatThrownBy(() -> parameterService.saveParameter(
            parameterSaveContext, param2.getCategoryHid(), param2, new ParameterValuesChanges()))
            .isInstanceOf(OperationException.class)
            .hasMessageContaining("Дублирование xslName");
    }

    @Test
    public void testXslNameDuplicationSkippedIfNotChanged() {
        Parameter param1 = new Parameter();
        param1.setCategoryHid(HID1);
        param1.setType(Param.Type.NUMERIC);
        param1.setXslName("xslName1");
        param1.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "param name")));

        parameterDAOMock.insertParameter(param1);
        parameterLoaderServiceStub.addCategoryParam(param1);

        Parameter param2 = new Parameter();
        param2.setCategoryHid(HID1);
        param2.setType(Param.Type.NUMERIC);
        param2.setXslName("xslName1");
        param2.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "param name 2")));

        parameterDAOMock.insertParameter(param2);
        parameterLoaderServiceStub.addCategoryParam(param2);

        CategoryParam paramUpdated = param1.copy();
        paramUpdated.setId(param1.getId());
        paramUpdated.setAdvFilterIndex(100);

        parameterService.saveParameter(
            parameterSaveContext, paramUpdated.getCategoryHid(), paramUpdated, new ParameterValuesChanges());

        // No exception means validation passed
    }

    @Test
    public void testUseExistingDirectIncludeOptionOfGlParameter() {
        CategoryParam glParameter = createAndSaveParameter(KnownIds.GLOBAL_CATEGORY_ID, XslNames.VENDOR,
            Param.Type.ENUM, CategoryParam.LocalValueInheritanceStrategy.DIRECT_INCLUDE);
        Option option = createDirectIncludeOption(glParameter);
        glParameter = parameterTestStorage.getRealIdToParam().get(glParameter.getRealParamId());
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(glParameter);

        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        Option localOption = option.copy();
        localOption.setParent(option);
        EnumAlias newAlias = new EnumAlias(Word.EMPTY_ID, Word.DEFAULT_LANG_ID, "alias");
        localOption.setInheritanceStrategy(Option.InheritanceStrategy.INHERIT);
        localOption.addAlias(newAlias);
        parameterValuesChanges.getAdded().add(localOption);

        InheritedParameter localParameter = createInheritedParameterWithOverride(HID1, glParameter);
        parameterLoaderServiceStub.addCategoryParam(glParameter);
        parameterLoaderServiceStub.addAllCategoryParams(localParameter);

        Option savedOption = parameterService.addValue(parameterSaveContext, localParameter, localOption);
        assertThat(savedOption.getId()).isNotEqualTo(option.getId());
        assertThat(savedOption.getAliases()).hasSize(1);
        assertThat(savedOption.getAliases().get(0)).isEqualTo(newAlias);

        Set<Long> categoryHids = parameterTestStorage.getDirectInclude().get(localParameter.getRealParamId());
        assertThat(categoryHids).contains(option.getId());
    }

    @Test
    public void testConcurrentAddOverridenOption() {
        Parameter parameter = createAndSaveParameter(HID1, XslNames.VENDOR, Param.Type.ENUM);
        Option option = createOption(parameter);

        InheritedParameter inheritedParameter = createInheritedParameter(HID2, parameter);

        parameterLoaderServiceStub.addAllCategoryParams(inheritedParameter);

        //имитация изменения параметра в параллельном потоке
        parameterDAOMock.touchParam(inheritedParameter, Timestamp.from(Instant.now().plusSeconds(1)));

        OptionImpl overridenOption = new OptionImpl(Option.OptionType.SIMPLE);
        overridenOption.setParent(option);

        // вендора теперь сохраняются без проверок - дабы ускорить
        Assertions.assertThatNoException().isThrownBy(() -> {
            parameterService.addLocalVendor(parameterSaveContext, HID2, inheritedParameter, overridenOption);
        });
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testRealConcurrentAddOverriddenOption() throws InterruptedException {
        Parameter parameter = createAndSaveParameter(HID1, XslNames.VENDOR, Param.Type.ENUM);
        Option option = createOption(parameter);

        InheritedParameter inheritedParameter = createInheritedParameter(HID2, parameter);

        parameterLoaderServiceStub.addAllCategoryParams(inheritedParameter);

        ExecutorService s = Executors.newFixedThreadPool(TEST_THREAD_COUNT);
        List<Callable<Void>> tasks = Collections.nCopies(TEST_THREAD_COUNT, () -> {
            OptionImpl overridenOption = new OptionImpl(Option.OptionType.VENDOR);
            overridenOption.setParent(option);
            try {
                parameterService.addLocalVendor(parameterSaveContext, HID2, inheritedParameter, overridenOption);
            } catch (Exception e) {
                if (e instanceof ConcurrentUpdateException) {
                    //выполняем откат транзакции ))
                    parameterDAOMock.removeOption(overridenOption, inheritedParameter);
                } else {
                    throw e;
                }
                log.error(e);
            }
            return null;
        });
        s.invokeAll(tasks);

        CategoryParam savedOverride = parameterTestStorage.getRealIdToParam()
            .get(inheritedParameter.getRealParamId());

        // вендора теперь сохраняются без проверок - дабы ускорить
        assertThat(savedOverride.getOptions()).hasSize(TEST_THREAD_COUNT);
    }

    public Option createOption(CategoryParam param) {
        Option option = newRandom.nextObject(OptionImpl.class, "id", "parent");
        option.setParamId(param.getRealParamId());
        parameterDAOMock.insertEnumOption(param, (OptionImpl) option);
        return option;
    }

    public Option createNewOption(CategoryParam param) {
        Option option = newRandom.nextObject(OptionImpl.class, "id", "parent");
        option.setParamId(param.getRealParamId());
        option.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "option")));
        option.setInheritanceStrategy(Option.InheritanceStrategy.DIRECT_INCLUDE);
        return option;
    }

    public Option createDirectIncludeOption(CategoryParam param) {
        Option option;
        if (XslNames.VENDOR.equals(param.getXslName())) {
            option = new OptionImpl(Option.OptionType.VENDOR);
        } else {
            option = RandomTestUtils.randomObject(OptionImpl.class, "id", "parent");
        }
        option.setParamId(param.getRealParamId());
        option.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "option")));
        option.setInheritanceStrategy(Option.InheritanceStrategy.DIRECT_INCLUDE);
        parameterDAOMock.insertEnumOption(param, (OptionImpl) option);
        return option;
    }

    public Parameter createParameter(long hid, String xslName, Param.Type type,
                                     CategoryParam.LocalValueInheritanceStrategy localValueInheritanceStrategy,
                                     boolean clearOptions) {
        Parameter result = newRandom.nextObject(Parameter.class, "id");
        result.setCategoryHid(hid);
        result.setXslName(xslName);
        result.setType(type);
        result.setTimestamp(null);
        result.setCopySourceParams(null);
        result.setLocalValueInheritanceStrategy(localValueInheritanceStrategy);
        if (clearOptions) {
            result.clearOptions();
        }
        return result;
    }

    public Parameter createAndSaveParameter(long hid, String xslName, Param.Type type,
                                            CategoryParam.LocalValueInheritanceStrategy localValueInheritanceStrategy,
                                            boolean clearOptions) {
        Parameter result = createParameter(hid, xslName, type, localValueInheritanceStrategy, clearOptions);
        parameterDAOMock.insertParameter(result);
        return result;
    }

    public Parameter createAndSaveParameter(long hid, String xslName, Param.Type type,
                                            CategoryParam.LocalValueInheritanceStrategy localValueInheritanceStrategy) {
        return createAndSaveParameter(hid, xslName, type, localValueInheritanceStrategy, false);
    }

    public Parameter createAndSaveParameter(long hid, String xslName, Param.Type type, boolean clearOptions) {
        return createAndSaveParameter(hid, xslName, type, CategoryParam.LocalValueInheritanceStrategy.INHERIT,
            clearOptions);
    }

    public Parameter createAndSaveParameter(long hid, String xslName, Param.Type type) {
        return createAndSaveParameter(hid, xslName, type, false);
    }

    public InheritedParameter createInheritedParameter(long hid, CategoryParam parameter) {
        InheritedParameter inheritedParameter = new InheritedParameter(parameter);
        inheritedParameter.setCategoryHid(hid);
        parameterDAOMock.insertParameter(inheritedParameter);
        updateInheritedParameterSavedTs(inheritedParameter);
        return inheritedParameter;
    }

    public InheritedParameter createInheritedParameterWithOverride(long hid, CategoryParam parameter) {
        InheritedParameter inheritedParameter = createInheritedParameter(hid, parameter);
        inheritedParameter.addOverride(new ParameterOverride());
        parameterDAOMock.createOverride(hid, inheritedParameter);
        updateInheritedParameterSavedTs(inheritedParameter);
        return inheritedParameter;
    }

    private void updateInheritedParameterSavedTs(InheritedParameter inheritedParameter) {
        Timestamp savedTs = parameterDAOMock.getParameterTimestamp(
            inheritedParameter.getCategoryHid(),
            inheritedParameter.getId());
        if (inheritedParameter.getOverride() != null) {
            inheritedParameter.getOverride().setTimestamp(savedTs);
        } else {
            inheritedParameter.setTimestamp(savedTs);
        }
    }
}
