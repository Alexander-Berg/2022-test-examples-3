package ru.yandex.market.mbo.db.params;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotService;
import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.IndexedModelQueryService;
import ru.yandex.market.mbo.db.params.audit.ParameterAuditService;
import ru.yandex.market.mbo.db.params.validators.ParameterValidationService;
import ru.yandex.market.mbo.db.recommendations.RecommendationValidationService;
import ru.yandex.market.mbo.db.templates.OutputTemplateService;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.CategoryPatterns;
import ru.yandex.market.mbo.gwt.models.params.CategoryRules;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.LinkType;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.ParamOptionsAccessType;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.GLRuleOverride;
import ru.yandex.market.mbo.gwt.models.visual.InheritedGLRule;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.validator.OptionNameDuplicationValidator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for ParameterService.
 *
 * @author moskovkin@yandex-team.ru
 * @since 02.06.17
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ParameterServiceMockitoTest {

    public static final long HID_1 = 1;
    public static final long HID_1_2 = 3;

    public static final long NEW_OPTION_ID = 0;
    public static final Option NEW_OPTION = OptionBuilder.newBuilder()
        .setId(NEW_OPTION_ID)
        .setFilterValue(false)
        .setPublished(true)
        .addName("new value")
        .build();

    public static final long PARAM_ID = 1;

    @Mock
    private UserManager userManager;
    @Mock
    private TransactionTemplate contentTx;
    @Mock
    private NamedParameterJdbcTemplate contentJdbc;
    @Mock
    private WordDBService wordService;
    @Mock
    private IParameterLoaderService parameterLoader;
    @Mock
    private ParameterLinkService parameterLinkService;
    @Mock
    private KnowledgeDepotService kd;
    @Mock
    private TaskTracker taskTracker;
    @Mock
    private TovarTreeForVisualService tovarTree;
    @Mock
    private TovarTreeDao tovarTreeDao;
    @Mock
    private GLRulesService rulesService;
    @Mock
    private PatternService patternService;
    @Mock
    private IdGenerator kdepotIdGenerator;
    @Mock
    private ValueLinkServiceInterface valueLinkService;
    @Mock
    private OutputTemplateService outputTemplateService;
    @Mock
    private RecommendationValidationService recommendationValidationService;
    @Mock
    private ParameterDAO parameterDAO;
    @Mock
    private TovarTree tovarTreeNode;
    @Mock
    private ParameterValidationService parameterValidationService;
    @Mock
    private ParameterAuditService parameterAuditService;
    @Mock
    private IndexedModelQueryService indexedModelQueryService;

    @InjectMocks
    private ParameterServiceOriginal parameterService;

    private ParameterSaveContext saveContext;
    private final AutoUser autoUser = new AutoUser(1L);

    @Before
    public void setUp() throws Exception {
        ParameterServiceReader parameterServiceReader =
                new ParameterServiceReader(tovarTree, tovarTreeDao);
        parameterServiceReader.setParameterLoader(parameterLoader);
        parameterServiceReader.setParameterDAO(parameterDAO);
        parameterServiceReader.setParameterValidationService(parameterValidationService);
        parameterService.setParameterDAO(parameterDAO);
        parameterService.setParameterLoader(parameterLoader);
        parameterService.setParameterAuditService(parameterAuditService);
        parameterService.setParameterValidationService(parameterValidationService);
        parameterService.setWordService(wordService);
        parameterService.setParameterLinkService(parameterLinkService);
        parameterService.setValueLinkService(valueLinkService);
        parameterService.setParameterServiceReader(parameterServiceReader);
        saveContext = parameterService.createDefaultSaveContext(autoUser.getId());
    }

    /**
     * Stub ParameterService.subTree() method.
     *
     * @param categoryId - id of toplevel category.
     * @param subtree    - ParameterService.subTree() result stub.
     */
    private void prepareCategorySubtree(long categoryId, List<Long> subtree) {
        when(tovarTree
            .loadSchemeWholeTree())
            .thenReturn(tovarTreeNode);

        when(tovarTreeNode
            .findAllHids(eq(Arrays.asList(categoryId))))
            .thenReturn(new HashSet<>(subtree));
    }

    private ParameterValuesChanges createValuesChanges(List<Option> updates, List<Option> deletes) {
        ParameterValuesChanges result = new ParameterValuesChanges();
        updates.stream().forEach(option -> result.valueUpdated(option));
        deletes.stream().forEach(option -> result.valueDeleted(option));
        return result;
    }

    @Test
    public void checkContainsParamInRuleOrPatternWhenOverriddenGlobalRuleContainsParameter() {
        Parameter param1 = CategoryParamBuilder.newBuilder()
            .setId(PARAM_ID)
            .setCategoryHid(HID_1)
            .setXslName("param_1")
            .build();
        InheritedParameter param2 = new InheritedParameter(param1);
        param2.setCategoryHid(HID_1_2);

        prepareCategorySubtree(HID_1_2, Arrays.asList(HID_1_2));

        GLRulePredicate rulePredicate = new GLRulePredicate();
        rulePredicate.setParamId(PARAM_ID);
        GLRule globalRule = new GLRuleImpl();
        globalRule.setHid(KnownIds.GLOBAL_CATEGORY_ID);
        globalRule.getIfs().add(rulePredicate);
        GLRuleOverride overrideRule = new GLRuleOverride();
        overrideRule.setHid(HID_1);
        GLRule rule = new InheritedGLRule(globalRule, overrideRule);

        when(rulesService.loadCategoryRulesByHid(HID_1_2)).then(new Answer<CategoryRules>() {
            @Override
            public CategoryRules answer(InvocationOnMock invocation) throws Throwable {
                CategoryRules rules = new CategoryRules();
                rules.addRule(rule);
                return rules;
            }
        });
        when(patternService.loadCategoryPatternsByHid(Mockito.anyLong())).thenReturn(new CategoryPatterns());
        when(rulesService.loadRules(anyCollection())).thenReturn(new HashMap<>());

        parameterService.checkContainsParamInRuleOrPattern(param2);

        // saveParameter not called
        verify(parameterDAO, Mockito.times(0))
            .runAfterTransaction(Mockito.any(Runnable.class));
    }

    @Test
    public void checkContainsParamInRuleOrPatternWhenNoManualRuleContainsParameter() {
        Parameter param1 = CategoryParamBuilder.newBuilder()
            .setId(PARAM_ID)
            .setCategoryHid(HID_1)
            .setXslName("param_1")
            .build();

        prepareCategorySubtree(HID_1, Arrays.asList(HID_1));

        GLRulePredicate rulePredicate = new GLRulePredicate();
        rulePredicate.setParamId(PARAM_ID);
        GLRule rule = new GLRuleImpl();
        rule.setType(GLRuleType.SIZE_MIGRATION);
        rule.setHid(HID_1);
        rule.getIfs().add(rulePredicate);

        when(rulesService.loadCategoryRulesByHid(HID_1)).then(new Answer<CategoryRules>() {
            @Override
            public CategoryRules answer(InvocationOnMock invocation) throws Throwable {
                CategoryRules rules = new CategoryRules();
                rules.addRule(rule);
                return rules;
            }
        });
        when(patternService.loadCategoryPatternsByHid(Mockito.anyLong())).thenReturn(new CategoryPatterns());
        when(rulesService.loadRules(anyCollection())).then(new Answer<Map<Long, List<GLRule>>>() {
            @Override
            public Map<Long, List<GLRule>> answer(InvocationOnMock invocation) throws Throwable {
                Map<Long, List<GLRule>> rules = new HashMap<>();
                rules.put(rule.getId(), Arrays.asList(rule));
                return rules;
            }
        });

        List<GLRule> deletedRules = parameterService.checkContainsParamInRuleOrPattern(param1);
        List<GLRule> expectedDeletedRules = Arrays.asList(rule);
        Assert.assertEquals(expectedDeletedRules, deletedRules);

        // saveParameter not called
        verify(parameterDAO, Mockito.times(0))
            .runAfterTransaction(Mockito.any(Runnable.class));
    }

    @Test(expected = OperationException.class)
    public void testThrowIfCanNotAddGlobalParametersIfParamExists() {
        Map<Long, CategoryParam> globalParameters = new HashMap<>();
        Set<Long> mutualLinkedParams = new HashSet<>();
        Map<Long, Set<Long>> partialLinkedParams = new HashMap<>();
        Map<String, List<Pair<Long, Long>>> existingParameters = new HashMap<>();

        CategoryParam param = new Parameter();
        param.setId(1);
        param.setXslName("xsl1");

        existingParameters.put("xsl1", ImmutableList.of(new ImmutablePair<>(1L, 2L)));

        globalParameters.put(param.getId(), param);

        parameterService.throwIfCanNotAddGlobalParameters(
            Collections.emptySet(), globalParameters, mutualLinkedParams, partialLinkedParams, existingParameters,
            Collections.emptySet()
        );

        // saveParameter not called
        verify(parameterDAO, Mockito.times(0))
            .runAfterTransaction(Mockito.any(Runnable.class));
    }


    @Test(expected = OperationException.class)
    public void testAddGlobalParameterIfPartialParameterNotAdded() {
        final long parameterForAddId = 1L;
        final long partialParameterId = 2L;

        Map<Long, CategoryParam> globalParameters = new HashMap<>();

        Map<Long, Set<Long>> partialLinkedParams = new HashMap<>();
        partialLinkedParams.put(parameterForAddId, Collections.singleton(partialParameterId));

        CategoryParam param = new Parameter();
        param.setId(parameterForAddId);
        param.setXslName("xsl1");

        globalParameters.put(param.getId(), param);

        parameterService.throwIfCanNotAddGlobalParameters(
            Collections.emptySet(), globalParameters, Collections.emptySet(), partialLinkedParams,
            Collections.emptyMap(), Collections.emptySet()
        );
    }

    @Test
    public void testAddGlobalParameterIfPartialParametersAddedTogether() {
        final long parameterForAddId = 1L;
        final long partialParameterId = 2L;

        Map<Long, CategoryParam> globalParameters = new HashMap<>();

        Map<Long, Set<Long>> partialLinkedParams = new HashMap<>();
        partialLinkedParams.put(parameterForAddId, Collections.singleton(partialParameterId));
        partialLinkedParams.put(partialParameterId, Collections.singleton(parameterForAddId));

        CategoryParam firstParam = new Parameter();
        firstParam.setId(parameterForAddId);
        firstParam.setXslName("xsl1");
        CategoryParam secondParam = new Parameter();
        secondParam.setId(partialParameterId);
        secondParam.setXslName("xsl2");

        globalParameters.put(parameterForAddId, firstParam);
        globalParameters.put(partialParameterId, secondParam);

        parameterService.throwIfCanNotAddGlobalParameters(
            Collections.emptySet(), globalParameters, Collections.emptySet(), partialLinkedParams,
            Collections.emptyMap(), Collections.emptySet()
        );
    }

    @Test
    public void testAddGlobalParameterIfPartialParametersAlreadyAdded() {
        final long parameterForAddId = 1L;
        final long partialParameterId = 2L;

        Map<Long, CategoryParam> globalParameters = new HashMap<>();

        Map<Long, Set<Long>> partialLinkedParams = new HashMap<>();
        partialLinkedParams.put(parameterForAddId, Collections.singleton(partialParameterId));

        CategoryParam firstParam = new Parameter();
        firstParam.setId(parameterForAddId);
        firstParam.setXslName("xsl1");

        globalParameters.put(parameterForAddId, firstParam);

        parameterService.throwIfCanNotAddGlobalParameters(
            Collections.singleton(partialParameterId), globalParameters, Collections.emptySet(), partialLinkedParams,
            Collections.emptyMap(), Collections.emptySet()
        );
    }

    @Test
    public void testAllowAddGlobalParametersIfSameInheritedParamExists() {
        Map<Long, CategoryParam> globalParameters = new HashMap<>();
        Set<Long> linkedParams = new HashSet<>();
        Map<Long, Set<Long>> partialLinkedParams = new HashMap<>();
        Map<String, List<Pair<Long, Long>>> existingParameters = new HashMap<>();

        CategoryParam param = new Parameter();
        param.setId(1);
        param.setXslName("xsl1");

        existingParameters.put("xsl1", ImmutableList.of(new ImmutablePair<>(1L, param.getId())));

        globalParameters.put(param.getId(), param);

        // Should not throw
        parameterService.throwIfCanNotAddGlobalParameters(
            Collections.emptySet(), globalParameters, linkedParams, partialLinkedParams, existingParameters,
            Collections.emptySet()
        );
    }

    @Test(expected = OperationException.class)
    public void testAddGlobalParametersIfParamExists() {
        Map<Long, CategoryParam> globalParameters = new HashMap<>();

        CategoryParam param = new Parameter();
        param.setId(1);
        param.setXslName("xsl1");

        globalParameters.put(param.getId(), param);

        Map<String, List<Pair<Long, Long>>> existingParams = new HashMap<>();
        existingParams.put("xsl1", ImmutableList.of(new ImmutablePair<>(100500L, 2L)));
        doReturn(existingParams)
            .when(parameterDAO)
            .getExistingParams(anyList(), anyList());

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setParameters(Collections.emptyList());

        doReturn(categoryEntities)
            .when(parameterLoader)
            .loadCategoryEntitiesByHid(2);

        doReturn(Collections.singletonList(param))
            .when(parameterLoader)
            .loadGlobalParameters(Stream.of(param.getId()).collect(Collectors.toList()));

        doReturn(Collections.emptySet())
            .when(parameterLinkService)
            .getMutualLinkedParameterIds(
                eq(globalParameters.keySet()),
                eq(KnownIds.GLOBAL_CATEGORY_ID));

        doReturn(Collections.emptySet())
            .when(parameterLoader)
            .loadBreakInheritanceParameters(2);

        TovarCategory category = new TovarCategory();
        category.setHid(2);

        TovarTree tree = new TovarTree();
        tree.getRoot().addChild(new TovarCategoryNode(category));
        doReturn(tree)
            .when(tovarTreeDao)
            .loadTreeScheme();

        parameterService.addGlobalParameters(saveContext, 2, globalParameters.keySet());

        // saveParameter not called
        verify(parameterDAO, Mockito.times(0))
            .runAfterTransaction(Mockito.any(Runnable.class));
    }


    @Test
    public void testCachesTouchedWhenAddOptionToGlobalParameter() {
        long parameterId = 10L;
        CategoryParam paramOriginal = new Parameter();
        paramOriginal.setId(parameterId);
        paramOriginal.setType(Param.Type.ENUM);
        paramOriginal.setXslName("global_param_xsl");
        paramOriginal.setAccess(ParamOptionsAccessType.SIMPLE);
        paramOriginal.setNames(Collections.singletonList(new Word(255, "Global Param")));

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.addParameter(paramOriginal);

        ParameterValuesChanges valuesChanges = createValuesChanges(Collections.singletonList(NEW_OPTION),
            Collections.emptyList());

        when(parameterLoader.loadCategoryEntitiesByHid(eq(IParameterLoaderService.GLOBAL_ENTITIES_HID)))
            .thenReturn(categoryEntities);
        when(parameterLoader.loadParameter(eq(parameterId)))
            .thenReturn(paramOriginal);
        //  mocking call in updateLinkedParams
        when(parameterLinkService
            .getLinkedParameters(eq(paramOriginal), eq(LinkType.RANGE_MIN), eq(LinkType.RANGE_MAX)))
            .thenReturn(new MultiMap<>());
        //  mocking call in getFreeMboDumpIdForOption
        when(contentJdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
            .thenReturn(1L);

        ArgumentCaptor<Runnable> updateParameterCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(parameterDAO).doWithLock(anyCollection(), updateParameterCaptor.capture());

        saveContext.setForceSave(true);
        parameterService.saveParameter(saveContext, IParameterLoaderService.GLOBAL_ENTITIES_HID,
            paramOriginal, valuesChanges);
        updateParameterCaptor.getValue().run();

        verify(parameterDAO, Mockito.times(1))
            .touchParams(eq(Collections.singleton(parameterId)));
        verify(parameterDAO, Mockito.times(1))
            .touch(eq(Collections.singleton(IParameterLoaderService.GLOBAL_ENTITIES_HID)));
        verify(parameterDAO, Mockito.times(1))
            .touchByGlobalParamIds(eq(Collections.singleton(parameterId)));
    }

    /**
     * Fix it in MBO-23973.
     */
    @Test
    @Ignore
    @SuppressWarnings("unchecked")
    public void testSaveLineChangesWithDuplicates() {
        // Creating value with 'new value' to verify that
        // we can add a new option with the same name.
        CategoryParam param = new Parameter();
        param.setId(1);
        param.setXslName(XslNames.VENDOR_LINE);
        param.addOption(OptionBuilder.newBuilder().setId(2).setFilterValue(false)
            .addName("new value").setPublished(true).build());

        OptionNameDuplicationValidator validator = new OptionNameDuplicationValidator();
        validator.setValueLinkService(valueLinkService);
        validator.setParameterLoaderService(parameterLoader);
        parameterValidationService = new ParameterValidationService(
            null, null, null, null, validator,
            parameterLinkService, indexedModelQueryService,
            null, null, null, null, null,
            null, null, parameterDAO, patternService, parameterLoader);
        parameterService.setParameterValidationService(parameterValidationService);

        when(parameterDAO.doWithLock(anyCollection(), any(Supplier.class))).then(invocation -> {
            Supplier<Object> action = invocation.getArgument(1);
            return action.get();
        });

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setParameters(Collections.singletonList(param));

        Mockito.doReturn(categoryEntities)
            .when(parameterLoader).loadCategoryEntitiesByHid(Mockito.eq(IParameterLoaderService.GLOBAL_ENTITIES_HID));
        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.valueUpdated(OptionBuilder.newBuilder()
            .setId(1)
            .setPublished(true)
            .build()
        );

        // Should be able to save param with duplicate value name.
        parameterService.saveLinesChanges(saveContext, 1, parameterValuesChanges);

        // saveParameter not called
        verify(parameterDAO, Mockito.times(0))
            .runAfterTransaction(Mockito.any(Runnable.class));
    }
}
