package ru.yandex.market.ff.service.implementation.calendaring;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.service.CalendaringClientCachingService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.RegionTimezoneService;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;


class CalendaringClientCachingServiceImplTest extends SoftAssertionSupport {


    private final CalendaringServiceClientApi calendaringServiceClient =
            Mockito.mock(CalendaringServiceClientApi.class);
    private final RegionTimezoneService regionTimezoneService = Mockito.mock(RegionTimezoneService.class);
    private final ConcreteEnvironmentParamService paramService = Mockito.mock(ConcreteEnvironmentParamService.class);

    private final CalendaringClientCachingServiceImpl service = new CalendaringClientCachingServiceImpl(
            calendaringServiceClient,
            regionTimezoneService,
            paramService,
            300
    );

    @Test
    void testDefault() {
        long warehouseId = 1L;
        assertEquals(CalendaringClientCachingService.DEFAULT_TIMEZONE, service.getZoneId(warehouseId));
        Mockito.verify(calendaringServiceClient).getTimezoneByWarehouseId(warehouseId);
        Mockito.verify(regionTimezoneService).findByRegionId(warehouseId);
    }

    @Test
    void testLuckyPass() {
        long warehouseId = 300L;
        ZoneId zoneId = ZoneId.of("Asia/Yekaterinburg");
        when(calendaringServiceClient.getTimezoneByWarehouseId(warehouseId)).thenReturn(zoneId);
        assertEquals(zoneId, service.getZoneId(warehouseId));

        Mockito.verify(calendaringServiceClient).getTimezoneByWarehouseId(warehouseId);
        Mockito.verify(regionTimezoneService, never()).findByRegionId(warehouseId);
        Mockito.verify(regionTimezoneService).saveIfNotExist(any());
    }

    @Test
    void testGetFromTableZoneId() {
        long warehouseId = 1L;
        when(paramService.loadWarehouseTimezoneFromTable()).thenReturn(true);
        assertEquals(CalendaringClientCachingService.DEFAULT_TIMEZONE, service.getZoneId(warehouseId));
        Mockito.verify(calendaringServiceClient, never()).getTimezoneByWarehouseId(anyLong());
        Mockito.verify(regionTimezoneService).findByRegionId(warehouseId);
    }
}
