package ru.yandex.market.psku.postprocessor.service.migration;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.ModelBuilder;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.MigratedPskusChangeOwnershipInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ShopBusinessDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuChangeOwnershipProcessStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MigratedPskusChangeOwnershipInfo;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.psku.postprocessor.service.migration.MigrationTestUtils.createShopSkuParam;

public class PskuOwnershipChangeServiceTest extends BaseDBTest {
    private static final long HID = 91491;
    private static final long PSKU_ID1 = 101L;
    private static final long PSKU_ID2 = 102L;
    private static final long PSKU_ID3 = 103L;
    private static final long PMODEL_ID1 = 201L;
    private static final long MSKU_ID = 1000L;
    private static final long TARGET_BUSINESS_ID = 10_000L;
    private static final long SRC_BUSINESS_ID = 20_000L;

    private static final String TARGET_OFFER_ID = "target";
    private static final String SRC_OFFER_ID = "src";

    @Autowired
    MigratedPskusChangeOwnershipInfoDao dao;
    private PskuOwnershipChangeService pskuOwnershipChangeService;
    private ModelStorageHelper modelStorageHelper;
    private ModelStorageServiceMock modelStorageServiceMock;

    @Before
    public void setup() {
        modelStorageServiceMock = new ModelStorageServiceMock();
        modelStorageHelper = Mockito.spy(
            new ModelStorageHelper(
                modelStorageServiceMock,
                modelStorageServiceMock
            )
        );
        pskuOwnershipChangeService =
            new PskuOwnershipChangeService(dao, modelStorageHelper, Mockito.mock(ShopBusinessDao.class), true);

        modelStorageServiceMock.putModels(
            ModelBuilder.newBuilder(MSKU_ID, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.SKU.name())
                .supplierId(SRC_BUSINESS_ID)
                .parameterValue(createShopSkuParam(SRC_OFFER_ID))
                .build(),
            ModelBuilder.newBuilder(PSKU_ID1, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SRC_BUSINESS_ID)
                .parameterValue(createShopSkuParam(SRC_OFFER_ID))
                .withSkuParentRelation(HID, PMODEL_ID1)
                .build(),
            ModelBuilder.newBuilder(PSKU_ID2, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SRC_BUSINESS_ID)
                .withSkuParentRelation(HID, PMODEL_ID1)
                .parameterValue(createShopSkuParam(SRC_OFFER_ID))
                .build(),
            ModelBuilder.newBuilder(PMODEL_ID1, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .supplierId(SRC_BUSINESS_ID)
                .withSkuRelations(HID, PSKU_ID1, PSKU_ID2)
                .build()
        );
    }

    @Test
    public void testTwoPskuWithTheSamePmodelChangeSupplierId() {
        assertBusinessId(PSKU_ID1, SRC_BUSINESS_ID);
        assertBusinessId(PMODEL_ID1, SRC_BUSINESS_ID);
        assertBusinessId(PSKU_ID2, SRC_BUSINESS_ID);

        createPSkuMigrationInfo(PSKU_ID1, PskuChangeOwnershipProcessStatus.READY_FOR_PROCESS);

        pskuOwnershipChangeService.doChange();

        verify(modelStorageHelper, times(2))
            .findModels(anyList());

        verify(modelStorageHelper, times(1))
            .executeSaveModelRequest(Mockito.any(ModelCardApi.SaveModelsGroupRequest.class));

        assertBusinessId(PSKU_ID1, TARGET_BUSINESS_ID);
        assertBusinessId(PMODEL_ID1, TARGET_BUSINESS_ID);
        assertBusinessId(PSKU_ID2, SRC_BUSINESS_ID);

        int news = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.NEW).size();
        int ready = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.READY_FOR_PROCESS).size();
        int changedCount = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.CHANGED).size();

        assertEquals(0, news);
        assertEquals(0, ready);
        assertEquals(1, changedCount);
    }

    @Test
    public void testNothingToChange() {
        assertBusinessId(PSKU_ID1, SRC_BUSINESS_ID);
        assertBusinessId(PMODEL_ID1, SRC_BUSINESS_ID);
        assertBusinessId(PSKU_ID2, SRC_BUSINESS_ID);

        createPSkuMigrationInfo(PSKU_ID1, PskuChangeOwnershipProcessStatus.CHANGED);
        createPSkuMigrationInfo(PSKU_ID2, PskuChangeOwnershipProcessStatus.NEW);

        pskuOwnershipChangeService.doChange();

        verify(modelStorageHelper, never())
            .findModels(anyList());

        verify(modelStorageHelper, never())
            .executeSaveModelRequest(Mockito.any(ModelCardApi.SaveModelsGroupRequest.class));

        assertBusinessId(PSKU_ID1, SRC_BUSINESS_ID);
        assertBusinessId(PMODEL_ID1, SRC_BUSINESS_ID);
        assertBusinessId(PSKU_ID2, SRC_BUSINESS_ID);

        int changed = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.CHANGED).size();
        int notNeedToChange = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.NOT_NEED_TO_CHANGE).size();
        int news = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.NEW).size();

        assertEquals(1, changed);
        assertEquals(0, notNeedToChange);
        assertEquals(1, news);
    }

    @Test
    public void testIfMskuDoNothing() {
        createPSkuMigrationInfo(MSKU_ID, PskuChangeOwnershipProcessStatus.READY_FOR_PROCESS);

        pskuOwnershipChangeService.doChange();

        verify(modelStorageHelper, times(1))
            .findModels(anyList());

        verify(modelStorageHelper, never())
            .executeSaveModelRequest(Mockito.any(ModelCardApi.SaveModelsGroupRequest.class));

        int changed = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.CHANGED).size();
        int notNeedToChange = dao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.NOT_NEED_TO_CHANGE).size();

        assertEquals(0, changed);
        assertEquals(1, notNeedToChange);
    }

    @Test
    public void testChangeNotOwnedPsku() {
        modelStorageServiceMock.putModels(
            ModelBuilder.newBuilder(PSKU_ID3, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SRC_BUSINESS_ID)
                .parameterValue(createShopSkuParam("other_sku_id"))
                .withSkuParentRelation(HID, PMODEL_ID1)
                .build(),
            ModelBuilder.newBuilder(PMODEL_ID1, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .supplierId(SRC_BUSINESS_ID)
                .withSkuRelations(HID, PSKU_ID3)
                .build()
        );
        PskuOwnershipChangeService pskuOwnershipChangeServiceLocal =
            new PskuOwnershipChangeService(dao, modelStorageHelper, Mockito.mock(ShopBusinessDao.class),
                false);

        createPSkuMigrationInfo(PSKU_ID3, PskuChangeOwnershipProcessStatus.READY_FOR_PROCESS);
        pskuOwnershipChangeServiceLocal.doChange();
        List<MigratedPskusChangeOwnershipInfo> infos = dao.fetchByPskuId(PSKU_ID3);

        Assertions.assertThat(infos.get(0)).extracting(MigratedPskusChangeOwnershipInfo::getProcessingStatus)
            .isEqualTo(PskuChangeOwnershipProcessStatus.PSKU_NOT_OWNED_BY_OFFER);
    }

    @Test
    public void testChangeOwnershipPskuOwnershipIgnored() {
        modelStorageServiceMock.putModels(
            ModelBuilder.newBuilder(PSKU_ID3, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SRC_BUSINESS_ID)
                .parameterValue(createShopSkuParam("other_sku_id"))
                .withSkuParentRelation(HID, PMODEL_ID1)
                .build(),
            ModelBuilder.newBuilder(PMODEL_ID1, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .supplierId(SRC_BUSINESS_ID)
                .withSkuRelations(HID, PSKU_ID3)
                .build()
        );
        createPSkuMigrationInfo(PSKU_ID3, PskuChangeOwnershipProcessStatus.READY_FOR_PROCESS);
        pskuOwnershipChangeService.doChange();
        List<MigratedPskusChangeOwnershipInfo> infos = dao.fetchByPskuId(PSKU_ID3);

        Assertions.assertThat(infos.get(0)).extracting(MigratedPskusChangeOwnershipInfo::getProcessingStatus)
            .isEqualTo(PskuChangeOwnershipProcessStatus.CHANGED);
    }

    private Long createPSkuMigrationInfo(
        Long pskuId,
        PskuChangeOwnershipProcessStatus status
    ) {
        MigratedPskusChangeOwnershipInfo changeOwnershipInfo = new MigratedPskusChangeOwnershipInfo();
        changeOwnershipInfo.setPskuId(pskuId);
        changeOwnershipInfo.setDeletingBusinessId(SRC_BUSINESS_ID);
        changeOwnershipInfo.setDeletingOfferId(SRC_OFFER_ID);
        changeOwnershipInfo.setAcceptingBusinessId(TARGET_BUSINESS_ID);
        changeOwnershipInfo.setAcceptingOfferId(TARGET_OFFER_ID);
        dao.insertWithProcessingStatus(changeOwnershipInfo, status);
        return changeOwnershipInfo.getId();
    }

    private void assertBusinessId(Long modelId, Long supplierId) {
        modelStorageServiceMock.getModels(HID, Collections.singletonList(modelId))
            .forEach(model -> assertEquals((long) supplierId, model.getSupplierId()));
    }
}
