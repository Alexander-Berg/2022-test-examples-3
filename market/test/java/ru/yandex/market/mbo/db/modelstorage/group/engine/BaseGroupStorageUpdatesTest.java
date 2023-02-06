package ru.yandex.market.mbo.db.modelstorage.group.engine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import javolution.testing.AssertionException;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;

import ru.yandex.common.util.db.MultiIdGenerator;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.db.KeyValueMapService;
import ru.yandex.market.mbo.db.modelstorage.ModelRulesExecutorService;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.StatsIndexedModelQueryService;
import ru.yandex.market.mbo.db.modelstorage.YtSaasIndexesWrapper;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditContext;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditContextProvider;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.generalization.ModelGeneralizationService;
import ru.yandex.market.mbo.db.modelstorage.generalization.ModelGeneralizationServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.db.modelstorage.index.yt.CompositeIndexDecider;
import ru.yandex.market.mbo.db.modelstorage.index.yt.YtIndexReader;
import ru.yandex.market.mbo.db.modelstorage.partnergeneralization.PartnerGeneralizationServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.ModelSavePreprocessingServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.ModelSavePreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.CategoryIdChangePreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.CommonPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ConcurrentModificationPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.DeletedModelsPrerocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.FirstPublishedPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelDescriptionPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelGeneralizationPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelPickerPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelPicturePreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelRulesPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelSourcePreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModificationToModelPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.NamesToAliasesPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.NewModelPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.PModelGeneralizationPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.SignModificationsPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.SkuMovePreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.SkuNamePreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.StringValueDeduplicatePreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.TransitionsPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.UpdateVendorPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.stubs.ClusterTransitionsServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.GroupStorageUpdatesStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStoreInterfaceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelTransitionRepositoryStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.StatsIndexedModelQueryServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.YtModelIndexByIdReaderStub;
import ru.yandex.market.mbo.db.modelstorage.transitions.ClusterTransitionsService;
import ru.yandex.market.mbo.db.modelstorage.transitions.ModelTransitionsService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationContextStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.dump.DumpValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.processing.OutOfBoundsParamValueErrorProcessor;
import ru.yandex.market.mbo.db.modelstorage.validation.processing.ValidationErrorProcessingService;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.YtModelIndexReaders;
import ru.yandex.market.mbo.db.params.guru.BaseGuruServiceImpl;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.user.AutoUser;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation.RelationType.SKU_PARENT_MODEL;

/**
 * Базовый класс, для облегчения создания и проверки результатов тестирования.
 * Все наследники этого класса должны так или иначе тестировать
 * {@link ru.yandex.market.mbo.db.modelstorage.StorageUpdates}.
 *
 * @author s-ermakov
 */
public class BaseGroupStorageUpdatesTest {

    protected static final long USER_ID = 228;
    protected static final AutoUser AUTO_USER = new AutoUser(USER_ID);
    // fields

    protected OperationStats stats;
    protected ModelSaveContext context;

    protected BaseGuruServiceImpl guruService;
    protected CategoryParametersServiceClient categoryParametersServiceClient;
    protected StatsIndexedModelQueryService modelQueryService;
    protected GroupStorageUpdatesStub storage;
    protected ModelStoreInterfaceStub modelStore;
    protected ModelValidationService validationService;
    protected ModelGeneralizationService generalizationService;
    protected PartnerGeneralizationServiceImpl partnerGeneralizationService;
    protected ModelRulesExecutorService ruleService;
    protected ClusterTransitionsService clusterTransitionsService;
    protected AuditServiceMock auditServiceMock;
    protected ModelAuditServiceImpl modelAuditService;
    protected ModelAuditContextProvider modelAuditContextProvider;
    protected ModelAuditContext auditContext;
    protected ModelImageSyncService imageSyncService;

    protected MultiIdGenerator idGenerator;
    protected MultiIdGenerator generatedIdGenerator;
    protected ValidationErrorProcessingService validationErrorProcessingService;
    protected ModelTransitionRepositoryStub modelTransitionRepositoryStub;

    @Before
    public void before() {
        context = new ModelSaveContext(USER_ID);
        stats = new OperationStats();

        guruService = createGuruService();
        categoryParametersServiceClient = createCategoryParametersServiceClient();
        modelStore = createModelStore();
        validationService = createModelValidationService();
        clusterTransitionsService = createModelTransitionsService();
        generalizationService = createModelGeneralizationService();
        partnerGeneralizationService = createPartnerGeneralizationService();
        ruleService = createRuleService();
        auditServiceMock = new AuditServiceMock();
        modelAuditService = new ModelAuditServiceImpl(auditServiceMock);
        modelAuditContextProvider = Mockito.mock(ModelAuditContextProvider.class);
        auditContext = Mockito.mock(ModelAuditContext.class);
        modelTransitionRepositoryStub = new ModelTransitionRepositoryStub();
        imageSyncService = new ModelImageSyncService(categoryParametersServiceClient);

        when(auditContext.getStats()).thenReturn(stats.getSaveStats());
        when(modelAuditContextProvider.createContext()).thenReturn(auditContext);
        validationErrorProcessingService = createValidationErrorProcessingService();

        YtModelIndexReaders readers = createModelIndexReader();
        YtSaasIndexesWrapper modelStorageInternalService = new YtSaasIndexesWrapper(
            readers,
            null,
            createCompositeIndexDecider()
        );
        modelQueryService = new StatsIndexedModelQueryServiceStub(modelStore, modelStorageInternalService);

        storage = new GroupStorageUpdatesStub(
            modelStore,
            modelQueryService,
            clusterTransitionsService,
            new ModelTransitionsService(modelTransitionRepositoryStub),
            new ModelSavePreprocessingServiceImpl(modelQueryService, ImmutableList.<ModelSavePreprocessor>builder()
                .add(new NewModelPreprocessor())
                .add(new ConcurrentModificationPreprocessor())
                .add(new TransitionsPreprocessor())
                .add(new NamesToAliasesPreprocessor(guruService))
                .add(new DeletedModelsPrerocessor())
                .add(new SkuMovePreprocessor())
                .add(new ModificationToModelPreprocessor())
                .add(new CategoryIdChangePreprocessor())
                .add(new ModelPicturePreprocessor(imageSyncService))
                .add(new ModelDescriptionPreprocessor())
                .add(new ModelPickerPreprocessor(categoryParametersServiceClient))
                .add(new SignModificationsPreprocessor(categoryParametersServiceClient))
                .add(new ModelRulesPreprocessor(ruleService))
                .add(new ModelGeneralizationPreprocessor(generalizationService))
                .add(new PModelGeneralizationPreprocessor(partnerGeneralizationService))
                .add(new UpdateVendorPreprocessor())
                .add(new SkuNamePreprocessor())
                .add(new FirstPublishedPreprocessor())
                .add(new StringValueDeduplicatePreprocessor())
                .add(new CommonPreprocessor())
                .add(new ModelSourcePreprocessor())
                .build()),
            validationService,
            modelAuditContextProvider,
            validationErrorProcessingService,
            modelAuditService,
            modelTransitionRepositoryStub);

        modelStore.setGroupStorageUpdatesStub(storage);
    }

    protected ValidationErrorProcessingService createValidationErrorProcessingService() {
        return new ValidationErrorProcessingService(
            Collections.singletonList(new OutOfBoundsParamValueErrorProcessor()));
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    protected CategoryParametersServiceClient createCategoryParametersServiceClient() {
        return CategoryParametersServiceClientStub.ofCategory(3L, emptyList());
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    protected BaseGuruServiceImpl createGuruService() {
        BaseGuruServiceImpl result = new BaseGuruServiceImpl();
        result.addCategory(3L, 5L, true);
        return result;
    }

    protected ModelStoreInterfaceStub createModelStore() {
        idGenerator = createIdGenerator();
        generatedIdGenerator = createGeneratedIdGenerator();
        return new ModelStoreInterfaceStub(storage, idGenerator, generatedIdGenerator);
    }

    protected ModelValidationService createModelValidationService(List<ModelValidator> validators) {
        return new ModelValidationService(validators, emptyList(), emptyList(), this::createModelValidationContext);
    }

    protected ModelValidationService createModelValidationService() {
        return createModelValidationService(emptyList());
    }

    protected ModelValidationContext createModelValidationContext() {
        DumpValidationService dumpValidationService = Mockito.mock(DumpValidationService.class);
        ModelValidationContextStub validationContextStub = createValidationContextStub(dumpValidationService);
        return validationContextStub;
    }

    protected ModelValidationContextStub createValidationContextStub(DumpValidationService dumpValidationService) {
        ModelValidationContextStub validationContextStub = new ModelValidationContextStub(dumpValidationService);
        validationContextStub.setStatsModelStorageService(storage);
        validationContextStub.addParam(KnownIds.NAME_PARAM_ID, XslNames.NAME, true, SkuParameterMode.SKU_NONE);
        validationContextStub.addParam(KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR, true, SkuParameterMode.SKU_NONE);
        return validationContextStub;
    }

    protected ModelGeneralizationService createModelGeneralizationService() {
        return new ModelGeneralizationServiceImpl(guruService, AUTO_USER, categoryParametersServiceClient);
    }

    private PartnerGeneralizationServiceImpl createPartnerGeneralizationService() {
        return new PartnerGeneralizationServiceImpl(AUTO_USER);
    }

    protected ModelRulesExecutorService createRuleService() {
        return model -> false;
    }

    protected ClusterTransitionsServiceStub createModelTransitionsService() {
        return new ClusterTransitionsServiceStub();
    }


    protected YtModelIndexReaders createModelIndexReader() {
        return new YtModelIndexReaders(
            modelReaderStub(),
            null,
            null,
            null,
            null,
            null,
            null,
            createCompositeIndexDecider(),
            null,
            null
        );
    }

    protected CompositeIndexDecider createCompositeIndexDecider() {
        return new CompositeIndexDecider(
            Arrays.asList(new YtIndexReader[]{modelReaderStub()})
        );
    }

    protected KeyValueMapService keyValueMapService() {
        KeyValueMapService mock = Mockito.mock(KeyValueMapService.class);
        return mock;
    }

    protected YtModelIndexByIdReaderStub modelReaderStub() {
        return new YtModelIndexByIdReaderStub(() -> storage);
    }

    private MultiIdGenerator createIdGenerator() {
        return new MultiIdGeneratorStub(0);
    }

    private MultiIdGenerator createGeneratedIdGenerator() {
        return new MultiIdGeneratorStub(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
    }

    // help methods

    /**
     * Инициализируем хранилище тестовыми моделями, как будто они там и были.
     */
    protected void putToStorage(CommonModel... models) {
        putToStorage(Arrays.asList(models));
    }

    protected void putToStorage(Collection<CommonModel> models) {
        storage.putToStorage(models);
    }

    // get
    protected CommonModel searchById(long id) {
        return storage.searchById(id, new ReadStats());
    }

    // create
    protected CommonModel createGuruModel(long id) {
        return createGuruModel(id, builder -> {
        });
    }

    protected CommonModel createGuruModel(long id, Consumer<CommonModelBuilder> builder) {
        return createGuruModel(id, 1, 1, builder);
    }

    protected CommonModel createSku(long id, long categoryId, long parentModelId) {
        return createModel(id, categoryId, 1, CommonModel.Source.SKU,
            b -> b.modelRelation(parentModelId, categoryId, SKU_PARENT_MODEL));
    }

    protected CommonModel createGuruModel(long id, long categoryId, Consumer<CommonModelBuilder> builder) {
        return createGuruModel(id, categoryId, 1, builder);
    }

    protected CommonModel createGuruModel(long id, long categoryId, long vendorId,
                                          Consumer<CommonModelBuilder> builder) {
        return createModel(id, categoryId, vendorId, CommonModel.Source.GURU, builder);
    }

    protected CommonModel createModel(long id, long categoryId, long vendorId, CommonModel.Source source,
                                      Consumer<CommonModelBuilder> builder) {
        CommonModelBuilder modelBuilder = CommonModelBuilder.newBuilder()
            .id(id).category(categoryId).vendorId(vendorId)
            .source(source)
            .modificationDate(new Date())
            .currentType(source);

        builder.accept(modelBuilder);

        return modelBuilder.getModel();
    }

    // assert

    protected void assertTransitionsCount(int expectedNumber) {
        Assert.assertEquals(expectedNumber,
            ((ClusterTransitionsServiceStub) clusterTransitionsService).getTransitionsCount());
    }

    protected void assertRelations(CommonModel actual,
                                   long id1, long categoryId1, ModelRelation.RelationType relationType1) {
        assertRelations(actual, Arrays.asList(id1), Arrays.asList(categoryId1), Arrays.asList(relationType1));
    }

    protected void assertRelations(CommonModel actual,
                                   long id1, long categoryId1, ModelRelation.RelationType relationType1,
                                   long id2, long categoryId2, ModelRelation.RelationType relationType2) {
        assertRelations(actual,
            Arrays.asList(id1, id2),
            Arrays.asList(categoryId1, categoryId2),
            Arrays.asList(relationType1, relationType2));
    }

    private void assertRelations(CommonModel actual, List<Long> ids, List<Long> categoryIds,
                                 List<ModelRelation.RelationType> relationTypes) {
        Assert.assertEquals("Expected model to have " + ids.size() + " relations",
            ids.size(), actual.getRelations().size());

        for (int i = 0; i < ids.size(); i++) {
            long id = ids.get(i);
            long categoryId = categoryIds.get(i);
            ModelRelation.RelationType relationType = relationTypes.get(i);

            ModelRelation relation = actual.getRelation(id)
                .orElseThrow(() -> new AssertionException(String.format("Expected model id %d " +
                    "doesn't contain relation to id %d", actual.getId(), id)));

            Assert.assertEquals(categoryId, relation.getCategoryId());
            Assert.assertEquals(relationType, relation.getType());
        }
    }
}
