package ru.yandex.market.mbo.db.params;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.catalogue.CategoryFavoriteVendorServiceMock;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.model.Language;
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
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.ParameterOverride;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplates;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.tt.TaskTrackerMock;
import ru.yandex.market.mbo.utils.RandomTestUtils;
import ru.yandex.market.mbo.validator.OptionAliasDuplicationValidator;
import ru.yandex.market.mbo.validator.OptionNameDuplicationValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link ParameterService}.
 */
public class ParameterServiceInheritanceTest {
    private static final Logger log = LoggerFactory.getLogger(ParameterServiceInheritanceTest.class);
    private static final long SEED = 1L;

    private static final long HID1 = 1L;
    private static final long HID2 = 2L;
    private static final long HID3 = 3L;

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
    VendorGoodContentExclusionService vendorGoodContentExclusionService;


    private final ParameterSaveContext parameterSaveContext = new ParameterSaveContext(0L);
    private final IdGenerator kdepotIdGenerator = new IdGeneratorStub();
    private final ParameterTestStorage parameterTestStorage = new ParameterTestStorage();
    private final ParameterLoaderServiceStub parameterLoaderServiceStub = new ParameterLoaderServiceStub();
    private final ParameterDAOMock parameterDAOMock = new ParameterDAOMock(kdepotIdGenerator, parameterTestStorage);
    private final RecipeService recipeService = new RecipeService(null, new RecipeServiceDaoMock());

    private ParameterService parameterService;
    private KnowledgeDepotServiceMock kd;
    private TovarTreeDaoMock tovarTreeDao;
    private ModelRuleService modelRuleService;
    private ModelRuleTaskService modelRuleTaskService;
    private EnhancedRandom newRandom;


    @Before
    public void setUp() {
        newRandom = RandomTestUtils.createNewRandom(SEED);

        when(parameterLinkService.getLinkedParameters(any(CategoryParam.class), any()))
            .thenReturn(new MultiMap<>());
        when(glRulesService.loadCategoryRulesByHid(anyLong())).thenReturn(new CategoryRules());
        when(patternService.loadCategoryPatternsByHid(any())).thenReturn(new CategoryPatterns());
        when(outputTemplateService.getTemplates(anyLong())).thenReturn(new OutputTemplates());


        kd = new KnowledgeDepotServiceMock();

        tovarTreeDao = new TovarTreeDaoMock();
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
            recipeService,
            modelFormService,
            outputTemplateService,
            ytSaasIndexesWrapper,
            parameterDAOMock,
            patternService,
            parameterLoaderServiceStub
        );

        ParameterServiceReader  parameterServiceReader = new ParameterServiceReader(
                new TovarTreeForVisualServiceMock(tovarTreeDao),
                tovarTreeDao);
        parameterServiceReader.setParameterDAO(parameterDAOMock);
        parameterServiceReader.setParameterValidationService(parameterValidationService);
        parameterServiceReader.setParameterLoader(parameterLoaderServiceStub);

        ParameterServiceFast  parameterServiceFast = new ParameterServiceFast(
                new TransactionTemplateMock());

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
    }

    @Test
    public void testInheritance() {
        Parameter parent = createAndSaveParameter(HID1, "Parent enum parameter", Param.Type.ENUM,
            CategoryParam.LocalValueInheritanceStrategy.INHERIT);
        parent.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "enum")));

        Set<String> options = new LinkedHashSet<String>() {{
            add("option1");
            add("option2");
            add("option3");
        }};

        ParameterValuesChanges parentValuesChanges = new ParameterValuesChanges();
        List<Option> parentOptions = new ArrayList<>();
        options.forEach(o -> {
            Option option = new OptionImpl();
            option.addName(new Word(Word.DEFAULT_LANG_ID, o));
            option.setPublished(true);
            parent.addOption(option);
            parentOptions.add(option);
            parentValuesChanges.valueUpdated(option);
        });

        // сохранение через parametervaluechanges

        parameterService.saveParameter(
            parameterSaveContext, parent.getCategoryHid(), parent, parentValuesChanges);

        parameterLoaderServiceStub.addCategoryParam(parent);

        log.debug("parent = " + parent);

        CategoryParam savedParent = parameterLoaderServiceStub
            .loadCategoryEntitiesByHid(parent.getCategoryHid()).getParameterById(parent.getId());

        assertThat(savedParent).isNotNull();

        parameterLoaderServiceStub.addCategoryParam(savedParent);

        InheritedParameter child = createInheritedParameter(HID2, savedParent);
        child.setDescription("dd");
        child.setLocalizedAliases(WordUtil.defaultWords("enum_child", "enum_child_alias"));

        log.debug("child before save = " + child);

        // ожидание: сохраняется унаследованный параметр и загружается из категорийного кэша
        parameterService.saveParameter(
            parameterSaveContext, child.getCategoryHid(), child, new ParameterValuesChanges());

        // NB! hack
        child.setTimestamp(parameterTestStorage.getRealIdToParam().get(child.getRealParamId()).getTimestamp());
        parameterLoaderServiceStub.addCategoryParam(child);
        // /hack

        log.debug("just saved child.getTimestamp() = " + child.getTimestamp());
        log.debug("just saved child override.getTimestamp() = " + child.getOverride().getTimestamp());
        log.debug("just saved child parent.getTimestamp() = " + child.getParent().getTimestamp());


        CategoryParam savedChild = parameterLoaderServiceStub
            .loadCategoryEntitiesByHid(child.getCategoryHid()).getParameterById(child.getId());

        assertThat(savedChild).isNotNull();

        log.debug("savedChild = " + savedChild);
        log.debug("savedChild.getId() = " + savedChild.getId());
        log.debug("savedChild.getRealParamId() = " + savedChild.getRealParamId());
        log.debug("savedChild.getTimestamp() = " + savedChild.getTimestamp());

        // задаём переопределённую и наследуемую опции
        Option parentOptionToChange = parentOptions.get(1);
        OptionImpl childOption = new OptionImpl(parentOptionToChange);
        childOption.setPublished(false);

        log.debug("childOption = " + childOption);

        savedChild.removeOption(parentOptionToChange);
        savedChild.addOption(childOption);

        ParameterValuesChanges overridenParameterValueChanges = new ParameterValuesChanges();
        // unsupported, as it goes directly to db, no mock:
        // at ru.yandex.market.mbo.db.params.ParameterService.enableValueInheritance(ParameterService.java:962)
//        overridenParameterValueChanges.valueInherited(parentOptionToInherit);
        // unsupported, fails on validation
//        overridenParameterValueChanges.valueDeleted(parentOptionToChange);
        overridenParameterValueChanges.valueUpdated(childOption);

        // ожидание: параметр сохранён, добавлены изменения опций
        parameterService.saveParameter(
            parameterSaveContext, savedChild.getCategoryHid(), savedChild,
            overridenParameterValueChanges);

        parameterLoaderServiceStub.addCategoryParam(savedChild);

        CategoryParam savedParameter = parameterLoaderServiceStub
            .loadCategoryEntitiesByHid(savedChild.getCategoryHid()).getParameterById(savedChild.getId());

        assertThat(savedParameter).isNotNull();

        log.debug("parent = " + parent);
        log.debug("parent.getOptions() = " + parent.getOptions());
        log.debug("savedParameter = " + savedParameter);
        log.debug("savedParameter.getOptions() = " + savedParameter.getOptions());


        assertThat(savedParameter.getTimestamp()).isNotNull();
        assertThat(savedParameter.getComment()).isEqualTo(child.getComment());

        List<Option> savedOverrideOptions = savedParameter.getOptions();
        log.debug("savedParameter = " + savedParameter);

        // check simple OptionImpl consistency
        Option savedOverridenOption = savedOverrideOptions.stream().filter(o -> "option1".equals(o.getName()))
            .findFirst().get();
        assertThat(savedOverridenOption).isNotNull();

        // check inherited overriden options
        Option inheritedOverridenOption = savedOverrideOptions.stream().filter(o -> "option2".equals(o.getName()))
            .findFirst().get();
        assertThat(inheritedOverridenOption).isNotNull();
        Option parentOption = parentOptions.stream().filter(o -> "option2".equals(o.getName()))
            .findFirst().get();
        assertThat(parentOption).isNotNull();

        assertThat(inheritedOverridenOption.fullyEquals(parentOption)).isNotEqualTo(true);

        assertThat(parentOption.isPublished()).isEqualTo(true);
        assertThat(inheritedOverridenOption.isPublished()).isEqualTo(false);

        log.debug("savedOverrideOptions = " + savedOverrideOptions);
    }

    @Test
    public void testInheritanceWithAliases() {
        Parameter parent = createAndSaveParameter(HID1, "Parent enum parameter", Param.Type.ENUM,
            CategoryParam.LocalValueInheritanceStrategy.INHERIT);
        parent.setNames(singletonList(new Word(Word.DEFAULT_LANG_ID, "enum")));

        Set<String> options = new LinkedHashSet<String>() {{
            add("option1");
            add("option2");
            add("option3");
        }};

        ParameterValuesChanges parentValuesChanges = new ParameterValuesChanges();
        List<Option> parentOptions = new ArrayList<>();
        options.forEach(o -> {
            Option option = new OptionImpl();
            option.addName(new Word(Word.DEFAULT_LANG_ID, o));
            parent.addOption(option);
            parent.setPublished(true);
            parentOptions.add(option);
            parentValuesChanges.valueUpdated(option);
        });

        // сохранение через parametervaluechanges

        parameterService.saveParameter(
            parameterSaveContext, parent.getCategoryHid(), parent, parentValuesChanges);

        parameterLoaderServiceStub.addCategoryParam(parent);

        log.debug("parent = " + parent);

        CategoryParam savedParent = parameterLoaderServiceStub
            .loadCategoryEntitiesByHid(parent.getCategoryHid()).getParameterById(parent.getId());

        assertThat(savedParent).isNotNull();

        parameterLoaderServiceStub.addCategoryParam(savedParent);

        InheritedParameter child = createInheritedParameter(HID2, savedParent);
        child.setDescription("dd");
        child.setLocalizedAliases(WordUtil.defaultWords("enum_child", "enum_child_alias"));

        log.debug("child before save = " + child);

        // ожидание: сохраняется унаследованный параметр и загружается из категорийного кэша
        parameterService.saveParameter(
            parameterSaveContext, child.getCategoryHid(), child, new ParameterValuesChanges());

        // NB! hack
        child.setTimestamp(parameterTestStorage.getRealIdToParam().get(child.getRealParamId()).getTimestamp());
        parameterLoaderServiceStub.addCategoryParam(child);

        CategoryParam savedChild = parameterLoaderServiceStub
            .loadCategoryEntitiesByHid(child.getCategoryHid()).getParameterById(child.getId());

        assertThat(savedChild).isNotNull();

        // задаём переопределённую и наследуемую опции
        List<Option> localOptions = new ArrayList<>();
        Option parentOptionToChange = parentOptions.get(1);
        OptionImpl childOption = new OptionImpl(parentOptionToChange);
        childOption.setActive(false);
        childOption.setAliases(new ArrayList<EnumAlias>() {{
            add(new EnumAlias(0, Language.RUSSIAN.getId(), "такое"));
        }});

        log.debug("childOption = " + childOption);

        savedChild.setOptions(localOptions);

        ParameterValuesChanges overridenParameterValueChanges = new ParameterValuesChanges();
        overridenParameterValueChanges.valueUpdated(childOption);

        // ожидание: параметр не сохранён, не даём изменять имя -- валимся на валидации
        parameterService.saveParameter(
            parameterSaveContext, savedChild.getCategoryHid(), savedChild,
            overridenParameterValueChanges);
    }

    public Parameter createParameter(long hid, String xslName, Param.Type type,
                                     CategoryParam.LocalValueInheritanceStrategy localValueInheritanceStrategy) {
        Parameter result = newRandom.nextObject(Parameter.class, "id");
        result.setCategoryHid(hid);
        result.setXslName(xslName);
        result.setType(type);
        result.setTimestamp(null);
        result.setCopySourceParams(null);
        result.setLocalValueInheritanceStrategy(localValueInheritanceStrategy);
        return result;
    }

    public Parameter createAndSaveParameter(long hid, String xslName, Param.Type type,
                                            CategoryParam.LocalValueInheritanceStrategy localValueInheritanceStrategy) {
        Parameter result = createParameter(hid, xslName, type, localValueInheritanceStrategy);
        parameterDAOMock.insertParameter(result);
        return result;
    }

    public Option createOption(CategoryParam param) {
        Option option = newRandom.nextObject(OptionImpl.class, "id", "parent");
        option.setParamId(param.getRealParamId());
        parameterDAOMock.insertEnumOption(param, (OptionImpl) option);
        return option;
    }

    public Option createDirectIncludeOption(CategoryParam param) {
        Option option = RandomTestUtils.randomObject(OptionImpl.class, "id", "parent");
        option.setParamId(param.getRealParamId());
        option.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "option")));
        option.setInheritanceStrategy(Option.InheritanceStrategy.DIRECT_INCLUDE);
        parameterDAOMock.insertEnumOption(param, (OptionImpl) option);
        return option;
    }

    public Parameter createAndSaveParameter(long hid, String xslName, Param.Type type) {
        return createAndSaveParameter(hid, xslName, type, CategoryParam.LocalValueInheritanceStrategy.INHERIT);
    }

    public InheritedParameter createInheritedParameter(long hid, CategoryParam parameter) {
        InheritedParameter inheritedParameter = new InheritedParameter(parameter);
        inheritedParameter.setCategoryHid(hid);
        return inheritedParameter;
    }

    public InheritedParameter createInheritedParameterWithOverride(long hid, CategoryParam parameter) {
        InheritedParameter inheritedParameter = createInheritedParameter(hid, parameter);
        inheritedParameter.addOverride(new ParameterOverride());
        parameterDAOMock.createOverride(hid, inheritedParameter);
        return inheritedParameter;
    }
}
