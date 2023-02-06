package ru.yandex.market.wms.receiving.controller;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.core.base.request.ChangeParentRequest;
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class RelocationTaskControllerNewTest extends ReceivingIntegrationTest {

    @Autowired
    @MockBean
    private CoreClient coreClient;

    private final ArgumentCaptor<ChangeParentRequest> changeParentRequestArgumentCaptor =
            ArgumentCaptor.forClass(ChangeParentRequest.class);

    @BeforeEach
    void setUp() {
        Mockito.reset(coreClient);
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/change-not-my-task-status/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/change-not-my-task-status/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void changeNotMyTaskStatusTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/pick-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/change-not-my-task-status/request.json")))
                .andExpect(status().isForbidden())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/change-not-my-task-status/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/change-task-status/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/change-task-status/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void changeTaskStatusTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/pick-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/change-task-status/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/change-task-status-to-wrong/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/change-task-status-to-wrong/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void changeTaskStatusToWrongTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/pick-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/change-task-status-to-wrong/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/change-task-status-to-wrong/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/change-task-status-same/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/change-task-status-same/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void changeTaskStatusToTheSameStatus() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/pick-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/change-task-status-same/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/get-task-assigned/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/get-task-assigned/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskAssignedTest() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/get-task-assigned/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/get-task-for-object-when-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/get-task-for-object-when-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskForObjectWhenExistTest() throws Exception {
        mockMvc.perform(get("/relocation-new/objects/PLT00004/APLCMNT"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/get-task-for-object-when-exist/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/get-task-for-object-when-not-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/get-task-for-object-when-not-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskForObjectWhenNotExistTest() throws Exception {
        mockMvc.perform(get("/relocation-new/objects/PLT00004/APLCMNT"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/get-task-for-object-when-not-exist/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/get-task-new-with-recommended-locs/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/get-task-new-with-recommended-locs/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskNewWithRecommendedLocsTest() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/get-task-new-with-recommended-locs/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/get-task-new-without-recommended-locs/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/get-task-new-without-recommended-locs/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getTaskNewWithoutRecommendedLocsTest() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/get-task-new-without-recommended-locs/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-not-my-task/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/move-not-my-task/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveNotMyTaskTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/move-not-my-task/request.json")))
                .andExpect(status().isForbidden())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/move-not-my-task/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-location/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/move-to-location/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToLocationTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/move-to-location/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/prevent-duplicate-task/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/prevent-duplicate-task/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToLocationPreventDuplicateTaskExistTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/prevent-duplicate-task/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-not-existing-location/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/move-to-not-existing-location/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToNotExistingLocationTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/relocation-task/move-to-not-existing-location/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/move-to-not-existing-location/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-not-sutable-location-with-other-receipt/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/" +
            "move-to-not-sutable-location-with-other-receipt/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToNotSuitableLocationWithOtherReceiptTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/relocation-task/move-to-not-sutable-location-with-other-receipt/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/move-to-not-sutable-location-with-other-receipt/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-not-sutable-location-with-other-type/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/" +
            "move-to-not-sutable-location-with-other-type/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToNotSuitableLocationWithOtherTypeTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/relocation-task/move-to-not-sutable-location-with-other-type/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/move-to-not-sutable-location-with-other-type/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-location-returns/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/move-to-location-returns/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToLocationTestReturns() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/move-to-location-returns/request.json")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/relocation-new/tasks/2/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/relocation-task/move-to-location-returns/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-location-returns/before-tare-duplicates.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/move-to-location-returns/after-tare-duplicates.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToLocationTestReturnsAnomalyTareDuplicates() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/move-to-location-returns/request.json")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/relocation-new/tasks/2/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/relocation-task/move-to-location-returns/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-location-unredeemed/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/move-to-location-unredeemed/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToLocationTestUnredeemed() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/relocation-task/move-to-location-unredeemed/request.json")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/relocation-new/tasks/2/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/relocation-task/move-to-location-unredeemed/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/move-to-location-unredeemed/before-tare-duplicates.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/move-to-location-unredeemed/after-tare-duplicates.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void moveToLocationTestUnredeemedAnomalyTareDuplicates() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/relocation-task/move-to-location-unredeemed/request.json")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/relocation-new/tasks/2/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/relocation-task/move-to-location-unredeemed/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/pick-task/1/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/pick-task/1/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickTaskTaskNotFound() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/APLCMNT/pick-extra-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task-new/pick-extra-task/1/request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/pick-task/2/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/pick-task/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickTaskTaskIsNotNew() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/APLCMNT/pick-extra-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task-new/pick-extra-task/2/request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/pick-task/3/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/pick-task/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickTaskOK() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/APLCMNT/pick-extra-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task-new/pick-extra-task/2/request.json")))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/picked-containers/1/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/picked-containers/1/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getPickedTasks() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/APLCMNT/picked-containers"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/picked-containers/1/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/picked-containers/2/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/picked-containers/2/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getPickedTasksNotInIntransit() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/APLCMNT/picked-containers"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/picked-containers/2/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/picked-containers/3/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/picked-containers/3/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getPickedTasksNoTasksForUser() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/APLCMNT/picked-containers"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/picked-containers/3/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/picked-containers/4/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/picked-containers/4/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getPickedTasksNoAnomaly() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/APLCMNT/picked-containers"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/picked-containers/4/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/picked-containers/5/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/picked-containers/5/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getPickedTasksMultipleTasksResult() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/APLCMNT/picked-containers"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/picked-containers/5/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/dissociate-task/1/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/dissociate-task/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void dissociateTask() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/APLCMNT/start-placement/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/dissociate-task/2/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/dissociate-task/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void dissociateTaskWrongStatusNotUpdated() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/APLCMNT/start-placement/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/dissociate-task/3/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/dissociate-task/3/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void dissociateTaskNoPlacementTasksAvailable() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/APLCMNT/start-placement/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/skip-task/1/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/skip-task/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void skipTask() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/skip/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/skip-task/1/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/skip-task/2/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/skip-task/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void skipTaskWrongStatusNotUpdated() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/skip/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/skip-task/2/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/skip-task/3/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/skip-task/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void skipTaskOversizeNotUpdatedWhenSkipped() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/skip/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/skip-task/3/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/skip-task/4/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/skip-task/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void skipTaskEmptyReasonIsUpdated() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/skip/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/skip-task/4/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/skip-task/5/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/skip-task/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void skipTaskNextTaskIsUpdated() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/skip/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task/skip-task/5/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/active-containers/1/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/active-containers/1/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getActiveTasks() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/active/APLCMNT"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/active-containers/1/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/active-containers/2/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/active-containers/2/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getActiveTasksDifferentUser() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/active/APLCMNT"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/active-containers/2/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/active-containers/3/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/relocation-task/active-containers/3/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getActiveTasksDifferentStatus() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/active/APLCMNT"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task/active-containers/3/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task-new/destination-parent/1/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task-new/destination-parent/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void destinationParentOk() throws Exception {
        when(coreClient.getChildContainers(eq("CART00001"))).thenReturn(
                new GetChildContainersResponse(List.of("TM0001", "TM0002")));
        mockMvc.perform(put("/relocation-new/tasks/MSRMNT/destination-parent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/relocation-task-new/destination-parent/1/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/relocation-task/dissociate-task/1/before.xml")
    public void getTargetLoc() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/1/target-loc"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task-new/get-target-loc/response.json"))
                );
    }

    @Test
    @DatabaseSetup("/controller/relocation-task-new/pick-task/before.xml")
    @ExpectedDatabase(value = "/controller/relocation-task-new/pick-task/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void changeTaskStatusForMeasurementTest() throws Exception {
        mockMvc.perform(put("/relocation-new/tasks/1/pick-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/relocation-task-new/pick-task/request.json")))
                .andExpect(status().isOk());

        verify(coreClient, times(1)).postChangeParent(
                changeParentRequestArgumentCaptor.capture());

        verify(coreClient).postChangeParent(argThat((arg) -> arg.getContainerIdList().equals(
                Collections.singletonList("TM00001"))));
        verify(coreClient).postChangeParent(argThat((arg) -> arg.getNewParentContainerId().equals("CART0001")));
    }

    @Test
    @DatabaseSetup("/controller/relocation-task-new/get-task-assigned/immutable-state.xml")
    public void getMeasurementTaskAssignedTest() throws Exception {
        mockMvc.perform(get("/relocation-new/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/relocation-task-new/get-task-assigned/response.json")));
    }
}
