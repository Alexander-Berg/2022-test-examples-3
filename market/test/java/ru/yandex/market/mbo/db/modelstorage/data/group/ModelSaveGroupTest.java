package ru.yandex.market.mbo.db.modelstorage.data.group;

import javolution.testing.AssertionException;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.gwt.models.modelstorage.CategoryModelId;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelSaveGroupTest extends BaseModelSaveGroupTest {

    private static final long CATEGORY_ID = 900500L;

    @Test
    public void parseModelsOfDifferentOperations() throws Exception {
        List<CommonModel> models = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> before = Arrays.asList(updatedModel1, new CommonModel(deletedModel1).setDeleted(false));
        ModelSaveGroup group = ModelSaveGroup.fromModels(models);
        group.addBeforeModels(before);

        Assert.assertEquals(1, group.getModelsToCreate().size());
        Assert.assertEquals(1, group.getModelsToChange().size());
        Assert.assertEquals(1, group.getModelsToRemove().size());

        Assert.assertEquals(3, group.getModelsOfType(ModelChanges.Operation.values()).size());
        Assert.assertEquals(3, group.getModelsSize());
        Assert.assertEquals(3, group.getModels().size());
    }

    @Test
    public void parseAndAddModelsOfDifferentOperations() throws Exception {
        List<CommonModel> models = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);
        List<CommonModel> before = Arrays.asList(
            updatedModel1, new CommonModel(deletedModel1).setDeleted(false),
            updatedModel2, new CommonModel(deletedModel2).setDeleted(false));

        ModelSaveGroup group = ModelSaveGroupFactory.create(models, additionalModels);
        group.addBeforeModels(before);

        Assert.assertEquals(2, group.getModelsToCreate().size());
        Assert.assertEquals(2, group.getModelsToChange().size());
        Assert.assertEquals(2, group.getModelsToRemove().size());

        Assert.assertEquals(6, group.getModelsOfType(ModelChanges.Operation.values()).size());
        Assert.assertEquals(6, group.getModelsSize());
        Assert.assertEquals(6, group.getModels().size());
    }

    @Test
    public void testContainsModel() throws Exception {
        List<CommonModel> models = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);

        ModelSaveGroup group = ModelSaveGroupFactory.create(models, additionalModels);

        Assert.assertTrue(group.containsModel(updatedModel1.getId()));
        Assert.assertTrue(group.containsModel(updatedModel2.getId()));
        Assert.assertFalse(group.containsModel(100500));
    }

    @Test
    public void testGetByIds() throws Exception {
        List<CommonModel> models = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);

        ModelSaveGroup group = ModelSaveGroupFactory.create(models, additionalModels);

        List<CommonModel> modelsFromGroup = group.getByIds(Arrays.asList(
            updatedModel1.getId(), deletedModel1.getId(), updatedModel2.getId(), deletedModel2.getId()));
        List<CommonModel> expected = Arrays.asList(updatedModel1, deletedModel1, updatedModel2, deletedModel2);
        Assert.assertEquals(expected, modelsFromGroup);
    }

    @Test
    public void testSingleModelsInstance() throws Exception {
        CommonModel model1 = createAndSaveModel();
        CommonModel model2 = createAndSaveModel();
        model1.setId(100);
        model2.setId(103);

        ModelSaveGroup group = ModelSaveGroupFactory.create(Collections.singleton(model1),
            Collections.singleton(model2));

        Assert.assertSame(model1, group.getById(model1.getId()));
        Assert.assertSame(model2, group.getById(model2.getId()));
    }

    @Test
    public void testAddAll() throws Exception {
        List<CommonModel> requestedModels = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);
        CommonModel model1 = createModel();
        CommonModel model2 = createAndSaveModel();

        ModelSaveGroup group = ModelSaveGroupFactory.create(requestedModels, additionalModels);
        group.addStorageModels(Arrays.asList(model1, model2));
        group.setStatus(createdModel2, OperationStatusType.OK);
        group.setStatus(updatedModel2, OperationStatusType.OK);
        group.setStatus(deletedModel2, OperationStatusType.OK);
        group.setStatus(model1, OperationStatusType.OK);
        group.setStatus(model2, OperationStatusType.OK);

        Assert.assertEquals(8, group.getModelsSize());
        Assert.assertSame(model2, group.getById(model2.getId()));

        // check operation types didn't change
        List<OperationType> expectedRequestedOperations = Arrays.asList(OperationType.CREATE, OperationType.CHANGE,
            OperationType.REMOVE);
        List<OperationType> expectedAdditionalOperations = Arrays.asList(OperationType.CREATE, OperationType.CHANGE,
            OperationType.REMOVE, OperationType.CREATE, OperationType.CHANGE);

        Assert.assertEquals(expectedRequestedOperations, getRequestedOperationTypes(group));
        Assert.assertEquals(expectedAdditionalOperations, getAdditionalOperationTypes(group));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddAllShouldFailIfGroupAlreadyContainsModel() throws Exception {
        List<CommonModel> requestedModels = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);
        CommonModel model1 = createModel();
        CommonModel model2 = createAndSaveModel();
        model2.setId(updatedModel2.getId());

        ModelSaveGroup group = ModelSaveGroupFactory.create(requestedModels, additionalModels);
        group.addStorageModels(Arrays.asList(model1, model2));
    }

    @Test
    public void testAddAllIfAbsent() throws Exception {
        List<CommonModel> models = Arrays.asList(updatedModel1, deletedModel1);
        CommonModel model1 = createAndSaveModel();
        CommonModel model2 = createAndSaveModel();
        model2.setDeleted(true);
        model2.setId(updatedModel1.getId());

        ModelSaveGroup group = ModelSaveGroup.fromModels(models);
        group.addAllIfAbsent(Arrays.asList(model1, model2));
        group.setStatus(model1, OperationStatusType.OK);

        Assert.assertEquals(3, group.getModelsSize());
        Assert.assertSame(model1, group.getById(model1.getId()));
        Assert.assertSame(updatedModel1, group.getById(model2.getId()));
        Assert.assertSame(deletedModel1, group.getById(deletedModel1.getId()));

        // check operation types didn't change
        List<OperationType> expectedRequestedOperations = Arrays.asList(OperationType.CHANGE, OperationType.REMOVE);
        List<OperationType> expectedAdditionalOperations = Arrays.asList(OperationType.CHANGE);

        Assert.assertEquals(expectedRequestedOperations, getRequestedOperationTypes(group));
        Assert.assertEquals(expectedAdditionalOperations, getAdditionalOperationTypes(group));
    }

    @Test
    public void testAddAllWithOverride() throws Exception {
        List<CommonModel> models = Arrays.asList(updatedModel1, deletedModel1);
        CommonModel model1 = createAndSaveModel();
        CommonModel model2 = createAndSaveModel();
        model2.setDeleted(true);
        model2.setId(updatedModel1.getId());

        ModelSaveGroup group = ModelSaveGroup.fromModels(models);
        group.addAllWithOverride(Arrays.asList(model1, model2));
        group.setStatus(model1, OperationStatusType.OK);

        Assert.assertEquals(3, group.getModelsSize());
        Assert.assertSame(model1, group.getById(model1.getId()));
        Assert.assertSame(model2, group.getById(model2.getId()));
        Assert.assertSame(deletedModel1, group.getById(deletedModel1.getId()));

        // check operation types didn't change
        List<OperationType> expectedRequestedOperations = Arrays.asList(OperationType.CHANGE, OperationType.REMOVE);
        List<OperationType> expectedAdditionalOperations = Arrays.asList(OperationType.CHANGE);

        Assert.assertEquals(expectedRequestedOperations, getRequestedOperationTypes(group));
        Assert.assertEquals(expectedAdditionalOperations, getAdditionalOperationTypes(group));
    }

    @Test
    public void testAddExistingByReferenceModelsInGroupModelDoesntAddNothing() {
        ModelSaveGroup group = ModelSaveGroup.fromModels(createdModel1, createdModel2);

        group.addStorageModels(Arrays.asList(createdModel1, createdModel2));
        Assert.assertEquals(2, group.getModelsSize());

        group.addAllWithOverride(Arrays.asList(createdModel1, createdModel2));
        Assert.assertEquals(2, group.getModelsSize());

        group.addAllIfAbsent(Arrays.asList(createdModel1, createdModel2));
        Assert.assertEquals(2, group.getModelsSize());
    }

    @Test
    public void testBeforeModels() throws Exception {
        List<CommonModel> requestedModels = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);

        ModelSaveGroup group = ModelSaveGroupFactory.create(requestedModels, additionalModels);

        Assert.assertEquals(0, group.getBeforeModels().size());
        Assert.assertEquals(4, group.getModelsWithoutBefore().size());

        Map<Long, CommonModel> beforeModelsMap = getModels(createdModel1, updatedModel1, createdModel2, deletedModel2);

        group.addBeforeModels(beforeModelsMap.values());
        Assert.assertEquals(2, group.getModelsWithoutBefore().size());
        Assert.assertEquals(2, group.getBeforeModels().size());
        group.forEachPair((before, after) -> {
            if (after == createdModel1 || after == createdModel2) {
                Assert.assertNull("Created model should not contain have before model", before);
                return;
            }
            if (after == deletedModel1 || after == updatedModel2) {
                Assert.assertNull("Before model was not passed", before);
                return;
            }
            Assert.assertNotNull("Expected to find before model", before);
        });
    }

    @Test
    public void testGetBeforeById() throws Exception {
        List<CommonModel> requestedModels = Arrays.asList(createdModel1, updatedModel1, deletedModel1);
        List<CommonModel> additionalModels = Arrays.asList(createdModel2, updatedModel2, deletedModel2);
        Map<Long, CommonModel> beforeModelsMap = getModels(createdModel1, updatedModel1, createdModel2, deletedModel2);

        ModelSaveGroup group = ModelSaveGroupFactory.create(requestedModels, additionalModels);
        group.addBeforeModels(beforeModelsMap.values());

        for (Long id : beforeModelsMap.keySet()) {
            assertThat(group.getBeforeById(id))
                .isEqualTo(beforeModelsMap.get(id));
        }
    }


    @Test
    public void testChangeModelIdAndBeforeModelIdWontChange() throws Exception {
        List<CommonModel> models = Collections.singletonList(updatedModel1);
        CommonModel beforeModel = storageService.getModel(updatedModel1.getCategoryId(), updatedModel1.getId())
            .orElseThrow(() -> new AssertionException("Expected to have model id: " + updatedModel1.getId()));

        ModelSaveGroup group = ModelSaveGroup.fromModels(models);
        group.addBeforeModels(Collections.singleton(beforeModel));

        // check if instance is the same
        Assert.assertSame(updatedModel1, group.getModels().get(0));
        Assert.assertSame(beforeModel, group.getBeforeModels().get(0));

        // check equal ids
        Assert.assertEquals(updatedModel1.getId(), beforeModel.getId());

        // change id
        updatedModel1.setId(100500);

        // check equal ids
        Assert.assertNotEquals(updatedModel1.getId(), beforeModel.getId());
    }

    @Test
    public void testFailedAdditionalNotAddedToRequestedWhenOk() {
        // Проверяем, что если основные модели успешно сохранились, то additional-ошибки не попадут в requested.
        CommonModel okReqModel1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID).endModel();
        CommonModel okReqModel2 = CommonModelBuilder.newBuilder(2, CATEGORY_ID).endModel();
        CommonModel badAddModel1 = CommonModelBuilder.newBuilder(3, CATEGORY_ID).endModel();
        CommonModel badAddModel2 = CommonModelBuilder.newBuilder(4, CATEGORY_ID).endModel();

        ModelSaveGroup group = ModelSaveGroup.fromModels(okReqModel1, okReqModel2);
        group.addStorageModels(Arrays.asList(badAddModel1, badAddModel2));
        group.setStatus(okReqModel1, OperationStatusType.NO_OP);
        group.setStatus(okReqModel2, OperationStatusType.OK);
        group.setStatus(badAddModel1, OperationStatusType.INTERNAL_ERROR);
        group.setStatus(badAddModel2, OperationStatusType.VALIDATION_ERROR);

        GroupOperationStatus overallStatus = group.generateOverallStatus();
        Assert.assertEquals(2, overallStatus.getRequestedModelStatuses().size());
        Assert.assertEquals(2, overallStatus.getAdditionalModelStatues().size());
        assertThat(overallStatus.getRequestedModelStatuses()).containsExactlyInAnyOrder(
            operationStatus(okReqModel1, OperationStatusType.NO_OP),
            operationStatus(okReqModel2, OperationStatusType.OK)
        );
        assertThat(overallStatus.getAdditionalModelStatues()).containsExactlyInAnyOrder(
            operationStatus(badAddModel1, OperationStatusType.INTERNAL_ERROR),
            operationStatus(badAddModel2, OperationStatusType.VALIDATION_ERROR)
        );
    }

    @Test
    public void testMissingModelsTreatedGuilty() {
        // Проверяем, что если есть ненайденные модели, то они попадают в requested-группу ошибок и допы не трогаем.
        CommonModel badReqModel1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID).endModel();
        CommonModel badReqModel2 = CommonModelBuilder.newBuilder(2, CATEGORY_ID).endModel();
        CommonModel badAddModel1 = CommonModelBuilder.newBuilder(3, CATEGORY_ID).endModel();
        CommonModel badAddModel2 = CommonModelBuilder.newBuilder(4, CATEGORY_ID).endModel();

        ModelSaveGroup group = ModelSaveGroup.fromModels(badReqModel1, badReqModel2);
        group.addStorageModels(Arrays.asList(badAddModel1, badAddModel2));
        group.setStatus(badReqModel1, OperationStatusType.FAILED_MODEL_IN_GROUP);
        group.setStatus(badReqModel2, OperationStatusType.FAILED_MODEL_IN_GROUP);
        group.setStatus(badAddModel1, OperationStatusType.INTERNAL_ERROR);
        group.setStatus(badAddModel2, OperationStatusType.VALIDATION_ERROR);
        group.addMissingModel(5L, OperationStatusType.MODEL_NOT_FOUND.getDefaultStatusMessage());

        GroupOperationStatus overallStatus = group.generateOverallStatus();
        Assert.assertEquals(3, overallStatus.getRequestedModelStatuses().size());
        Assert.assertEquals(2, overallStatus.getAdditionalModelStatues().size());
        assertThat(overallStatus.getRequestedModelStatuses()).containsExactlyInAnyOrder(
            operationStatus(badReqModel1, OperationStatusType.FAILED_MODEL_IN_GROUP),
            operationStatus(badReqModel2, OperationStatusType.FAILED_MODEL_IN_GROUP),
            new OperationStatus(OperationStatusType.MODEL_NOT_FOUND, OperationType.CHANGE, 5L)
        );
        assertThat(overallStatus.getAdditionalModelStatues()).containsExactlyInAnyOrder(
            operationStatus(badAddModel1, OperationStatusType.INTERNAL_ERROR),
            operationStatus(badAddModel2, OperationStatusType.VALIDATION_ERROR)
        );
    }

    @Test
    public void testFailedAdditionalNotAddedToRequestedWhenRequestedGuilty() {
        // Проверяем, что если в запрошенных есть упавшие по своей вине, то additional-ошибки не попадут в requested.
        CommonModel okReqModel1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID).endModel();
        CommonModel badReqModel2 = CommonModelBuilder.newBuilder(2, CATEGORY_ID).endModel();
        CommonModel badAddModel1 = CommonModelBuilder.newBuilder(3, CATEGORY_ID).endModel();
        CommonModel badAddModel2 = CommonModelBuilder.newBuilder(4, CATEGORY_ID).endModel();

        ModelSaveGroup group = ModelSaveGroup.fromModels(okReqModel1, badReqModel2);
        group.addStorageModels(Arrays.asList(badAddModel1, badAddModel2));
        group.setStatus(okReqModel1, OperationStatusType.FAILED_MODEL_IN_GROUP);
        group.setStatus(badReqModel2, OperationStatusType.VALIDATION_ERROR);
        group.setStatus(badAddModel1, OperationStatusType.INTERNAL_ERROR);
        group.setStatus(badAddModel2, OperationStatusType.VALIDATION_ERROR);

        GroupOperationStatus overallStatus = group.generateOverallStatus();
        Assert.assertEquals(2, overallStatus.getRequestedModelStatuses().size());
        Assert.assertEquals(2, overallStatus.getAdditionalModelStatues().size());
        assertThat(overallStatus.getRequestedModelStatuses()).containsExactlyInAnyOrder(
            operationStatus(okReqModel1, OperationStatusType.FAILED_MODEL_IN_GROUP),
            operationStatus(badReqModel2, OperationStatusType.VALIDATION_ERROR)
        );
        assertThat(overallStatus.getAdditionalModelStatues()).containsExactlyInAnyOrder(
            operationStatus(badAddModel1, OperationStatusType.INTERNAL_ERROR),
            operationStatus(badAddModel2, OperationStatusType.VALIDATION_ERROR)
        );
    }

    @Test
    public void testGetByIdWillReturnModelsWithNegativeId() {
        // negative ids were introduced in MBO-17756

        CommonModel newGuruModel = CommonModelBuilder.newBuilder(-1, CATEGORY_ID)
            .withSkuRelations(CATEGORY_ID, -2)
            .endModel();
        CommonModel newSkuModel = CommonModelBuilder.newBuilder(-2, CATEGORY_ID)
            .withSkuParentRelation(newGuruModel)
            .endModel();
        CommonModel otherGuruModel1 = CommonModelBuilder.newBuilder(-3, CATEGORY_ID)
            .endModel();
        CommonModel otherGuruModel2 = CommonModelBuilder.newBuilder(-4, CATEGORY_ID)
            .endModel();
        CommonModel otherGuruModel3 = CommonModelBuilder.newBuilder(-5, CATEGORY_ID)
            .endModel();

        ModelSaveGroup group = ModelSaveGroup.fromModels(newGuruModel, newSkuModel);
        // add other guru models via add* methods
        group.addStorageModels(Collections.singleton(otherGuruModel1));
        group.addAllIfAbsent(Collections.singleton(otherGuruModel2));
        group.addAllWithOverride(Collections.singleton(otherGuruModel3));

        CommonModel actualGuruModel = group.getById(-1);
        CommonModel actualSkuModel = group.getById(-2);
        CommonModel actualGuruModel1 = group.getById(-3);
        CommonModel actualGuruModel2 = group.getById(-4);
        CommonModel actualGuruModel3 = group.getById(-5);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actualGuruModel).isEqualTo(newGuruModel);
            softly.assertThat(actualSkuModel).isEqualTo(newSkuModel);
            softly.assertThat(actualGuruModel1).isEqualTo(otherGuruModel1);
            softly.assertThat(actualGuruModel2).isEqualTo(otherGuruModel2);
            softly.assertThat(actualGuruModel3).isEqualTo(otherGuruModel3);
        });
    }

    private OperationStatus operationStatus(CommonModel model, OperationStatusType statusType) {
        OperationStatus status = new OperationStatus(statusType, OperationType.CHANGE, model.getId());
        if (!statusType.isFailure()) {
            status.setModel(model);
        }
        return status;
    }

    private Map<Long, CommonModel> getModels(CommonModel... models) {
        List<CategoryModelId> categoryModelIds = Stream.of(models)
            .map(CategoryModelId::from)
            .collect(Collectors.toList());
        return storageService.getModels(categoryModelIds, new ReadStats()).stream()
            .collect(Collectors.toMap(CommonModel::getId, Function.identity()));
    }
}
