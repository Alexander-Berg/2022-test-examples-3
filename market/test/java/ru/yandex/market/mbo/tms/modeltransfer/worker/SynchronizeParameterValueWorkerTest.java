package ru.yandex.market.mbo.tms.modeltransfer.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.db.transfer.ModelTransferWorkerHelper;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.gwt.exceptions.OptionPropertyDuplicationException;
import ru.yandex.market.mbo.gwt.exceptions.dto.OptionParametersDuplicationDto;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfParametersConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.tms.modeltransfer.CopyLandingConfigBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ListOfModelsConfigBuilder;
import ru.yandex.market.mbo.user.AutoUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.ENUM1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.ID_GENERATOR;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getEnumParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getGlobalVendor;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getInheritedVendorParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getLocalVendor;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getNameParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getOption;

/**
 * @author danfertev
 * @since 05.10.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SynchronizeParameterValueWorkerTest {
    private static final long SOURCE_CATEGORY_ID = 100L;
    private static final long TARGET_CATEGORY_ID = 200L;
    private static final long GLOBAL_VENDOR_ID1 = 1L;
    private static final long GLOBAL_VENDOR_ID2 = 2L;
    private static final long ALIAS_ID = 777L;

    private SynchronizeParameterValueWorker worker;
    private ParameterLoaderServiceStub parameterLoaderService;
    private ParameterService parameterService;
    private AutoUser autoUser = new AutoUser(666L);
    private ModelStorageServiceStub modelStorageService;
    private GuruVendorsReader guruVendorsReader;
    private GlobalVendorService globalVendorService;
    private ValueLinkServiceInterface valueLinkService;
    private RecipeService recipeService;

    private Map<Long, CommonModel> modelsMap = new HashMap<>();
    private Map<Pair<Long, Long>, Long> globalToLocalVendorMap = new HashMap<>();
    private Map<Long, GlobalVendor> globalVendorMap = new HashMap<>();
    private Map<Long, OptionImpl> localVendorMap = new HashMap<>();

    private InheritedParameter sourceVendorParam = getInheritedVendorParam(SOURCE_CATEGORY_ID);
    private InheritedParameter targetVendorParam = getInheritedVendorParam(TARGET_CATEGORY_ID);

    @Before
    public void setUp() {
        modelStorageService = mock(ModelStorageServiceStub.class, Mockito.CALLS_REAL_METHODS);
        guruVendorsReader = mock(GuruVendorsReader.class);
        parameterLoaderService = new ParameterLoaderServiceStub();
        parameterService = mock(ParameterService.class);
        globalVendorService = mock(GlobalVendorService.class);
        valueLinkService = mock(ValueLinkServiceInterface.class);
        recipeService = mock(RecipeService.class);
        worker = new SynchronizeParameterValueWorker(parameterLoaderService, parameterService,
            modelStorageService, valueLinkService, recipeService, autoUser, guruVendorsReader, globalVendorService);

        modelStorageService.setModelsMap(modelsMap);

        when(guruVendorsReader.getLocalVendorIdFromGlobal(anyLong(), anyLong())).then(args -> {
            long categoryId = args.getArgument(0);
            long globalVendorId = args.getArgument(1);
            return globalToLocalVendorMap.get(Pair.of(globalVendorId, categoryId));
        });

        when(guruVendorsReader.getLocalVendor(anyLong(), anyLong())).then(args -> {
            long categoryId = args.getArgument(0);
            long globalVendorId = args.getArgument(1);
            Long localVendorId = globalToLocalVendorMap.get(Pair.of(globalVendorId, categoryId));
            return localVendorMap.get(localVendorId);
        });

        when(globalVendorService.loadVendor(anyLong())).then(args -> {
            long vendorId = args.getArgument(0);
            return globalVendorMap.get(vendorId);
        });

        doAnswer(args -> {
            long categoryId = args.getArgument(1);
            OptionImpl localVendor = args.getArgument(3);
            localVendor.setId(ID_GENERATOR.incrementAndGet());
            long localVendorId = localVendor.getId();
            long globalVendorId = localVendor.getParent().getId();
            globalToLocalVendorMap.put(Pair.of(globalVendorId, categoryId), localVendorId);
            localVendorMap.put(localVendorId, localVendor);
            return null;
        }).when(parameterService).addLocalVendor(any(), anyLong(), any(), any());

        doAnswer(args -> {
            CategoryParam param = args.getArgument(2);
            ParameterValuesChanges changes = args.getArgument(3);
            changes.getAdded().forEach(param::addOption);
            return param;
        }).when(parameterService).saveParameter(any(), anyLong(), any(), any());

        globalVendor(GLOBAL_VENDOR_ID1);
        globalVendor(GLOBAL_VENDOR_ID2);

        parameterLoaderService.addCategoryEntities(new CategoryEntities(SOURCE_CATEGORY_ID, Collections.emptyList()));
        parameterLoaderService.addCategoryEntities(new CategoryEntities(TARGET_CATEGORY_ID, Collections.emptyList()));

        parameterLoaderService.addCategoryParam(sourceVendorParam);
        parameterLoaderService.addCategoryParam(targetVendorParam);
    }

    @Test(expected = OperationException.class)
    public void noModelsFound() {
        parameterLoaderService.addCategoryParam(getNameParam(SOURCE_CATEGORY_ID));
        parameterLoaderService.addCategoryParam(getNameParam(TARGET_CATEGORY_ID));

        worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, 10L)
                .build()));
    }

    @Test
    public void noSourceLocalVendor() {
        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        when(guruVendorsReader.getLocalVendor(eq(SOURCE_CATEGORY_ID), eq(GLOBAL_VENDOR_ID1))).thenReturn(null);
        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            String.format("В исходной категории %d не найден локальный вендор для глобального %d",
                SOURCE_CATEGORY_ID, GLOBAL_VENDOR_ID1));
    }

    @Test
    public void newLocalVendor() {
        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
    }

    @Test
    public void localVendorExists() {
        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        OptionImpl newVendor = localVendor(GLOBAL_VENDOR_ID1, TARGET_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            String.format("В категории назначения уже определен %s",
                ModelTransferWorkerHelper.getOptionText(newVendor)));
    }

    @Test
    public void localVendorUpdated() {
        OptionImpl localVendor1 = localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        localVendor1.addAlias(new EnumAlias(ALIAS_ID, Word.DEFAULT_LANG_ID, "alias",
            EnumAlias.ExtractionType.FOR_PARAMS, EnumAlias.Type.GENERAL));
        localVendor1.setCutOffWords(WordUtil.defaultWords("cut1, cut2"));
        OptionImpl localVendor2 = localVendor(GLOBAL_VENDOR_ID1, TARGET_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertThat(localVendor2.getAliases(EnumAlias.Type.GENERAL))
            .containsExactlyElementsOf(localVendor1.getAliases(EnumAlias.Type.GENERAL));
        assertThat(localVendor2.getCutOffWords())
            .containsExactlyElementsOf(localVendor1.getCutOffWords());
    }

    @Test
    public void localVendorAddFailed() {
        OptionImpl sourceLocalVendor = localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        doThrow(new OperationException("BOOM"))
            .when(parameterService).addLocalVendor(any(), anyLong(), any(), any());

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.FAILURE, sourceVendorParam,
            String.format("Невозможно добавить локальный вендор: %d %s - BOOM", 0L, sourceLocalVendor.getName()));
    }

    @Test
    public void localVendorUpdateFailed() {
        OptionImpl localVendor1 = localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        localVendor1.addAlias(new EnumAlias(ALIAS_ID, Word.DEFAULT_LANG_ID, "alias",
            EnumAlias.ExtractionType.FOR_PARAMS, EnumAlias.Type.GENERAL));
        localVendor1.setCutOffWords(WordUtil.defaultWords("cut1, cut2"));
        localVendor(GLOBAL_VENDOR_ID1, TARGET_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        doThrow(new OperationException("BOOM"))
            .when(parameterService).saveParameter(any(), anyLong(), any(), any());

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.WARNING, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.WARNING, GLOBAL_VENDOR_ID1, "BOOM"));
    }

    @Test
    public void localVendorWithNonGroupedDuplicateAliases() {
        OptionImpl localVendor1 = localVendor(GLOBAL_VENDOR_ID1, TARGET_CATEGORY_ID);
        localVendor1.addAlias(new EnumAlias(ALIAS_ID, Word.DEFAULT_LANG_ID, "alias",
            EnumAlias.ExtractionType.FOR_PARAMS, EnumAlias.Type.GENERAL));
        localVendor1.setCutOffWords(WordUtil.defaultWords("cut1, cut2"));
        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        OptionImpl localVendor2 = localVendor(GLOBAL_VENDOR_ID2, TARGET_CATEGORY_ID);
        OptionParametersDuplicationDto duplicationDto = new OptionParametersDuplicationDto();
        duplicationDto.getNonGroupedDuplications().put("alias", Cf.set(localVendor1, localVendor2));

        doThrow(new OptionPropertyDuplicationException("BOOM",
            OptionPropertyDuplicationException.ExceptionType.ALIAS_DUPLICATION,
            duplicationDto))
            .when(parameterService).saveParameter(any(), anyLong(), any(), any());

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.WARNING, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.WARNING, GLOBAL_VENDOR_ID1,
                String.format("Найдены дублирующиеся %s: [%s] в [%s]",
                    "алиасы", "alias", ModelTransferWorkerHelper.getOptionText(localVendor2))));
    }

    @Test
    public void localVendorWithGroupedDuplicateAliases() {
        OptionImpl localVendor1 = localVendor(GLOBAL_VENDOR_ID1, TARGET_CATEGORY_ID);
        localVendor1.addAlias(new EnumAlias(ALIAS_ID, Word.DEFAULT_LANG_ID, "alias",
            EnumAlias.ExtractionType.FOR_PARAMS, EnumAlias.Type.GENERAL));
        localVendor1.setCutOffWords(WordUtil.defaultWords("cut1, cut2"));
        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        OptionImpl localVendor2 = localVendor(GLOBAL_VENDOR_ID2, TARGET_CATEGORY_ID);
        OptionParametersDuplicationDto duplicationDto = new OptionParametersDuplicationDto();
        duplicationDto.getGroupedDuplications().computeIfAbsent(localVendor1, o -> new HashMap<>())
            .put("alias", Cf.set(localVendor1, localVendor2));

        doThrow(new OptionPropertyDuplicationException("BOOM",
            OptionPropertyDuplicationException.ExceptionType.ALIAS_DUPLICATION,
            duplicationDto))
            .when(parameterService).saveParameter(any(), anyLong(), any(), any());

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.WARNING, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.WARNING, GLOBAL_VENDOR_ID1,
                String.format("Найдены дублирующиеся %s: [%s] в [%s]",
                    "алиасы", "alias", ModelTransferWorkerHelper.getOptionText(localVendor2))));
    }

    @Test
    public void multipleNewLocalVendors() {
        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        localVendor(GLOBAL_VENDOR_ID2, SOURCE_CATEGORY_ID);
        CommonModel model1 = guru(GLOBAL_VENDOR_ID1);
        CommonModel model2 = guru(GLOBAL_VENDOR_ID2);
        storeModel(model1, model2);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model1.getId(), model2.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1),
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID2));
    }

    @Test
    public void inheritedSourceLocalVendorExist() {
        OptionImpl sourceVendor = localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        localVendor(GLOBAL_VENDOR_ID2, SOURCE_CATEGORY_ID);
        sourceVendorParam.addOriginalValuesWithoutAnyChecks(Collections.singletonList(sourceVendor));
        OptionImpl targetVendor = localVendor(GLOBAL_VENDOR_ID1, TARGET_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID2);
        storeModel(model);

        Recipe landing = new Recipe();
        long sourceLandingId = ID_GENERATOR.incrementAndGet();
        landing.setId(sourceLandingId);
        landing.setHid(SOURCE_CATEGORY_ID);
        landing.setNavigation(false);
        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(sourceVendorParam.getId());
        filter.setParamType(Param.Type.ENUM);
        filter.setValueIds(Collections.singletonList(sourceVendor.getValueId()));
        landing.addFilter(filter);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean()))
            .thenReturn(Collections.singletonList(landing));

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build(),
            CopyLandingConfigBuilder.newBuilder()
                .landings(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, sourceLandingId)
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID2));
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            String.format("В категории назначения уже определен %s",
                ModelTransferWorkerHelper.getOptionText(targetVendor)));
    }

    @Test
    public void inheritedSourceLocalVendorAdded() {
        OptionImpl sourceVendor = localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        localVendor(GLOBAL_VENDOR_ID2, SOURCE_CATEGORY_ID);
        sourceVendorParam.addOriginalValuesWithoutAnyChecks(Collections.singletonList(sourceVendor));
        CommonModel model = guru(GLOBAL_VENDOR_ID2);
        storeModel(model);

        Recipe landing = new Recipe();
        long sourceLandingId = ID_GENERATOR.incrementAndGet();
        landing.setId(sourceLandingId);
        landing.setHid(SOURCE_CATEGORY_ID);
        landing.setNavigation(false);
        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(sourceVendorParam.getId());
        filter.setParamType(Param.Type.ENUM);
        filter.setValueIds(Collections.singletonList(sourceVendor.getValueId()));
        landing.addFilter(filter);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean()))
            .thenReturn(Collections.singletonList(landing));

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build(),
            CopyLandingConfigBuilder.newBuilder()
                .landings(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, sourceLandingId)
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID2));
    }

    @Test
    public void noOptionToSync() {
        parameterLoaderService.addCategoryParam(getEnumParam(SOURCE_CATEGORY_ID));
        parameterLoaderService.addCategoryParam(getEnumParam(TARGET_CATEGORY_ID));

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
    }

    @Test
    public void newOption() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceEnumParam,
            optionStatusMessage(ResultEntry.Status.SUCCESS, sourceOption));
    }

    @Test
    public void newOptionAddFailed() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        doThrow(new OperationException("BOOM"))
            .when(parameterService).saveParameter(any(), anyLong(), any(), any());

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.WARNING, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.WARNING, GLOBAL_VENDOR_ID1, "BOOM"));
        assertParamResultEntry(results, ResultEntry.Status.FAILURE, sourceEnumParam,
            String.format("Невозможно синхронизировать опция: %d %s - %s",
                0L, sourceOption.getName(), "BOOM"));
    }

    @Test
    public void optionExists() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        Option targetOption = getOption("option1", targetEnumParam.getId());
        targetEnumParam.addOption(targetOption);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceEnumParam,
            String.format(
                "Опция уже определена в категории назначения. В исходной категории %s. В категории назначения %s.",
                ModelTransferWorkerHelper.getOptionText(sourceOption),
                ModelTransferWorkerHelper.getOptionText(targetOption)));
    }

    @Test
    public void noSourceParamForOption() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        parameterLoaderService.loadCategoryEntitiesByHid(SOURCE_CATEGORY_ID).removeParameter(sourceEnumParam);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, null,
            String.format("В исходной категории не существует параметра с xslName %s", ENUM1));
    }

    @Test
    public void noTargetParamForOption() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        parameterLoaderService.addCategoryParam(sourceEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.FAILURE, sourceEnumParam,
            String.format("В категории назначения не найден параметр с xslName %s", ENUM1));
    }

    @Test
    public void noTargetParamForOptionButParamIsIgnored() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        parameterLoaderService.addCategoryParam(sourceEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build(),
            new ListOfParametersConfig(Collections.singletonList(sourceEnumParam.getId()))));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceEnumParam,
            String.format("Опция %d параметра %d [%s] проигнорирована, так как параметр включен в список исключений",
                sourceOption.getId(), sourceEnumParam.getId(), ENUM1));
    }

    @Test
    public void newOptionWithValueLink() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        long sourceParamId = sourceEnumParam.getId();
        long sourceOptionId = sourceOption.getId();
        when(valueLinkService.findConstraintLinksForOptionIds(anyList())).thenReturn(Collections.singletonList(
            new ValueLink(sourceParamId, sourceOptionId, sourceParamId, sourceOptionId,
                LinkDirection.REVERSE, ValueLinkType.GENERAL, SOURCE_CATEGORY_ID)
        ));

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.FAILURE, sourceEnumParam,
            String.format("Опция в исходной категории %s имеет связи, но связанная опция не найдена.",
                ModelTransferWorkerHelper.getOptionText(sourceOption)));
    }

    @Test
    public void directUniqueInheritanceOption() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceOption.setInheritanceStrategy(Option.InheritanceStrategy.DIRECT_UNIQUE);
        sourceEnumParam.addOption(sourceOption);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1, sourceEnumParam, sourceOption);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.FAILURE, sourceEnumParam,
            String.format(
                "Невозможно скопировать опцию с наследованием DIRECT_UNIQUE. Необходимо ручное действие. Опция %s.",
                ModelTransferWorkerHelper.getOptionText(sourceOption)));
    }

    @Test
    public void optionFromLanding() {
        CategoryParam sourceEnumParam = getEnumParam(SOURCE_CATEGORY_ID, ENUM1);
        Option sourceOption = getOption("option1", sourceEnumParam.getId());
        sourceEnumParam.addOption(sourceOption);
        CategoryParam targetEnumParam = getEnumParam(TARGET_CATEGORY_ID, ENUM1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);
        parameterLoaderService.addCategoryParam(targetEnumParam);

        localVendor(GLOBAL_VENDOR_ID1, SOURCE_CATEGORY_ID);
        CommonModel model = guru(GLOBAL_VENDOR_ID1);
        storeModel(model);

        Recipe landing = new Recipe();
        landing.setId(ID_GENERATOR.incrementAndGet());
        landing.setHid(SOURCE_CATEGORY_ID);
        landing.setNavigation(false);
        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(sourceEnumParam.getId());
        filter.setParamType(Param.Type.ENUM);
        filter.setValueIds(Collections.singletonList(sourceOption.getValueId()));
        landing.addFilter(filter);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean()))
            .thenReturn(Collections.singletonList(landing));

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, model.getId())
                .build(),
            CopyLandingConfigBuilder.newBuilder()
                .landings(SOURCE_CATEGORY_ID, TARGET_CATEGORY_ID, landing.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceVendorParam,
            vendorStatusMessage(ResultEntry.Status.SUCCESS, GLOBAL_VENDOR_ID1));
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS, sourceEnumParam,
            optionStatusMessage(ResultEntry.Status.SUCCESS, sourceOption));
    }

    private CommonModel guru(long globalVendorId) {
        return CommonModelBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), SOURCE_CATEGORY_ID, globalVendorId)
            .title("Guru model")
            .currentType(CommonModel.Source.GURU)
            .getModel();
    }

    private CommonModel guru(long globalVendorId, CategoryParam param, Option option) {
        return CommonModelBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), SOURCE_CATEGORY_ID, globalVendorId)
            .title("Guru model")
            .currentType(CommonModel.Source.GURU)
            .param(param).setOption(option.getId())
            .getModel();
    }

    private void storeModel(CommonModel... models) {
        for (CommonModel model : models) {
            modelsMap.put(model.getId(), model);
        }
    }

    private void globalVendor(long globalVendorId) {
        globalVendorMap.put(globalVendorId, getGlobalVendor(globalVendorId));
    }

    private OptionImpl localVendor(long globalVendorId, long categoryId) {
        CategoryParam vendorParam = parameterLoaderService.getCategoryEntitiesMap().get(categoryId).getParameters()
            .stream()
            .filter(p -> p.getXslName().equals(XslNames.VENDOR))
            .findFirst()
            .get();
        GlobalVendor globalVendor = globalVendorMap.get(globalVendorId);
        OptionImpl localVendor = getLocalVendor(vendorParam, globalVendor);
        globalToLocalVendorMap.put(Pair.of(globalVendorId, categoryId), localVendor.getId());
        localVendorMap.put(localVendor.getId(), localVendor);
        return localVendor;
    }


    private void assertParamResultEntry(List<ParameterResultEntry> entries,
                                        ResultEntry.Status status, CategoryParam sourceParam,
                                        String... expectedStatusMessages) {
        List<ParameterResultEntry> results = entries.stream()
            .filter(e -> {
                ParameterInfo sourceParamInfo = e.getSourceParameter();
                if (sourceParamInfo == null) {
                    return sourceParam == null;
                } else {
                    return sourceParam != null && sourceParamInfo.getParamId() == sourceParam.getId();
                }
            })
            .collect(Collectors.toList());
        List<String> statusMessages = new ArrayList<>();
        results.forEach(entry -> {
            assertThat(entry).isNotNull();
            assertThat(entry.getStatus()).isEqualTo(status);
            if (sourceParam != null) {
                assertThat(entry.getSourceParameter()).isEqualTo(ParameterInfo.from(sourceParam));
                CategoryParam targetParam = parameterLoaderService.loadCategoryEntitiesByHid(TARGET_CATEGORY_ID)
                    .getParameterByName(sourceParam.getXslName());
                if (targetParam != null) {
                    assertThat(entry.getDestinationParameter()).isEqualTo(ParameterInfo.from(targetParam));
                }
            }
            statusMessages.add(entry.getStatusMessage());
        });
        assertThat(statusMessages).containsOnlyOnce(expectedStatusMessages);
    }

    private String successMessage(Option option) {
        return String.format("Синхронизировано %s", ModelTransferWorkerHelper.getOptionText(option));
    }

    private String failureMessage(Option option, String failureMessage) {
        return String.format("Невозможно синхронизировать %s - %s",
            ModelTransferWorkerHelper.getOptionText(option), failureMessage);
    }

    private String vendorStatusMessage(ResultEntry.Status status, long globalVendorId, String failureMessage) {
        switch (status) {
            case SUCCESS:
                OptionImpl successVendor = localVendorMap.get(
                    globalToLocalVendorMap.get(Pair.of(globalVendorId, TARGET_CATEGORY_ID)));
                return successMessage(successVendor);
            case WARNING:
                OptionImpl warningVendor = localVendorMap.get(
                    globalToLocalVendorMap.get(Pair.of(globalVendorId, TARGET_CATEGORY_ID)));
                return failureMessage(warningVendor, failureMessage);
            case FAILURE:
                OptionImpl failureVendor = localVendorMap.get(
                    globalToLocalVendorMap.get(Pair.of(globalVendorId, SOURCE_CATEGORY_ID)));
                return failureMessage(failureVendor, failureMessage);
            default:
                return "";
        }
    }

    private String vendorStatusMessage(ResultEntry.Status status, long globalVendorId) {
        return vendorStatusMessage(status, globalVendorId, "");
    }

    private String optionStatusMessage(
        ResultEntry.Status status, Option sourceOption, String failureMessage) {
        switch (status) {
            case SUCCESS:
                CategoryParam sourceParam = parameterLoaderService.loadCategoryEntitiesByHid(SOURCE_CATEGORY_ID)
                    .getParameterById(sourceOption.getParamId());
                Option targetOption = parameterLoaderService.loadCategoryEntitiesByHid(TARGET_CATEGORY_ID)
                    .getParameterByName(sourceParam.getXslName())
                    .getOptions()
                    .stream()
                    .filter(o -> o.getName().equals(sourceOption.getName()))
                    .findFirst()
                    .get();
                return successMessage(targetOption);
            case FAILURE:
                return failureMessage(sourceOption, failureMessage);
            default:
                return "";
        }
    }

    private String optionStatusMessage(ResultEntry.Status status, Option sourceOption) {
        return optionStatusMessage(status, sourceOption, "");
    }
}
