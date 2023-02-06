package ru.yandex.market.core.order.shipment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.any;

@DbUnitDataSet(before = "regions.csv")
class FirstMileWarehouseInfoServiceImplTest extends FunctionalTest {
    private static final Long WH_ID = 12L;
    private static final Long WH_ID_2 = 14L;
    private static final Long WH_ID_3 = 15L;

    @Autowired
    private RegionService regionService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private FirstMileWarehouseInfoService firstMileWarehouseInfoService;

    @Test
    void testGetCorrectFederalArea() {
        mockLMSClient();
        String actualFederalArea = firstMileWarehouseInfoService.getWarehouseFederalAreaInfo(WH_ID);
        Assertions.assertThat(actualFederalArea).isEqualTo("Центральный федеральный округ");
    }

    @Test
    void testGetCorrectBatchFederalAreas() {
        mockBulkLMSClient();
        Set<Long> whSet = Set.of(WH_ID, WH_ID_2, WH_ID_3);
        Map<Long, String> actualWarehousesFederalAreaInfo =
                firstMileWarehouseInfoService.getWarehousesFederalAreaInfo(whSet);
        Assertions.assertThat(actualWarehousesFederalAreaInfo.keySet()).containsExactlyInAnyOrderElementsOf(whSet);
        Assertions.assertThat(actualWarehousesFederalAreaInfo.get(WH_ID)).isEqualTo("Центральный федеральный округ");
        Assertions.assertThat(actualWarehousesFederalAreaInfo.get(WH_ID_2)).isEqualTo("Южный федеральный округ");
        Assertions.assertThat(actualWarehousesFederalAreaInfo.get(WH_ID_3)).isEqualTo("Центральный федеральный округ");
    }

    @Test
    void testEmptyDataFromLMS() {
        mockEmptyFromLMS();
        String actualFederalArea = firstMileWarehouseInfoService.getWarehouseFederalAreaInfo(WH_ID);
        Assertions.assertThat(actualFederalArea).isEqualTo("");
    }

    @Test
    void testErrorFromLMS() {
        mockErrorFromLMS();
        String actualFederalArea = firstMileWarehouseInfoService.getWarehouseFederalAreaInfo(WH_ID);
        Assertions.assertThat(actualFederalArea).isEqualTo("");

        Map<Long, String> warehousesFederalAreaInfo =
                firstMileWarehouseInfoService.getWarehousesFederalAreaInfo(Set.of(WH_ID_2, WH_ID_3));
        Assertions.assertThat(warehousesFederalAreaInfo).isEmpty();
    }

    private void mockLMSClient() {
        LogisticsPointResponse response = LogisticsPointResponse.newBuilder()
                .active(true)
                .partnerId(WH_ID)
                .address(Address.newBuilder()
                        .locationId(6)
                        .build())
                .build();
        Mockito.when(lmsClient.getLogisticsPoints(any())).thenReturn(List.of(response));
    }

    private void mockBulkLMSClient() {
        List<LogisticsPointResponse> logisticsPointResponses = List.of(LogisticsPointResponse.newBuilder()
                        .active(true)
                        .partnerId(WH_ID)
                        .address(Address.newBuilder()
                                .locationId(6)
                                .build())
                        .build(),
                LogisticsPointResponse.newBuilder()
                        .active(true)
                        .partnerId(WH_ID_2)
                        .address(Address.newBuilder()
                                .locationId(5)
                                .build())
                        .build(),
                LogisticsPointResponse.newBuilder()
                        .active(true)
                        .partnerId(WH_ID_3)
                        .address(Address.newBuilder()
                                .locationId(4)
                                .build())
                        .build()

        );
        Mockito.when(lmsClient.getLogisticsPoints(any())).thenReturn(logisticsPointResponses);
    }

    private void mockEmptyFromLMS() {
        Mockito.when(lmsClient.getLogisticsPoints(any())).thenReturn(Collections.emptyList());
    }

    private void mockErrorFromLMS() {
        Mockito.when(lmsClient.getLogisticsPoints(any())).thenThrow(new RuntimeException("LMS is unavailable"));
    }
}
