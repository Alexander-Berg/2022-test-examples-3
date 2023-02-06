package ru.yandex.market.logistics.utilizer.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.dto.WarehouseDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.utilizer.util.JsonAssertUtils.assertFileNonExtensibleEquals;

public class UtilizationCycleStateControllerTest extends AbstractContextualTest {

    private static final WarehouseDto WAREHOUSE_1 = getWarehouseDto(172L, "Яндекс.Маркет (Софьино)");
    private static final WarehouseDto WAREHOUSE_2 = getWarehouseDto(171L, "Яндекс.Маркет (Томилино)");

    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/warehouse-stock-types/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/warehouse-stock-types/1/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStockWithoutVendorId() throws Exception {
        mockMvc.perform(get("/utilization-cycle/warehouse-stock-types"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/warehouse-stock-types/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/warehouse-stock-types/2/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getUtilizationStocksTypesWithDifferentWarehouses() throws Exception {
        List<PartnerResponse> lmsResponse = List.of(
                getPartnerResponse(WAREHOUSE_1),
                getPartnerResponse(WAREHOUSE_2)
        );

        when(lmsClient.searchPartners(any())).thenReturn(lmsResponse);

        ResultActions perform = mockMvc.perform(get("/utilization-cycle/100500/warehouse-stock-types"));
        String actualResult = perform
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "fixtures/controller/utilization-cycle/warehouse-stock-types/2/response.json",
                actualResult);

        verify(lmsClient).searchPartners(any());
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/warehouse-stock-types/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/warehouse-stock-types/3/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getUtilizationStocksTypesReturnsEmptyForUnknownVendor() throws Exception {

        ResultActions perform = mockMvc.perform(get("/utilization-cycle/123/warehouse-stock-types"));
        String actualResult = perform
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "fixtures/empty.json",
                actualResult);

        verifyNoMoreInteractions(lmsClient);
    }

    /**
     * stocks-for-utilization-document - 1
     * Возвращается ошибка, если не передали обязательные параметры
     */
    @Test
    @DatabaseSetup("classpath:fixtures/empty.xml")
    @ExpectedDatabase(value = "classpath:fixtures/empty.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStocksForUtilizationWithIncorrectRequest() throws Exception {
        mockMvc.perform(get("/utilization-cycle/123/stocks-for-utilization-document"))
                .andExpect(status().isBadRequest());
    }

    /**
     * stocks-for-utilization-document - 2
     * Поставщик неизвестен (Для запрошенного постащика нет UtilizationCycleState)
     * Возвращается ответ без URL документа
     */
    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/2/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStocksForUtilizationReturnsEmptyForUnknownVendor() throws Exception {

        String actualResult = mockMvc.perform(get("/utilization-cycle/123/stocks-for-utilization-document")
                .param("warehouseId", "172")
                .param("stockType", "DEFECT")
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "fixtures/controller/utilization-cycle/stocks-for-utilization-document/2/response.json",
                actualResult);
    }

    /**
     * stocks-for-utilization-document - 3
     * Состояние для тройки "поставщик-склад-тип стока" - ACTIVE
     * Возвращается полноценный ответ
     */
    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/3/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStocksForUtilizationReturnsSuccessful() throws Exception {
        String actualResult = mockMvc.perform(get("/utilization-cycle/100500/stocks-for-utilization-document")
                .param("warehouseId", "172")
                .param("stockType", "DEFECT")
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "fixtures/controller/utilization-cycle/stocks-for-utilization-document/3/response.json",
                actualResult);
    }

    /**
     * stocks-for-utilization-document - 4
     * Состояние для тройки "поставщик-склад-тип стока" - DEPRECATED
     * Возвращается ответ без URL документа
     */
    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/4/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/4/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStocksForUtilizationReturnsEmptyUrlForDeprecatedState() throws Exception {
        String actualResult = mockMvc.perform(get("/utilization-cycle/100500/stocks-for-utilization-document")
                .param("warehouseId", "172")
                .param("stockType", "DEFECT")
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "fixtures/controller/utilization-cycle/stocks-for-utilization-document/4/response.json",
                actualResult);
    }

    /**
     * stocks-for-utilization-document - 5
     * Для запрошенного склада нет UtilizationCycleState
     * Возвращается ответ без URL документа
     */
    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/5/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/5/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStocksForUtilizationReturnsEmptyUrlForUnknownWarehouse() throws Exception {
        String actualResult = mockMvc.perform(get("/utilization-cycle/100500/stocks-for-utilization-document")
                .param("warehouseId", "171")
                .param("stockType", "DEFECT")
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "fixtures/controller/utilization-cycle/stocks-for-utilization-document/5/response.json",
                actualResult);
    }

    /**
     * stocks-for-utilization-document - 6
     * Для запрошенного типа стока нет UtilizationCycleState
     * Возвращается ответ без URL документа
     */
    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/6/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/6/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStocksForUtilizationReturnsEmptyUrlForUnknownStockType() throws Exception {
        String actualResult = mockMvc.perform(get("/utilization-cycle/100500/stocks-for-utilization-document")
                .param("warehouseId", "172")
                .param("stockType", "SURPLUS")
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "fixtures/controller/utilization-cycle/stocks-for-utilization-document/6/response.json",
                actualResult);
    }

    /**
     * stocks-for-utilization-document - 7
     * Передали некорректный stockType
     * Возвращается ошибка
     */
    @Test
    @DatabaseSetup("classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/6/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/utilization-cycle/stocks-for-utilization-document/6/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getStocksForUtilizationReturnsErrorForIncorrectStockType() throws Exception {
        mockMvc.perform(get("/utilization-cycle/123/stocks-for-utilization-document")
                .param("warehouseId", "172")
                .param("stockType", "IncorrectStockType")
        )
                .andExpect(status().isBadRequest());
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
