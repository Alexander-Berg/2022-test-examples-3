package ru.yandex.market.wms.replenishment.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.json.JSONException
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class PickingTaskControllerTest : IntegrationTest() {
    /** Get current picking task, no picked items, can pick container  */
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/db.xml")
    @Test
    fun `current picking task`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/get-current.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Get current picking task, no picked items, can pick container  */
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/db.xml")
    @Test
    fun `current picking task new url`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/get-current.json",
            MockMvcRequestBuilders.put("/picking/current/turnover"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Get current picking task, no picked items, can pick container  */
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/db.xml")
    @Test
    fun `current picking task for order`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/order-get-current.json",
            MockMvcRequestBuilders.put("/picking/current/order"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Get current picking task, no picked items, can pick container  */
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/db.xml")
    @Test
    fun `current picking task for withdrawal`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/withdrawal-get-current.json",
            MockMvcRequestBuilders.put("/picking/current/withdrawal"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Get current picking task, picked 1 item  */
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/picked.xml")
    @Test
    fun `current picking task picked`() {
            assertApiCall(
                "controller/replenishment-task/picking/request/get-task/get-current.json",
                "controller/replenishment-task/picking/response/get-task/get-current-one-picked.json",
                MockMvcRequestBuilders.put("/picking/current"),
                MockMvcResultMatchers.status().isOk
            )
        }

    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/picked-virtual.xml")
    @Test
    fun `current task with virtual uits`() {
            assertApiCall(
                "controller/replenishment-task/picking/request/get-task/get-current.json",
                "controller/replenishment-task/picking/response/get-task/" +
                    "get-current-one-picked-virtual.json",
                MockMvcRequestBuilders.put("/picking/current"),
                MockMvcResultMatchers.status().isOk
            )
        }

    /** Get current picking nouit task, no picked items, fromId replaced by parent pallet  */
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/get-task/virtual-unit-id-links.xml",
        "/controller/replenishment-task/picking/db/get-task/picked-virtual.xml"
    )
    @Test
    fun `current task with virtual uits and parent unit id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/" +
                "get-current-virtual-pallet.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * При назначении нового таска должно учитываться, что нет открытых заданий по этой же ячеке на других
     * пользователей
     */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/get-task/picked-virtual-busy.xml",
        "/controller/replenishment-task/picking/db/get-task/virtual-unit-id-links.xml"
    )
    fun `check users`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            null,
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isNoContent
        )
    }

    /** Skipping task with empty loc  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/get-task/before/assign-with-empty-loc.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/get-task/after/assign-with-empty-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign with empty loc`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/get-current-with-empty-loc.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Skipping task with empty loc for order  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/get-task/before/order-assign-with-empty-loc.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/get-task/after/order-assign-with-empty-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign with empty loc for order`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/order-get-current-with-empty-loc.json",
            MockMvcRequestBuilders.put("/picking/current/order"),
            MockMvcResultMatchers.status().isOk
        )
    }


    /** Skipping task with missing id  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/get-task/before/assign-with-missing-id.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/get-task/after/assign-with-missing-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign with missing id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            null,
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Skipping last picking task with missing id  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/get-task/before/assign-last-task-missing-id.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/get-task/after/assign-last-task-missing-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign last with missing id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            null,
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isNoContent
        )
    }

    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/before/assign-with-inconsistent-balance.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/get-task/after/assign-with-inconsistent-balance.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign with inconsistent balance`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/" +
                "assign-with-inconsistent-balance.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Start picking task (containerId as identifier)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/after/start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/start.json",
            null,
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Start picking task (containerId as identifier)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/after/order-start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task for order`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/order-start.json",
            null,
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Start picking task (containerId as identifier) toLoc is INTRANSIT  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db-intransit.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/after/order-start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task for order with toLoc INTRANSIT`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/order-start-intransit.json",
            null,
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Start picking task (containerId as identifier) toLoc is INTRANSIT toId is null */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db-intransit.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/before/db-intransit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task for order with toLoc INTRANSIT no id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/order-start-intransit-no-id.json",
            null,
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Start picking task (ToLoc and containerId as identifier)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/after/order-start-toloc-toid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task for order with toLoc and toId`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/order-start-toloc-toid.json",
            null,
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Start picking task (toLoc as identifier)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/after/order-start-toloc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task for order with toLoc`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/order-start-toloc.json",
            null,
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Start picking task (serialNumber as identifier)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/after/start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task by serial`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/start-by-serial.json",
            null,
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Start picking task with wrong identifier  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/before/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task wrong identifier`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/start-wrong-identifier.json",
            "controller/replenishment-task/picking/response/start-wrong-identifier.json",
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Start picking task with wrong toId  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/start/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/start/before/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start picking task wrong to id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/start/start-wrong-to-id.json",
            "controller/replenishment-task/picking/response/wrong-to-id.json",
            MockMvcRequestBuilders.post("/picking/start"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Change toLoc  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/change-to-loc/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/change-to-loc/after/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change toLoc`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/change-to-loc/change-to-loc.json",
            null,
            MockMvcRequestBuilders.patch("/picking/to-loc"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Change toId  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/change-to-id/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/change-to-id/after/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change to id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/change-to-id/change-to-id.json",
            null,
            MockMvcRequestBuilders.patch("/picking/to-id"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Change toId with wrong task status  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/change-to-id/before/wrong-status.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/change-to-id/before/wrong-status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change to id wrong task status`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/change-to-id/change-to-id.json",
            "controller/replenishment-task/picking/response/change-to-id/change-to-id-wrong-status.json",
            MockMvcRequestBuilders.patch("/picking/to-id"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Change toId with picked items  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/change-to-id/before/picked.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/change-to-id/before/picked.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change to id picked items`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/change-to-id/change-to-id.json",
            "controller/replenishment-task/picking/response/change-to-id/change-to-id-picked-items.json",
            MockMvcRequestBuilders.patch("/picking/to-id"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Change toId with wrong format  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/picking/db/change-to-id/before/db.xml",
        "/controller/replenishment-task/picking/db/id_regex_config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/change-to-id/before/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change to id wrong format`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/change-to-id/change-to-id-wrong-format.json",
            "controller/replenishment-task/picking/response/wrong-to-id.json",
            MockMvcRequestBuilders.patch("/picking/to-id"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Pick by serial number (serialInventories count by lot)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-serial.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial.json",
            "controller/replenishment-task/picking/response/picking/pick-serial.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick by serial number (serialInventories count by lot)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/order-by-serial.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number for order`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/order-pick-serial.json",
            "controller/replenishment-task/picking/response/picking/order-pick-serial.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick by serial number (serialInventories count by sku and storerKey)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-without-lot.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-serial-without-lot.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number without lot`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial.json",
            "controller/replenishment-task/picking/response/picking/pick-serial.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick last by serial number, lift task cancelled  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/by-serial-last.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-serial-last.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number last`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-last.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-last.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick last by serial number for order, lift task cancelled  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/by-serial-last.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/order-by-serial-last.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number last for order`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/order-pick-serial-last.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-last.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick last with another items in container  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/pick-last-with-other-items.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/pick-last-with-other-items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick last with other items`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-last.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-last.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick last with another items in container  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/pick-last-with-other-items.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/order-pick-last-with-other-items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick last with other items for order`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/order-pick-serial-last.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-last.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick with cart not in 'id' sql table  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/by-serial-new-id.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-serial-new-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number new id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-new-id.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-new-id.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Finish picking task with other open picking task  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/pick-last-with-other-task.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/pick-last-with-other-task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick last with other open task`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-last.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-last.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick by wrong serial number (not from needed containerId)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/before/picking.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number wrong container`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-wrong.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-wrong.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Pick by wrong serial number (not from needed lot)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-wrong-lot.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/before/picking-wrong-lot.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number wrong lot`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-wrong-lot.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-wrong-lot.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }


    /** Pick by wrong serial number (wrong sku)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-wrong-sku.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/before/picking-wrong-sku.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number wrong sku`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-wrong-sku.json",
            "controller/replenishment-task/picking/response/picking/pick-serial-wrong-sku.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Pick all items in loc  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick loc`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-loc.json",
            null,
            MockMvcRequestBuilders.post("/picking/pick/loc"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick by containerId  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-by-container.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-container.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick container id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-container.json",
            null,
            MockMvcRequestBuilders.post("/picking/pick/container-id"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick by containerId with quantity  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-by-container.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-container.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick container id with qty`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-container-qty.json",
            "controller/replenishment-task/picking/response/picking/pick-container-qty-1.json",
            MockMvcRequestBuilders.post("/picking/pick/container-id"),
            MockMvcResultMatchers.status().isOk
        )
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-container.json",
            "controller/replenishment-task/picking/response/picking/pick-container-qty-2.json",
            MockMvcRequestBuilders.post("/picking/pick/container-id"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/before/picking.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick container id with wrong qty`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-container-qty-wrong.json",
            "controller/replenishment-task/picking/response/picking/pick-container-qty-wrong.json",
            MockMvcRequestBuilders.post("/picking/pick/container-id"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Pick by wrong containerId  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/before/picking.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick by wrong container id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-container-wrong.json",
            "controller/replenishment-task/picking/response/wrong-container.json",
            MockMvcRequestBuilders.post("/picking/pick/container-id"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Pick with wrong toId  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/before/picking.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick with wrong to id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-to-id-wrong.json",
            "controller/replenishment-task/picking/response/picking/pick-to-id-wrong.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Pick by serial number to empty LOC happy path  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-serial-to-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number to loc`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-to-loc.json",
            "controller/replenishment-task/picking/response/picking/pick-serial.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick by serial number to LOC with ID happy path  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-loc-with-container.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/by-serial-to-loc-with-container.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick serial number to loc with container`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-to-loc-with-container.json",
            "controller/replenishment-task/picking/response/picking/pick-serial.json",
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Pick by serial number to LOC INTRANSIT without specifying ID  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    fun `pick serial number to loc intransit`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-to-loc-intransit.json",
            null,
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Pick by serial number to not existing LOC  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking.xml"
    )
    fun `pick serial number to loc not exists`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/pick-serial-to-loc-notexists.json",
            null,
            MockMvcRequestBuilders.post("/picking/pick/serial-number"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Lost items in container (serialInventories found by lot)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/by-serial-last.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick items lost`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Lost items in container (serialInventories found by sku and storerKey)  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-without-lot.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost-without-lot.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick items lost found by sku`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Trying to move to lost items in different location  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/lost-not-in-loc.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost-not-in-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `lost items not in loc`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Moving container to lost  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-loc-two-containers.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost-container.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick items lost container`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost-container.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Moving loc balances to lost  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-loc-two-containers.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick items lost loc`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost-loc.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Moving loc balances to lost for order replenishment tasks */
    @Test
    @DatabaseSetup(value = ["/controller/replenishment-task/sku-loc-common.xml", "/controller/replenishment-task/picking/db/picking/before/picking-loc-two-containers-ord.xml"])
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost-loc-ord.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick items lost loc order replenishment`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost-loc.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Moving container to lost for order replenishment tasks */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-loc-two-containers-ord.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost-container-ord.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick items lost container order replenishment`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost-container.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Moving pallet balances to lost  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/picking/before/picking-pallet.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/picking/after/lost-pallet.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick items lost pallet`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/shortage/lost-pallet.json",
            null,
            MockMvcRequestBuilders.post("/picking/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Finish picking with items left in container  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/finish/before/no-space.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/finish/after/no-space.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick no space in cart`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/finish.json",
            "controller/replenishment-task/picking/response/finish-no-space.json",
            MockMvcRequestBuilders.post("/picking/premature-finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Finish picking with items left in container  */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/picking/db/finish/before/no-space.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/picking/db/finish/after/no-space-with-force.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pick no space in cart force`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/finish.json",
            null,
            MockMvcRequestBuilders.post("/picking/force-finish"),
            MockMvcResultMatchers.status().isNoContent
        )
    }


    /** Check valid containerId  */
    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/check-container/check-container.xml")
    fun `check valid container id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/check-identifier/check-containerid-valid.json",
            null,
            MockMvcRequestBuilders.post("/picking/check/container"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Check invalid containerId  */
    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/check-container/check-container.xml")
    fun `check invalid container id`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/check-identifier/" +
                "check-containerid-invalid.json",
            "controller/replenishment-task/picking/response/check-identifier/check-container-wrong.json",
            MockMvcRequestBuilders.post("/picking/check/container"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Check valid serialNumber  */
    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/check-container/check-container.xml")
    fun `check valid serial number`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/check-identifier/check-serial-valid.json",
            null,
            MockMvcRequestBuilders.post("/picking/check/container"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /** Check invalid serialNumber  */
    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/check-container/check-container.xml")
    fun `check invalid serial number`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/check-identifier/check-serial-invalid.json",
            "controller/replenishment-task/picking/response/check-identifier/check-serial-wrong.json",
            MockMvcRequestBuilders.post("/picking/check/container"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /** Split picking task  */
    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/split/before.xml")
    fun `split picking task`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/picking/split-picking-task.json",
            "controller/replenishment-task/picking/response/picking/split-picking-task.json",
            MockMvcRequestBuilders.post("/picking/split"),
            MockMvcResultMatchers.status().isOk
        )
    }


    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/unassigned.xml")
    @Deprecated(
        """container picking feature is disabled
      Assign new picking task, can pick container"""
    )
    fun `assign picking task`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/get-current-assign.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/container-with-other-lot.xml")
    @Deprecated(
        """container picking feature is disabled
      Assign new picking task, cannot pick container - has other lot"""
    )
    fun `assign picking task with other lot`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/get-current-has-another-lot.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/container-with-other-sku.xml")
    @Deprecated(
        """container picking feature is disabled
      Assign new picking task, cannot pick container - exactly needed amount, but has other sku"""
    )
    fun `assign picking task with other sku needed amount`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/get-current-has-another-sku.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/container-plus-other-sku.xml")
    @Deprecated(
        """container picking feature is disabled
      Assign new picking task, cannot pick container - exactly needed amount of needed sku,
      but additionally has other sku"""
    )
    fun `assign picking task plus other sku`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/" +
                "get-current-has-another-sku-additional.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @Ignore
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/splitted-tasks.xml")
    @Deprecated(
        """new task is now being assigned to user in /premature-finish
      Assign second part of previous task after /premature-finish call to the same user"""
    )
    fun `assign picking task by previous`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/assign-by-previous.json",
            "controller/replenishment-task/picking/response/get-task/assign-by-previous.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign new picking task, total items in loc more than needed to pick according to tasks
     */
    @Test
    @Deprecated("")
    @DatabaseSetup("/controller/replenishment-task/picking/db/get-task/assign-total-items.xml")
    fun `assign picking task total items`() {
        assertApiCall(
            "controller/replenishment-task/picking/request/get-task/get-current.json",
            "controller/replenishment-task/picking/response/get-task/assign-total-items.json",
            MockMvcRequestBuilders.put("/picking/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    private fun assertApiCall(
        requestFile: String?,
        responseFile: String?,
        request: MockHttpServletRequestBuilder,
        status: ResultMatcher
    ) {
        val requestBody = if (requestFile == null) "" else FileContentUtils.getFileContent(requestFile)
        val result = mockMvc.perform(
            request
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status)
        if (responseFile != null) {
            val response = FileContentUtils.getFileContent(responseFile)
            try {
                result.andExpect(MockMvcResultMatchers.content().json(response, false))
            } catch (e: JSONException) {
                result.andExpect(MockMvcResultMatchers.content().string(response))
            }
        }
    }
}
