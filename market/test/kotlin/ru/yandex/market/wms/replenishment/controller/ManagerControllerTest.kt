package ru.yandex.market.wms.replenishment.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest

class ManagerControllerTest : IntegrationTest() {


    @Test
    @DatabaseSetup("/controller/manager/before.xml")
    @ExpectedDatabase("/controller/manager/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun selectAll() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/manage/tasks"),
            MockMvcResultMatchers.status().isOk,
            emptyMap(),
            "controller/manager/tasks-list/response-all.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/before.xml")
    @ExpectedDatabase("/controller/manager/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun selectOne() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/manage/tasks"),
            MockMvcResultMatchers.status().isOk,
            mapOf(
                "filter" to
                    "taskKey=='0000000108'" +
                    ";priority==5" +
                    ";sku=='SKU_2'" +
                    ";storer=='STORER_1'" +
                    ";qty==10" +
                    ";type==REP_WD_PK" +
                    ";user==user3" +
                    ";status==IN_PROCESS" +
                    ";fromLoc==PALLETE02"
            ),
            "controller/manager/tasks-list/response-one.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/user/pick/before.xml")
    @ExpectedDatabase("/controller/manager/user/pick/assign-after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun assignPickTask() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/manage/tasks/user"),
            MockMvcResultMatchers.status().isOk,
            "controller/manager/user/assign-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/user/pick/before.xml")
    @ExpectedDatabase("/controller/manager/user/pick/unassign-after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun unassignPickTask() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/manage/tasks/user"),
            MockMvcResultMatchers.status().isOk,
            "controller/manager/user/unassign-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/user/pick/before.xml")
    @ExpectedDatabase("/controller/manager/user/pick/unassign2-after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun unassign2PickTask() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/manage/tasks/user"),
            MockMvcResultMatchers.status().isOk,
            "controller/manager/user/unassign2-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/user/pick/before.xml")
    @ExpectedDatabase("/controller/manager/user/pick/reassign-after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun reassignPickTask() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/manage/tasks/user"),
            MockMvcResultMatchers.status().isOk,
            "controller/manager/user/reassign-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/user/move/before.xml")
    @ExpectedDatabase("/controller/manager/user/move/assign-after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun assignMoveTask() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/manage/tasks/user"),
            MockMvcResultMatchers.status().isOk,
            "controller/manager/user/assign-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/user/move/before.xml")
    @ExpectedDatabase("/controller/manager/user/move/unassign-after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun unassignMoveTask() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/manage/tasks/user"),
            MockMvcResultMatchers.status().isOk,
            "controller/manager/user/unassign-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/manager/user/move/before.xml")
    @ExpectedDatabase("/controller/manager/user/move/reassign-after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun reassignMoveTask() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/manage/tasks/user"),
            MockMvcResultMatchers.status().isOk,
            "controller/manager/user/reassign-request.json",
        )
    }
}
