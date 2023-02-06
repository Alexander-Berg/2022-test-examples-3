package ru.yandex.market.wms.picking.modules.controller;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.picking.modules.async.PickingContainerTransportService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

    public class AssignUserToTaskControllerTest extends IntegrationTest {

    @Autowired
    @SpyBean
    PickingContainerTransportService pickingContainerTransportService;

    @Test
    @DatabaseSetup("/controller/assign-user-to-task/user_activity.xml")
    @DatabaseSetup("/controller/assign-user-to-task/task_detail.xml")
    @ExpectedDatabase(value = "/controller/assign-user-to-task/1/after.xml", assertionMode = NON_STRICT)
    public void assignUserToTaskHappyPath() throws Exception {
        callTwice(() -> assertHttpCall(
                post("/assign-user-to-task"),
                status().isOk(),
                "controller/assign-user-to-task/1/request.json",
                "controller/assign-user-to-task/1/response.json"
        ));
    }

    @Test
    @DatabaseSetup("/controller/assign-user-to-task/user_activity.xml")
    @DatabaseSetup("/controller/assign-user-to-task/task_detail.xml")
    @ExpectedDatabase(value = "/controller/assign-user-to-task/2/after.xml", assertionMode = NON_STRICT)
    public void assignUserToTaskWithOneFailed() throws Exception {
        callTwice(() -> assertHttpCall(
                post("/assign-user-to-task"),
                status().isOk(),
                "controller/assign-user-to-task/2/request.json",
                "controller/assign-user-to-task/2/response.json"
        ));
    }

    @Test
    @DatabaseSetup("/controller/assign-user-to-task/user_activity.xml")
    @DatabaseSetup("/controller/assign-user-to-task/task_detail.xml")
    @ExpectedDatabase(value = "/controller/assign-user-to-task/3/after.xml", assertionMode = NON_STRICT)
    public void assignUserToTaskWithAllFailed() throws Exception {
        callTwice(() -> assertHttpCall(
                post("/assign-user-to-task"),
                status().isOk(),
                "controller/assign-user-to-task/3/request.json",
                "controller/assign-user-to-task/3/response.json"
        ));
    }

    @Test
    @DatabaseSetup("/controller/unassign-user-from-task/user_activity.xml")
    @DatabaseSetup("/controller/unassign-user-from-task/task_detail.xml")
    @ExpectedDatabase(value = "/controller/unassign-user-from-task/1/after.xml", assertionMode = NON_STRICT)
    public void unassignUserFromTaskHappyPath() throws Exception {
        callTwice(() -> assertHttpCall(
                post("/unassign-user-from-task"),
                status().isOk(),
                "controller/unassign-user-from-task/1/request.json",
                "controller/unassign-user-from-task/1/response.json"
        ));
    }

    @Test
    @DatabaseSetup("/controller/unassign-user-from-task/user_activity.xml")
    @DatabaseSetup("/controller/unassign-user-from-task/task_detail.xml")
    @ExpectedDatabase(value = "/controller/unassign-user-from-task/2/after.xml", assertionMode = NON_STRICT)
    public void unassignUserFromTaskWithOneFailed() throws Exception {
        callTwice(() -> assertHttpCall(
                post("/unassign-user-from-task"),
                status().isOk(),
                "controller/unassign-user-from-task/2/request.json",
                "controller/unassign-user-from-task/2/response.json"
        ));
    }

    @Test
    @DatabaseSetup("/controller/unassign-user-from-task/user_activity.xml")
    @DatabaseSetup("/controller/unassign-user-from-task/task_detail.xml")
    @ExpectedDatabase(value = "/controller/unassign-user-from-task/3/after.xml", assertionMode = NON_STRICT)
    public void unassignUserFromTaskWithAllFailed() throws Exception {
        callTwice(() -> assertHttpCall(
                post("/unassign-user-from-task"),
                status().isOk(),
                "controller/unassign-user-from-task/3/request.json",
                "controller/unassign-user-from-task/3/response.json"
        ));
    }

    @Test
    @DatabaseSetup("/controller/unassign-user-from-task/4/before.xml")
    @ExpectedDatabase(value = "/controller/unassign-user-from-task/4/after.xml", assertionMode = NON_STRICT)
    public void unassignUserFromTaskHappyPathNoStation() throws Exception {
        callTwice(() -> assertHttpCall(
                post("/unassign-user-from-task"),
                status().isOk(),
                "controller/unassign-user-from-task/4/request.json",
                "controller/unassign-user-from-task/4/response.json"
        ));

        Mockito.verify(pickingContainerTransportService, Mockito.atLeastOnce())
                .defineStationForWaveAsync("W5", "TM001", "PICKTO", Set.of("BATCH001"));
    }
}
