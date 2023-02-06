package ru.yandex.market.mbo.db.modelstorage;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.configs.TestIndexesHolder;
import ru.yandex.market.mbo.configs.YtTestConfiguration;
import ru.yandex.market.mbo.db.modelstorage.ExpectedIndexChangesBuilder.RowType;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface.ModelStoreException;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreCallbackResult;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreResult;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.UniqueVendorCodeValidator;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelStore;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelUtil;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.CurrentTypeUtil;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.YtModelCatalogIndexPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.YtModelIndexReaders;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.model.YtModelIndexByAlias;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.model.YtModelIndexByBarcode;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.YtModelIndexByCategoryVendorIdPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.YtModelIndexByGroupIdPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.YtModelIndexByIdPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.model.YtModelIndexByVendorCode;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelQuality;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.yt.util.table.YtTableHttpApi;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author gilmulla
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {YtTestConfiguration.class})
@SuppressWarnings("checkstyle:magicnumber")
public class YtModelStorageTest extends AbstractModelStoreTest {

    @Resource
    private TestIndexesHolder testIndexesHolder;
    private Yt yt;
    private YtTableRpcApi rpcApi;
    private YtModelStore ytModelStore;

    private static final long MODEL_ID_1 = 1;
    private static final long MODEL_ID_2 = 2;
    private static final long MODEL_ID_3 = 3;
    private static final long MODEL_ID_4 = 4;
    private static final String ISO_CODE_1 = "1";
    private static final String MODEL_BARCODE_1 = "124567454646";
    private static final String MODEL_BARCODE_2 = "524567563232";
    private static final String MODEL_BARCODE_3 = "204525701343";
    private static final String MODEL_BARCODE_4 = "893472385483";

    private static final Random RANDOM = new Random();

    @Before
    public void prepare() throws InterruptedException {
        yt = testIndexesHolder.yt();
        rpcApi = testIndexesHolder.rpcApi();
        ytModelStore = testIndexesHolder.ytModelStore();

        // еще прилось таки почистить таблицы все, потому что для некоторых обязательно непересечене
    }

    @After
    public void clearTables() {
        // сами тесты разведены друг между другом через последовательный Id
        // но на всякий случай мы таки чистим все.
        testIndexesHolder.clearTables();
    }

    @Override
    public YtModelStore getModelStore() {
        return ytModelStore;
    }

    /**
     * Этот метод проверяет корректность метода ModelStoreInterface.getModelById,
     * на котором основаны все остальные тесты. Для проверки используется
     * "сырой" yt api. Прочие тесты уже используют метод ModelStoreInterface.getModels.
     * Тем самым мы заменяем априорное предположение, что метод ModelStoreInterface.getModels
     * работает корректно, реальным тестом.
     *
     * @throws ModelStoreException
     * @throws NoSuchElementException
     * @throws InvalidProtocolBufferException
     */
    @Test
    public void testGetModelsWithPlainYtApi()
        throws ModelStoreException, InvalidProtocolBufferException, NoSuchElementException {

        Model modelToSave = createTestModel();

        ModelStoreSaveGroup modelStoreSaveGroup = new ModelStoreSaveGroup(
            singletonList(modelToSave),
            singletonList(modelToSave.getId()),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        ModelStoreResult modelStoreResult = ytModelStore
            .saveModels(modelStoreSaveGroup, 1L, new OperationStats());
        assertThat(modelStoreResult.getModelStoreStatus().getStatus())
            .isEqualTo(ModelStorage.OperationStatusType.OK);

        // 1. Читаем сохраненную модель "голым" yt api
        ListF<YTreeMapNode> queryKeys = new ArrayListF<>();
        queryKeys.add(YTree.mapBuilder()
            .key(YtModelColumns.CATEGORY_ID).value(modelToSave.getCategoryId())
            .key(YtModelColumns.MODEL_ID).value(modelToSave.getId()).buildMap());

        YtTableHttpApi.NodeWrapper node = readWithYtApi(modelToSave.getCategoryId(), modelToSave.getId());
        Model ytApiModel = ModelStorage.Model.parseFrom(
            node.bytesValue(YtModelColumns.DATA));

        // 2. Читаем сохраненную модель методом getModels(categoryId, modelIds)
        Model storageModel = ytModelStore.getModelById(modelToSave.getCategoryId(), modelToSave.getId());

        // 3. Убедимся, что модели, прочитанные сырым api и методом getModels - эквивалентны
        Assert.assertEquals(ytApiModel, storageModel);

        // Теперь можно использовать метод getModels в других тестах
    }

    @Test
    public void testGetModels() throws ModelStoreException {
        Model model1 = Model.newBuilder().mergeFrom(createTestModel()).setCategoryId(1).build();
        Model model2 = Model.newBuilder().mergeFrom(createTestModel()).setCategoryId(1).build();

        ModelStoreSaveGroup modelStoreSaveGroup = new ModelStoreSaveGroup(
            asList(model1, model2),
            asList(model1.getId(), model2.getId()),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        ModelStoreResult modelStoreResult = ytModelStore
            .saveModels(modelStoreSaveGroup, 1L, new OperationStats());
        assertThat(modelStoreResult.getModelStoreStatus().getStatus())
            .isEqualTo(ModelStorage.OperationStatusType.OK);

        Model expected1 = ytModelStore.getModelById(1, model1.getId());
        Model expected2 = ytModelStore.getModelById(1, model2.getId());

        List<Model> models = ytModelStore.getModels(1, asList(model1.getId(), model2.getId()));

        Model found1 = models.stream().filter(model -> model.getId() == model1.getId()).findFirst().orElse(null);
        Model found2 = models.stream().filter(model -> model.getId() == model2.getId()).findFirst().orElse(null);

        Assert.assertArrayEquals(expected1.toByteArray(), found1.toByteArray());
        Assert.assertArrayEquals(expected2.toByteArray(), found2.toByteArray());
    }

    @Test
    public void testCheckAndUpdateOverlappingParallelModificationFail() throws ModelStoreInterface.ModelStoreException {
        // Модель в хранилище
        ModelStorage.Model modelInStore = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setModifiedTs(MODIFIED_TS + 100)
            .build();
        createModels(singletonList(modelInStore));

        // Эту модель начинают менять две транзакции
        // Первая транзакция - тестовая, вторая транзакция - создаваемая методом checkAndUpdate(Model)
        // Причем первая (тестовая)  транзакция фиксируется в тот момент, когда вторая (checkAndUpdate) уже началась,
        // но не успела завершиться

        // Изменения, производимые транзакцией 1
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder().mergeFrom(modelInStore)
            .setArticle("Transaction 1")
            .setModifiedTs(MODIFIED_TS + 200)
            .build();

        // Изменения, производимые транзакцией 2
        ModelStorage.Model model2 = ModelStorage.Model.newBuilder().mergeFrom(modelInStore)
            .setArticle("Transaction 2")
            .build();

        // Start transaction 1 (test transaction)
        ApiServiceTransaction tr = rpcApi.getClient().startTransaction(TestIndexesHolder.TEST_TRANSACTION_OPTIONS)
            .join();

        // Start transaction 2
        // Данная транзакция успешно стартует, так как время модификации в базе (MODIFIED_TS + 100)
        // заведомо меньше времени модификации модели транзакцией 2 (MODIFIED_TS + 300)
        Map<Long, Boolean> res = ytModelStore.checkAndUpdateWithCallback(singletonList(model2),
            MODIFIED_TS + 300, dbModel -> {
                // Первая (тестовая) транзакция меняет модель
                ModifyRowsRequest request = rpcApi.createModifyRowRequest();
                request.addUpdate(YtModelUtil.modelToMap(model1));
                tr.modifyRows(request).join();
                tr.commit().join();
            }, new OperationStats());

        // Транзакция 2 не должна поменять модель - поскольку транзакция 1 успела зафиксировать свои изменения  раньше
        Assert.assertFalse(res.get(model2.getId()));

        // Проверяем модель в базе - она должна соответствовать изменениям транзакции 1
        ModelStorage.Model storedModel = getModelStore()
            .getModelById(modelInStore.getCategoryId(), modelInStore.getId());
        assertThat(model1).isEqualTo(storedModel);
    }

    @Test
    public void testCheckAndUpdateOverlappingParallelModificationSuccess()
        throws ModelStoreInterface.ModelStoreException {
        // Модель в хранилище
        ModelStorage.Model modelInStore = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setModifiedTs(MODIFIED_TS + 100)
            .build();
        createModels(singletonList(modelInStore));

        // Эту модель начинают менять две транзакции
        // Первая транзакция - создаваемая методом checkAndUpdate(Model), вторая транзакция - тестовая
        // Причем первая (создаваемая методом checkAndUpdate)  транзакция фиксируется в тот момент, когда
        // вторая (тестовая) уже началась, но не успела завершиться.

        // Изменения, производимые транзакцией 1
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder().mergeFrom(modelInStore)
            .setArticle("Transaction 1")
            .build();

        // Изменения, производимые транзакцией 2
        ModelStorage.Model model2 = ModelStorage.Model.newBuilder().mergeFrom(modelInStore)
            .setArticle("Transaction 2")
            .setModifiedTs(MODIFIED_TS + 300)
            .build();
        // Start transaction 1
        // Данная транзакция успешно стартует, так как время модификации в базе (MODIFIED_TS + 100)
        // заведомо меньше времени модификации модели транзакцией 2 (MODIFIED_TS + 200)
        ApiServiceTransaction[] tr1 = new ApiServiceTransaction[1];
        Map<Long, Boolean> res = ytModelStore.checkAndUpdateWithCallback(singletonList(model1),
            MODIFIED_TS + 200, dbModel -> {
                // Start transaction 2 (test transaction)
                tr1[0] = rpcApi.getClient().startTransaction(TestIndexesHolder.TEST_TRANSACTION_OPTIONS).join();

                // Вторая (тестовая) транзакция меняет модель, но не успевает фиксировать до завершения первой
                ModifyRowsRequest request = rpcApi.createModifyRowRequest();
                request.addUpdate(YtModelUtil.modelToMap(model2));
                tr1[0].modifyRows(request).join();
            }, new OperationStats());

        Throwable throwable = Assertions.catchThrowable(() -> tr1[0].commit().join());
        assertThat(throwable).isInstanceOf(CompletionException.class);
        assertThat(YtModelStore.isTransactionConflict(throwable.getCause())).isTrue();

        // Транзакция 1 должна успешно поменять модель - поскольку транзакция 2 не успела зафиксировать свои изменения
        Assert.assertTrue(res.get(model1.getId()));

        // Проверяем модель в базе - она должна соответствовать изменениям транзакции 1
        ModelStorage.Model storedModel = getModelStore()
            .getModelById(modelInStore.getCategoryId(), modelInStore.getId());
        // Не можем сравнивать тела моделей, так как не знаем дату модификации, назначенный внутри checkAndUpdate
        // Поэтому сравниваем характерные поля
        Assert.assertEquals("Transaction 1", storedModel.getArticle());
    }

    private YtTableHttpApi.NodeWrapper readWithYtApi(long categoryId, long modelId) {
        ListF<YTreeMapNode> queryKeys = new ArrayListF<>();
        queryKeys.add(YTree.mapBuilder()
            .key(YtModelColumns.CATEGORY_ID).value(categoryId)
            .key(YtModelColumns.MODEL_ID).value(modelId).buildMap());

        YTreeMapNode[] result = new YTreeMapNode[1];
        yt.tables().lookupRows(testIndexesHolder.modelTablePath(),
            YTableEntryTypes.YSON,
            queryKeys,
            YTableEntryTypes.YSON,
            row -> {
                result[0] = row;
            });
        if (result[0] == null) {
            return null;
        }
        return new YtTableHttpApi.NodeWrapper(result[0]);
    }

    /**
     * Данный тест проверяет, что при сохранении модельки без groupModelId она посчитается и попадет в протобафку
     * и индекс.
     * См. тест ниже для большей информации.
     *
     * @throws ModelStoreInterface.ModelStoreException
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void shouldComputeGroupModelIdForModelWhenInserting()
        throws ModelStoreInterface.ModelStoreException, InvalidProtocolBufferException {
        Model testModel = createTestModel();
        ModelStorage.Model modelToSave = testModel.toBuilder().clearGroupModelId().build();
        createModels(singletonList(modelToSave));
        ModelStorage.Model modelFetched = ytModelStore.getModelById(modelToSave.getCategoryId(), modelToSave.getId());
        // computed group model id when saved
        // read with YT api to check what proto-data has
        YtTableHttpApi.NodeWrapper node = readWithYtApi(modelToSave.getCategoryId(), modelToSave.getId());
        Model ytApiModel = ModelStorage.Model.parseFrom(node.bytesValue(YtModelColumns.DATA));
        Assert.assertEquals(ytApiModel.getGroupModelId(), modelFetched.getGroupModelId());
        // computed group model id stored in index too
        YtTableHttpApi.NodeWrapper modelGroupIndex = getModelGroupIndex(testModel);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(modelToSave.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();
    }

    /**
     * Обратный тест - кладем сырым ytApi (имитируем старые модельки) без groupModelId в протке.
     * И проверяем, что при чтении через getModels будет проставлено groupId.
     * <p>
     * Этот тест и тест выше вместе дают консистентное поведение на время перехода.
     * Сейчас groupModelId не стоит в старых протках, но оно нам нужно в коде, поэтому тянем его из колонки.
     * А при сохранении оно должно попадать в протку, колонку (как и раньше) и индекс.
     *
     * @throws ModelStoreInterface.ModelStoreException
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void shouldComputeGroupModelIdWhenExtractingModel()
        throws ModelStoreInterface.ModelStoreException, InvalidProtocolBufferException {
        Model testModel = createTestModel();
        ModelStorage.Model modelToSave = testModel.toBuilder().clearGroupModelId().build();
        ApiServiceTransaction tr = rpcApi.getClient().startTransaction(TestIndexesHolder.TEST_TRANSACTION_OPTIONS)
            .join();
        ModifyRowsRequest request = rpcApi.createModifyRowRequest();
        request.addUpdate(YtModelUtil.modelToMap(modelToSave));
        tr.modifyRows(request).join();
        tr.commit().join();

//        plain api does not contain groupModelId
        YtTableHttpApi.NodeWrapper node = readWithYtApi(modelToSave.getCategoryId(), modelToSave.getId());
        Model ytApiModel = ModelStorage.Model.parseFrom(node.bytesValue(YtModelColumns.DATA));
        assertThat(ytApiModel.hasGroupModelId()).isFalse();
        // computed group model id while extracting model
        ModelStorage.Model modelFetched = ytModelStore
            .getModelById(modelToSave.getCategoryId(), modelToSave.getId());
        assertThat(modelFetched.getGroupModelId()).isEqualTo(testModel.getGroupModelId());
    }

    @Test
    public void testCreateNewModel() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model modelToSave = createTestModel();

        createModels(singletonList(modelToSave));

        ModelStorage.Model modelFetched = ytModelStore
            .getModelById(modelToSave.getCategoryId(), modelToSave.getId());

        Assert.assertEquals(modelToSave, modelFetched);

        YtTableHttpApi.NodeWrapper modelIndex = getModelIndex(modelToSave);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(modelToSave.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(modelIndex.boolValue(YtModelColumns.PARENT_ID)).isNull();
        assertThat(modelIndex.longValue(YtModelColumns.GROUP_MODEL_ID))
            .isEqualTo(modelToSave.getGroupModelId());
        assertThat(modelIndex.boolValue(YtModelColumns.IS_SKU)).isTrue();
        assertThat(modelIndex.boolValue(YtModelColumns.PUBLISHED)).isTrue();
        assertThat(modelIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));

        YtTableHttpApi.NodeWrapper modelGroupIndex = getModelGroupIndex(modelToSave);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(modelToSave.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(modelGroupIndex.boolValue(YtModelColumns.PARENT_ID)).isNull();
        assertThat(modelGroupIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));

        YtTableHttpApi.NodeWrapper modelCategoryVendorIndex = getModelCategoryVendorIndex(modelToSave);
        assertThat(modelCategoryVendorIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(modelCategoryVendorIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));
    }

    @Test
    public void testUpdateModelNotImportantFields() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model modelToSave = createTestModel().toBuilder().build();

        createModels(singletonList(modelToSave));

        Model updatedModel = modelToSave.toBuilder()
            .setArticle("Updated article")
            .build();
        updateModels(updatedModel);
        ModelStorage.Model modelFetched = ytModelStore
            .getModelById(modelToSave.getCategoryId(), modelToSave.getId());


        Assert.assertNotEquals(modelToSave, modelFetched);
        Assert.assertEquals(updatedModel, modelFetched);

        // all fine with updated model
        YtTableHttpApi.NodeWrapper modelIndex = getModelIndex(updatedModel);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(updatedModel.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(modelIndex.boolValue(YtModelColumns.PARENT_ID)).isNull();
        assertThat(modelIndex.longValue(YtModelColumns.GROUP_MODEL_ID))
            .isEqualTo(updatedModel.getGroupModelId());
        assertThat(modelIndex.boolValue(YtModelColumns.IS_SKU)).isTrue();
        assertThat(modelIndex.boolValue(YtModelColumns.PUBLISHED)).isTrue();
        assertThat(modelIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));

        YtTableHttpApi.NodeWrapper modelGroupIndex = getModelGroupIndex(updatedModel);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(updatedModel.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(modelGroupIndex.boolValue(YtModelColumns.PARENT_ID)).isNull();
        assertThat(modelGroupIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));

        YtTableHttpApi.NodeWrapper modelCategoryVendorIndex = getModelCategoryVendorIndex(updatedModel);
        assertThat(modelCategoryVendorIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(modelCategoryVendorIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));

        // index have not changed so no problem finding same model
        modelIndex = getModelIndex(modelToSave);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(modelToSave.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();

        modelGroupIndex = getModelGroupIndex(modelToSave);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(modelToSave.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();

        modelCategoryVendorIndex = getModelCategoryVendorIndex(modelToSave);
        assertThat(modelCategoryVendorIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(modelCategoryVendorIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));
    }

    @Test
    public void testUpdateModelFailsOnPreaction() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model modelToSave = createTestModel().toBuilder().clearArticle().build();

        createModels(singletonList(modelToSave));

        Model updatedModel = modelToSave.toBuilder()
            .setArticle("Updated something")
            .build();
        ModelStoreResult storeResult = updateModels(
            ms -> ModelStoreCallbackResult.failure(new ModelStoreException("Test exception")),
            updatedModel);
        assertThat(storeResult.getModelStoreStatus().getStatus())
            .isEqualTo(ModelStorage.OperationStatusType.INTERNAL_ERROR);
        assertThat(storeResult.getUpdatedModelIds()).isEmpty();
        assertThat(storeResult.getBeforeModelsToAudit()).isEmpty();
        assertThat(storeResult.getModelIdsToAudit()).isEmpty();
        assertThat(storeResult.getModelTransitions()).isEmpty();
        assertThat(storeResult.getModelStoreStatus().getStatusModelIds()).containsExactlyInAnyOrder(
            modelToSave.getId()
        );
        assertThat(storeResult.getModelStoreStatus().getStatusModels()).containsExactlyInAnyOrder(
            updatedModel
        );
    }

    @Test
    public void testDeleteModel() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model modelToSave = createTestModel();

        createModels(singletonList(modelToSave));

        Model updatedModel = modelToSave.toBuilder()
            .setDeleted(true)
            .build();
        updateModels(updatedModel);
        ModelStorage.Model modelFetched = ytModelStore
            .getModelById(modelToSave.getCategoryId(), modelToSave.getId());


        Assert.assertNotEquals(modelToSave, modelFetched);
        Assert.assertEquals(updatedModel, modelFetched);
        // all is same, except deleted flag is true now
        YtTableHttpApi.NodeWrapper modelIndex = getModelIndex(updatedModel);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(updatedModel.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isTrue();
        assertThat(modelIndex.boolValue(YtModelColumns.PARENT_ID)).isNull();
        assertThat(modelIndex.longValue(YtModelColumns.GROUP_MODEL_ID))
            .isEqualTo(updatedModel.getGroupModelId());
        assertThat(modelIndex.boolValue(YtModelColumns.IS_SKU)).isTrue();
        assertThat(modelIndex.boolValue(YtModelColumns.PUBLISHED)).isTrue();
        assertThat(modelIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));

        YtTableHttpApi.NodeWrapper modelGroupIndex = getModelGroupIndex(updatedModel);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(updatedModel.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isTrue();
        assertThat(modelGroupIndex.boolValue(YtModelColumns.PARENT_ID)).isNull();
        assertThat(modelGroupIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));

        YtTableHttpApi.NodeWrapper modelCategoryVendorIndex = getModelCategoryVendorIndex(updatedModel);
        assertThat(modelCategoryVendorIndex.boolValue(YtModelColumns.DELETED)).isTrue();
        assertThat(modelCategoryVendorIndex.longValue(YtModelColumns.CURRENT_TYPE).intValue())
            .isEqualTo(CurrentTypeUtil.convertToYtColumnValue(CommonModel.Source.GURU));
    }

    @Test
    public void testTransferModelToOtherCategory() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model modelToSave = createTestModel();

        createModels(singletonList(modelToSave));

        ModelStorage.Model updatedModel = modelToSave.toBuilder().setCategoryId(15).build();
        updateModelsCategory(15, singletonList(modelToSave));

        ModelStorage.Model modelFetched = ytModelStore
            .getModelById(updatedModel.getCategoryId(), modelToSave.getId());

        Assert.assertNotEquals(modelToSave, modelFetched);
        Assert.assertEquals(updatedModel, modelFetched);

        // all fine with updated model
        YtTableHttpApi.NodeWrapper modelIndex = getModelIndex(updatedModel);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(updatedModel.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        YtTableHttpApi.NodeWrapper modelGroupIndex = getModelGroupIndex(updatedModel);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(updatedModel.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(getModelCategoryVendorIndex(updatedModel)
            .boolValue(YtModelColumns.DELETED)).isFalse();

        // some trickery here - found index would actually be from updated model, not modelToSave
        modelIndex = getModelIndex(modelToSave);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(updatedModel.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        modelGroupIndex = getModelGroupIndex(modelToSave);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(updatedModel.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        // and these index will have deleted flag (since key is model+category)
        assertThat(getModelCategoryVendorIndex(modelToSave)).isNull();
    }

    @Test
    public void testSingleIndexNullRowsNotPresent() {
        Model modelProto = createTestModel();
        Model psku10 = modelProto.toBuilder()
            .setCurrentType("PARTNER_SKU") // not supported by catalog index
            .setSourceType("PARTNER_SKU")
            .build();
        ModelStorage.Model psku20 = modelProto.toBuilder()
            .setCurrentType("SKU") // supported
            .setSourceType("PARTNER_SKU")
            .build();
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = Collections.singletonMap(
            MODEL_ID_1, psku10
        );
        ModifyRowsRequest modifyRowsRequest = testIndexesHolder.ytModelCatalogIndexWriter()
            .requestIndex(singletonList(psku20), beforeModelMap);

        modifyRowsRequest.getRows().forEach(row -> {
            boolean allNull = row.getValues().stream().allMatch(v -> v.getValue() == null);
            assertThat(allNull).as("Check there are no index rows with NULLs").isFalse();
        });
    }

    @Test
    public void testDeleteAndMoveToOtherCategoryInSameContext() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model modelToSave1 = createTestModel();
        ModelStorage.Model modelToSave2 = createTestModel();
        createModels(asList(modelToSave1, modelToSave2));

        Model deletedModel1 = modelToSave1.toBuilder()
            .setPublished(false)
            .setDeleted(true)
            .build();
        ModelStorage.Model newCategoryModel1 = modelToSave1.toBuilder()
            .setPublished(false)
            .setCategoryId(15).build();
        Model deletedModel2 = modelToSave2.toBuilder()
            .setPublished(false)
            .setDeleted(true)
            .build();
        ModelStorage.Model newCategoryModel2 = modelToSave2.toBuilder()
            .setPublished(false)
            .setCategoryId(15).build();

        ModelStoreSaveGroup modelStoreSaveGroup = new ModelStoreSaveGroup(
            asList(newCategoryModel1, deletedModel1, deletedModel2, newCategoryModel2),
            emptyList(),
            Collections.emptyMap(),
            asList(modelToSave1, modelToSave2).stream()
                .collect(Collectors.groupingBy(Model::getCategoryId,
                    Collectors.mapping(Model::getId, Collectors.toSet()))));
        ModelStoreResult storeResult = ytModelStore.saveModels(modelStoreSaveGroup, TEST_MODEL_MODIFIED_TS,
            new OperationStats());
        assertThat(storeResult.getModelStoreStatus().getStatus())
            .isEqualTo(ModelStorage.OperationStatusType.OK);

        ModelStorage.Model modelFetched1 = ytModelStore
            .getModelById(newCategoryModel1.getCategoryId(), modelToSave1.getId());
        Assert.assertNotEquals(modelToSave1, modelFetched1);
        Assert.assertEquals(newCategoryModel1, modelFetched1);
        ModelStorage.Model modelFetched2 = ytModelStore
            .getModelById(newCategoryModel2.getCategoryId(), modelToSave2.getId());
        Assert.assertNotEquals(modelToSave2, modelFetched2);
        Assert.assertEquals(newCategoryModel2, modelFetched2);

        YtTableHttpApi.NodeWrapper modelIndex = getModelIndex(newCategoryModel1);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(newCategoryModel1.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();

        modelIndex = getModelIndex(newCategoryModel2);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(newCategoryModel1.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();
    }

    @Test
    public void testChangeVendor() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model modelToSave = createTestModel();

        createModels(singletonList(modelToSave));

        Model updatedModel = modelToSave.toBuilder()
            .setVendorId(13L)
            .build();
        updateModels(updatedModel);
        ModelStorage.Model modelFetched = ytModelStore
            .getModelById(modelToSave.getCategoryId(), modelToSave.getId());


        Assert.assertNotEquals(modelToSave, modelFetched);
        Assert.assertEquals(updatedModel, modelFetched);

        // all fine with updated model
        YtTableHttpApi.NodeWrapper modelIndex = getModelIndex(updatedModel);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(updatedModel.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        YtTableHttpApi.NodeWrapper modelGroupIndex = getModelGroupIndex(updatedModel);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(updatedModel.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        assertThat(getModelCategoryVendorIndex(updatedModel)
            .boolValue(YtModelColumns.DELETED)).isFalse();

        // some trickery here - found index would actually be from updated model, not modelToSave
        modelIndex = getModelIndex(modelToSave);
        assertThat(modelIndex.longValue(YtModelColumns.CATEGORY_ID)).isEqualTo(updatedModel.getCategoryId());
        assertThat(modelIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        modelGroupIndex = getModelGroupIndex(modelToSave);
        assertThat(modelGroupIndex.longValue(YtModelColumns.CATEGORY_ID))
            .isEqualTo(updatedModel.getCategoryId());
        assertThat(modelGroupIndex.boolValue(YtModelColumns.DELETED)).isFalse();
        // and these index will have deleted flag (since key is category+vendor)
        assertThat(getModelCategoryVendorIndex(modelToSave)).isNull();
    }


    @Test
    public void testIndexingSimpleAdd() {
        long hid = 1L;
        long vendorId = 123;
        long parentId = 678;
        String alias = "Проверка 34";
        String vendorCode = "super-vendor-code";
        List<ModelStorage.ModelOrBuilder> models = singletonList(
            Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .setVendorId(vendorId)
                .setParentId(parentId)
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1))
                .addParameterValues(makeVendorCodeParam(vendorCode))
                .addParameterValues(makeAliasParam(alias))
                .build()
        );
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = Collections.singletonMap(
            MODEL_ID_1, Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setCurrentType("GURU").build()
        );
        ModifyRowsRequest bcModifyRowsRequest = testIndexesHolder.ytModelIndexByBarcodeWriter()
            .requestIndex(models, beforeModelMap);
        ModifyRowsRequest vcModifyRowsRequest = testIndexesHolder.ytModelIndexByVendorCodeWriter()
            .requestIndex(models, beforeModelMap);
        ModifyRowsRequest aliasModifyRowsRequest = testIndexesHolder.ytModelIndexByAliasWriter()
            .requestIndex(models, beforeModelMap);

        ModifyRowsRequest bcExpected = new ExpectedIndexChangesBuilder(bcModifyRowsRequest)
            .addExpectedBarcodeChanges(RowType.UPDATE, MODEL_ID_1, hid, MODEL_BARCODE_1)
            .build();
        ModifyRowsRequest vcExpected = new ExpectedIndexChangesBuilder(vcModifyRowsRequest)
            .addExpectedVendorCodeChanges(RowType.UPDATE, MODEL_ID_1, vendorId, hid,
                vendorCode)
            .build();
        ModifyRowsRequest aliasExpected = new ExpectedIndexChangesBuilder(aliasModifyRowsRequest)
            .addExpectedAliasChanges(RowType.UPDATE, MODEL_ID_1, vendorId, hid, parentId, alias)
            .build();

        assertThat(bcModifyRowsRequest.getRows()).containsExactlyInAnyOrderElementsOf(bcExpected.getRows());
        assertThat(vcModifyRowsRequest.getRows()).containsExactlyInAnyOrderElementsOf(vcExpected.getRows());
        assertThat(aliasModifyRowsRequest.getRows())
            .containsExactlyInAnyOrderElementsOf(aliasExpected.getRows());
    }

    @Test
    public void testIndexingDiff() {
        long hid = 1L;
        long vendorId = 123;
        long parentId = 678;
        String vendorCode1 = "super-vendor-code1";
        String vendorCode2 = "super-vendor-code2";
        String vendorCode3 = "super-vendor-code3";
        String alias1 = "какой-то алиас";
        String alias2 = "another alias";
        String alias3 = "thirdAlias";
        List<ModelStorage.ModelOrBuilder> models = asList(
            Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setVendorId(vendorId)
                .setParentId(parentId)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_4))
                .addParameterValues(makeVendorCodeParam(vendorCode1))
                .addParameterValues(makeVendorCodeParam(vendorCode2))
                .addParameterValues(makeVendorCodeParam(vendorCode3))
                .addParameterValues(makeAliasParam(alias1))
                .addParameterValues(makeAliasParam(alias2))
                .addParameterValues(makeAliasParam(alias3))
                .build(),
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setVendorId(vendorId)
                .setParentId(parentId)
                .setCurrentType("SKU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_4))
                .addParameterValues(makeVendorCodeParam(vendorCode2))
                .addParameterValues(makeVendorCodeParam(vendorCode3))
                .addParameterValues(makeAliasParam(alias2))
                .addParameterValues(makeAliasParam(alias3))
                .build()
        );
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = new HashMap<>();
        beforeModelMap.put(MODEL_ID_1,
            Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setVendorId(vendorId)
                .setParentId(parentId)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .addParameterValues(makeVendorCodeParam(vendorCode1))
                .addParameterValues(makeVendorCodeParam(vendorCode2))
                .addParameterValues(makeAliasParam(alias1))
                .addParameterValues(makeAliasParam(alias2))
                .build()
        );
        beforeModelMap.put(MODEL_ID_2,
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setVendorId(vendorId)
                .setParentId(parentId)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_4))
                .addParameterValues(makeVendorCodeParam(vendorCode1))
                .addParameterValues(makeAliasParam(alias1))
                .addParameterValues(makeAliasParam(alias3))
                .build()
        );
        ModifyRowsRequest bcModifyRowsRequest = testIndexesHolder.ytModelIndexByBarcodeWriter()
            .requestIndex(models, beforeModelMap);
        ModifyRowsRequest vcModifyRowsRequest = testIndexesHolder.ytModelIndexByVendorCodeWriter()
            .requestIndex(models, beforeModelMap);
        ModifyRowsRequest aliasModifyRowsRequest = testIndexesHolder.ytModelIndexByAliasWriter()
            .requestIndex(models, beforeModelMap);

        ModifyRowsRequest bcExpected = new ExpectedIndexChangesBuilder(bcModifyRowsRequest)
            .addExpectedBarcodeChanges(RowType.UPDATE, MODEL_ID_1, hid, MODEL_BARCODE_4)
            .addExpectedBarcodeChanges(RowType.DELETE, MODEL_ID_1, hid, MODEL_BARCODE_1)
            .build();
        ModifyRowsRequest vcExpected = new ExpectedIndexChangesBuilder(vcModifyRowsRequest)
            .addExpectedVendorCodeChanges(RowType.UPDATE, MODEL_ID_1, vendorId, hid, vendorCode3)
            .addExpectedVendorCodeChanges(RowType.DELETE, MODEL_ID_2, vendorId, hid, vendorCode1)
            .addExpectedVendorCodeChanges(RowType.UPDATE, MODEL_ID_2, vendorId, hid, vendorCode2, vendorCode3)
            .build();
        ModifyRowsRequest aliasExpected = new ExpectedIndexChangesBuilder(aliasModifyRowsRequest)
            .addExpectedAliasChanges(RowType.UPDATE, MODEL_ID_1, vendorId, hid, parentId, alias3)
            .addExpectedAliasChanges(RowType.DELETE, MODEL_ID_2, vendorId, hid, parentId, alias1, alias3)
            .build();
        assertThat(bcModifyRowsRequest.getRows()).containsExactlyInAnyOrderElementsOf(bcExpected.getRows());
        assertThat(vcModifyRowsRequest.getRows()).containsExactlyInAnyOrderElementsOf(vcExpected.getRows());
        assertThat(aliasModifyRowsRequest.getRows())
            .containsExactlyInAnyOrderElementsOf(aliasExpected.getRows());
    }

    @Test
    public void testBarcodeIndexingDeleteAndTransferTest() {
        long hid = 1L;
        List<ModelStorage.ModelOrBuilder> models = asList(
            Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .build(),
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setCurrentType("SKU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_4))
                .build()
        );
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = new HashMap<>();
        beforeModelMap.put(MODEL_ID_1,
            Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .build()
        );
        beforeModelMap.put(MODEL_ID_2,
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setCurrentType("SKU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_4))
                .build()
        );
        ModifyRowsRequest modifyRowsRequest = testIndexesHolder.ytModelIndexByBarcodeWriter()
            .requestIndex(models, beforeModelMap);

        // delete request here should be first
        ModifyRowsRequest expected = new ExpectedIndexChangesBuilder(modifyRowsRequest)
            .addExpectedBarcodeChanges(RowType.DELETE, MODEL_ID_1, hid, MODEL_BARCODE_3)
            .addExpectedBarcodeChanges(RowType.UPDATE, MODEL_ID_2, hid, MODEL_BARCODE_3)
            .build();

        assertThat(modifyRowsRequest.getRows()).containsExactlyElementsOf(expected.getRows());
    }

    @Test
    public void testBarcodeIndexingWithBeforeNull() {
        long hid = 1L;
        List<ModelStorage.ModelOrBuilder> models = singletonList(
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2)).build()
        );
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = new HashMap<>(); //do not add before model
        ModifyRowsRequest modifyRowsRequest = testIndexesHolder.ytModelIndexByBarcodeWriter()
            .requestIndex(models, beforeModelMap);

        ModifyRowsRequest expected = new ExpectedIndexChangesBuilder(modifyRowsRequest)
            .addExpectedBarcodeChanges(RowType.UPDATE, MODEL_ID_2, hid,
                MODEL_BARCODE_1, MODEL_BARCODE_2)
            .build();

        assertThat(modifyRowsRequest.getRows()).containsExactlyInAnyOrderElementsOf(expected.getRows());
    }

    @Test
    public void testBarcodeIndexingDeleted() {
        long hid = 1L;
        List<ModelStorage.ModelOrBuilder> models = singletonList(
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setCurrentType("SKU")
                .setDeleted(true)
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2)).build()
        );
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = new HashMap<>();
        beforeModelMap.put(MODEL_ID_2,
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setCurrentType("SKU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_4))
                .build()
        );
        ModifyRowsRequest modifyRowsRequest = testIndexesHolder.ytModelIndexByBarcodeWriter()
            .requestIndex(models, beforeModelMap);

        // модель удалена, поэтому должны удалиться все баркоды старой версии модели
        ModifyRowsRequest expected = new ExpectedIndexChangesBuilder(modifyRowsRequest)
            .addExpectedBarcodeChanges(RowType.DELETE, MODEL_ID_2, hid,
                MODEL_BARCODE_2, MODEL_BARCODE_3, MODEL_BARCODE_4)
            .build();

        assertThat(modifyRowsRequest.getRows()).containsExactlyInAnyOrderElementsOf(expected.getRows());
    }

    @Test
    public void testBarcodeIndexingWithRemove() {
        long hid = 1;
        List<ModelStorage.ModelOrBuilder> models = singletonList(
            Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setCurrentType("GURU").build()
        );
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = Collections.singletonMap(
            MODEL_ID_1, Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .build()
        );
        ModifyRowsRequest modifyRowsRequest = testIndexesHolder.ytModelIndexByBarcodeWriter()
            .requestIndex(models, beforeModelMap);

        ExpectedIndexChangesBuilder expectedBuilder = new ExpectedIndexChangesBuilder(modifyRowsRequest);
        expectedBuilder.addExpectedBarcodeChanges(RowType.DELETE, MODEL_ID_1, hid,
            MODEL_BARCODE_1, MODEL_BARCODE_2, MODEL_BARCODE_3);

        assertThat(modifyRowsRequest.getRows())
            .containsExactlyInAnyOrderElementsOf(expectedBuilder.build().getRows());
    }


    @Test
    public void testModelIndexReader() throws ModelStoreException, InterruptedException {
        List<Model> models = prepareSomeModels();

        YtModelIndexReaders ytModelIndexReaders = testIndexesHolder.ytModelIndexReaders();

        awaitForConditionOrThrow(() -> ytModelIndexReaders.findInModelIndex(new MboIndexesFilter()
            .setModelIds(models.stream().map(Model::getId)
                .collect(Collectors.toSet()))).size() == models.size());

        List<YtModelIndexByIdPayload> inModelIndex = ytModelIndexReaders.findInModelIndex(new MboIndexesFilter()
            .setModelIds(models.stream().map(Model::getId)
                .collect(Collectors.toSet())));
        assertThat(inModelIndex).containsExactlyInAnyOrderElementsOf(convertToIdIndex(models));

        inModelIndex = ytModelIndexReaders.findInModelIndex(new MboIndexesFilter()
            .setModelIds(models.stream().map(Model::getId).collect(Collectors.toSet()))
            .setDeleted(false));
        assertThat(inModelIndex).containsExactlyInAnyOrderElementsOf(
            convertToIdIndex(models.stream().filter(model -> !model.getDeleted()).collect(Collectors.toList())));

        inModelIndex = ytModelIndexReaders.findInModelIndex(new MboIndexesFilter()
            .setModelIds(models.stream().map(Model::getId).collect(Collectors.toSet()))
            .setCurrentTypes(singleton(CommonModel.Source.SKU))
            .setDeleted(false));
        assertThat(inModelIndex).containsExactlyInAnyOrderElementsOf(
            convertToIdIndex(models.stream().filter(model -> model.getCurrentType().equals("SKU"))
                .collect(Collectors.toList())));
    }


    @Test
    public void testGroupModelIndexReader() throws ModelStoreException, InterruptedException {
        List<Model> models = prepareSomeModels();
        YtModelIndexReaders ytModelIndexReaders = testIndexesHolder.ytModelIndexReaders();

        awaitForConditionOrThrow(() -> ytModelIndexReaders.findInGroupModelIndex(new MboIndexesFilter()
            .setGroupModelIds(models.stream().map(Model::getGroupModelId)
                .collect(Collectors.toSet()))).size() == models.size());

        List<YtModelIndexByGroupIdPayload> inModelIndex = ytModelIndexReaders.findInGroupModelIndex(
            new MboIndexesFilter().setGroupModelIds(models.stream()
                .filter(model -> model.getCurrentType().equals("GURU"))
                .map(Model::getGroupModelId).collect(Collectors.toSet())));
        assertThat(inModelIndex)
            .containsExactlyInAnyOrderElementsOf(convertToGroupModelId(models));

        inModelIndex = ytModelIndexReaders.findInGroupModelIndex(new MboIndexesFilter()
            .setGroupModelIds(models.stream().map(Model::getGroupModelId).collect(Collectors.toSet()))
            .setDeleted(false));
        assertThat(inModelIndex).containsExactlyInAnyOrderElementsOf(
            convertToGroupModelId(models.stream().filter(model -> !model.getDeleted()).collect(Collectors.toList())));
    }

    @Test
    public void testCategoryVendorIndexReader() throws ModelStoreException, InterruptedException {
        List<Model> models = prepareSomeModels();

        YtModelIndexReaders ytModelIndexReaders = testIndexesHolder.ytModelIndexReaders();

        awaitForConditionOrThrow(() -> ytModelIndexReaders.findInCategoryVendorIndex(new MboIndexesFilter()
            .setCategoryId(models.stream().map(Model::getCategoryId).findAny().get())).size() == models.size());

        List<YtModelIndexByCategoryVendorIdPayload> inModelIndex = ytModelIndexReaders.findInCategoryVendorIndex(
            new MboIndexesFilter().setCategoryId(models.stream().map(Model::getCategoryId).findFirst()
                .orElse(null)));
        assertThat(inModelIndex)
            .containsExactlyInAnyOrderElementsOf(convertToCategoryVendorId(models));

        inModelIndex = ytModelIndexReaders.findInCategoryVendorIndex(
            new MboIndexesFilter().setCategoryId(models.stream().map(Model::getCategoryId).findFirst()
                .orElse(null))
                .setDeleted(false));
        assertThat(inModelIndex).containsExactlyInAnyOrderElementsOf(
            convertToCategoryVendorId(models.stream()
                .filter(model -> !model.getDeleted()).collect(Collectors.toList())));
    }

    @Ignore
    @Test
    public void testYtModelCatalogIndexReader() throws ModelStoreException, InterruptedException {
        List<Model> models = prepareSomeModels();
        List<Model> guruModels = models.stream()
            .filter(m -> m.getCurrentType().equals("GURU")).collect(Collectors.toList());

        final YtModelIndexReaders ytModelIndexReaders = testIndexesHolder.ytModelIndexReaders();

        awaitForConditionOrThrow(() -> {
            MboIndexesFilter filter = new MboIndexesFilter()
                .setCategoryId(models.stream().map(Model::getCategoryId).findAny().get())
                .setQualities(singletonList(ModelQuality.OPERATOR));
            return ytModelIndexReaders.getCatalogModels(filter).size() == guruModels.size();
        });

        MboIndexesFilter filter = new MboIndexesFilter()
            .setCategoryId(models.stream().map(Model::getCategoryId).findFirst().get())
            .setQualities(singletonList(ModelQuality.OPERATOR));

        List<YtModelCatalogIndexPayload> inModelIndex = ytModelIndexReaders.getCatalogModels(filter);

        Assert.assertEquals(guruModels.size(), inModelIndex.size());

        assertThat(inModelIndex)
            .containsExactlyInAnyOrderElementsOf(convertToYtModelCatalogIndexPayload(guruModels));

    }

    @Test
    public void testVendorCodeModelIndexReader() throws ModelStoreException, InterruptedException {
        long vendorId = 123987L;
        long categoryId = 1090L;
        String vendorCode1 = "some-vc-1";
        String vendorCode2 = "some-vc-2";
        String vendorCode3 = "some-vc-3";
        String notUsedVendorCode = "not-used-vendor-code";
        ModelStorage.Model deletedModel = createTestModel().toBuilder().setDeleted(true).setVendorId(vendorId)
            .setCurrentType("SKU").addParameterValues(makeVendorCodeParam(vendorCode1))
            .setCategoryId(categoryId)
            .build();
        ModelStorage.Model sku = createTestModel().toBuilder()
            .setCurrentType("SKU")
            .setCategoryId(categoryId)
            .setVendorId(vendorId)
            .addParameterValues(makeVendorCodeParam(vendorCode1))
            .addParameterValues(makeVendorCodeParam(vendorCode2)).build();
        ModelStorage.Model cluster = createTestModel().toBuilder()
            .setCurrentType("CLUSTER")
            .setCategoryId(categoryId)
            .setVendorId(vendorId).addParameterValues(makeVendorCodeParam(vendorCode3)).build();
        ModelStorage.Model guru = createTestModel().toBuilder()
            .setCurrentType("GURU")
            .setCategoryId(categoryId)
            .setVendorId(vendorId).addParameterValues(makeVendorCodeParam(vendorCode3)).build();
        List<ModelStorage.Model> models = asList(deletedModel, sku, cluster, guru);
        createModels(models);

        List<Model> acceptedModels = models.stream().filter(model -> !model.hasDeleted() || !model.getDeleted())
            .filter(
                (model) -> UniqueVendorCodeValidator.SUPPORTED_MODEL_TYPES.stream()
                    .map(CommonModel.Source::name).anyMatch((type) -> model.getCurrentType().equals(type)))
            .collect(Collectors.toList());
        List<String> vendorCodes = acceptedModels.stream().flatMap(model ->
            model.getParameterValuesList().stream()
                .filter(param -> param.getXslName().equals(XslNames.VENDOR_CODE))
                .map(ModelStorage.ParameterValue::getStrValueList)
                .flatMap(Collection::stream)
                .map(ModelStorage.LocalizedString::getValue)
        ).collect(Collectors.toList());

        YtModelIndexReaders ytModelIndexReaders = testIndexesHolder.ytModelIndexReaders();
        List<YtModelIndexByVendorCode> expectedSearchResult = convertToVendorCodeModelId(acceptedModels);
        awaitForConditionOrThrow(() -> ytModelIndexReaders.getModelIdsByVendorCodes(vendorCodes, vendorId, categoryId,
            new ReadStats()).size() >= expectedSearchResult.size()
        );

        List<YtModelIndexByVendorCode> inModelIndex = ytModelIndexReaders
            .getModelIdsByVendorCodes(vendorCodes, vendorId, categoryId, new ReadStats());
        assertThat(inModelIndex)
            .containsExactlyInAnyOrderElementsOf(expectedSearchResult);
        assertThat(ytModelIndexReaders.getModelIdsByVendorCodes(emptyList(), vendorId,
            categoryId, new ReadStats())).isEmpty();
        assertThat(ytModelIndexReaders.getModelIdsByVendorCodes(
            singletonList(notUsedVendorCode), vendorId, categoryId, new ReadStats())).isEmpty();
    }

    @Test
    @Ignore("Modification was deleted")
    public void testAliasModelIndexReader() throws ModelStoreException {
        long vendorId = 123987L;
        long categoryId = 1090L;
        String alias1 = "some alias 1";
        String alias2 = "еще один алиас 2";
        String alias3 = "третий 3";
        String notUsedAlias = "алиас не используется";
        ModelStorage.Model deletedModel = createTestModel().toBuilder().setDeleted(true).setVendorId(vendorId)
            .setCurrentType("GURU")
            .addParameterValues(makeAliasParam(alias1))
            .setCategoryId(categoryId)
            .build();
        ModelStorage.Model guru1 = createTestModel().toBuilder()
            .setCurrentType("GURU")
            .setCategoryId(categoryId)
            .setVendorId(vendorId)
            .addParameterValues(makeAliasParam(alias1))
            .addParameterValues(makeAliasParam(alias2))
            .build();
        ModelStorage.Model sku = createTestModel().toBuilder()
            .setCurrentType("SKU")
            .setCategoryId(categoryId)
            .setVendorId(vendorId)
            .addParameterValues(makeAliasParam(alias3))
            .build();
        ModelStorage.Model guru2 = createTestModel().toBuilder()
            .setCurrentType("GURU")
            .setCategoryId(categoryId)
            .setVendorId(vendorId)
            .addParameterValues(makeAliasParam(alias3))
            .build();
        ModelStorage.Model parentNullModel = createTestModel().toBuilder()
            .setCurrentType("GURU")
            .setCategoryId(categoryId)
            .setVendorId(vendorId)
            .addParameterValues(makeAliasParam(alias2))
            .addParameterValues(makeAliasParam(alias3))
            .build();
        List<ModelStorage.Model> models = asList(deletedModel, guru1, sku, guru2, parentNullModel);
        createModels(models);

        YtModelIndexReaders ytModelIndexReaders = testIndexesHolder.ytModelIndexReaders();
        awaitForConditionOrThrow(() -> ytModelIndexReaders.getModelIdsByAliases(
            singletonList(alias1), singleton(categoryId), singleton(vendorId), emptySet(),
            new ReadStats()).size() > 0
        );

        List<YtModelIndexByAlias> inModelIndex = ytModelIndexReaders
            .getModelIdsByAliases(asList(alias1, alias2, alias3),
                singleton(categoryId), singleton(vendorId), emptySet(), new ReadStats());
        List<YtModelIndexByAlias> inModelIndexWithParentNull = ytModelIndexReaders
            .getModelIdsByAliases(asList(alias2, alias3),
                singleton(categoryId), singleton(vendorId), emptySet(), new ReadStats());
        // первый запрос должен найти модели guru1, guru2 и не найти parentNullModel
        assertThat(inModelIndex).containsExactlyInAnyOrderElementsOf(convertToAliasModelId(asList(guru1, guru2)));
        // и проверяем, что модель parentNullModel можно найти по условию parent_id = null :
        assertThat(inModelIndexWithParentNull).containsExactlyInAnyOrderElementsOf(
            convertToAliasModelId(singletonList(parentNullModel))
        );
        assertThat(ytModelIndexReaders.getModelIdsByAliases(emptyList(), singleton(categoryId), singleton(vendorId),
            emptySet(), new ReadStats())).isEmpty();
        assertThat(ytModelIndexReaders.getModelIdsByAliases(singletonList(notUsedAlias), singleton(categoryId),
            singleton(vendorId), emptySet(), new ReadStats())).isEmpty();
    }

    private List<YtModelIndexByVendorCode> convertToVendorCodeModelId(List<ModelStorage.Model> models) {
        return models.stream().flatMap(model -> YtModelUtil.modelToVendorCodeIndexMap(model).stream())
            .map(indexMap -> new YtModelIndexByVendorCode(
                indexMap.get(YtModelColumns.VENDOR_CODE).toString(),
                (Long) indexMap.get(YtModelColumns.VENDOR_ID),
                (Long) indexMap.get(YtModelColumns.CATEGORY_ID),
                (Long) indexMap.get(YtModelColumns.MODEL_ID)
            ))
            .collect(Collectors.toList());
    }

    private List<YtModelIndexByAlias> convertToAliasModelId(List<ModelStorage.Model> models) {
        return models.stream().flatMap(model -> YtModelUtil.modelToAliasIndexMap(model).stream())
            .map(indexMap -> new YtModelIndexByAlias(
                (Long) indexMap.get(YtModelColumns.CATEGORY_ID),
                (Long) indexMap.get(YtModelColumns.VENDOR_ID),
                indexMap.get(YtModelColumns.ALIAS_TOKEN).toString(),
                (Long) indexMap.get(YtModelColumns.MODEL_ID)
            ))
            .collect(Collectors.toList());
    }

    private List<YtModelIndexByGroupIdPayload> convertToGroupModelId(List<ModelStorage.Model> models) {
        return models.stream().map(model ->
            new YtModelIndexByGroupIdPayload(model.getId(), model.getCategoryId(), model.getDeleted(),
                model.getGroupModelId(), model.hasParentId() ? model.getParentId() : null,
                CommonModel.Source.valueOf(model.getCurrentType())))
            .collect(Collectors.toList());
    }

    private List<YtModelIndexByCategoryVendorIdPayload> convertToCategoryVendorId(List<ModelStorage.Model> models) {
        return models.stream().map(model ->
            new YtModelIndexByCategoryVendorIdPayload(model.getCategoryId(), model.getVendorId(),
                CommonModel.Source.valueOf(model.getCurrentType()),
                model.getId(), model.getDeleted(), model.hasParentId() ? model.getParentId() : null))
            .collect(Collectors.toList());
    }

    private List<YtModelCatalogIndexPayload> convertToYtModelCatalogIndexPayload(List<ModelStorage.Model> models) {
        return models.stream()
            .map(model -> new YtModelCatalogIndexPayload(model.getId(), model.getCategoryId()))
            .collect(Collectors.toList());
    }

    private List<YtModelIndexByIdPayload> convertToIdIndex(List<ModelStorage.Model> models) {
        return models.stream().map(model -> {
            Boolean isSku = model.getParameterValuesList().stream()
                .filter(parameterValue -> parameterValue.getXslName().equals(XslNames.IS_SKU))
                .map(ModelStorage.ParameterValue::getBoolValue)
                .findFirst().orElse(null);
            Boolean published = model.hasPublished() ? model.getPublished() : null;
            return new YtModelIndexByIdPayload(model.getId(), model.getCategoryId(), model.getDeleted(),
                model.getGroupModelId(),
                model.hasParentId() ? model.getParentId() : null,
                CommonModel.Source.valueOf(model.getCurrentType()),
                isSku, published);
        }).collect(Collectors.toList());
    }


    private List<ModelStorage.Model> prepareSomeModels() throws ModelStoreException {
        ModelStorage.Model guruModelIsSku = createTestModel();
        ModelStorage.Model deletedModel = createTestModel().toBuilder().setDeleted(true).build();

        ModelStorage.Model guruModel = createTestModel();
        ModelStorage.ParameterValue isSkuParam = guruModel.getParameterValuesList().stream()
            .filter(parameterValue ->
                parameterValue.getXslName().equals(XslNames.IS_SKU)).findFirst().orElse(null);
        guruModel = guruModel.toBuilder()
            .removeParameterValues(
                guruModel.toBuilder().getParameterValuesList().indexOf(isSkuParam))
            .build();

        ModelStorage.Model sku = createTestModel().toBuilder()
            .setCurrentType("SKU")
            .setGroupModelId(guruModel.getId())
            .setParentId(guruModel.getId())
            .addRelations(ModelStorage.Relation.newBuilder()
                .setCategoryId(guruModel.getCategoryId())
                .setId(guruModel.getId())
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL))
            .build();
        isSkuParam = sku.getParameterValuesList().stream()
            .filter(parameterValue ->
                parameterValue.getXslName().equals(XslNames.IS_SKU)).findFirst().orElse(null);
        sku = sku.toBuilder()
            .removeParameterValues(
                sku.toBuilder().getParameterValuesList().indexOf(isSkuParam))
            .build();

        guruModel = guruModel.toBuilder().addRelations(ModelStorage.Relation.newBuilder()
            .setCategoryId(guruModel.getCategoryId())
            .setId(sku.getId())
            .setType(ModelStorage.RelationType.SKU_MODEL)).build();

        List<Model> models = asList(guruModelIsSku, deletedModel, guruModel, sku);
        createModels(models);
        return models;
    }

    @Test
    public void testBarcodeIndexingWithDifferentTypes() {
        long hid = 1L;
        List<ModelStorage.ModelOrBuilder> models = asList(
            Model.newBuilder().setCategoryId(hid)
                .setId(MODEL_ID_1).setCurrentType("CLUSTER")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1)).build(),
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setCurrentType("SKU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2)).build(),
            Model.newBuilder().setId(MODEL_ID_3)
                .setCategoryId(hid)
                .setCurrentType("CLUSTER")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .build(),
            Model.newBuilder().setId(MODEL_ID_4)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_4)).build()
        );
        Map<Long, ModelStorage.ModelOrBuilder> beforeModelMap = new HashMap<>();

        beforeModelMap.put(MODEL_ID_1,
            Model.newBuilder().setId(MODEL_ID_1)
                .setCategoryId(hid)
                .setCurrentType("CLUSTER")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .build()
        );
        beforeModelMap.put(MODEL_ID_2,
            Model.newBuilder().setId(MODEL_ID_2)
                .setCategoryId(hid)
                .setCurrentType("CLUSTER")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_3))
                .build()
        );
        beforeModelMap.put(MODEL_ID_3,
            Model.newBuilder().setId(MODEL_ID_3)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1))
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_2))
                .build()
        );
        beforeModelMap.put(MODEL_ID_4,
            Model.newBuilder().setId(MODEL_ID_4)
                .setCategoryId(hid)
                .setCurrentType("GURU")
                .addParameterValues(makeBarcodeParam(MODEL_BARCODE_1))
                .build()
        );

        ModifyRowsRequest modifyRowsRequest = testIndexesHolder.ytModelIndexByBarcodeWriter()
            .requestIndex(models, beforeModelMap);

        ExpectedIndexChangesBuilder expectedBuilder = new ExpectedIndexChangesBuilder(modifyRowsRequest);

        // у первой модели старая и новая версия неподдерживаемого типа, должна быть заигнорирована.
        // у второй модели новая версия поддерживаемого типа, должны произойти update:
        expectedBuilder.addExpectedBarcodeChanges(ExpectedIndexChangesBuilder.RowType.UPDATE, MODEL_ID_2, hid,
            MODEL_BARCODE_2);
        // у третьей модели должны удалиться старые баркоды, так как она стала неподдерживаемой
        expectedBuilder.addExpectedBarcodeChanges(ExpectedIndexChangesBuilder.RowType.DELETE, MODEL_ID_3, hid,
            MODEL_BARCODE_1, MODEL_BARCODE_2);
        // у четвертой должны удалиться старые баркоды и добавиться новые (так как обе версии поддерживаемые)
        expectedBuilder.addExpectedBarcodeChanges(ExpectedIndexChangesBuilder.RowType.DELETE, MODEL_ID_4, hid,
            MODEL_BARCODE_1);
        expectedBuilder.addExpectedBarcodeChanges(ExpectedIndexChangesBuilder.RowType.UPDATE, MODEL_ID_4, hid,
            MODEL_BARCODE_4);

        assertThat(modifyRowsRequest.getRows())
            .containsExactlyInAnyOrderElementsOf(expectedBuilder.build().getRows());
    }

    @Test
    public void testBarcodeIndexReadSimple() throws ModelStoreInterface.ModelStoreException, InterruptedException {
        ModelStorage.Model proto = createTestModel();
        final String someBarcode = randomBarcode();
        final String someBarcode2 = randomBarcode();
        ModelStorage.Model model = Model.newBuilder(proto)
            .addParameterValues(makeBarcodeParam(someBarcode)).build();
        createModels(singletonList(model));

        awaitForConditionOrThrow(() -> testIndexesHolder.ytModelIndexReaders()
            .getModelIdByBarcode(someBarcode, new ReadStats()).isPresent());

        Optional<YtModelIndexByBarcode> modelIdOpt = testIndexesHolder.ytModelIndexReaders()
            .getModelIdByBarcode(someBarcode, new ReadStats());
        assertThat(modelIdOpt.isPresent()).isTrue();
        assertThat(modelIdOpt.get().getModelId()).isEqualTo(model.getId());
        assertThat(modelIdOpt.get().getCategoryId()).isEqualTo(model.getCategoryId());
        Map<String, YtModelIndexByBarcode> modelIdMap = testIndexesHolder.ytModelIndexReaders()
            .getModelIdsByBarcodes(asList(someBarcode, someBarcode2), new ReadStats());
        assertThat(modelIdMap).containsEntry(someBarcode,
            new YtModelIndexByBarcode(someBarcode, model.getId(), model.getCategoryId()));
    }

    @Test
    public void testBarcodeIndexReadUnexistingAndEmpty() {
        final String someBarcode = randomBarcode();
        Optional<YtModelIndexByBarcode> modelIdOptUnexisting = testIndexesHolder.ytModelIndexReaders()
            .getModelIdByBarcode(someBarcode, new ReadStats());
        assertThat(modelIdOptUnexisting.isPresent()).isFalse();
        Map<String, YtModelIndexByBarcode> modelIds = testIndexesHolder.ytModelIndexReaders()
            .getModelIdsByBarcodes(emptyList(), new ReadStats());
        assertThat(modelIds).isEmpty();
    }

    @Test
    public void testBarcodeIndexReadTwo() throws ModelStoreInterface.ModelStoreException, InterruptedException {
        final String someBarcode1 = randomBarcode();
        final String someBarcode2 = randomBarcode();
        final String someBarcode3 = randomBarcode();
        ModelStorage.Model proto1 = createTestModel();
        ModelStorage.Model model1 = Model.newBuilder(proto1)
            .addParameterValues(makeBarcodeParam(someBarcode1)).build();
        ModelStorage.Model proto2 = createTestModel();
        ModelStorage.Model model2 = Model.newBuilder(proto2)
            .addParameterValues(makeBarcodeParam(someBarcode2))
            .addParameterValues(makeBarcodeParam(someBarcode3)).build();
        createModels(asList(model1, model2));

        awaitForConditionOrThrow(() -> testIndexesHolder.ytModelIndexReaders()
            .getModelIdsByBarcodes(asList(someBarcode1, someBarcode2, someBarcode3), new ReadStats())
            .size() == 3);

        Map<String, YtModelIndexByBarcode> modelIdsMap = testIndexesHolder.ytModelIndexReaders()
            .getModelIdsByBarcodes(asList(someBarcode1, someBarcode2, someBarcode3), new ReadStats());
        Map<String, YtModelIndexByBarcode> expected = ImmutableMap.of(
            someBarcode1, new YtModelIndexByBarcode(someBarcode1, model1.getId(), model1.getCategoryId()),
            someBarcode2, new YtModelIndexByBarcode(someBarcode2, model2.getId(), model2.getCategoryId()),
            someBarcode3, new YtModelIndexByBarcode(someBarcode3, model2.getId(), model2.getCategoryId())
        );
        assertThat(modelIdsMap).containsAllEntriesOf(expected);
        assertThat(expected).containsAllEntriesOf(modelIdsMap);
    }

    private void awaitForConditionOrThrow(Callable<Boolean> callable) {
        Awaitility
            .await()
            .pollInterval(100L, TimeUnit.MILLISECONDS)
            .atMost(30, TimeUnit.SECONDS)
            .until(callable);
    }

    @Override
    protected void createModels(List<ModelStorage.Model> models) throws ModelStoreException {
        ModelStoreSaveGroup modelStoreSaveGroup = new ModelStoreSaveGroup(
            models,
            models.stream().map(Model::getId).collect(Collectors.toList()),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        ModelStoreResult storeResult = ytModelStore.saveModels(modelStoreSaveGroup, TEST_MODEL_MODIFIED_TS,
            new OperationStats());
        assertThat(storeResult.getModelStoreStatus().getStatus())
            .isEqualTo(ModelStorage.OperationStatusType.OK);
        assertThat(storeResult.getUpdatedModelIds()).isEmpty();
        assertThat(storeResult.getIgnoredModelIds()).isEmpty();
    }

    private void updateModels(ModelStorage.Model... models) {
        ModelStoreResult storeResult = updateModels(ms -> ModelStoreCallbackResult.success(), models);
        assertThat(storeResult.getModelStoreStatus().getStatus())
            .isEqualTo(ModelStorage.OperationStatusType.OK);
        assertThat(storeResult.getUpdatedModelIds())
            .containsExactlyInAnyOrderElementsOf(Arrays.stream(models).map(Model::getId).collect(Collectors.toList()));
        assertThat(storeResult.getIgnoredModelIds()).isEmpty();
    }

    private ModelStoreResult updateModels(
        Function<List<Model.Builder>, ModelStoreCallbackResult> beforeModelsSaveCallback,
        ModelStorage.Model... models) {

        ModelStoreSaveGroup modelStoreSaveGroup = new ModelStoreSaveGroup(
            asList(models),
            emptyList(),
            Collections.emptyMap(),
            Arrays.stream(models).collect(
                Collectors.groupingBy(Model::getCategoryId,
                    Collectors.mapping(Model::getId, Collectors.toSet())))
        );
        return ytModelStore.saveModels(modelStoreSaveGroup, beforeModelsSaveCallback,
            TEST_MODEL_MODIFIED_TS, new OperationStats());
    }

    private void updateModelsCategory(long newCategory, List<ModelStorage.Model> models) throws ModelStoreException {
        Map<Long, Set<Long>> beforeModels = models.stream().collect(
            Collectors.groupingBy(Model::getCategoryId,
                Collectors.mapping(Model::getId, Collectors.toSet())));

        List<ModelStorage.Model> modelsWithNewCategory = models.stream()
            .map(model -> model.toBuilder()
                .setCategoryId(newCategory)
                .build())
            .collect(Collectors.toList());
        ModelStoreSaveGroup modelStoreSaveGroup = new ModelStoreSaveGroup(
            modelsWithNewCategory,
            emptyList(),
            Collections.emptyMap(),
            beforeModels
        );
        ModelStoreResult storeResult = ytModelStore.saveModels(modelStoreSaveGroup, TEST_MODEL_MODIFIED_TS,
            new OperationStats());
        assertThat(storeResult.getModelStoreStatus().getStatus())
            .isEqualTo(ModelStorage.OperationStatusType.OK);
        assertThat(storeResult.getUpdatedModelIds())
            .containsExactlyInAnyOrderElementsOf(models.stream().map(Model::getId).collect(Collectors.toList()));
        assertThat(storeResult.getIgnoredModelIds()).isEmpty();
    }


    private YtTableHttpApi.NodeWrapper getModelIndex(Model model) {
        // modelIndexByIdSchema
        ListF<YTreeMapNode> queryKeys = new ArrayListF<>();
        queryKeys.add(YTree.mapBuilder()
            .key(YtModelColumns.MODEL_ID).value(model.getId())
            .buildMap());

        return readWithYtApi(queryKeys, testIndexesHolder.modelIndexByIdTablePath());
    }

    private YtTableHttpApi.NodeWrapper getModelGroupIndex(Model model) {
        // modelIndexByGroupIdSchema
        ListF<YTreeMapNode> queryKeys = new ArrayListF<>();
        queryKeys.add(YTree.mapBuilder()
            .key(YtModelColumns.GROUP_MODEL_ID).value(model.getGroupModelId())
            .key(YtModelColumns.MODEL_ID).value(model.getId())
            .buildMap());

        return readWithYtApi(queryKeys, testIndexesHolder.modelIndexByGroupIdTablePath());
    }

    private YtTableHttpApi.NodeWrapper getModelCategoryVendorIndex(Model model) {
        // modelIndexByCategoryVendorIdSchema
        ListF<YTreeMapNode> queryKeys = new ArrayListF<>();
        queryKeys.add(YTree.mapBuilder()
            .key(YtModelColumns.CATEGORY_ID).value(model.getCategoryId())
            .key(YtModelColumns.VENDOR_ID).value(model.getVendorId())
            .key(YtModelColumns.CURRENT_TYPE).value(CurrentTypeUtil.convertToYtColumnValue(model))
            .key(YtModelColumns.MODEL_ID).value(model.getId())
            .buildMap());

        return readWithYtApi(queryKeys, testIndexesHolder.modelIndexByCategoryVendorIdTablePath());
    }

    private YtTableHttpApi.NodeWrapper readWithYtApi(ListF<YTreeMapNode> queryKeys, YPath path) {
        YTreeMapNode[] result = new YTreeMapNode[1];
        yt.tables().lookupRows(path,
            YTableEntryTypes.YSON,
            queryKeys,
            YTableEntryTypes.YSON,
            row -> {
                result[0] = row;
            });
        if (result[0] == null) {
            return null;
        }
        return new YtTableHttpApi.NodeWrapper(result[0]);
    }

    private ModelStorage.ParameterValue makeBarcodeParam(String barcode) {
        return makeParam(XslNames.BAR_CODE, barcode);
    }

    private ModelStorage.ParameterValue makeVendorCodeParam(String vendorCode) {
        return makeParam(XslNames.VENDOR_CODE, vendorCode);
    }

    private ModelStorage.ParameterValue makeAliasParam(String alias) {
        return makeParam(XslNames.ALIASES, alias);
    }

    private ModelStorage.ParameterValue makeParam(String name, String value) {
        ModelStorage.LocalizedString paramStr = ModelStorage.LocalizedString.newBuilder()
            .setIsoCode(ISO_CODE_1)
            .setValue(value)
            .build();
        return ModelStorage.ParameterValue.newBuilder().setXslName(name).addStrValue(paramStr).build();
    }

    private String randomBarcode() {
        return String.valueOf(Math.abs(RANDOM.nextInt()));
    }
}
