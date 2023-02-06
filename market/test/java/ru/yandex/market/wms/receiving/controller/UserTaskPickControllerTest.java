package ru.yandex.market.wms.receiving.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class UserTaskPickControllerTest extends ReceivingIntegrationTest {

    @Test
    @Disabled
    @DatabaseSetup("/controller/user-pick-task/assign-relocation-task-by-id/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/assign-relocation-task-by-id/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignRelocationTaskById() throws Exception {
        mockMvc.perform(put("/my/tasks/relocation/1"))
                .andExpect(status().isOk());
    }

    @Test
    @Disabled
    @DatabaseSetup("/controller/user-pick-task/assign-not-new-relocation-task-by-id/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/assign-not-new-relocation-task-by-id/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignNotNewRelocationTaskById() throws Exception {
        mockMvc.perform(put("/my/tasks/relocation/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/assign-not-new-relocation-task-by-id/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-consolidation-task/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-consolidation-task/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickConsolidationTask() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-consolidation-task/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-consolidation-task/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-all-tasks-picked/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-all-tasks-picked/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPickAllTaskPicked() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-all-tasks-picked/request.json")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/1/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedWhenStatusIs0() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/1/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/1/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/2/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedWhenStatusIs2() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/2/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/2/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/3/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskReturnedNullWhenStatusIs3() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/3/request.json")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/4/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedWithSmallestSkipCount() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/4/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/4/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/5/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedNoPickedNoOversize() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/5/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/5/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/6/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/6/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedNoPickedReturnOversizeFirst() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/6/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/6/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/7/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/7/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedPickedExistOversizeOnly() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/7/request.json")))
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/7/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/8/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/8/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedPickedExistNoLogLocReturnFirst() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/8/request.json")))
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/8/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/9/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/9/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedPickedExistReturnSmallestLogLoc() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/9/request.json")))
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/9/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/10/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/10/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedPickedExistReturnSmallestLogLocNoAvailable() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/10/request.json")))
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/10/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/11/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/11/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedPickedExistReturnSmallestLogLocSmallestSkipCount() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/11/request.json")))
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/11/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/12/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/12/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedNoPickedAllOversizeAreSkippedPickFirst() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/12/request.json")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/13/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/13/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedNoPickedTwoOversizeAreSkippedPickFirst() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/13/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/13/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/user-pick-task/pick-placement-task/14/before.xml")
    @ExpectedDatabase(value = "/controller/user-pick-task/pick-placement-task/14/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pickPlacementTaskAssignedNoPickedAllOversizeAreSkippedRegularPicked() throws Exception {
        mockMvc.perform(post("/my/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/user-pick-task/pick-placement-task/14/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/user-pick-task/pick-placement-task/14/response.json")));
    }

}
