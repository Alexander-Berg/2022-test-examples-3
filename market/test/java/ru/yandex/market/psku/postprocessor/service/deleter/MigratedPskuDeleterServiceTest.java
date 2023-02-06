package ru.yandex.market.psku.postprocessor.service.deleter;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.ModelBuilder;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.MigratedPskusDeleteInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ShopBusinessDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TransitionDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuDeleteProcessStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MigratedPskusDeleteInfo;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Transition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.SHOP_TO_BUSINESS_ID;
import static ru.yandex.market.psku.postprocessor.service.migration.MigrationTestUtils.createShopSkuParam;


public class MigratedPskuDeleterServiceTest extends BaseDBTest {
    private static final int BIZ_ID = 1;
    private static final int SHOP_ID = 11;
    private static final String OFFER_ID = "offer";
    private static final int HID = 91491;
    private static final long PSKU_ID1 = 101L;
    private static final long PSKU_ID2 = 102L;
    private static final long PSKU_ID3 = 103L;
    private static final long PSKU_ID4 = 104L;
    private static final long MSKU_ID = 201L;
    private static final long PMODEL_ID1 = 100501L;
    private static final long PMODEL_ID2 = 100502L;
    private static final long EXISTING_SKU_ID1 = 1001L;
    private static final long EXISTING_SKU_ID2 = 1002L;
    private static final long EXISTING_SKU_ID3 = 1003L;
    private static final long EXISTING_SKU_ID4 = 1004L;
    private static final long EXISTING_SKU_ID5 = 1005L;
    private static final long EXISTING_MODEL_1 = 2001L;
    private static final long EXISTING_MODEL_2 = 2002L;
    private static final long EXISTING_MODEL_3 = 2003L;

    private static final String MBOC_TEST_USER = "mbocTestUser";

    @Autowired
    MigratedPskusDeleteInfoDao migratedPskusDeleteInfoDao;
    @Autowired
    TransitionDao transitionDao;
    @Autowired
    ShopBusinessDao shopBusinessDao;
    private MigratedPskuDeleterService migratedPskuDeleterService;
    private MigratedPModelDeleterService migratedPModelDeleterService;
    private ModelStorageHelper modelStorageHelper;
    private ModelStorageServiceMock modelStorageServiceMock;
    private MboMappingsServiceMock mboMappingsServiceMock;


    @Before
    public void setup() {
        mboMappingsServiceMock = new MboMappingsServiceMock();
        modelStorageServiceMock = new ModelStorageServiceMock();
        modelStorageHelper = Mockito.spy(new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock));
        migratedPskuDeleterService = new MigratedPskuDeleterService(
                migratedPskusDeleteInfoDao, modelStorageHelper, mboMappingsServiceMock, shopBusinessDao, MBOC_TEST_USER,
                true
        );
        migratedPModelDeleterService = new MigratedPModelDeleterService(modelStorageHelper, migratedPskusDeleteInfoDao,
                transitionDao);
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(EXISTING_SKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, EXISTING_MODEL_1)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_SKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_MODEL_2)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_SKU_ID3, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_MODEL_2)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_SKU_ID4, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_MODEL_3)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_SKU_ID5, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_MODEL_3)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_MODEL_1, HID)
                        .currentType(ModelStorage.ModelType.GURU)
                        .withSkuRelations(HID, EXISTING_SKU_ID1)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_MODEL_2, HID)
                        .currentType(ModelStorage.ModelType.GURU)
                        .withSkuRelations(HID, EXISTING_SKU_ID2)
                        .withSkuRelations(HID, EXISTING_SKU_ID3)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_MODEL_3, HID)
                        .currentType(ModelStorage.ModelType.GURU)
                        .withSkuRelations(HID, EXISTING_SKU_ID4)
                        .withSkuRelations(HID, EXISTING_SKU_ID5)
                        .build()
        );
        shopBusinessDao.dsl().insertInto(SHOP_TO_BUSINESS_ID)
                .set(SHOP_TO_BUSINESS_ID.BUSINESS_ID, (long) BIZ_ID)
                .set(SHOP_TO_BUSINESS_ID.SHOP_ID, (long) SHOP_ID)
                .execute();
    }

    @Test
    public void deletePskuWhenNotFoundInMbo() {
        // trying to delete psku that does not exist in mbo
        Long absentPSkuInfoId = createPSkuMigrationInfo(100L, 1000L);
        migratedPskuDeleterService.doDelete();
        MigratedPskusDeleteInfo absentPSkuInfo = migratedPskusDeleteInfoDao.findById(absentPSkuInfoId);
        assertThat(absentPSkuInfo.getProcessingStatus()).isEqualTo(PskuDeleteProcessStatus.READY_FOR_PROCESS);
        assertThat(absentPSkuInfo.getDeletingPskuPmodelId()).isNull();
    }

    @Test
    public void deleteOnePskuOutOfTwo() {
        // remove PSKU_ID1 under PMODEL_ID1, but PMODEL_ID1 has another psku, so p-model should not be deleted
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(SHOP_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(SHOP_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        .withSkuRelations(HID, PSKU_ID1)
                        .withSkuRelations(HID, PSKU_ID2)
                        .build());
        Long infoId = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID1);
        migratedPskuDeleterService.doDelete();
        checkPSkuIsDeletedProperly(infoId, PSKU_ID1, EXISTING_SKU_ID1, PMODEL_ID1);
        Mockito.verify(modelStorageHelper).removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(),
                Mockito.eq(true));
    }

    @Test
    public void deletePskuAndModelSimple() {
        // delete single psku under p-model
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        // modelStorageMock can't handle relations cleaning in remove, so we don't put them
                        .build());
        Long infoId = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID1);
        migratedPskuDeleterService.doDelete();
        checkPSkuIsDeletedProperly(infoId, PSKU_ID1, EXISTING_SKU_ID1, PMODEL_ID1);

        migratedPModelDeleterService.doDelete();
        checkPModelIsDeletedProperly(PMODEL_ID1, EXISTING_MODEL_1);
        Mockito.verify(modelStorageHelper, Mockito.times(2))
                .removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(), Mockito.eq(true));
    }

    @Test
    public void deleteWhenCantDeterminePModelTransition() {
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        // modelStorageMock can't handle relations cleaning in remove, so we don't put them
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID2)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID2, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        // modelStorageMock can't handle relations cleaning in remove, so we don't put them
                        .build()
        );
        Long infoId = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID1);
        Long infoId2 = createPSkuMigrationInfo(PSKU_ID2, EXISTING_SKU_ID2);
        migratedPskuDeleterService.doDelete();
        // both pskus are removed with transitions to their skus
        checkPSkuIsDeletedProperly(infoId, PSKU_ID1, EXISTING_SKU_ID1, PMODEL_ID1);
        checkPSkuIsDeletedProperly(infoId2, PSKU_ID2, EXISTING_SKU_ID2, PMODEL_ID2);

        // then second sku is removed by someone (or some process)
        modelStorageServiceMock.removeModels(ModelStorage.RemoveModelsRequest.newBuilder()
                .setById(true)
                .addModels(ModelStorage.Model.newBuilder()
                        .setId(EXISTING_SKU_ID2)
                        .build())
                .build());
        // then we try to delete left pmodels
        migratedPModelDeleterService.doDelete();
        // first pmodel is deleted
        checkPModelIsDeletedProperly(PMODEL_ID1, EXISTING_MODEL_1);
        // second pmodel should not be deleted, since we cant create transition
        checkTransitionNotCreated(PMODEL_ID2);
        List<MigratedPskusDeleteInfo> indeterminableTrans = migratedPskusDeleteInfoDao.fetchById(infoId2);
        assertThat(indeterminableTrans)
                .extracting(MigratedPskusDeleteInfo::getProcessingStatus)
                .containsOnly(PskuDeleteProcessStatus.CANT_CALCULATE_PMODEL_TRANSITION);
        assertThat(indeterminableTrans)
                .extracting(MigratedPskusDeleteInfo::getDeletingPskuPmodelId)
                .containsOnly(PMODEL_ID2);
        Mockito.verify(modelStorageHelper, Mockito.times(2))
                .removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(), Mockito.eq(true));
    }

    @Test
    public void deletePskuWhenNotOwnedByOffer() {
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam("some_other_offers_shop_sku"))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        // modelStorageMock can't handle relations cleaning in remove, so we don't put them
                        .build());
        Long infoId = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID1);
        MigratedPskuDeleterService migratedPskuDeleterServiceLocal = new MigratedPskuDeleterService(
                migratedPskusDeleteInfoDao, modelStorageHelper, mboMappingsServiceMock, shopBusinessDao, MBOC_TEST_USER,
                false
        );
        migratedPskuDeleterServiceLocal.doDelete();
        checkTransitionNotCreated(PSKU_ID1);

        migratedPModelDeleterService.doDelete();
        checkTransitionNotCreated(PMODEL_ID1);
        Mockito.verify(modelStorageHelper, Mockito.times(0))
                .removeModels(Mockito.anyList(), Mockito.anyList(), Mockito.any());

        List<MigratedPskusDeleteInfo> infos = migratedPskusDeleteInfoDao.fetchById(infoId);
        Assertions.assertThat(infos.size()).isEqualTo(1);
        Assertions.assertThat(infos.get(0)).extracting(MigratedPskusDeleteInfo::getProcessingStatus)
                .isEqualTo(PskuDeleteProcessStatus.PSKU_NOT_OWNED_BY_OFFER);
    }

    @Test
    public void deletePskuOwnershipIgnored() {
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam("some_other_offers_shop_sku"))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        // modelStorageMock can't handle relations cleaning in remove, so we don't put them
                        .build());
        Long infoId = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID1);
        migratedPskuDeleterService.doDelete();
        checkPSkuIsDeletedProperly(infoId, PSKU_ID1, EXISTING_SKU_ID1, PMODEL_ID1);

        migratedPModelDeleterService.doDelete();
        checkPModelIsDeletedProperly(PMODEL_ID1, EXISTING_MODEL_1);
        Mockito.verify(modelStorageHelper, Mockito.times(2))
                .removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(), Mockito.eq(true));
    }

    @Test
    public void deletePskuWhenModelHadOtherPskuThenBecomeEmpty() {
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        .withSkuRelations(HID, PSKU_ID2)
                        .build());
        // first we delete psku1
        Long infoId1 = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID2);
        migratedPskuDeleterService.doDelete();
        migratedPModelDeleterService.doDelete();
        checkPSkuIsDeletedProperly(infoId1, PSKU_ID1, EXISTING_SKU_ID2, PMODEL_ID1);
        Mockito.verify(modelStorageHelper).removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(),
                Mockito.eq(true));
        // prepare p-model by simulating like it become empty
        modelStorageServiceMock.putModels(ModelBuilder.newBuilder(PMODEL_ID1, HID)
                .currentType(ModelStorage.ModelType.PARTNER)
                .build());
        // second we delete left psku
        Long infoId2 = createPSkuMigrationInfo(PSKU_ID2, EXISTING_SKU_ID3);
        migratedPskuDeleterService.doDelete();
        checkPSkuIsDeletedProperly(infoId2, PSKU_ID2, EXISTING_SKU_ID3, PMODEL_ID1);
        migratedPModelDeleterService.doDelete();
        // and model now should be deleted
        checkPModelIsDeletedProperly(PMODEL_ID1, EXISTING_MODEL_2);
        Mockito.verify(modelStorageHelper, Mockito.times(3))
                .removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(), Mockito.eq(true));
    }

    @Test
    public void deletePskuWhenAcceptedSkusOwnedByDifferentModels() {
        // a case when skus that pskus accepted for have different parent models
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID3, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        // modelStorageMock can't handle relations cleaning in remove, so we don't put them
                        .build());
        Long infoId1 = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID1);
        Long infoId2 = createPSkuMigrationInfo(PSKU_ID2, EXISTING_SKU_ID2);
        Long infoId3 = createPSkuMigrationInfo(PSKU_ID3, EXISTING_SKU_ID3);
        migratedPskuDeleterService.doDelete();
        checkPSkuIsDeletedProperly(infoId1, PSKU_ID1, EXISTING_SKU_ID1, PMODEL_ID1);
        checkPSkuIsDeletedProperly(infoId2, PSKU_ID2, EXISTING_SKU_ID2, PMODEL_ID1);
        checkPSkuIsDeletedProperly(infoId3, PSKU_ID3, EXISTING_SKU_ID3, PMODEL_ID1);
        migratedPModelDeleterService.doDelete();
        // EXISTING_MODEL_2 is most popular (it has 2 skus)
        checkPModelIsDeletedProperly(PMODEL_ID1, EXISTING_MODEL_2);
        Mockito.verify(modelStorageHelper, Mockito.times(2))
                .removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(), Mockito.eq(true));
    }

    @Test
    public void deletePskuWhenAcceptedSkusOwnedByDifferentEqualModels() {
        // a case when skus that pskus accepted for have different parent models and their popularity is equal
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID3, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PSKU_ID4, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.PARTNER_SKU.name())
                        .withSkuParentRelation(HID, PMODEL_ID1)
                        .supplierId(BIZ_ID)
                        .parameterValue(createShopSkuParam(OFFER_ID))
                        .build(),
                ModelBuilder.newBuilder(PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        // modelStorageMock can't handle relations cleaning in remove, so we don't put them
                        .build());
        Long infoId1 = createPSkuMigrationInfo(PSKU_ID1, EXISTING_SKU_ID2);
        Long infoId2 = createPSkuMigrationInfo(PSKU_ID2, EXISTING_SKU_ID3);
        Long infoId3 = createPSkuMigrationInfo(PSKU_ID3, EXISTING_SKU_ID4);
        Long infoId4 = createPSkuMigrationInfo(PSKU_ID4, EXISTING_SKU_ID5);
        migratedPskuDeleterService.doDelete();
        checkPSkuIsDeletedProperly(infoId1, PSKU_ID1, EXISTING_SKU_ID2, PMODEL_ID1);
        checkPSkuIsDeletedProperly(infoId2, PSKU_ID2, EXISTING_SKU_ID3, PMODEL_ID1);
        checkPSkuIsDeletedProperly(infoId3, PSKU_ID3, EXISTING_SKU_ID4, PMODEL_ID1);
        checkPSkuIsDeletedProperly(infoId4, PSKU_ID4, EXISTING_SKU_ID5, PMODEL_ID1);
        migratedPModelDeleterService.doDelete();
        // EXISTING_MODEL_2 and EXISTING_MODEL_3 have the same amount of skus, hence EXISTING_MODEL_3
        // should be chosen by ID compare
        checkPModelIsDeletedProperly(PMODEL_ID1, EXISTING_MODEL_2);
        Mockito.verify(modelStorageHelper, Mockito.times(2))
                .removeModels(Mockito.anyCollection(), Mockito.anyCollection(), Mockito.any(), Mockito.eq(true));
    }

    @Test
    public void handleIncorrectMskuCreation() {
        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(MSKU_ID, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .source(ModelStorage.ModelType.SKU.name())
                        .build());
        mboMappingsServiceMock.addMapping(HID, BIZ_ID, OFFER_ID, MSKU_ID);

        Long infoId1 = createPSkuMigrationInfo(MSKU_ID, EXISTING_SKU_ID1);
        MigratedPskusDeleteInfo initialInfo = migratedPskusDeleteInfoDao.findById(infoId1);

        migratedPskuDeleterService.doDelete();

        MigratedPskusDeleteInfo updatedInfo = migratedPskusDeleteInfoDao.findById(infoId1);

        checkTransitionNotCreated(MSKU_ID);

        assertEquals(initialInfo.getDeletingPskuId(), updatedInfo.getAcceptingPskuId());
        assertEquals(initialInfo.getAcceptingPskuId(), updatedInfo.getDeletingPskuId());

        long newMappingSkuId = mboMappingsServiceMock.getMappingSkuId(BIZ_ID, OFFER_ID);
        assertEquals(MSKU_ID, newMappingSkuId);
    }

    private void checkPSkuIsDeletedProperly(long relatedDeleteInfoId, long pSkuId, long acceptedSkuId,
                                            long pModelId) {
        MigratedPskusDeleteInfo info = migratedPskusDeleteInfoDao.findById(relatedDeleteInfoId);

        // check status is stored
        assertThat(info.getProcessingStatus()).isEqualTo(PskuDeleteProcessStatus.PSKU_DELETED);
        assertThat(info.getDeletingPskuPmodelId()).isEqualTo(pModelId);

        // check psku is deleted
        Optional<ModelStorage.Model> pSkuOpt = modelStorageHelper.findModel(pSkuId, true);
        assertThat(pSkuOpt).isPresent().get().extracting(ModelStorage.Model::getDeleted)
                .withFailMessage(String.format("Expecting psku %d to be deleted, but it wasn't", pSkuId))
                .isEqualTo(true);

        // check transition
        checkTransitionCreated(pSkuId, acceptedSkuId);
    }

    private void checkPModelIsDeletedProperly(long pModelId, long acceptedModelId) {
        Optional<ModelStorage.Model> pModelOpt = modelStorageHelper.findModel(pModelId, true);
        assertThat(pModelOpt).isPresent().get().extracting(ModelStorage.Model::getDeleted)
                .withFailMessage(String.format("Expecting p-model %d to be deleted, but it wasn't", pModelId))
                .isEqualTo(true);
        checkTransitionCreated(pModelId, acceptedModelId);
        assertThat(migratedPskusDeleteInfoDao.fetchByDeletingPskuPmodelId(pModelId))
                .extracting(MigratedPskusDeleteInfo::getProcessingStatus)
                .containsOnly(PskuDeleteProcessStatus.PMODEL_DELETED);
        List<Transition> transitions = transitionDao.fetchByOldId(pModelId);
        assertThat(transitions.size()).isEqualTo(1);
        assertThat(transitions.get(0)).extracting(Transition::getNewId).isEqualTo(acceptedModelId);
    }

    private void checkTransitionCreated(long oldEntityId, long newEntityId) {
        ModelStorage.ModelTransition transition = modelStorageServiceMock.getTransitionsMap().get(oldEntityId);
        assertThat(transition).as("Transition for " + oldEntityId).isNotNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(transition.getOldEntityId()).as("Old entity ID").isEqualTo(oldEntityId);
            softly.assertThat(transition.getNewEntityId()).as("New entity ID").isEqualTo(newEntityId);
        });
    }

    private void checkTransitionNotCreated(long oldEntityId) {
        ModelStorage.ModelTransition transition = modelStorageServiceMock.getTransitionsMap().get(oldEntityId);
        assertThat(transition).as("Transition for " + oldEntityId).isNull();
    }

    private Long createPSkuMigrationInfo(long deletingPskuId, long acceptedPskuId) {
        MigratedPskusDeleteInfo migratedPskusDeleteInfo = new MigratedPskusDeleteInfo();
        migratedPskusDeleteInfo.setDeletingBusinessId((long) BIZ_ID);
        migratedPskusDeleteInfo.setDeletingOfferId(OFFER_ID);
        migratedPskusDeleteInfo.setDeletingPskuId(deletingPskuId);
        migratedPskusDeleteInfo.setAcceptingPskuId(acceptedPskuId);
        migratedPskusDeleteInfoDao
                .insertWithProcessingStatus(migratedPskusDeleteInfo, PskuDeleteProcessStatus.READY_FOR_PROCESS);
        return migratedPskusDeleteInfo.getId();
    }
}
