package ru.yandex.market.wms.picking.modules.controller;

import java.util.Collections;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PickingTaskControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListOld() throws Exception {
        assertHttpCall(get("/assigned-tasks"),
                status().isOk(),
                Collections.emptyMap(),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListFilter1Old() throws Exception {
        assertHttpCall(get("/assigned-tasks"),
                status().isOk(),
                Map.of("filter", "userId=='U3',userId=='U4'"),
                "controller/picking-task/assigned-tasks-list/filter1-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListFilter2Old() throws Exception {
        assertHttpCall(get("/assigned-tasks"),
                status().isOk(),
                Map.of("filter", "zone=='RACK'"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListFilter3Old() throws Exception {
        assertHttpCall(get("/assigned-tasks"),
                status().isOk(),
                Map.of("filter", "qty=='1',qty=='2'"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListSort1Old() throws Exception {
        assertHttpCall(get("/assigned-tasks"),
                status().isOk(),
                Map.of("sort", "userId", "order", "ASC"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListSort2Old() throws Exception {
        assertHttpCall(get("/assigned-tasks"),
                status().isOk(),
                Map.of("sort", "zone", "order", "ASC"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListLimit1Old() throws Exception {
        assertHttpCall(get("/assigned-tasks"),
                status().isOk(),
                Map.of("limit", "8", "offset", "0"),
                "controller/picking-task/assigned-tasks-list/limit1-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskList() throws Exception {
        assertHttpCall(get("/picking-tasks"),
                status().isOk(),
                Collections.emptyMap(),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListFilter1() throws Exception {
        assertHttpCall(get("/picking-tasks"),
                status().isOk(),
                Map.of("filter", "userId=='U3',userId=='U4'"),
                "controller/picking-task/assigned-tasks-list/filter1-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListFilter2() throws Exception {
        assertHttpCall(get("/picking-tasks"),
                status().isOk(),
                Map.of("filter", "zone=='RACK'"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListFilter3() throws Exception {
        assertHttpCall(get("/picking-tasks"),
                status().isOk(),
                Map.of("filter", "qty=='1',qty=='2'"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListSort1() throws Exception {
        assertHttpCall(get("/picking-tasks"),
                status().isOk(),
                Map.of("sort", "userId", "order", "ASC"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListSort2() throws Exception {
        assertHttpCall(get("/picking-tasks"),
                status().isOk(),
                Map.of("sort", "zone", "order", "ASC"),
                "controller/picking-task/assigned-tasks-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getPickingTaskListLimit1() throws Exception {
        assertHttpCall(get("/picking-tasks"),
                status().isOk(),
                Map.of("limit", "8", "offset", "0"),
                "controller/picking-task/assigned-tasks-list/limit1-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getTaskDetailsList() throws Exception {
        assertHttpCall(get("/task-details"),
                status().isOk(),
                Collections.emptyMap(),
                "controller/picking-task/task-details-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getTaskDetailsListOrder1() throws Exception {
        assertHttpCall(get("/task-details"),
                status().isOk(),
                Map.of("sort", "status", "order", "ASC"),
                "controller/picking-task/task-details-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getTaskDetailsListOrder2() throws Exception {
        assertHttpCall(get("/task-details"),
                status().isOk(),
                Map.of("sort", "fromLoc", "order", "ASC"),
                "controller/picking-task/task-details-list/all-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getTaskDetailsListFilter1() throws Exception {
        assertHttpCall(get("/task-details"),
                status().isOk(),
                Map.of("filter", "ASSIGNMENTNUMBER=='A2'"),
                "controller/picking-task/task-details-list/filter1-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getTaskDetailsListFilter2() throws Exception {
        assertHttpCall(get("/task-details"),
                status().isOk(),
                Map.of("filter", "fromLoc=='C4-10-0001'"),
                "controller/picking-task/task-details-list/filter2-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getTaskDetailsListLimit1() throws Exception {
        assertHttpCall(get("/task-details"),
                status().isOk(),
                Map.of("offset", "0", "limit", "8"),
                "controller/picking-task/task-details-list/limit1-response.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/update-priority/update-priority-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void changeTaskPriority() throws Exception {
        assertHttpCall(put("/update-task-priority"),
                status().isOk(),
                "controller/picking-task/update-priority/update-priority-request.json");
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @ExpectedDatabase(value = "/controller/picking-task/unassign-task/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unassignUser() throws Exception {
        assertHttpCall(MockMvcRequestBuilders.post("/unassign-task"),
                status().isOk(),
                "controller/picking-task/unassign-task/request.json",
                "controller/picking-task/unassign-task/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @DatabaseSetup(value = "/controller/picking-task/unassign-last-task/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/picking-task/unassign-last-task/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unassignLastTaskFromUser() throws Exception {
        assertHttpCall(MockMvcRequestBuilders.post("/unassign-task"),
                status().isOk(),
                "controller/picking-task/unassign-last-task/request.json",
                "controller/picking-task/unassign-last-task/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/common.xml")
    @DatabaseSetup(value = "/controller/picking-task/unassign-not-last-task/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/picking-task/unassign-not-last-task/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unassignNotLastTaskFromUser() throws Exception {
        assertHttpCall(MockMvcRequestBuilders.post("/unassign-task"),
                status().isOk(),
                "controller/picking-task/unassign-not-last-task/request.json",
                "controller/picking-task/unassign-not-last-task/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-task/assign-task/before.xml")
    @ExpectedDatabase(value = "/controller/picking-task/assign-task/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void assignTask() throws Exception {
        assertHttpCall(MockMvcRequestBuilders.post("/assign-task"),
                status().isOk(),
                "controller/picking-task/assign-task/request.json",
                "controller/picking-task/assign-task/response.json"
        );
    }
}
