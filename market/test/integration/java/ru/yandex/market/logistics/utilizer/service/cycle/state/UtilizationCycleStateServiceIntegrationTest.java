package ru.yandex.market.logistics.utilizer.service.cycle.state;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.dto.WarehouseDto;
import ru.yandex.market.logistics.utilizer.domain.dto.WarehouseStocksDto;
import ru.yandex.market.logistics.utilizer.domain.enums.StockType;
import ru.yandex.market.logistics.utilizer.service.cycle.UtilizationCycleStateService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UtilizationCycleStateServiceIntegrationTest extends AbstractContextualTest {

    private static final long VENDOR_ID = 100500L;

    private static final WarehouseDto WAREHOUSE_1 = getWarehouseDto(172L, "Яндекс.Маркет (Софьино)");
    private static final WarehouseDto WAREHOUSE_2 = getWarehouseDto(171L, "Яндекс.Маркет (Томилино)");

    @Autowired
    private UtilizationCycleStateService utilizationCycleStateService;
    @Autowired
    private LMSClient lmsClient;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/empty.xml")
    @ExpectedDatabase(value = "classpath:fixtures/empty.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void getEmptyUtilizationStocks() {
        List<WarehouseStocksDto> actual = utilizationCycleStateService.getUtilizationStocks(VENDOR_ID);

        softly.assertThat(actual).isEmpty();
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/state/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/state/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void getUtilizationStocksForNotActiveCycle() {
        List<PartnerResponse> lmsResponse = List.of(
                getPartnerResponse(WAREHOUSE_1),
                getPartnerResponse(WAREHOUSE_2)
        );

        when(lmsClient.searchPartners(any())).thenReturn(lmsResponse);

        List<WarehouseStocksDto> actual = utilizationCycleStateService.getUtilizationStocks(VENDOR_ID);

        softly.assertThat(actual).isEmpty();
        verifyNoMoreInteractions(lmsClient);
    }

    /**
     * В базе утилизатора есть данные о складе для которого ничего не возвращается из LMS
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/state/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/state/3/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void getUtilizationStocksForUnknownWarehouses() {
        List<PartnerResponse> lmsResponse = List.of(
                getPartnerResponse(WAREHOUSE_1)
        );

        when(lmsClient.searchPartners(any())).thenReturn(lmsResponse);

        assertThrows(IllegalStateException.class,
                () -> utilizationCycleStateService.getUtilizationStocks(VENDOR_ID),
                "Unknown warehouse ids in LMS: [171]"
        );

        verify(lmsClient).searchPartners(any());
    }

    /**
     * В базе утилизатора нет данных о складе для которого что-то вернулось из LMS
     * Игнорируем этот склад
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/state/4/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/state/4/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void unexpectedReturnFromLms() {
        List<PartnerResponse> lmsResponse = List.of(
                getPartnerResponse(WAREHOUSE_1),
                getPartnerResponse(WAREHOUSE_2)
        );

        when(lmsClient.searchPartners(any())).thenReturn(lmsResponse);

        List<WarehouseStocksDto> actual = utilizationCycleStateService.getUtilizationStocks(VENDOR_ID);
        List<WarehouseStocksDto> expected = List.of(
                getWarehouseStocksDto(WAREHOUSE_1, Set.of(StockType.DEFECT, StockType.EXPIRED))
        );

        softly.assertThat(actual).isEqualTo(expected);
        verify(lmsClient).searchPartners(any());
    }

    /**
     * Есть разные типы стоков для разных складов
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/state/5/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/state/5/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void getUtilizationStocksForDifferentWarehouses() {
        List<PartnerResponse> lmsResponse = List.of(
                getPartnerResponse(WAREHOUSE_1),
                getPartnerResponse(WAREHOUSE_2)
        );

        when(lmsClient.searchPartners(any())).thenReturn(lmsResponse);

        List<WarehouseStocksDto> actual = utilizationCycleStateService.getUtilizationStocks(VENDOR_ID);
        List<WarehouseStocksDto> expected = List.of(
                getWarehouseStocksDto(WAREHOUSE_1, Set.of(StockType.DEFECT, StockType.EXPIRED)),
                getWarehouseStocksDto(WAREHOUSE_2, Set.of(StockType.EXPIRED))
        );

        softly.assertThat(actual).isEqualTo(expected);
        verify(lmsClient).searchPartners(any());
    }

    /**
     * Есть UtilizationCycleState в DEPRECATED статусе, их игнорируем при поиске
     * Данные возвращаются только для ACTIVE
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/state/6/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/state/6/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void getUtilizationStocksForActiveAndDeprecatedStates() {
        List<PartnerResponse> lmsResponse = List.of(
                getPartnerResponse(WAREHOUSE_2)
        );

        when(lmsClient.searchPartners(any())).thenReturn(lmsResponse);

        List<WarehouseStocksDto> actual = utilizationCycleStateService.getUtilizationStocks(VENDOR_ID);
        List<WarehouseStocksDto> expected = List.of(
                getWarehouseStocksDto(WAREHOUSE_2, Set.of(StockType.EXPIRED))
        );

        softly.assertThat(actual).isEqualTo(expected);
        verify(lmsClient).searchPartners(any());
    }

    private WarehouseStocksDto getWarehouseStocksDto(WarehouseDto warehouseDto,
                                                     Set<StockType> stockTypes) {
        return WarehouseStocksDto.builder()
                .warehouse(warehouseDto)
                .stockTypes(stockTypes)
                .build();
    }

    private static WarehouseDto getWarehouseDto(long warehouseId, String warehouseName) {
        return WarehouseDto.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouseName)
                .build();
    }

    private PartnerResponse getPartnerResponse(WarehouseDto warehouseDto) {
        return PartnerResponse.newBuilder()
                .id(warehouseDto.getWarehouseId())
                .readableName(warehouseDto.getWarehouseName())
                .build();
    }
}
