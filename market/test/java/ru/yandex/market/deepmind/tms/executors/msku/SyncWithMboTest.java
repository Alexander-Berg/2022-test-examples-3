package ru.yandex.market.deepmind.tms.executors.msku;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;

import static ru.yandex.market.mbo.http.ModelStorage.Model;
import static ru.yandex.market.mbo.http.ModelStorage.ModelType;
import static ru.yandex.market.mbo.http.ModelStorage.Relation;
import static ru.yandex.market.mbo.http.ModelStorage.RelationType;

public class SyncWithMboTest extends BaseImportMskuTest {
    private final Model correctModel = createModel(1L, ModelType.FAST_SKU.name(), false).build();
    private final Model deletedModel = createModel(2L, ModelType.SKU.name(), true).build();
    private final Model illegalTypeModel = createModel(3L, ModelType.MODEL_TRANSFER.name(), false).build();
    private final Model nonexistentTypeModel = createModel(4L, "nonexistent type", false).build();

    @Before
    public void savingModels() {
        var modelsToSave = List.of(
            correctModel,
            deletedModel,
            illegalTypeModel,
            nonexistentTypeModel
        );
        mboModelsServiceMock.saveModels(modelsToSave);

        var loadedModels =
            mboModelsServiceMock.loadModels(
                modelsToSave.stream().map(Model::getId).collect(Collectors.toList()),
                Set.of(), Set.of(), true
            );
        Assertions.assertThat(loadedModels).hasSize(modelsToSave.size());
    }

    @Test
    public void mskusAreSavedProperly() {
        importMskuService.syncWithMbo(List.of(1L, 2L, 3L, 4L));
        Assertions.assertThat(deepmindMskuRepository.findAll())
            .extracting(Msku::getId)
            .containsExactly(1L);
    }

    @Test
    public void statusIsBeingCreatedWhileSyncing() {
        importMskuService.syncWithMbo(List.of(1L, 2L, 3L, 4L));
        Assertions.assertThat(mskuStatusRepository.findById(correctModel.getId())).isPresent();
    }

    @Test
    public void mskuDeletedInMboAreBeingDeleted() {
        mboModelsServiceMock.saveModels(List.of(createModel(5L, ModelType.SKU.name(), true).build()));
        deepmindMskuRepository.save(createMsku(5L));
        importMskuService.syncWithMbo(List.of(1L, 2L, 3L, 4L, 5L));
        Assertions.assertThat(deepmindMskuRepository.findAll())
            .filteredOn(msku -> !msku.getDeleted())
            .hasSize(1)
            .extracting(Msku::getId)
            .doesNotContain(5L);
    }

    @Test
    public void mskuNotPresentedInMboAreBeingDeleted() {
        importMskuService.syncWithMbo(List.of());

        deepmindMskuRepository.save(createMsku(5L));
        importMskuService.syncWithMbo(List.of(1L, 2L, 3L, 4L, 5L));
        Assertions.assertThat(deepmindMskuRepository.findAll())
            .filteredOn(msku -> !msku.getDeleted())
            .hasSize(1)
            .extracting(Msku::getId)
            .doesNotContain(5L);
    }

    @Test
    public void mskuIsBeingUpdated() {
        var vendorId = 123_456L;
        importMskuService.syncWithMbo(List.of(1L, 2L, 3L, 4L));
        mboModelsServiceMock.saveModels(List.of(
            createModel(correctModel.getId(), ModelType.SKU.name(), false)
                .setVendorId(vendorId)
                .build()
        ));
        importMskuService.syncWithMbo(List.of(correctModel.getId()));
        Assertions.assertThat(deepmindMskuRepository.findById(correctModel.getId()))
            .isPresent().get()
            .extracting(Msku::getVendorId)
            .isEqualTo(vendorId);
    }

    private Model.Builder createModel(Long modelId, String currentType, boolean deleted) {
        return Model.newBuilder()
            .setId(modelId)
            .setCurrentType(currentType)
            .addRelations(createParent(modelId + 100))
            .setModifiedTs(Instant.now().toEpochMilli())
            .setDeleted(deleted);
    }

    private Relation createParent(Long parentId) {
        return Relation.newBuilder()
            .setId(parentId)
            .setType(RelationType.SKU_PARENT_MODEL)
            .setCategoryId(0L)
            .build();
    }

    private Msku createMsku(long mskuId) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setCategoryId(0L)
            .setVendorId(0L)
            .setDeleted(false)
            .setModifiedTs(Instant.now())
            .setSkuType(SkuTypeEnum.FAST_SKU);
    }
}
