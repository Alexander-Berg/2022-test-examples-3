package ru.yandex.market.wms.receiving.controller;

import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.model.enums.AnomalyLotStatus;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyContainer;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyLot;
import ru.yandex.market.wms.common.spring.enums.AnomalyCategory;
import ru.yandex.market.wms.common.spring.enums.ReceivingItemType;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.model.enums.ScanningOperationType;
import ru.yandex.market.wms.receiving.service.ScanningOperationLog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ReceiptConsolidationTaskControllerTest extends ReceivingIntegrationTest {

    @SpyBean
    @Autowired
    private ScanningOperationLog scanningOperationLog;

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-items-from-other-receipt/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-items-from-other-receipt/immutable-state" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateItemsFromOtherReceiptTest() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/consolidate-items-from-other-receipt" +
                        "/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/consolidate-items-from" +
                        "-other-receipt/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-when-task-has-container/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-when-task-has-container/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateWhenTaskHasContainerTest() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/consolidate-when-task-has-container/request" +
                        ".json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-when-task-has-no-container/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-when-task-has-no-container/immutable" +
            "-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateWhenTaskHasNoContainerTest() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/consolidate-when-task-has-no-container" +
                        "/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/consolidate-when-task-has" +
                        "-no-container/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-when-task-not-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-when-task-not-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateWhenTaskNotExistTest() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/consolidate-when-task-not-exist/request" +
                        ".json")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/consolidate-when-task-not" +
                        "-exist/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-when-task-not-my/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-when-task-not-my/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateWhenTaskNotMyTest() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/consolidate-when-task-not-my/request.json")))
                .andExpect(status().isForbidden())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/consolidate-when-task-not" +
                        "-my/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-returns/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-returns/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateReturns() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/consolidate-returns/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-returns-different-task/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-returns-different-task/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateReturnsDifferentTask() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/" +
                        "consolidate-returns-different-task/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/" +
                        "consolidate-returns-different-task/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-unredeemed/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-unredeemed/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateUnredeemed() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/receipt-consolidation/consolidate-unredeemed/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-when-item-not-placed/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-when-item-not-placed/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consolidateWhenItemNotPlacedTest() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/consolidate-when-item-not-placed/request" +
                                ".json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/" +
                        "consolidate-when-item-not-placed/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/get-not-existing-task/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/get-not-existing-task/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getNotExistingTaskTest() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-not-existing-task" +
                        "/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/get-task-in-progress-without-container/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/get-task-in-progress-without-container/immutable" +
            "-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskInProgressWithoutContainerTest() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-task-in-progress" +
                        "-without-container/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/get-task-in-progress-with-container/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/get-task-in-progress-with-container/immutable-state" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskInProgressWithContainerTest() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-task-in-progress-with" +
                        "-container/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/get-task-finished/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/get-task-finished/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskReturns() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-task-finished/response" +
                        ".json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/get-task-returns/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/get-task-returns/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskFinishedTest() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-task-returns/response" +
                        ".json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/get-task-unredeemed/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/get-task-unredeemed/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getUnredeemedTaskFinishedTest() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/receipt-consolidation/get-task-unredeemed/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/consolidate-when-item-not-placed/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/consolidate-when-item-not-placed/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskWithItemNotPlacedTest() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/" +
                        "consolidate-when-item-not-placed/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-buf-when-task-has-items-to-consolidate/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-buf-when-task-has-items-to-consolidate/after" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToBufWhenTaskHasItemsToConsolidateTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-to-buf-when-task-has-items-to" +
                        "-consolidate/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-buf-when-task-has-no-items-to-consolidate/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-buf-when-task-has-no-items-to-consolidate" +
            "/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToBufWhenTaskHasNoItemsToConsolidateTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-to-buf-when-task-has-no-items-to" +
                        "-consolidate/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-buf-when-task-not-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-buf-when-task-not-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToBufWhenTaskNotExistTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-to-buf-when-task-not-exist/request" +
                        ".json")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/move-to-buf-when-task-not" +
                        "-exist/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-buf-when-task-not-my/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-buf-when-task-not-my/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToBufWhenTaskNotMyTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-to-buf-when-task-not-my/request.json")))
                .andExpect(status().isForbidden())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/move-to-buf-when-task-not" +
                        "-my/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-not-buf/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-not-buf/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToNotBufTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-to-not-buf/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/move-to-not-buf/response" +
                        ".json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-unknown-location/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-unknown-location/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToUnknownLocationTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-to-unknown-location/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/move-to-unknown-location" +
                        "/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-buf-returns-status-finished/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-buf-returns-status-finished" +
            "/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToBufReturnsTaskFinished() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/" +
                        "move-to-buf-returns-status-finished/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-buf-unredeemed-status-finished/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-to-buf-unredeemed-status-finished" +
            "/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToBufUnredeemedTaskFinished() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/receipt-consolidation/" +
                                "move-to-buf-unredeemed-status-finished/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/move-to-buf-when-task-has-items-to-consolidate/before.xml")
    public void checkRunScanningOperationLogInTask() throws Exception {
        Mockito.reset(scanningOperationLog);

        mockMvc.perform(put("/receipt-consolidation/tasks/1/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/receipt-consolidation/move-to-buf-when-task-has-items-to" +
                                "-consolidate/request.json")))
                .andExpect(status().isOk());

        var anomalyContainer = AnomalyContainer.builder(null)
                .status(AnomalyLotStatus.NEW)
                .transportUnitId("IZ00001")
                .receiptKey("00002")
                .loc("A_CONS-01")
                .build();

        var anomalyLot = AnomalyLot.builder(anomalyContainer)
                .id("1")
                .storerKey("1")
                .description("Некоторый товар 1")
                .types(Set.of(ReceivingItemType.DAMAGED))
                .amount(1)
                .category(AnomalyCategory.FOOD.getCategory())
                .build();
        Mockito.verify(scanningOperationLog).writeAnomaliesOperation(
                ScanningOperationType.MOVE_ANOMALY_CONTAINER_TO_LOC,
                Collections.singletonList(anomalyLot),
                "REJ_BUF-01"
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/put-container-to-finished-task/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/put-container-to-finished-task/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void putContainerToFinishedTaskTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/put-container-to-finished-task/request" +
                        ".json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/put-container-to-finished" +
                        "-task/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/put-container-to-not-my-task/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/put-container-to-not-my-task/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void putContainerToNotMyTaskTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/put-container-to-not-my-task/request.json")))
                .andExpect(status().isForbidden())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/put-container-to-not-my" +
                        "-task/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/put-container-to-task-with-container/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/put-container-to-task-with-container/immutable-state" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void putContainerToTaskWithContainerTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/put-container-to-task-with-container" +
                        "/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/put-container-to-task-with" +
                        "-container/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/put-container-to-unknown-task/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/put-container-to-unknown-task/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void putContainerToUnknownTaskTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/put-container-to-unknown-task/request.json")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/put-container-to-unknown" +
                        "-task/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/put-new-container-to-task-without-container/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/put-new-container-to-task-without-container/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void putNewContainerToTaskWithoutContainerTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/put-new-container-to-task-without-container" +
                        "/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/put-not-empty-container-to-task-without-container/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/put-not-empty-container-to-task-without-container" +
            "/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void putNotEmptyContainerToTaskWithoutContainerTest() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/receipt-consolidation/" +
                                "put-not-empty-container-to-task-without-container/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/receipt-consolidation/" +
                                "put-not-empty-container-to-task-without-container/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/put-no-container-to-task-without-container/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/put-no-container-to-task-without-container/immutable" +
            "-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void putNoContainerToTaskWithoutContainer() throws Exception {
        mockMvc.perform(put("/receipt-consolidation/tasks/1/current-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/put-no-container-to-task-without-container" +
                        "/request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/new_consolidation_before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/new_consolidation_before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getFinalContainersOk() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1/get-final-containers")
                .param("anomalyLotKey", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-final-container/" +
                                                         "get-final-container-ok-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/new_consolidation_before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/new_consolidation_before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getFinalContainersEmpty() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/2/get-final-containers")
                        .param("anomalyLotKey", "4"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-final-container/" +
                                                         "get-final-container-empty-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/new_consolidation_before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/new_consolidation_before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getFinalContainersALL() throws Exception {
        mockMvc.perform(get("/receipt-consolidation/tasks/1/get-final-containers"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/receipt-consolidation/get-final-container/" +
                                                         "get-final-container-all-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/new_consolidation_before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-anomaly-lot-to-final-container/" +
                              "new_consolidation_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void linkFinalContainerToTaskOk() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/2/move-anomaly-to-final-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-anomaly-lot-to-final-container/" +
                                        "link-final-container-to-task-ok-request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/new_consolidation_before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-anomaly-lot-to-final-container/" +
                              "new_consolidation_link_not_duplicate_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void linkFinalContainerToTaskNotDuplicate() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/move-anomaly-to-final-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-anomaly-lot-to-final-container/" +
                                                "link-final-container-to-task-not-duplicate-request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/new_consolidation_before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/move-anomaly-lot-to-final-container/" +
                              "new_consolidation_move_anomaly_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveAnomalyLotToFinalContainerOk() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/move-anomaly-to-final-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-anomaly-lot-to-final-container/" +
                                        "move-anomaly-lot-to-final-container-ok-request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt-consolidation/new_consolidation_before.xml")
    @ExpectedDatabase(value = "/controller/receipt-consolidation/new_consolidation_before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveAnomalyLotToFinalContainerCategory() throws Exception {
        mockMvc.perform(post("/receipt-consolidation/tasks/1/move-anomaly-to-final-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-consolidation/move-anomaly-lot-to-final-container/" +
                                        "move-anomaly-lot-to-final-container-missmatch-category-request.json")))
                .andExpect(status().isBadRequest());
    }
}
