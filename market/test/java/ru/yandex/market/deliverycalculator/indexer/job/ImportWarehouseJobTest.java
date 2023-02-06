package ru.yandex.market.deliverycalculator.indexer.job;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class ImportWarehouseJobTest extends FunctionalTest {

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private ImportWarehouseJob tested;

    @BeforeEach
    void setUpConfigurations() {
        doReturn(mockLmsPartnerResponse()).when(lmsClient).searchPartners(any());
        doReturn(mockLmsWarehouseResponse()).when(lmsClient).getLogisticsPoints(any());
    }

    /**
     * 2 скада создаются.
     * 1 склад не изменяется.
     * 2 скалада деактивируются.
     */
    @Test
    @DisplayName("Успешный импорт складов.")
    @DbUnitDataSet(
            before = {
                    "warehouse/ImportWarehouseJobTest.testImportWarehouses.before.csv",
                    "regions.csv",
                    "preferable_regions.csv"
            },
            after = "warehouse/ImportWarehouseJobTest.testImportWarehouses.after.csv"
    )
    void testImportWarehouses() {
        tested.doJob(null);
    }

    @Test
    @DisplayName("Успешный импорт складов, никакие склады не обновляются.")
    @DbUnitDataSet(
            before = {
                    "warehouse/ImportWarehouseJobTest.testImportWarehouses.after.csv",
                    "regions.csv",
                    "preferable_regions.csv"
            },
            after = "warehouse/ImportWarehouseJobTest.testImportWarehouses.after.csv"
    )
    void testImportWarehouses_repeatCall() {
        tested.doJob(null);
    }

    private static List<LogisticsPointResponse> mockLmsWarehouseResponse() {
        LogisticsPointResponse point1 = LogisticsPointResponse.newBuilder()
                .active(true)
                .id(1234L)
                .partnerId(1L)
                .name("W1")
                .address(Address.newBuilder()
                        .locationId(98585)
                        .settlement("STT")
                        .postCode("123123")
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.ONE)
                        .street("")
                        .house("")
                        .housing("")
                        .building("")
                        .apartment("")
                        .comment("")
                        .region("")
                        .addressString("")
                        .shortAddressString("")
                        .build())
                .build();
        LogisticsPointResponse point2 = LogisticsPointResponse.newBuilder()
                .active(true)
                .id(2345L)
                .partnerId(2L)
                .name("W2")
                .address(Address.newBuilder()
                        .locationId(213)
                        .settlement("STT")
                        .postCode("123123")
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.ONE)
                        .street("")
                        .house("")
                        .housing("")
                        .building("")
                        .apartment("")
                        .comment("")
                        .region("")
                        .addressString("")
                        .shortAddressString("")
                        .build())
                .build();
        LogisticsPointResponse point3 = LogisticsPointResponse.newBuilder()
                .active(true)
                .id(3456L)
                .partnerId(3L)
                .name("W3")
                .address(Address.newBuilder()
                        .locationId(10776)
                        .settlement("STT")
                        .postCode("123123")
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.ONE)
                        .street("")
                        .house("")
                        .housing("")
                        .building("")
                        .apartment("")
                        .comment("")
                        .region("")
                        .addressString("")
                        .shortAddressString("")
                        .build())
                .build();
        LogisticsPointResponse point4 = LogisticsPointResponse.newBuilder()
                .active(true)
                .id(4567L)
                .partnerId(4L)
                .address(Address.newBuilder()
                        .locationId(158606)
                        .settlement("STT")
                        .postCode("123123")
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.ONE)
                        .street("")
                        .house("")
                        .housing("")
                        .building("")
                        .apartment("")
                        .comment("")
                        .region("")
                        .addressString("")
                        .shortAddressString("")
                        .build())
                .build();
        LogisticsPointResponse point7 = LogisticsPointResponse.newBuilder()
                .active(true)
                .id(7890L)
                .partnerId(7L)
                .name("W7")
                .address(Address.newBuilder()
                        .locationId(10776)
                        .settlement("STT")
                        .postCode("123123")
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.ONE)
                        .street("")
                        .house("")
                        .housing("")
                        .building("")
                        .apartment("")
                        .comment("")
                        .region("")
                        .addressString("")
                        .shortAddressString("")
                        .build())
                .build();

        return List.of(point1, point2, point3, point4, point7);
    }

    private static List<PartnerResponse> mockLmsPartnerResponse() {
        PartnerResponse partner1 = PartnerResponse.newBuilder()
                .id(1L)
                .build();
        PartnerResponse partner2 = PartnerResponse.newBuilder()
                .id(2L)
                .build();
        PartnerResponse partner3 = PartnerResponse.newBuilder()
                .id(3L)
                .build();
        PartnerResponse partner4 = PartnerResponse.newBuilder()
                .id(4L)
                .build();
        PartnerResponse partner5 = PartnerResponse.newBuilder()
                .id(7L)
                .build();
        PartnerResponse partner9 = PartnerResponse.newBuilder()
                .id(9L)
                .build();

        return List.of(partner1, partner2, partner3, partner4, partner5, partner9);
    }
}
