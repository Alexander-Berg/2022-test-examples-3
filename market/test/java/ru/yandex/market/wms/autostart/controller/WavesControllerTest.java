package ru.yandex.market.wms.autostart.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemType;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ProblemOrderDto;
import ru.yandex.market.wms.replenishment.client.ReplenishmentClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class WavesControllerTest extends AutostartIntegrationTest {

    @Autowired
    @SpyBean
    private ReplenishmentClient replenishmentClient;

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void listWavesReturnsLastPageOffsetEqualToWavesCount() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("offset", "4")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/waves/response/last-page-with-offset-equal-to-waves-count.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void listWavesReturnsAllWaves() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/get-all-waves.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void listWavesReturnsLastPageOffsetGreaterThanWavesCount() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("offset", "30")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/last-page-with-offset-greater-than-waves-count.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsFromFirstPage() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/first-page-offset-not-set.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsNextPage() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("offset", "1")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/next-page-offset-set.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsFromFirstPageWithDescSortByStatus() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "1")
                .param("sort", "status")
                .param("order", "desc")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/order-by-status-desc.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsFromFirstPageWithFilterByStatus() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "5")
                .param("filter", "status=gt=NOT_STARTED")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-status.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsFromFirstPageWithFilterByEditDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "5")
                .param("filter", "editDate=='2021-06-22 15:00:00'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-edit-date.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsFromFirstPageWithFilterByEditDateRange() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "5")
                .param("filter", "editDate=ge='2021-06-22 15:00:00';editDate=le='2021-06-22 16:00:59'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-edit-date-range.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsFromFirstPageWithFilterByAddDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "5")
                .param("filter", "addDate=='2021-06-22 15:00:00'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-add-date.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsFromFirstPageWithFilterByAddDateRange() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "5")
                .param("filter", "addDate=ge='2021-06-22 15:00:00';addDate=le='2021-06-22 16:00:59'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-add-date-range.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesSequentialPageLoadsResultFilteredByStatusAndInProcess() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "status==ALLOCATED;inProcess==CREATED")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-status-allocated.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesByOrdersCount() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "ordersCount==1")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-orders-count.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesFilterByWaveType() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "waveType==SINGLE")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-wave-type.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesOrderByWaveType() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "2")
                .param("sort", "waveType")
                .param("order", "asc")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/order-by-wave-type.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesFilterByBatchOrderNumber() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "batchOrderNumber==B002")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-batch-order-number.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesOrderByBatchOrderNumber() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "1")
                .param("sort", "batchOrderNumber")
                .param("order", "desc")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/order-by-batch-order-number.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesOrderByPriority() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "3")
                .param("sort", "priority")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/order-by-priority-asc.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesOrderByAssigned() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "3")
                .param("sort", "assignedTasksCount")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/order-by-assigned-asc.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesOrderByUnassigned() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("offset", "1")
                .param("sort", "unassignedTasksCount")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/order-by-unassigned-asc.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesFilterByPriority() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "priority==3")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-priority.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesFilterByAssigned() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "assignedTasksCount=gt=1")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-assigned.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesListByUnassigned() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "unassignedTasksCount=ge=1")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-unassigned.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/partially-sorted-wave/before.xml")
    public void testListWavesWhenWaveIsPartiallySorted() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/partially-sorted-wave.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/completed-wave/before.xml")
    public void testListWavesWhenWaveIsCompleted() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/completed-wave.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/startwave/before-no-orders-exception.xml")
    public void testStartWavesThrowsNoOrdersException() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/start-waves.json")));

        result.andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/startwave/before-throws-when-start-reason-required.xml")
    @ExpectedDatabase(value = "/controller/waves/db/startwave/before-throws-when-start-reason-required.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWavesThrowsWhenStartReasonRequired() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/waves/request/start-waves-throws-when-start-reason-required.json")));

        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/start-waves-throws-when-start-reason-required.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/startwave/before-throws-when-start-reason-required.xml")
    @ExpectedDatabase(value = "/controller/waves/db/startwave/before-throws-when-start-reason-required.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWavesBlankReasonThrowsWhenStartReasonRequired() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/waves/request/start-waves-blank-reason-throws-when-start-reason-required.json")));

        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/start-waves-throws-when-start-reason-required.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/startwave/before-wave-reservation-is-canceled.xml")
    @ExpectedDatabase(value = "/controller/waves/db/startwave/before-wave-reservation-is-canceled.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testStartWavesThrowsWhenWaveReservationIsCanceled() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/waves/request/start-waves.json")));

        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/start-waves-throws-when-wave-reservation-is-canceled.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/get-non-reservable-orders/immutable.xml")
    @ExpectedDatabase(value = "/controller/waves/db/get-non-reservable-orders/immutable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getNonReservableOrdersFromWave() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves/WAVE-001/non-reservable-orders"))
                .andExpect(content().json(getFileContent(
                        "controller/waves/db/get-non-reservable-orders/response.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/delete-orders-from-wave/before-wave-not-started.xml")
    @ExpectedDatabase(value = "/controller/waves/db/delete-orders-from-wave/after-wave-not-started.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteOrdersFromWaveNotStarted() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/WAVE-001/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/delete-orders-request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/delete-orders-from-wave/before-wave-canceled-allocation.xml")
    @ExpectedDatabase(value = "/controller/waves/db/delete-orders-from-wave/after-wave-canceled-allocation.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteOrdersFromWaveCanceledAllocation() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/WAVE-001/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/delete-orders-request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/delete-orders-from-wave/before-wave-allocated.xml")
    @ExpectedDatabase(value = "/controller/waves/db/delete-orders-from-wave/after-wave-allocated.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteOrdersFromWaveAllocated() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/WAVE-001/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/delete-orders-request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/delete-orders-from-wave/before-wave-processed-by-wh-planner.xml")
    @ExpectedDatabase(value = "/controller/waves/db/delete-orders-from-wave/after-wave-processed-by-wh-planner.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteOrdersFromWaveProcessedByWhPlanner() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/WAVE-001/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/delete-orders-request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/delete-orders-from-wave/before-wave-delete-all-orders.xml")
    @ExpectedDatabase(value = "/controller/waves/db/delete-orders-from-wave/after-wave-delete-all-orders.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteAllOrdersWaveTypeIsNotChanged() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/WAVE-001/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/delete-orders-request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/delete-orders-from-wave/before-wave-not-started.xml")
    @ExpectedDatabase(value = "/controller/waves/db/delete-orders-from-wave/before-wave-not-started.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteOrdersFromWaveReturnWaveIsNullException() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/null/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/delete-orders-request.json")));

        result.andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/delete-orders-from-wave/before-order-is-dropped.xml")
    @ExpectedDatabase(value = "/controller/waves/db/delete-orders-from-wave/before-order-is-dropped.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteOrdersFromWaveOrderIsPacked() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/WAVE-001/delete-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/delete-orders-request.json")));

        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/delete-orders-order-is-dropped-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/deletewave/before.xml")
    @ExpectedDatabase(value = "/controller/waves/db/deletewave/after.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void testDeleteWave() throws Exception {
        ResultActions result = mockMvc.perform(delete("/waves/WAVE-001")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/fail-delete-wave/before.xml")
    public void testFailToDeleteWave() throws Exception {
        ResultActions result = mockMvc.perform(delete("/waves/WAVE-001")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/change-priority/before.xml")
    @ExpectedDatabase(value = "/controller/waves/db/change-priority/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testChangePriority() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/change-priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/change-priority.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/change-description/before.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/change-description/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testChangeDescription() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/WAVE-001/description")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/change-description.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/waves/db/change-description/before-wave-does-not-exist.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/change-description/before-wave-does-not-exist.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testChangeDescriptionWaveDoesNotExist() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/WAVE-005/description")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/waves/request/change-description.json")));

        result.andExpect(status().is5xxServerError())
                .andExpect(content()
                        .json(getFileContent(
                                "controller/waves/response/change-description-wave-does-not-exist.json"))
                );
    }

    @Test
    @DatabaseSetup("/controller/waves/db/reservewave/before-wave-is-already-started.xml")
    @ExpectedDatabase(value = "/controller/waves/db/reservewave/before-wave-is-already-started.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testReserveWavesThrowsWhenWaveIsAlreadyStarted() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/waves/request/reserve-waves.json")));

        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/reserve-waves-throws-when-wave-is-already-started.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/startreason/wave-with-start-reason.xml")
    public void getWaveWithStartReason() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/get-wave-with-start-reason.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesFilterByStartReason() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("filter", "startReason==01")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/filter-by-start-reason.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/immutable-state.xml")
    public void testListWavesOrderByStartReason() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .param("limit", "1")
                .param("sort", "startReason")
                .param("order", "desc")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/order-by-start-reason.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/big-withdrawal-immutable-state.xml")
    public void testListBigWithdrawalWavesItemsCalculatedCorrectly() throws Exception {
        ResultActions result = mockMvc.perform(get("/waves")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/waves/response/big-withdrawal-get-all-waves.json")));
    }

    @Test
    @DatabaseSetup("/controller/waves/db/replenish/before-big.xml")
    public void replenishBigWithdrawalTest() throws Exception {
        assertHttpCall(
                post("/waves/replenish"),
                status().isOk(),
                "controller/waves/db/replenish/request-big.json"
        );
        var result = List.of(
                new ProblemOrderDto("1", "1", 7, "ORDER-002", "1", ProblemType.OUT_OF_PICKING_STOCK),
                new ProblemOrderDto("1", "2", 3, "ORDER-002", "2", ProblemType.OUT_OF_PICKING_STOCK)
        );

        Mockito.verify(replenishmentClient, Mockito.times(1))
                .addNewProblems(result);
    }

    @Test
    @DatabaseSetup("/controller/waves/db/replenish/before-small.xml")
    public void replenishWithdrawalTest() throws Exception {
        assertHttpCall(
                post("/waves/replenish"),
                status().isOk(),
                "controller/waves/db/replenish/request-small.json"
        );
        var result = List.of(
                new ProblemOrderDto("1", "1", 8, "ORDER-002", "1", ProblemType.OUT_OF_PICKING_STOCK),
                new ProblemOrderDto("1", "2", 5, "ORDER-002", "2", ProblemType.OUT_OF_PICKING_STOCK),
                new ProblemOrderDto("1", "2", 4, "ORDER-001", "1", ProblemType.OUT_OF_PICKING_STOCK)
        );

        Mockito.verify(replenishmentClient, Mockito.times(1))
                .addNewProblems(result);
    }

    @Test
    @DatabaseSetup("/controller/waves/db/replenish/before-small-2.xml")
    public void replenishWithdrawalTest2() throws Exception {
        assertHttpCall(
                post("/waves/replenish"),
                status().isOk(),
                "controller/waves/db/replenish/request-small.json"
        );
        var result = List.of(
                new ProblemOrderDto("1", "1", 10, "ORDER-002", "1", ProblemType.OUT_OF_PICKING_STOCK),
                new ProblemOrderDto("1", "2", 5, "ORDER-002", "2", ProblemType.OUT_OF_PICKING_STOCK),
                new ProblemOrderDto("1", "2", 4, "ORDER-001", "1", ProblemType.OUT_OF_PICKING_STOCK)
        );

        Mockito.verify(replenishmentClient, Mockito.times(1))
                .addNewProblems(result);
    }
}
