package ru.yandex.market.wms.autostart.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.wms.autostart.core.model.request.wave.DeleteOrdersRequest;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
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
public class WavesControllerLargeTest extends TestcontainersConfiguration {

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/happyPath/standard-orders-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/standard-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesForOrdersOfStandardTypeHappyPass() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-fit-orders-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-fit-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesForOrdersOfOutboundFitTypeHappyPass() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-defect-orders-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-defect-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesForOrdersOfOutboundDefectTypeHappyPass() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-expired-orders-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-expired-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesForOrdersOfOutboundExpiredTypeHappyPass() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-surplus-orders-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/happyPath/outbound-surplus-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesForOrdersOfOutboundSurplusTypeHappyPass() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/testcontainers/wavesControllerLargeTest/db/with-fashion-and-non-fashion-flow-type/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/with-fashion-and-non-fashion-flow-type/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesForOrdersWithFashionAndNonFashionFlowType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/testcontainers/wavesControllerLargeTest/db/different-type-orders/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/different-type-orders/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCreateWavesDifferentTypeOrders() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "testcontainers/wavesControllerLargeTest/response/create-wave-diff-order-type.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/wavesControllerLargeTest/db/multi-and-mono-carriers/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/multi-and-mono-carriers/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCreateWavesMultiAndMonoCarrierOrders() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "testcontainers/wavesControllerLargeTest/response/create-wave-multi-mono-carriers.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/wavesControllerLargeTest/db/no-handlers-for-type/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/no-handlers-for-type/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesNoHandlersForType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "testcontainers/wavesControllerLargeTest/response/create-wave-no-handlers-for-type.json")
                        )
                );
    }

    @Test
    @DatabaseSetup("/testcontainers/wavesControllerLargeTest/db/order-doesnt-exist/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/order-doesnt-exist/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesOrderDoesNotExist() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "testcontainers/wavesControllerLargeTest/response/create-wave-order-doesnt-exists.json"
                        ))
                );
    }

    @Test
    @DatabaseSetup("/testcontainers/wavesControllerLargeTest/db/order-already-in-wave/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/order-already-in-wave/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCreateWavesOrderAlreadyInWave() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "testcontainers/wavesControllerLargeTest/response/create-wave-order-already-in-wave.json"
                        ))
                );
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/multi-buildings/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/multi-buildings/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldSetBuildingToWave() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/start-wave/without-sort-station/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/start-wave/without-sort-station/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWavesWithoutSorterStation() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/start-waves-without-sorter-station.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/singles/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/singles/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldCreateSingleWaveTypeIfAllSingles() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-single-orders/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-single-orders/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldCreateWithdrawalWaveTypeIfAllOrdersAreSinglesButOfWithdrawalType() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/start-wave/happy-pass/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/start-wave/happy-pass/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWaveForOrdersOfStandardType() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/start-wave/partially-reserved/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/start-wave/partially-reserved/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWaveAfterPartialReservationAndNonReservedOrdersRemoval() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        reservationResult.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/db/start-wave/partially-reserved/" +
                                "reservation-response.json")));

        ObjectMapper mapper = new ObjectMapper();
        String deleteRequest = mapper.writeValueAsString(DeleteOrdersRequest.builder()
                .orderIds(List.of(DeleteOrdersRequest.OrderId.builder().orderId("ORDER-002").build()))
                .build());

        ResultActions deleteResult = mockMvc.perform(post("/waves/WAVE-001/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(deleteRequest));
        deleteResult.andExpect(status().isOk());

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/immutable-state-nsqlconfig.xml",
            "/testcontainers/controller/waves/db/immutable-state-stations.xml",
            "/testcontainers/controller/waves/db/start-wave/disabled-consolidation/before.xml"
    })
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/start-wave/disabled-consolidation/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWaveForOrdersOfStandardTypeWithDisabledConsolidation() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/immutable-state-nsqlconfig.xml",
            "/testcontainers/controller/waves/db/immutable-state-stations.xml",
            "/testcontainers/controller/waves/db/start-wave/disabled-consolidation-force-false/before.xml"
    })
    @DatabaseSetup(value =
            "/testcontainers/controller/waves/db/start-wave/disabled-consolidation-force-false/disable-stations.xml",
            type = DatabaseOperation.UPDATE

    )
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/start-wave/disabled-consolidation-force-false/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWaveForOrdersOfStandardTypeWithDisabledConsolidationAndForceStartFalse() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/" +
                        "start-waves-disabled-consolidation.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-fit-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-fit-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWithdrawalWaveForOrdersOfOutboundFitType() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-defect-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-defect-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWithdrawalWaveForOrdersOfOutboundDefectType() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-expired-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-expired-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWithdrawalWaveForOrdersOfOutboundExpiredType() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-surplus-before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/withdrawal-start-wave/outbound-surplus-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWithdrawalWaveForOrdersOfOutboundSurplusType() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/start-wave/with-fashion-and-non-fashion-flow-type/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/start-wave/with-fashion-and-non-fashion-flow-type/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWaveForOrdersWithFashionAndNonFashionFlowType() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/start-wave/partially-sorted-orders/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/start-wave/partially-sorted-orders/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldStartWaveWithPartiallySortedOrder() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/start-wave/full-sorted-by-one-sku/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/start-wave/full-sorted-by-one-sku/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldStartWaveWithFullSortedOrderByOneSku() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/start-wave/full-sorted-by-all-sku/immutable-state.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/start-wave/full-sorted-by-all-sku/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldStartWaveWithFullSortedOrderByAllSku() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/db/start-wave/full-sorted-by-all-sku/response.json")));
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/start-wave/inconsistent-pick-details/immutable-state.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/start-wave/inconsistent-pick-details/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotStartWaveIfInconsistentPickDetails() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        result.andExpect(status().is5xxServerError())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/response/inconsistent-pick-details.json")));
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/singles-and-nonsingles/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/singles-and-nonsingles/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldCreateAllWaveTypeIfSinglesAndNonSingles() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/multi-building/before-order-building-one.xml")
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/multi-building/after-order-building-one.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPickFromBuildingOne() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/wavesControllerLargeTest/db/oversize/before.xml")
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/oversize/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldCreateAllWaveTypeIfSingleOversizeAndNonSingles() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/multi-building/before-order-building-two.xml")
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/multi-building/after-order-building-two.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPickFromBuildingTwo() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/multi-building/before-outbound.xml",
            "/testcontainers/controller/waves/db/multi-building/before-outbound-fit.xml"
    })
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/multi-building/after-outbound-fit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPickFromAllBuildingsForOrdersOfOutboundFitType() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/multi-building/before-outbound.xml",
            "/testcontainers/controller/waves/db/multi-building/before-outbound-defect.xml"
    })
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/multi-building/after-outbound-defect.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPickFromAllBuildingsForOrdersOfOutboundDefectType() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/multi-building/before-outbound.xml",
            "/testcontainers/controller/waves/db/multi-building/before-outbound-expired.xml"
    })
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/multi-building/after-outbound-expired.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPickFromAllBuildingsForOrdersOfOutboundExpiredType() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/multi-building/before-outbound.xml",
            "/testcontainers/controller/waves/db/multi-building/before-outbound-surplus.xml"
    })
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/multi-building/after-outbound-surplus.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPickFromAllBuildingsForOrdersOfOutboundSurplusType() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/wavesControllerLargeTest/db/singles-disabled/before.xml",
            type = REFRESH
    )
    @ExpectedDatabase(
            value = "/testcontainers/wavesControllerLargeTest/db/singles-disabled/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldCreateAllWaveTypeIfSingleDisabled() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/unreserve/before-unreserve-already-started-wave.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/unreserve/before-unreserve-already-started-wave.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCancelReservationOfAlreadyStartedWaveFails() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/unreserve/WAVE-001"));
        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "testcontainers/wavesControllerLargeTest/response/unreserve-wave-already-started.json")));
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/unreserve/before-fully-reserved-wave.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/unreserve/after-fully-reserved-wave.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCancelReservationFullyReservedWave() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/unreserve/WAVE-001"));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/unreserve/before-partially-reserved-wave.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/unreserve/after-partially-reserved-wave.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCancelReservationPartiallyReservedWave() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/unreserve/WAVE-001"));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/reserve/first-one-reserve/before.xml")
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/reserve/first-one-reserve/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldReserveWaveFirstOne() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/response/reserve-successful.json"
                )));
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/immutable-state-nsqlconfig.xml",
            "/testcontainers/controller/waves/db/immutable-state-stations.xml",
            "/testcontainers/controller/waves/db/reserve/first-one-reserve-without-cons/before.xml"
    })
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/reserve/enable-preselect.xml",
            type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/reserve/first-one-reserve-without-cons/after-preselect.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldReserveWaveFirstOneWithPreSelectSortStationAndDisabledConsolidation() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/response/reserve-successful.json"
                )));
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/immutable-state-nsqlconfig.xml",
            "/testcontainers/controller/waves/db/immutable-state-stations.xml",
            "/testcontainers/controller/waves/db/reserve/first-one-reserve-without-cons/before.xml"
    })
    @ExpectedDatabase(value =
            "/testcontainers/controller/waves/db/reserve/first-one-reserve-without-cons/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldReserveWaveFirstOneWithDisabledConsolidation() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/response/reserve-successful.json"
                )));
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/reserve/before-reserve-wave-reservation-is-canceled.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/reserve/after-reserve-wave-reservation-is-canceled.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testReserveWaveWhenWaveReservationIsCanceled() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/response/reserve-successful.json"
                )));
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/reserve/before-reserve-wave-using-fefo.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/reserve/after-reserve-wave-using-fefo.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testReserveWaveUsingFEFO() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "testcontainers/controller/waves/response/reserve-successful.json"
                )));
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/oversize/full-flow/configs.xml", type = REFRESH)
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/oversize/full-flow/create/before.xml", type = REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/oversize/full-flow/create/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void createOversizeWave() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/db/oversize/full-flow/create/request.json")));

        String response = getFileContent("testcontainers/controller/waves/db/oversize/full-flow/create/response.json");
        result.andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/oversize/full-flow/configs.xml", type = REFRESH)
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/oversize/full-flow/create/after.xml", type = REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/oversize/full-flow/reserve/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void reserveOversizeWave() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/db/oversize/full-flow/reserve/request.json")));

        String response = getFileContent("testcontainers/controller/waves/db/oversize/full-flow/reserve/response.json");
        result.andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/oversize/full-flow/configs.xml", type = REFRESH)
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/oversize/full-flow/start/before.xml", type = REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/oversize/full-flow/start/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void startOversizeWave() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/db/oversize/full-flow/start/request.json")));

        result.andExpect(status().isOk());
    }
}
