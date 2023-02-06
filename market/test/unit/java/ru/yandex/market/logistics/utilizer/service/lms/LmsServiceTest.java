package ru.yandex.market.logistics.utilizer.service.lms;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.config.cache.LmsCacheProperties;
import ru.yandex.market.logistics.utilizer.domain.dto.WarehouseDto;

import static org.mockito.ArgumentMatchers.any;

class LmsServiceTest extends SoftAssertionSupport {

    private static final Set<Long> WAREHOUSE_IDS = Set.of(171L, 172L);
    private static final WarehouseDto WAREHOUSE_1 = getWarehouseDto(172L, "Яндекс.Маркет (Софьино)");
    private static final WarehouseDto WAREHOUSE_2 = getWarehouseDto(171L, "Яндекс.Маркет (Томилино)");
    private static final List<PartnerResponse> LMS_RESPONSE = List.of(
            getPartnerResponse(WAREHOUSE_1),
            getPartnerResponse(WAREHOUSE_2)
    );

    private LmsService lmsService;
    private LMSClient lmsClient;

    @BeforeEach
    public void init() {
        lmsClient = Mockito.mock(LMSClient.class);
        LmsCacheProperties properties = new LmsCacheProperties();
        properties.setWarehouseCacheDuration(Duration.of(1, ChronoUnit.SECONDS));
        lmsService = new LmsService(lmsClient, properties);
    }

    @AfterEach
    public void after() {
        lmsService.invalidateCache();
    }

    @Test
    public void returnPreviouslyCachedValueIfFailedToComputeNewOneForGetWarehouses() throws Exception {
        Mockito.when(lmsClient.searchPartners(any()))
                .thenReturn(LMS_RESPONSE)
                .thenThrow(new RuntimeException("Connection timeout"));

        List<WarehouseDto> warehouses = lmsService.getWarehouses(WAREHOUSE_IDS);
        softly.assertThat(warehouses).isEqualTo(List.of(WAREHOUSE_1, WAREHOUSE_2));
        Thread.sleep(1001);
        warehouses = lmsService.getWarehouses(WAREHOUSE_IDS);
        softly.assertThat(warehouses).isEqualTo(List.of(WAREHOUSE_1, WAREHOUSE_2));
        Mockito.verify(lmsClient, Mockito.times(2)).searchPartners(any());
    }

    @Test
    public void secondRequestCachedForGetWarehouses() {
        Mockito.when(lmsClient.searchPartners(any())).thenReturn(LMS_RESPONSE);

        List<WarehouseDto> warehouses = lmsService.getWarehouses(WAREHOUSE_IDS);
        softly.assertThat(warehouses).isEqualTo(List.of(WAREHOUSE_1, WAREHOUSE_2));

        warehouses = lmsService.getWarehouses(WAREHOUSE_IDS);
        softly.assertThat(warehouses).isEqualTo(List.of(WAREHOUSE_1, WAREHOUSE_2));

        Mockito.verify(lmsClient).searchPartners(any());
    }

    @Test
    public void requestAfterCacheInvalidationExecutedForGetWarehouses() {
        Mockito.when(lmsClient.searchPartners(any()))
                .thenReturn(LMS_RESPONSE)
                .thenReturn(LMS_RESPONSE);

        List<WarehouseDto> warehouses = lmsService.getWarehouses(WAREHOUSE_IDS);
        softly.assertThat(warehouses).isEqualTo(List.of(WAREHOUSE_1, WAREHOUSE_2));

        lmsService.invalidateCache();
        warehouses = lmsService.getWarehouses(WAREHOUSE_IDS);
        softly.assertThat(warehouses).isEqualTo(List.of(WAREHOUSE_1, WAREHOUSE_2));

        Mockito.verify(lmsClient, Mockito.times(2)).searchPartners(any());
    }

    private static PartnerResponse getPartnerResponse(WarehouseDto warehouseDto) {
        return PartnerResponse.newBuilder()
                .id(warehouseDto.getWarehouseId())
                .readableName(warehouseDto.getWarehouseName())
                .build();
    }

    private static WarehouseDto getWarehouseDto(long warehouseId, String warehouseName) {
        return WarehouseDto.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouseName)
                .build();
    }
}
