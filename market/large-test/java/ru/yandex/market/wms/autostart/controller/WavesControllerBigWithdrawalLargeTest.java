package ru.yandex.market.wms.autostart.controller;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.ordermanagement.client.OrderManagementClient;
import ru.yandex.market.wms.replenishment.client.ReplenishmentClient;
import ru.yandex.market.wms.replenishment.core.dto.stock.SkuStockWithAnyHold;
import ru.yandex.market.wms.replenishment.core.dto.stock.StocksWithHoldsRequest;
import ru.yandex.market.wms.replenishment.core.dto.stock.StocksWithHoldsResponse;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetups({
        @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/configs.xml"),
        @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/locations.xml"),
        @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/sku.xml"),
        @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/holds.xml")
})
public class WavesControllerBigWithdrawalLargeTest extends TestcontainersConfiguration {
    @MockBean
    @Autowired
    private ReplenishmentClient replenishmentClient;

    @MockBean
    @Autowired
    private OrderManagementClient orderManagementClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(replenishmentClient, orderManagementClient);
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-defect-orders-before.xml",
            type = INSERT)
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-defect-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateBigWithdrawalForDefectStock() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/wavesControllerLargeTest/request/create-big-withdrawal-wave.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-expired-orders-before.xml",
            type = INSERT)
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-expired-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateBigWithdrawalForExpiredStock() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/wavesControllerLargeTest/request/create-big-withdrawal-wave.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-surplus-orders-before.xml",
            type = INSERT)
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-surplus-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateBigWithdrawalForSurplus() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/wavesControllerLargeTest/request/create-big-withdrawal-wave.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-fit-orders-before.xml",
            type = INSERT)
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/big-withdrawal-fit-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateBigWithdrawalForFitStock() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/wavesControllerLargeTest/request/create-big-withdrawal-wave.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-fit-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-fit-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalWaveForOrdersOfOutboundFitType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-fit-partial-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-fit-partial-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalWaveFitPartialWithCorrection() throws Exception {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request = new StocksWithHoldsRequest(Set.of(sku), Set.of()); //OUTBOUND_FIT
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 2, 6)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "testcontainers/wavesControllerLargeTest/response/reserve-partial-wd-wrong-holds.json")));

        Mockito.verify(replenishmentClient, Mockito.times(1)).getStocks(request);
        Mockito.verify(orderManagementClient, Mockito.times(1)).shortage(Mockito.any());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-fit-partial-only-pick-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-fit-partial-only-pick-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalWaveFitPartialWithFullCorrection() throws Exception {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request = new StocksWithHoldsRequest(Set.of(sku), Set.of()); //OUTBOUND_FIT
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 0, 0)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "testcontainers/wavesControllerLargeTest/response/reserve-partial-wd-wrong-holds-all-corrected.json")));

        Mockito.verify(replenishmentClient, Mockito.times(1)).getStocks(request);
        Mockito.verify(orderManagementClient, Mockito.times(1)).shortage(Mockito.any());
    }



    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-defect-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-defect-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalWaveForOrdersOfOutboundDefectType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-expired-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-expired-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalWaveForOrdersOfOutboundExpiredType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-surplus-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-surplus-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalWaveForOrdersOfOutboundSurplusType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-fit-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-fit-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReleaseBigWithdrawalWaveForOrdersOfOutboundFitType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-defect-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-defect-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReleaseBigWithdrawalWaveForOrdersOfOutboundDefectType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-expired-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-expired-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReleaseBigWithdrawalWaveForOrdersOfOutboundExpiredType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-surplus-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "big-withdrawal-surplus-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReleaseBigWithdrawalWaveForOrdersOfOutboundSurplusType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/unreserve/before-fully-reserved-wave.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/unreserve/after-fully-reserved-wave.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCancelReservationFullyReservedWave() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/unreserve/WAVE-001"));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/unreserve/before-partially-reserved-wave.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/unreserve/after-partially-reserved-wave.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCancelReservationPartiallyReservedWave() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/unreserve/WAVE-001"));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
            "before-reserve-after-partial-reserve.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "after-reserve-after-partial-reserve.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveAfterPartialReserve() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "before-reserve-after-release.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-release-wave/" +
                    "after-reserve-after-release.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveAfterRelease() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/reserve/WAVE-001"));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-use-cell-priority-planner-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/big-withdrawal-reserve-wave/" +
                    "big-withdrawal-use-cell-priority-planner-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalWaveUseCellPriorityPlanner() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk());
    }
}
