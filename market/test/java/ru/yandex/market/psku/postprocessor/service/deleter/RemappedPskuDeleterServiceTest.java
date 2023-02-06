package ru.yandex.market.psku.postprocessor.service.deleter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.ModelBuilder;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TransitionDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ModelType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.TransitionSource;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Transition;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.TRANSITION;


public class RemappedPskuDeleterServiceTest extends BaseDBTest {
    private static final int HID = 91491;
    private static final long EXISTING_PSKU_ID1 = 101L;
    private static final long EXISTING_PMODEL_ID1 = 100501L; // PModel with only one PSKU
    private static final long EXISTING_PSKU_ID2 = 102L;
    private static final long EXISTING_PMODEL_ID2 = 100502L; // PModel with two PSKU
    private static final long EXISTING_PSKU_ID3 = 103L;
    private static final long EXISTING_PMODEL_ID3 = 100503L; // PModel transition to absent GuruId
    private static final long EXISTING_MSKU_ID1 = 1001L;
    private static final long EXISTING_GURU_ID1 = 1000501L;
    private static final long FAKE_PSKU_ID = 9999999L;

    @Autowired
    PskuResultStorageDao pskuResultStorageDao;

    @Autowired
    private TransitionDao transitionDao;

    private RemappedPskuDeleterService remappedPskuDeleterService;
    private ModelStorageHelper modelStorageHelper;


    @Before
    public void setup() {
        ModelStorageServiceMock modelStorageServiceMock = new ModelStorageServiceMock();
        modelStorageHelper = new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock);
        remappedPskuDeleterService = new RemappedPskuDeleterService(pskuResultStorageDao, transitionDao, modelStorageHelper, 10);

        modelStorageServiceMock.putModels(
            ModelBuilder.newBuilder(EXISTING_PSKU_ID1, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .withSkuParentRelation(HID, EXISTING_PMODEL_ID1)
                .build(),
            ModelBuilder.newBuilder(EXISTING_PMODEL_ID1, HID)
                .currentType(ModelStorage.ModelType.PARTNER)
                .withSkuRelations(HID, EXISTING_PSKU_ID1)
                .build(),
            ModelBuilder.newBuilder(EXISTING_PSKU_ID2, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .withSkuParentRelation(HID, EXISTING_PMODEL_ID2)
                .build(),
            ModelBuilder.newBuilder(EXISTING_PMODEL_ID2, HID)
                .currentType(ModelStorage.ModelType.PARTNER)
                .withSkuRelations(HID, EXISTING_PSKU_ID2, FAKE_PSKU_ID)
                .build(),
            ModelBuilder.newBuilder(EXISTING_PSKU_ID3, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .withSkuParentRelation(HID, EXISTING_PMODEL_ID3)
                .build(),
            ModelBuilder.newBuilder(EXISTING_PMODEL_ID3, HID)
                .currentType(ModelStorage.ModelType.PARTNER)
                .withSkuRelations(HID, EXISTING_PSKU_ID3)
                .build(),
            ModelBuilder.newBuilder(EXISTING_MSKU_ID1, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .withSkuParentRelation(HID, EXISTING_GURU_ID1)
                .build(),
            ModelBuilder.newBuilder(EXISTING_GURU_ID1, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .withSkuRelations(HID, EXISTING_MSKU_ID1)
                .build()
        );
    }


    @Test
    public void whenNotFoundInModelStorageThenNotChange() {
        final Long pskuId = createPskuResultStorage(100L, 1000L);
        remappedPskuDeleterService.doDelete();
        PskuResultStorage psku = pskuResultStorageDao.findById(pskuId);
        assertThat(psku.getState()).isEqualTo(PskuStorageState.REMAPPED);
    }

    @Test
    public void whenRemappedSinglePSkuInPModelThenRemovePSkuAndPModel() {
        final Long pskuId = createPskuResultStorage(EXISTING_PSKU_ID1, EXISTING_MSKU_ID1);
        remappedPskuDeleterService.doDelete();
        PskuResultStorage psku = pskuResultStorageDao.findById(pskuId);
        assertThat(psku.getState()).isEqualTo(PskuStorageState.PSKU_DELETED);
        assertThat(psku.getPmodelIdForDelete()).isEqualTo(null);

        final Optional<ModelStorage.Model> pSkuOpt = modelStorageHelper.findModel(EXISTING_PSKU_ID1, true);
        assertThat(pSkuOpt).isPresent().get().extracting(ModelStorage.Model::getDeleted).isEqualTo(true);

        assertTransition(EXISTING_PSKU_ID1, EXISTING_MSKU_ID1, ModelType.SKU);
        assertTransition(EXISTING_PMODEL_ID1, EXISTING_GURU_ID1, ModelType.MODEL);
    }

    @Test
    public void whenRemappedMoreThanOnePSkuInPModelThenRemovePSku() {
        final Long pskuId = createPskuResultStorage(EXISTING_PSKU_ID2, EXISTING_MSKU_ID1);
        remappedPskuDeleterService.doDelete();
        PskuResultStorage psku = pskuResultStorageDao.findById(pskuId);
        assertThat(psku.getState()).isEqualTo(PskuStorageState.PSKU_DELETED);
        assertThat(psku.getPmodelIdForDelete()).isEqualTo(EXISTING_PMODEL_ID2);

        final Optional<ModelStorage.Model> pSkuOpt = modelStorageHelper.findModel(EXISTING_PSKU_ID2, true);
        assertThat(pSkuOpt).isPresent().get().extracting(ModelStorage.Model::getDeleted).isEqualTo(true);

        final Optional<ModelStorage.Model> pModelId = modelStorageHelper.findModel(EXISTING_PMODEL_ID2);
        assertThat(pModelId).isPresent().get().extracting(ModelStorage.Model::getDeleted).isEqualTo(false);

        assertTransition(EXISTING_PSKU_ID2, EXISTING_MSKU_ID1, ModelType.SKU);

        final Optional<Transition> notExistingPModelOpt = transitionDao.fetchOptional(
            TRANSITION.OLD_ID,
            EXISTING_PMODEL_ID2);

        assertThat(notExistingPModelOpt).isEmpty();
    }

    private void assertTransition(long oldId, long newId, ModelType modelType) {
        final Optional<Transition> existingPskuTransitionOpt = transitionDao.fetchOptional(
            TRANSITION.OLD_ID,
            oldId);

        final Transition expectedTransition = new Transition();
        expectedTransition.setOldId(oldId);
        expectedTransition.setNewId(newId);
        expectedTransition.setType(modelType);
        expectedTransition.setIsRemoved(true);
        expectedTransition.setSource(TransitionSource.REMAPPING);

        assertThat(existingPskuTransitionOpt).isPresent().get().isEqualToIgnoringGivenFields(expectedTransition, "id");
    }

    private Long createPskuResultStorage(long pskuId, long mskuId) {
        final PskuResultStorage psku = new PskuResultStorage();
        psku.setPskuId(pskuId);
        psku.setMskuMappedId(mskuId);
        psku.setCategoryId((long) HID);
        psku.setState(PskuStorageState.REMAPPED);
        psku.setCreateTime(Timestamp.from(Instant.now()));
        pskuResultStorageDao.insert(psku);
        return psku.getId();
    }
}
