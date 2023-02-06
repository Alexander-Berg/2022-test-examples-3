package ru.yandex.market.mbo.db.modelstorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author moskovkin@yandex-team.ru
 * @since 21.09.17
 */
@Ignore
@SuppressWarnings("checkstyle:magicnumber")
public abstract class AbstractModelStoreTest {

    public static final long MODIFIED_TS = 1460970840000L;
    public static final long TEST_MODEL_MODIFIED_TS = 1460970840100L;

    private AtomicLong modelIdIncrement = new AtomicLong(1);

    public abstract ModelStoreInterface getModelStore();

    @Test
    public void testCheckAndUpdateModelSingleModification()
        throws ModelStoreInterface.ModelStoreException, InterruptedException {

        ModelStorage.Model oldModel = createTestModel();
        createModels(Collections.singletonList(oldModel));

        ModelStorage.Model model = ModelStorage.Model.newBuilder().mergeFrom(oldModel)
            .setModifiedTs(MODIFIED_TS + 200)
            .build();

        long current = System.currentTimeMillis();
        boolean status = getModelStore().checkAndUpdate(model, current);

        Assert.assertTrue(status);

        ModelStorage.Model modelFetched = getModelStore().getModelById(model.getCategoryId(), model.getId());
        Assert.assertEquals(current, modelFetched.getModifiedTs());

        modelFetched = ModelStorage.Model.newBuilder().mergeFrom(modelFetched)
            .setModifiedTs(model.getModifiedTs())
            .build();

        Assert.assertArrayEquals(model.toByteArray(), modelFetched.toByteArray());
    }

    @Test
    public void testCheckAndUpdateNotOverlappingParallelModification() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model oldModel = createTestModel();
        createModels(Collections.singletonList(oldModel));

        ModelStorage.Model model = ModelStorage.Model.newBuilder().mergeFrom(oldModel)
            .setModifiedTs(MODIFIED_TS)
            .build();

        boolean status = getModelStore().checkAndUpdate(model, new Date().getTime());
        Assert.assertFalse(status);
    }

    @Test
    public void testCheckAndUpdateModelWithoutChanges() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model oldModel = createTestModel();
        createModels(Collections.singletonList(oldModel));

        ModelStorage.Model model = ModelStorage.Model.newBuilder().mergeFrom(createTestModel()).build();
        boolean status = getModelStore().checkAndUpdate(Collections.singletonMap(model.getId(), oldModel),
            Collections.singletonList(model), new Date().getTime()).get(model.getId());
        Assert.assertTrue(status);

        // make sure that the timestamp did not change
        ModelStorage.Model savedModel = getModelStore().getModelById(90796, oldModel.getId());
        Assert.assertEquals(1460970840100L, savedModel.getModifiedTs());
    }

    @Test
    public void testCheckAndUpdateModelWithChanges() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model oldModel = createTestModel();
        createModels(Collections.singletonList(oldModel));

        ModelStorage.Model newModel = ModelStorage.Model.newBuilder().mergeFrom(oldModel)
            .setSourceType("SKU")
            .setVendorId(123456L)
            .build();

        boolean status = getModelStore().checkAndUpdate(Collections.singletonMap(newModel.getId(), oldModel),
            Collections.singletonList(newModel), new Date().getTime()).get(newModel.getId());
        Assert.assertTrue(status);

        // make sure that the timestamp has changed
        ModelStorage.Model savedModel = getModelStore().getModelById(90796, oldModel.getId());
        Assert.assertNotEquals(1460970840100L, savedModel.getModifiedTs());
    }

    @Test
    public void testCheckAndUpdateModel2ModifiedCase() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model testModel = createTestModel();
        ModelStorage.Model model = ModelStorage.Model.newBuilder().mergeFrom(testModel)
            .setModifiedTs(MODIFIED_TS)
            .build();

        boolean status = getModelStore().checkAndUpdate(Collections.singletonMap(model.getId(), testModel),
            Collections.singletonList(model), new Date().getTime()).get(model.getId());
        Assert.assertFalse(status);
    }

    @Test
    public void testCheckAndUpdateModel2SingleModificationCase()
        throws ModelStoreInterface.ModelStoreException, InterruptedException {

        ModelStorage.Model oldModel = createTestModel();
        ModelStorage.Model model = ModelStorage.Model.newBuilder().mergeFrom(oldModel)
            .setModifiedTs(MODIFIED_TS + 200)
            .build();

        long current = System.currentTimeMillis();
        createModels(Collections.singletonList(oldModel));
        boolean status = getModelStore().checkAndUpdate(Collections.singletonMap(model.getId(), oldModel),
            Collections.singletonList(model), new Date().getTime()).get(model.getId());

        Assert.assertTrue(status);

        ModelStorage.Model modelFetched = getModelStore().getModelById(model.getCategoryId(), model.getId());
        Assert.assertTrue(modelFetched.getModifiedTs() >= current);

        modelFetched = ModelStorage.Model.newBuilder().mergeFrom(modelFetched)
            .setModifiedTs(model.getModifiedTs())
            .build();

        Assert.assertArrayEquals(model.toByteArray(), modelFetched.toByteArray());
    }

    @Test
    /*
     * Данный тест выполняется слишком долго, поэтому в хранилище с
     * большим количеством моделей решили его не выполнять
     */
    @Ignore
    public void testProcessAllGuruModels() throws ModelStoreInterface.ModelStoreException {
        List<Long> affectedIds = new ArrayList<>();

        ModelStorage.Model guruModel = ModelStorage.Model.newBuilder().mergeFrom(createTestModel()).build();
        affectedIds.add(guruModel.getId());
        List<Long> ids = getModelStore().saveClusters(Collections.singletonList(guruModel));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model nonGuruModel = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setSourceType("CLUSTER")
            .setCurrentType("CLUSTER")
            .build();
        affectedIds.add(nonGuruModel.getId());
        ids = getModelStore().saveClusters(Collections.singletonList(nonGuruModel));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model dGuruModel = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setDeleted(true)
            .setDeletedDate(MODIFIED_TS)
            .build();
        affectedIds.add(dGuruModel.getId());
        ids = getModelStore().saveClusters(Collections.singletonList(dGuruModel));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model dNonGuruModel = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setSourceType("CLUSTER")
            .setCurrentType("CLUSTER")
            .setDeleted(true)
            .setDeletedDate(MODIFIED_TS)
            .build();
        affectedIds.add(dNonGuruModel.getId());

        ids = getModelStore().saveClusters(Collections.singletonList(dNonGuruModel));
        Assert.assertTrue(ids.isEmpty());

        List<ModelStorage.Model> models = new ArrayList<>();
        getModelStore().processCategoryModels(90796L, CommonModel.Source.GURU, null, null, model -> {
            if (affectedIds.contains(model.getId())) {
                models.add(model);
            }
        });

        long count = models.stream()
            .filter(model -> model.getId() == guruModel.getId())
            .count();
        Assert.assertEquals(1, count);

        count = models.stream()
            .filter(model -> model.getId() == nonGuruModel.getId())
            .count();
        Assert.assertEquals(0, count);

        count = models.stream()
            .filter(model -> model.getId() == dNonGuruModel.getId())
            .count();
        Assert.assertEquals(0, count);

        count = models.stream()
            .filter(model -> model.getId() == dGuruModel.getId())
            .count();
        Assert.assertEquals(0, count);
    }

    @Test
    @Ignore
    public void testProcessCategoryModels() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setCategoryId(1).build();
        List<Long> ids = getModelStore().saveClusters(Collections.singletonList(model1));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model model2 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setCategoryId(1).build();
        ids = getModelStore().saveClusters(Collections.singletonList(model2));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model model3 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setCategoryId(2).build();
        ids = getModelStore().saveClusters(Collections.singletonList(model3));
        Assert.assertTrue(ids.isEmpty());

        List<ModelStorage.Model> models = new ArrayList<>();
        getModelStore().processCategoryModels(1, CommonModel.Source.GURU, null, null, models::add);

        Assert.assertEquals(2, models.size());

        models.sort(Comparator.comparing(ModelStorage.Model::getId));
        Assert.assertArrayEquals(model1.toByteArray(), models.get(0).toByteArray());
        Assert.assertArrayEquals(model2.toByteArray(), models.get(1).toByteArray());
    }

    @Test
    @Ignore
    public void testProcessCategoryModelsWithLimit() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setCategoryId(1).build();
        List<Long> ids = getModelStore().saveClusters(Collections.singletonList(model1));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model model2 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setCategoryId(1).build();
        ids = getModelStore().saveClusters(Collections.singletonList(model2));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model model3 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setCategoryId(1).build();
        ids = getModelStore().saveClusters(Collections.singletonList(model3));
        Assert.assertTrue(ids.isEmpty());

        ModelStorage.Model model4 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
            .setCategoryId(2).build();
        ids = getModelStore().saveClusters(Collections.singletonList(model4));
        Assert.assertTrue(ids.isEmpty());

        List<ModelStorage.Model> models = new ArrayList<>();
        getModelStore().processCategoryModels(1, CommonModel.Source.GURU, null, 2, models::add);

        Assert.assertEquals(2, models.size());

        models.sort(Comparator.comparing(ModelStorage.Model::getId));
        Assert.assertArrayEquals(model1.toByteArray(), models.get(0).toByteArray());
        Assert.assertEquals(model2.getId(), models.get(1).getId());
        Assert.assertArrayEquals(model2.toByteArray(), models.get(1).toByteArray());
    }

    @Test
    public void testCheckCategoryExists() throws ModelStoreInterface.ModelStoreException {
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder().mergeFrom(createTestModel()).setCategoryId(1)
            .build();
        createModels(Collections.singletonList(model1));

        AtomicBoolean exists = new AtomicBoolean();
        getModelStore().processCategoryModels(1, CommonModel.Source.GURU, null, 1, m -> {
            exists.set(true);
        });
        Assert.assertTrue(exists.get());

        /*
        Проверка не укладывается в таймаут
        status = getModelStore().checkCategoryExist(2, CommonModel.Source.GURU);
        Assert.assertFalse(status);*/
    }

    @Test
    @Ignore
    public void testSaveModelsBatch() throws ModelStoreInterface.ModelStoreException {

        List<ModelStorage.Model> models = new ArrayList<>();

        // Создать 100 моделей с идентификаторами от 1 до 100
        for (int i = 1; i < 101; i++) {
            models.add(ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
                .setCategoryId(1).build());
        }

        // Создать 50 моделей без идентификаторов
        for (int i = 0; i < 50; i++) {
            models.add(ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
                .setId(0).setCategoryId(1).build());
        }

        // Перемешать все с фиксированным сидом для воспроизводимости результатов
        Collections.shuffle(models, new Random(1000));

        List<Long> ids = getModelStore().saveClusters(models);
        Assert.assertEquals(50, ids.size());

        Iterator<Long> it = ids.iterator();
        for (ModelStorage.Model model : models) {
            long id = model.getId();
            if (id == 0) {
                Assert.assertTrue(it.hasNext());
                id = it.next();
                model = ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
                    .setId(id).setCategoryId(1).build();
            }

            ModelStorage.Model actual = getModelStore().getModelById(model.getCategoryId(), model.getId());
            Assert.assertArrayEquals(model.toByteArray(), actual.toByteArray());
        }

        Assert.assertFalse(it.hasNext());
    }

    @Test
    @Ignore
    public void testGetModelsBatch() throws ModelStoreInterface.ModelStoreException {

        List<ModelStorage.Model> models = new ArrayList<>();

        // Создать 100 моделей с идентификаторами от 1 до 100
        for (int i = 1; i < 101; i++) {
            models.add(ModelStorage.Model.newBuilder().mergeFrom(createTestModel())
                .setCategoryId(1).build());
        }

        // Сохранить без батч-режима
        for (ModelStorage.Model model : models) {
            getModelStore().saveClusters(Collections.singletonList(model)); // без батч-режима
        }

        List<Long> ids = models.stream().map(ModelStorage.Model::getId).collect(Collectors.toList());
        List<ModelStorage.Model> actualModels = getModelStore().getModels(1, ids);
        Assert.assertEquals(100, actualModels.size());

        Iterator<ModelStorage.Model> it = actualModels.iterator();
        for (ModelStorage.Model model : models) {
            ModelStorage.Model actual = it.next();
            Assert.assertArrayEquals(model.toByteArray(), actual.toByteArray());
        }
    }

    public long nextModelId() {
        return modelIdIncrement.getAndIncrement();
    }

    public ModelStorage.Model createTestModel() {
        long id = nextModelId();
        return ModelStorage.Model.newBuilder()
            .setId(id)
            .setGroupModelId(id)
            .setCategoryId(90796)
            .setVendorId(1039835)
            .setSourceType("GURU")
            .setCurrentType("GURU")
            .setModifiedTs(TEST_MODEL_MODIFIED_TS)
            .setPublished(true)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode(Language.RUSSIAN.getIsoCode())
                .setValue("TEST 1 Camarelo Figaro (2 \320\262 1)"))
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode(Language.RUSSIAN.getIsoCode())
                .setValue("TEST 1 Camarelo Figaro (2 \320\262 1)"))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(4900122)
                .setTypeId(1)
                .setOptionId(12109963)
                .setXslName("WheelsNum")
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .setModificationDate(MODIFIED_TS)
                .setValueType(MboParameters.ValueType.ENUM))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(4900170)
                .setTypeId(0)
                .setBoolValue(true)
                .setOptionId(12990423)
                .setXslName("MosquitoGauze")
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .setModificationDate(MODIFIED_TS)
                .setValueType(MboParameters.ValueType.BOOLEAN))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(4900184)
                .setTypeId(2)
                .setNumericValue("60")
                .setXslName("CompactWidth")
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .setModificationDate(MODIFIED_TS)
                .setValueType(MboParameters.ValueType.NUMERIC))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(12786240)
                .setTypeId(4)
                .addStrValue(ModelStorage.LocalizedString.newBuilder()
                    .setIsoCode(Language.RUSSIAN.getIsoCode())
                    .setValue("http://www.camarelo.pl/img/figaro/fi07a1.jpg"))
                .setXslName("XLPictureUrl_9")
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .setModificationDate(MODIFIED_TS)
                .setValueType(MboParameters.ValueType.STRING))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(15354452)
                .setBoolValue(true)
                .setXslName(XslNames.IS_SKU)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .setModificationDate(MODIFIED_TS)
                .setValueType(MboParameters.ValueType.BOOLEAN))
            .build();
    }

    protected abstract void createModels(List<ModelStorage.Model> models)
        throws ModelStoreInterface.ModelStoreException;
}
