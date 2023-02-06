package ru.yandex.market.wms.packing.controller;

import java.util.Collections;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerTest extends PackingIntegrationTest {

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/tasks/after-1.xml", assertionMode = NON_STRICT_UNORDERED)
    public void assignTasks() throws Exception {
        assertHttpCall(
                put("/tasks"),
                status().isOk(),
                "controller/tasks/assign-request.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_multiple/setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/tasks/setup.xml", type = INSERT)
    public void getAllTask() throws Exception {
        assertHttpCall(
                get("/tasks"),
                status().isOk(),
                Collections.emptyMap(),
                "controller/tasks/response/all.json"
        );

        assertHttpCall(
                get("/tasks"),
                status().isOk(),
                Map.of("users", "test-user,another-user"),
                "controller/tasks/response/available.json"
        );
    }
}
