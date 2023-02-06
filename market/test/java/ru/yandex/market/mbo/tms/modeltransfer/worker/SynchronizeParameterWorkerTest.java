package ru.yandex.market.mbo.tms.modeltransfer.worker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.params.ParameterLinkService;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Link;
import ru.yandex.market.mbo.gwt.models.params.LinkType;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.tms.modeltransfer.CopyLandingConfigBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ListOfModelsConfigBuilder;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.CATEGORY_ID1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.CATEGORY_ID2;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.CATEGORY_ID3;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.ENUM1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.ID_GENERATOR;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.copyParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getEnumParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getNameParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getOption;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getVendorParam;

/**
 * @author danfertev
 * @since 05.10.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SynchronizeParameterWorkerTest {
    private SynchronizeParameterWorker worker;
    private ParameterLoaderServiceStub parameterLoaderService;
    private ParameterService parameterService;
    private ModelStorageServiceStub modelStorageService;
    private ValueLinkServiceInterface valueLinkService;
    private ParameterLinkService parameterLinkService;
    private RecipeService recipeService;
    private AutoUser autoUser = new AutoUser(666L);

    private Map<Long, CommonModel> modelsMap = new HashMap<>();

    @Before
    public void setUp() {
        parameterLoaderService = new ParameterLoaderServiceStub();
        parameterService = mock(ParameterService.class);
        modelStorageService = mock(ModelStorageServiceStub.class, Mockito.CALLS_REAL_METHODS);
        modelStorageService.setModelsMap(modelsMap);
        valueLinkService = mock(ValueLinkServiceInterface.class);
        parameterLinkService = mock(ParameterLinkService.class);
        recipeService = mock(RecipeService.class);

        worker = new SynchronizeParameterWorker(parameterLoaderService, parameterService,
            modelStorageService, valueLinkService, recipeService, autoUser, parameterLinkService);
        doAnswer(args -> {
            long targetCategoryId = args.getArgument(1);
            Set<Long> ids = args.getArgument(2);
            Map<Long, CategoryParam> paramMap = parameterLoaderService.getCategoryEntitiesMap().values().stream()
                .flatMap(e -> e.getParameters().stream())
                .collect(Collectors.toMap(CategoryParam::getRealParamId, Function.identity(), (o1, o2) -> o1));

            ids.forEach(id -> {
                CategoryParam param = paramMap.get(id);
                parameterLoaderService.addCategoryParam(copyParam(targetCategoryId, param));
            });

            return null;
        }).when(parameterService).copyTo(any(), anyLong(), anySet());

        parameterLoaderService.addCategoryEntities(new CategoryEntities(CATEGORY_ID1, Collections.emptyList()));
        parameterLoaderService.addCategoryEntities(new CategoryEntities(CATEGORY_ID2, Collections.emptyList()));
        parameterLoaderService.addCategoryEntities(new CategoryEntities(CATEGORY_ID3, Collections.emptyList()));
    }

    @Test
    public void testAlreadySynced() {
        CategoryParam sourceNameParam = getNameParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceNameParam);
        parameterLoaderService.addCategoryParam(getNameParam(CATEGORY_ID2));

        CommonModel model = guru(CATEGORY_ID1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "В категории назначения уже существует соответствующий параметр", sourceNameParam, CATEGORY_ID2);
    }

    @Test
    public void testFilterParamByNoParamValue() {
        parameterLoaderService.addCategoryParam(getEnumParam(CATEGORY_ID1));

        CommonModel model = guru(CATEGORY_ID1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).isEmpty();
    }

    @Test
    public void testParametersCopied() {
        CategoryParam sourceNameParam = getNameParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceNameParam);
        CategoryParam sourceEnumParam = getEnumParam(CATEGORY_ID1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam.addOption(enumOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam, enumOption1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceNameParam, CATEGORY_ID2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam, CATEGORY_ID2);
    }

    @Test
    public void testCopyFromMultipleCategories() {
        CategoryParam sourceNameParam = getNameParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceNameParam);
        CategoryParam sourceEnumParam = getEnumParam(CATEGORY_ID2);
        Option enumOption1 = getOption("option1");
        sourceEnumParam.addOption(enumOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam);

        CommonModel model1 = guru(CATEGORY_ID1);
        CommonModel model2 = guru(CATEGORY_ID2, sourceEnumParam, enumOption1);
        storeModel(model1, model2);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID3, model1.getId())
                .models(CATEGORY_ID2, CATEGORY_ID3, model2.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceNameParam, CATEGORY_ID3);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam, CATEGORY_ID3);
    }

    @Test
    public void testCopyFailed() {
        CategoryParam sourceNameParam = getNameParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceNameParam);

        CommonModel model = guru(CATEGORY_ID1);
        storeModel(model);

        doThrow(new OperationException("Ошибка.")).when(parameterService).copyTo(any(), anyLong(), anySet());

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.FAILURE,
            "Копирование параметра завершилось с ошибкой: Ошибка.", sourceNameParam, CATEGORY_ID2);
    }

    @Test
    public void testCopySettingsSuccess() {
        CategoryParam sourceEnumParam1 = getEnumParam(CATEGORY_ID3, ENUM1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam1.addOption(enumOption1);

        InheritedParameter sourceEnumParam1Inh = new InheritedParameter(sourceEnumParam1);
        sourceEnumParam1Inh.setCategoryHid(CATEGORY_ID1);
        sourceEnumParam1Inh.setUseInSku(true);
        sourceEnumParam1Inh.getOverride().setId(ID_GENERATOR.incrementAndGet());
        parameterLoaderService.addCategoryParam(sourceEnumParam1);
        parameterLoaderService.loadCategoryEntitiesByHid(CATEGORY_ID1).inheritParameter(sourceEnumParam1Inh);

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam1, enumOption1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam1Inh, CATEGORY_ID2);

        CategoryParam copiedParam = parameterLoaderService.loadCategoryEntitiesByHid(CATEGORY_ID2)
            .getParameterByName(sourceEnumParam1.getXslName());
        assertThat(copiedParam.isUseInSku()).isTrue();
    }

    @Test
    public void testCopySettingsFailed() {
        CategoryParam sourceEnumParam1 = getEnumParam(CATEGORY_ID3, ENUM1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam1.addOption(enumOption1);

        InheritedParameter sourceEnumParam1Inh = new InheritedParameter(sourceEnumParam1);
        sourceEnumParam1Inh.setCategoryHid(CATEGORY_ID1);
        sourceEnumParam1Inh.setUseInSku(true);
        sourceEnumParam1Inh.getOverride().setId(ID_GENERATOR.incrementAndGet());
        parameterLoaderService.addCategoryParam(sourceEnumParam1);
        parameterLoaderService.loadCategoryEntitiesByHid(CATEGORY_ID1).inheritParameter(sourceEnumParam1Inh);

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam1, enumOption1);
        storeModel(model);

        doThrow(new OperationException("Ошибка."))
            .when(parameterService)
            .saveParameter(any(), anyLong(), any(), any());

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertParamResultEntry(results, ResultEntry.Status.FAILURE,
            "Невозможно скопировать настройки параметра: Ошибка.", sourceEnumParam1Inh, CATEGORY_ID2);
    }

    @Test
    public void testParametersCopyFromValueLink() {
        CategoryParam sourceEnumParam1 = getEnumParam(CATEGORY_ID1, ENUM1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam1.addOption(enumOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam1);
        CategoryParam sourceEnumParam2 = getEnumParam(CATEGORY_ID1);
        Option enumOption2 = getOption("option2");
        sourceEnumParam2.addOption(enumOption2);
        parameterLoaderService.addCategoryParam(sourceEnumParam2);

        parameterLoaderService.addCategoryParam(getEnumParam(CATEGORY_ID2, ENUM1));

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam1, enumOption1);
        storeModel(model);
        when(valueLinkService.findConstraintLinksForOptionIds(any())).thenReturn(Collections.singletonList(
            new ValueLink(sourceEnumParam1.getId(), enumOption1.getId(),
                sourceEnumParam2.getId(), enumOption2.getId(), LinkDirection.BIDIRECTIONAL)
        ));

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "В категории назначения уже существует соответствующий параметр", sourceEnumParam1, CATEGORY_ID2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam2, CATEGORY_ID2);
    }

    @Test
    public void testParametersCopyFromParamLink() {
        CategoryParam sourceEnumParam1 = getEnumParam(CATEGORY_ID1, ENUM1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam1.addOption(enumOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam1);
        CategoryParam sourceEnumParam2 = getEnumParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceEnumParam2);

        parameterLoaderService.addCategoryParam(getEnumParam(CATEGORY_ID2, ENUM1));

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam1, enumOption1);
        storeModel(model);
        when(parameterLinkService.getLinkedWith(anyLong(), anyCollection())).thenReturn(Collections.singletonList(
            new Link(CATEGORY_ID1, sourceEnumParam1.getId(), LinkType.DEFINITION, sourceEnumParam2.getId())
        ));

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "В категории назначения уже существует соответствующий параметр", sourceEnumParam1, CATEGORY_ID2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam2, CATEGORY_ID2);
    }

    @Test
    public void testParametersCopyFromParamLinkChain() {
        CategoryParam sourceEnumParam1 = getEnumParam(CATEGORY_ID1, ENUM1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam1.addOption(enumOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam1);
        CategoryParam sourceEnumParam2 = getEnumParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceEnumParam2);
        CategoryParam sourceEnumParam3 = getVendorParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceEnumParam3);

        parameterLoaderService.addCategoryParam(getEnumParam(CATEGORY_ID2, ENUM1));

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam1, enumOption1);
        storeModel(model);
        when(parameterLinkService.getLinkedWith(anyLong(), anyCollection())).then(args -> {
                Collection<Long> paramIds = args.getArgument(1);
                long sourceParam1Id = sourceEnumParam1.getId();
                long sourceParam2Id = sourceEnumParam2.getId();
                if (paramIds.contains(sourceParam1Id)) {
                    return Collections.singletonList(
                        new Link(CATEGORY_ID1, sourceParam1Id, LinkType.DEFINITION, sourceParam2Id));
                }
                if (paramIds.contains(sourceParam2Id)) {
                    return Collections.singletonList(
                        new Link(CATEGORY_ID1, sourceParam2Id, LinkType.DEFINITION, sourceEnumParam3.getId()));
                }
                return Collections.emptyList();
            }
        );

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(3);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "В категории назначения уже существует соответствующий параметр", sourceEnumParam1, CATEGORY_ID2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam2, CATEGORY_ID2);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam3, CATEGORY_ID2);
    }

    @Test
    public void testWrongType() {
        CategoryParam sourceNameParam = getNameParam(CATEGORY_ID1);
        parameterLoaderService.addCategoryParam(sourceNameParam);
        CategoryParam targetNameParam = getNameParam(CATEGORY_ID2);
        targetNameParam.setType(Param.Type.ENUM);
        parameterLoaderService.addCategoryParam(targetNameParam);

        CommonModel model = guru(CATEGORY_ID1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.FAILURE,
            "В категории назначения уже существует параметр с таким же xslName, но с другим типом",
            sourceNameParam, CATEGORY_ID2);
    }

    @Test
    public void testAddGlobalParameterFailed() {
        CategoryParam sourceEnumParam1 = getEnumParam(CATEGORY_ID1, ENUM1);
        sourceEnumParam1.setCategoryHid(KnownIds.GLOBAL_CATEGORY_ID);
        InheritedParameter sourceEnumParam1Inh = new InheritedParameter(sourceEnumParam1);
        sourceEnumParam1Inh.setCategoryHid(CATEGORY_ID1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam1Inh.addOption(enumOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam1);
        parameterLoaderService.loadCategoryEntitiesByHid(CATEGORY_ID1).inheritParameter(sourceEnumParam1Inh);

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam1Inh, enumOption1);
        storeModel(model);

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.FAILURE,
            "Добавление глобального параметра в категорию назначения неуспешно",
            sourceEnumParam1Inh, CATEGORY_ID2);
    }

    @Test
    public void testAddGlobalParameter() {
        CategoryParam sourceEnumParam1 = getEnumParam(CATEGORY_ID1, ENUM1);
        sourceEnumParam1.setCategoryHid(KnownIds.GLOBAL_CATEGORY_ID);
        InheritedParameter sourceEnumParam1Inh = new InheritedParameter(sourceEnumParam1);
        sourceEnumParam1Inh.setCategoryHid(CATEGORY_ID1);
        Option enumOption1 = getOption("option1");
        sourceEnumParam1Inh.addOption(enumOption1);
        parameterLoaderService.addCategoryParam(sourceEnumParam1);
        parameterLoaderService.loadCategoryEntitiesByHid(CATEGORY_ID1).inheritParameter(sourceEnumParam1Inh);

        CommonModel model = guru(CATEGORY_ID1, sourceEnumParam1Inh, enumOption1);
        storeModel(model);

        doAnswer(args -> {
            long categoryId = args.getArgument(1);
            Collection<Long> globalIds = args.getArgument(2);
            globalIds.forEach(id -> {
                CategoryEntities globalCategoryEntities = parameterLoaderService.loadCategoryEntitiesByHid(
                    KnownIds.GLOBAL_CATEGORY_ID);
                CategoryParam globalParam = globalCategoryEntities.getParameterById(id);
                InheritedParameter globalParamInh = new InheritedParameter(globalParam);
                globalParamInh.setCategoryHid(categoryId);
                parameterLoaderService.loadCategoryEntitiesByHid(categoryId).inheritParameter(globalParamInh);
            });
            return null;
        }).when(parameterService).addGlobalParameters(any(), anyLong(), anyCollection());

        List<ParameterResultEntry> results = worker.doWork(
            new ListOfModelParameterLandingConfig(ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Глобальный параметр добавлен в категорию назначения", sourceEnumParam1Inh, CATEGORY_ID2);
    }

    @Test
    public void testParametersCopyFromLanding() {
        CategoryParam sourceEnumParam = getEnumParam(CATEGORY_ID1, ENUM1);
        Option sourceOption = getOption("option1");
        sourceEnumParam.addOption(sourceOption);
        parameterLoaderService.addCategoryParam(sourceEnumParam);

        CommonModel model = guru(CATEGORY_ID1);
        storeModel(model);

        Recipe landing = new Recipe();
        landing.setId(ID_GENERATOR.incrementAndGet());
        landing.setHid(CATEGORY_ID1);
        landing.setNavigation(false);
        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(sourceEnumParam.getId());
        filter.setParamType(Param.Type.ENUM);
        filter.setValueIds(Collections.singletonList(sourceOption.getId()));
        landing.addFilter(filter);

        when(recipeService.getSearchRecipes(any(), any(), anyBoolean()))
            .thenReturn(Collections.singletonList(landing));

        List<ParameterResultEntry> results = worker.doWork(new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(CATEGORY_ID1, CATEGORY_ID2, model.getId())
                .build(),
            CopyLandingConfigBuilder.newBuilder()
                .landings(CATEGORY_ID1, CATEGORY_ID2, landing.getId())
                .build()));

        assertThat(results).hasSize(1);
        assertParamResultEntry(results, ResultEntry.Status.SUCCESS,
            "Параметр успешно скопирован в категорию назначения", sourceEnumParam, CATEGORY_ID2);
    }

    private CommonModel guru(long categoryId) {
        return CommonModelBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), categoryId, ID_GENERATOR.incrementAndGet())
            .title("Guru model")
            .currentType(CommonModel.Source.GURU)
            .getModel();
    }

    private void storeModel(CommonModel... models) {
        for (CommonModel model : models) {
            modelsMap.put(model.getId(), model);
        }
    }

    private CommonModel guru(long categoryId, CategoryParam param, Option option) {
        return CommonModelBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), categoryId, ID_GENERATOR.incrementAndGet())
            .title("Guru model")
            .currentType(CommonModel.Source.GURU)
            .param(param).setOption(option.getId())
            .getModel();
    }

    private void assertParamResultEntry(List<ParameterResultEntry> entries,
                                        ResultEntry.Status status, String statusMessage,
                                        CategoryParam sourceParam, long targetCategoryId) {
        ParameterResultEntry entry = entries.stream()
            .filter(e -> e.getSourceParameter().getParamId() == sourceParam.getId())
            .findFirst()
            .orElse(null);
        assertThat(entry).isNotNull();
        assertThat(entry.getStatus()).isEqualTo(status);
        assertThat(entry.getSourceParameter()).isEqualTo(ParameterInfo.from(sourceParam));
        if (status != ResultEntry.Status.FAILURE) {
            assertThat(entry.getDestinationParameter())
                .isEqualTo(ParameterInfo.from(parameterLoaderService.loadCategoryEntitiesByHid(targetCategoryId)
                    .getParameterByName(sourceParam.getXslName())));
        }
        assertThat(entry.getStatusMessage()).isEqualTo(statusMessage);
    }
}
