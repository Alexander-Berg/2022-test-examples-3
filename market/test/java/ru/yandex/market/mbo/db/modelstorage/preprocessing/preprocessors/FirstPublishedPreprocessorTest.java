package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Date;

@SuppressWarnings("checkstyle:magicNumber")
public class FirstPublishedPreprocessorTest extends BasePreprocessorTest {
    private FirstPublishedPreprocessor firstPublishedPreprocessor;

    @Before
    public void before() {
        super.before();
        firstPublishedPreprocessor = new FirstPublishedPreprocessor();
    }

    @Test
    public void testSingleModelPublish() {
        CommonModel modelInStorage = model(1);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(blueUnpublished(modelInStorage)),
            ImmutableList.of(blueUnpublished(unpublished(modelInStorage))));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId()))
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId()))
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    @Test
    public void testSingleModelOnlyWhitePublish() {
        CommonModel modelInStorage = model(1);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelInStorage),
            ImmutableList.of(unpublished(modelInStorage)));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel updatedModel = modelSaveGroup.getById(modelInStorage.getId());
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
    }

    @Test
    public void testSingleModelBluePublish() {
        CommonModel modelInStorage = model(1);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(unpublished(modelInStorage)),
            ImmutableList.of(blueUnpublished(unpublished(modelInStorage))));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel updatedModel = modelSaveGroup.getById(modelInStorage.getId());
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    @Test
    public void testSingleModelBlueUnpublish() {
        CommonModel modelInStorage = model(1);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(unpublished(modelInStorage)),
            ImmutableList.of(blueUnpublished(unpublished(modelInStorage))));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel updatedModel = modelSaveGroup.getById(modelInStorage.getId());
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    @Test
    public void testSingleModelUnpublish() {
        CommonModel modelInStorage = model(1);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(blueUnpublished(unpublished(modelInStorage))),
            ImmutableList.of(blueUnpublished(modelInStorage)));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel updatedModel = modelSaveGroup.getById(modelInStorage.getId());
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedWhiteDate).isEqualTo(updatedModel.getCreatedDate());
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedBlueDate).isEqualTo(updatedModel.getCreatedDate());
    }

    @Test
    public void testSingleModelWhiteUnpublish() {
        CommonModel modelInStorage = model(1);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(unpublished(modelInStorage)),
            ImmutableList.of(modelInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel updatedModel = modelSaveGroup.getById(modelInStorage.getId());
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedWhiteDate).isEqualTo(updatedModel.getCreatedDate());
        Assertions.assertThat(updatedModel)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
    }

    @Test
    public void testSingleModelPublishDatesSet() {
        Date date = new Date(100500);
        CommonModel modelInStorage = model(1);
        modelInStorage.setPublishedWhiteDate(date);
        modelInStorage.setPublishedBlueDate(date);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(blueUnpublished(modelInStorage)),
            ImmutableList.of(blueUnpublished(unpublished(modelInStorage))));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId()))
            .extracting(CommonModel::getPublishedWhiteDate).isEqualTo(date);
        Assertions.assertThat(modelSaveGroup.getById(modelInStorage.getId()))
            .extracting(CommonModel::getPublishedBlueDate).isEqualTo(date);
    }

    @Test
    public void testHierarchyPublish() {
        CommonModel modelInStorage = blueUnpublished(unpublished(model(1)));
        CommonModel modificationInStorage = modification(2, 1);
        CommonModel modelSkuInStorage = sku(3, modelInStorage);
        CommonModel modificationSkuInStorage = sku(4, modificationInStorage);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(published(modelInStorage), modificationInStorage,
                modelSkuInStorage, modificationSkuInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage, modelSkuInStorage, modificationSkuInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getModels())
            .extracting(CommonModel::getPublishedWhiteDate).doesNotContainNull();
        Assertions.assertThat(modelSaveGroup.getModels())
            .extracting(CommonModel::getPublishedBlueDate).doesNotContainNull();
    }

    @Test
    public void testHierarchyModificationPublish() {
        CommonModel modelInStorage = model(1);
        CommonModel modificationInStorage = blueUnpublished(unpublished(modification(2, 1)));
        CommonModel modelSkuInStorage = sku(3, modelInStorage);
        CommonModel modificationSkuInStorage = sku(4, modificationInStorage);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelInStorage, published(modificationInStorage),
                modelSkuInStorage, modificationSkuInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage, modelSkuInStorage, modificationSkuInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel model = modelSaveGroup.getById(1);
        CommonModel modification = modelSaveGroup.getById(2);
        CommonModel modelSku = modelSaveGroup.getById(3);
        CommonModel modificationSku = modelSaveGroup.getById(4);

        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
        Assertions.assertThat(modelSku)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(modelSku)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    @Test
    public void testHierarchyModificationBluePublish() {
        CommonModel modelInStorage = unpublished(model(1));
        CommonModel modificationInStorage = blueUnpublished(unpublished(modification(2, 1)));
        CommonModel modelSkuInStorage = sku(3, modelInStorage);
        CommonModel modificationSkuInStorage = sku(4, modificationInStorage);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelInStorage, published(modificationInStorage),
                modelSkuInStorage, modificationSkuInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage, modelSkuInStorage, modificationSkuInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel model = modelSaveGroup.getById(1);
        CommonModel modification = modelSaveGroup.getById(2);
        CommonModel modelSku = modelSaveGroup.getById(3);
        CommonModel modificationSku = modelSaveGroup.getById(4);

        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
        Assertions.assertThat(modelSku)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(modelSku)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    @Test
    public void testHierarchySkuPublish() {
        CommonModel modelInStorage = model(1);
        CommonModel modificationInStorage = modification(2, 1);
        CommonModel modelSkuInStorage = blueUnpublished(unpublished(sku(3, modelInStorage)));
        CommonModel modificationSkuInStorage = blueUnpublished(unpublished(sku(4, modificationInStorage)));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelInStorage, modificationInStorage,
                published(modelSkuInStorage), published(modificationSkuInStorage)),
            ImmutableList.of(modelInStorage, modificationInStorage, modelSkuInStorage, modificationSkuInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel model = modelSaveGroup.getById(1);
        CommonModel modification = modelSaveGroup.getById(2);
        CommonModel modelSku = modelSaveGroup.getById(3);
        CommonModel modificationSku = modelSaveGroup.getById(4);

        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
        Assertions.assertThat(modelSku)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(modelSku)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    @Test
    public void testHierarchyBluePublish() {
        CommonModel modelInStorage = blueUnpublished(unpublished(model(1)));
        CommonModel modificationInStorage = unpublished(modification(2, 1));
        CommonModel modelSkuInStorage = unpublished(sku(3, modelInStorage));
        CommonModel modificationSkuInStorage = unpublished(sku(4, modificationInStorage));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(bluePublished(modelInStorage), modificationInStorage,
                modelSkuInStorage, modificationSkuInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage, modelSkuInStorage, modificationSkuInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getModels())
            .extracting(CommonModel::getPublishedWhiteDate).containsOnlyNulls();
        Assertions.assertThat(modelSaveGroup.getModels())
            .extracting(CommonModel::getPublishedBlueDate).doesNotContainNull();
    }

    @Test
    public void testHierarchyUnpublish() {
        CommonModel modelInStorage = model(1);
        CommonModel modificationInStorage = modification(2, 1);
        CommonModel modelSkuInStorage = sku(3, modelInStorage);
        CommonModel modificationSkuInStorage = sku(4, modificationInStorage);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(unpublished(blueUnpublished(modelInStorage)), modificationInStorage,
                modelSkuInStorage, modificationSkuInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage, modelSkuInStorage, modificationSkuInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getModels())
            .allMatch(m -> m.getCreatedDate().equals(m.getPublishedWhiteDate()));
        Assertions.assertThat(modelSaveGroup.getModels())
            .allMatch(m -> m.getCreatedDate().equals(m.getPublishedBlueDate()));
    }

    @Test
    public void testMoveModificationToPublished() {
        CommonModel modelInStorage = blueUnpublished(unpublished(model(1)));
        CommonModel modificationInStorage = modification(2, 1);
        CommonModel modificationSkuInStorage = sku(3, modificationInStorage);
        CommonModel otherModelInStorage = model(4);
        CommonModel movedModification = modification(2, 4);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelInStorage, modificationInStorage, modificationSkuInStorage, otherModelInStorage),
            ImmutableList.of(modelInStorage, movedModification, modificationSkuInStorage, otherModelInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel model = modelSaveGroup.getById(1);
        CommonModel modification = modelSaveGroup.getById(2);
        CommonModel modificationSku = modelSaveGroup.getById(3);

        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedWhiteDate).isNull();
        Assertions.assertThat(model)
            .extracting(CommonModel::getPublishedBlueDate).isNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(modification)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(modificationSku)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    @Test
    public void testMoveSkuToPublished() {
        CommonModel modelInStorage = model(1);
        CommonModel modificationInStorage = blueUnpublished(unpublished(modification(2, 1)));
        CommonModel modification2InStorage = modification(3, 1);
        CommonModel modificationSkuInStorage = sku(4, modificationInStorage);
        CommonModel movedSkuInStorage = sku(4, modification2InStorage);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelInStorage, modificationInStorage, modification2InStorage, modificationSkuInStorage),
            ImmutableList.of(modelInStorage, modificationInStorage, modification2InStorage, movedSkuInStorage));

        firstPublishedPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel sku = modelSaveGroup.getById(4);

        Assertions.assertThat(sku)
            .extracting(CommonModel::getPublishedWhiteDate).isNotNull();
        Assertions.assertThat(sku)
            .extracting(CommonModel::getPublishedBlueDate).isNotNull();
    }

    public CommonModel published(CommonModel model) {
        return new CommonModel(model).setPublished(true);
    }

    public CommonModel bluePublished(CommonModel model) {
        return new CommonModel(model).setBluePublished(true);
    }

    public CommonModel unpublished(CommonModel model) {
        return new CommonModel(model).setPublished(false);
    }

    public CommonModel blueUnpublished(CommonModel model) {
        return new CommonModel(model).setBluePublished(false);
    }
}
