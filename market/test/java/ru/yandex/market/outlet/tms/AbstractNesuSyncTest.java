package ru.yandex.market.outlet.tms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.CreateShopPickupPointMetaResponse;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointMetaDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointMetaRequest;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "SyncOutletsWithNesuTest.common.before.csv")
abstract class AbstractNesuSyncTest extends FunctionalTest {

    private static final long PARTNER_ID = 51;

    @Autowired
    protected NesuClient nesuClient;


    @Test
    void testNothingToDo() {
        runProcess();
        verifyNoInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(
            before = "SyncOutletsWithNesuTest.testDelete.before.csv",
            after = "SyncOutletsWithNesuTest.testDelete.after.csv"
    )
    void testDeleteOperation() {
        runProcess();
        verify(nesuClient, only()).deleteShopPickupPointMeta(eq(PARTNER_ID), eq(111L));
        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(
            before = "SyncOutletsWithNesuTest.testCreate.before.csv",
            after = "SyncOutletsWithNesuTest.testCreate.after.csv"
    )
    void testCreateOperation() {
        mockNesuCreateOperation();

        // Есть данные для синхронизации (создание точки и тарифа)
        runProcess();
        verify(nesuClient, only()).createShopPickupPointMeta(eq(PARTNER_ID), any());
        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(
            before = "SyncOutletsWithNesuTest.testUpdate.before.csv",
            after = "SyncOutletsWithNesuTest.testUpdate.after.csv"
    )
    void testUpdateOperation() {
        final long nesuId = mockNesuUpdateOperation();

        // Есть данные для синхронизации (обновление точки и тарифа)
        runProcess();
        ArgumentCaptor<ShopPickupPointMetaRequest> requestArgumentCaptor =
                ArgumentCaptor.forClass(ShopPickupPointMetaRequest.class);
        verify(nesuClient, only()).updateShopPickupPointMeta(
                eq(PARTNER_ID),
                eq(nesuId),
                requestArgumentCaptor.capture()
        );
        Assertions.assertEquals(10, requestArgumentCaptor.getValue().getPickupPoint().getStoragePeriod());
        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(
            before = "SyncOutletsWithNesuTest.testResurrectionDelete.before.csv",
            after = "SyncOutletsWithNesuTest.testDelete.after.csv"
    )
    void testResurrectionDelete() {
        mockNesuCreateOperation();

        // В первую попытку удаления пришло понимает что идентификатор Nesu отпутствует
        // Необходимо пересоздать точку в Nesu, чтобы получить ID
        runProcess();
        verify(nesuClient, only()).createShopPickupPointMeta(eq(PARTNER_ID), any());
        verifyNoMoreInteractions(nesuClient);

        reset(nesuClient);

        // Вторая попытка удаления корректна, так как получили Nesu ID
        runProcess();
        verify(nesuClient, only()).deleteShopPickupPointMeta(eq(PARTNER_ID), eq(111L));
        verifyNoMoreInteractions(nesuClient);
    }

    protected abstract void runProcess();


    private void mockNesuCreateOperation() {
        final long nesuId = 111L;
        final CreateShopPickupPointMetaResponse meta = new CreateShopPickupPointMetaResponse();
        meta.setId(nesuId);
        when(nesuClient.createShopPickupPointMeta(eq(PARTNER_ID), any())).thenReturn(meta);
    }

    private long mockNesuUpdateOperation() {
        final long nesuId = 111L;
        final ShopPickupPointMetaDto meta = new ShopPickupPointMetaDto();
        meta.setId(nesuId);
        when(nesuClient.updateShopPickupPointMeta(eq(PARTNER_ID), eq(nesuId), any())).thenReturn(meta);
        return nesuId;
    }

}
