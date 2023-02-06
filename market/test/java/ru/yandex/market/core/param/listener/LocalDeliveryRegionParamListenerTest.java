package ru.yandex.market.core.param.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.geobase.model.RegionType;
import ru.yandex.market.core.param.model.NumberParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.shop.UpdateShopDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "LocalDeliveryRegionParamListenerTest.before.csv")
public class LocalDeliveryRegionParamListenerTest extends FunctionalTest {

    @Autowired
    private RegionService regionService;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private LocalDeliveryRegionParamListener localDeliveryRegionParamListener;

    @Test
    @DbUnitDataSet(
            after = "LocalDeliveryRegionParamListenerTest.create.Dbs.after.csv"
    )
    @DisplayName("[DBS] Создание локального региона доставки для партнёра")
    void testCreateLocalDeliveryRegionDbs() {
        long shopId = 2L;
        int regionId = 1;

        Region region = new Region(1L, "Moscow", 3L, RegionType.CITY);
        doReturn(region).when(regionService).getRegion(any(Long.class));

        NumberParamValue value = new NumberParamValue(ParamType.LOCAL_DELIVERY_REGION, shopId, regionId);
        localDeliveryRegionParamListener.onCreate(value, 100500L);

        verify(nesuClient, never()).updateShop(anyLong(), any(UpdateShopDto.class));
    }

    @Test
    @DisplayName("[DBS] Изменение локального региона доставки для партнёра")
    void testUpdateLocalDeliveryRegionDbs() {
        long shopId = 2L;
        int oldRegionId = 1;
        Integer newRegionId = 1;

        Region region = new Region(1L, "Moscow", 3L, RegionType.CITY);
        doReturn(region).when(regionService).getRegion(any(Long.class));

        NumberParamValue oldValue = new NumberParamValue(ParamType.LOCAL_DELIVERY_REGION, shopId, oldRegionId);
        NumberParamValue newValue = new NumberParamValue(ParamType.LOCAL_DELIVERY_REGION, shopId, newRegionId);
        localDeliveryRegionParamListener.onValueChanged(newValue, oldValue, 100500L);

        ArgumentCaptor<UpdateShopDto> captor = ArgumentCaptor.forClass(UpdateShopDto.class);
        verify(nesuClient, times(1)).updateShop(anyLong(), captor.capture());

        UpdateShopDto updateRequest = captor.getValue();
        assertEquals(newRegionId, updateRequest.getLocalDeliveryRegion());
    }

    @Test
    @DbUnitDataSet(
            after = "LocalDeliveryRegionParamListenerTest.create.Fbs.after.csv"
    )
    @DisplayName("[FBS] Создание локального региона доставки для партнёра")
    void testCreateLocalDeliveryRegionFbs() {
        Region region = new Region(1L, "Moscow", 3L, RegionType.CITY);
        doReturn(region).when(regionService).getRegion(any(Long.class));

        NumberParamValue value = new NumberParamValue(ParamType.LOCAL_DELIVERY_REGION, 10L, 1);
        localDeliveryRegionParamListener.onCreate(value, 100500L);
    }
}
