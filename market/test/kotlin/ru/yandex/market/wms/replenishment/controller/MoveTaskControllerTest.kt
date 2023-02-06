package ru.yandex.market.wms.replenishment.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.json.JSONException
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class MoveTaskControllerTest : IntegrationTest() {
    /**
     * Assign next descent task by areaKey, priority on task with linked waiting EXP_DATE picking task enabled
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/next-by-area-exp-date-first-on.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/move/db/get-task/before/assign-next-with-exp-date-task.xml",
        "/controller/replenishment-task/move/db/get-task/nsqlconfig-exp-date-first-enable.xml",
    )
    @Test
    fun `get next descent task by area exp date first enabled`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-next-exp-date-first-on-without-prev.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next descent task by areaKey, priority on task with linked waiting EXP_DATE picking task disabled
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/next-by-area-exp-date-first-off.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-next-with-exp-date-task.xml")
    )
    @Test
    fun `get next descent task by area exp date first disabled`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-next-exp-date-first-off-without-prev.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next descent task by areaKey, priority on task with linked waiting EXP_DATE picking task enabled,
     * but EXP_DATE picking task not exists
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/next-by-area.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-next.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/nsqlconfig-exp-date-first-enable.xml")
    )
    @Test
    fun `get next descent task by area exp date first enabled exp date task not exists`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-next-without-previous.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next descent task by areaKey
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/next-by-area.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-next.xml")
    )
    @Test
    fun `get next descent task by area`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-next-without-previous.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next descent task by areaKey for order
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/order-next-by-area.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/order-assign-next.xml")
    )
    @Test
    fun `get next descent task by area for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/order-get-next-without-previous.json",
            MockMvcRequestBuilders.put("/move/current/order"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next descent task by areaKey for order
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/order-next-by-area-priority.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/order-assign-next-priority.xml")
    )
    @Test
    fun `get next descent task by area with higher priority for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/order-get-next-without-previous.json",
            MockMvcRequestBuilders.put("/move/current/order"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next descent task by areaKey with box placed on pallet
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/descent-with-unit-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/start/descent/descent-with-unit-id.xml")
    )
    @Test
    fun `get next descent task by area with unit id link`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-next-with-parent-pallet.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next descent task by previous task key
     */
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/next-by-previous.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-next.xml")
    )
    @Test
    fun `get next descent task by previous task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous.json",
            "controller/replenishment-task/move/response/get-task/get-next-with-previous.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current descent task
     */
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/get-current.xml")
    )
    @Test
    fun `get current descent task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-current-descent.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current descent task
     */
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/get-current.xml")
    )
    @Test
    fun `get current descent task new url`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-current-descent.json",
            MockMvcRequestBuilders.put("/move/current/turnover"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current descent task for order
     */
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/get-current.xml")
    )
    @Test
    fun `get current descent task for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/order-get-current-descent.json",
            MockMvcRequestBuilders.put("/move/current/order"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current descent task for order
     */
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/get-current.xml")
    )
    @Test
    fun `get current descent task for withdrawal`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/withdrawal-get-current-descent.json",
            MockMvcRequestBuilders.put("/move/current/withdrawal"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/get-current-inconsistent-balance.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/move/db/get-task/before/get-current-inconsistent-balance.xml",
    )
    @Test
    fun `get current descent task inconsistent balances`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-next-inconsistent-balance.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current lift task
     */
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/start/lift/before.xml")
    )
    @Test
    fun `get current lift task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-current-lift.json",
            "controller/replenishment-task/move/response/get-task/get-current-lift.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current lift task
     */
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/start/lift/before.xml")
    )
    @Test
    fun `get current lift task for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-current-lift.json",
            "controller/replenishment-task/move/response/get-task/order-get-current-lift.json",
            MockMvcRequestBuilders.put("/move/current/order"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current lift task with default areaKey
     */
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/start/lift/before.xml")
    )
    @Test
    fun `get current lift task with default area key`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-current-empty.json",
            "controller/replenishment-task/move/response/get-task/get-current-lift.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Get current lift task without recommended loc
     */
    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/move/db/get-task/before/get-current-lift-to-loc-not-empty.xml",
    )
    @Test
    fun `get current lift task without suggested loc`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-current-lift.json",
            "controller/replenishment-task/move/response/get-task/" +
                "get-current-lift-no-loc-recommended.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task with no empty replenishment locations
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-no-empty-buf.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-no-empty-buf.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task no empty rep buf`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-current-lift.json",
            "controller/replenishment-task/move/response/get-task/assign-no-empty-buf.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task by previous task key with no empty replenishment locations
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-no-empty-buf.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-no-empty-buf.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task by key no empty rep buf`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-lift-by-lift-taskkey.json",
            "controller/replenishment-task/move/response/get-task/assign-no-empty-buf.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task with empty replenishment locations
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-empty-buf.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-empty-buf.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task with empty rep buf`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/assign-empty-buf.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task by previous taskKey with no tasks of different type
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-same-type.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-same-type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task same type`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous.json",
            "controller/replenishment-task/move/response/get-task/assign-same-type.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task by previous taskKey with tasks of different type
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-diff-type.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-diff-type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task different type`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous.json",
            "controller/replenishment-task/move/response/get-task/assign-diff-type.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task by areaKey only with lift tasks available
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-no-lift-tasks.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-no-lift-tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task no lift available`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/assign-no-lift-tasks.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task from the same area as toLoc in previous task
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-by-previous-last-area.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-by-previous-last-area" +
            ".xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun `assign task in area of last loc`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous-two-areas.json",
            "controller/replenishment-task/move/response/get-task/get-next-by-previous-last-area.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task in different area as toLoc in previous task (area choosed by user)
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-by-previous-user-area.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-by-previous-user-area" +
            ".xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun `assign task in user area`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous-two-areas.json",
            "controller/replenishment-task/move/response/get-task/get-next-by-previous-user-area.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task in same area after move down shortage
     */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/move/db/get-task/before/" +
            "assign-next-descent-after-shortage-last-area.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/" +
            "assign-next-descent-after-shortage-last-area.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task down after shortage last area`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous-two-areas.json",
            "controller/replenishment-task/move/response/get-task/" +
                "get-next-descent-by-previous-after-shortage.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next task in user initial area after move down shortage
     */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/move/db/get-task/before/" +
            "assign-next-descent-after-shortage-user-area.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/" +
            "assign-next-descent-after-shortage-user-area.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task down after shortage user area`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous-two-areas.json",
            "controller/replenishment-task/move/response/get-task/" +
                "get-next-descent-by-previous-after-shortage-user-area.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Assign next lift task in last area after move up shortage
     */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/move/db/get-task/before/" +
            "assign-next-lift-after-shortage-last-area.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/" +
            "assign-next-lift-after-shortage-last-area.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task up after shortage last area`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous-two-areas.json",
            "controller/replenishment-task/move/response/get-task/" +
                "get-next-lift-by-previous-after-shortage.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Try to assign next lift task in last area after move up shortage.
     * But only left tasks for moving down.
     */
    @Test
    @DatabaseSetup(
        "/controller/replenishment-task/move/db/get-task/before/" +
            "assign-next-lift-after-shortage-no-lift-tasks.xml"
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/" +
            "assign-next-lift-after-shortage-no-lift-tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign task after shortage from lift to descent`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-next-by-previous-two-areas.json",
            "controller/replenishment-task/move/response/get-task/" +
                "get-next-descent-by-previous-after-shortage-user-area.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Skipping task with empty loc
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-with-empty-loc.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-with-empty-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign with empty loc`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/get-next-with-empty-loc.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Skipping descent task with missing id
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-descent-with-missing-id.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-descent-with-missing-id" +
            ".xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun `assign descent with missing id`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            null,
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isNoContent
        )
    }

    /**
     * Assign lift task with missing id
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/assign-lift-with-missing-id.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/assign-lift-with-missing-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `assign lift with missing id`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-by-area.json",
            "controller/replenishment-task/move/response/get-task/assign-lift-with-missing-id.json",
            MockMvcRequestBuilders.put("/move/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start descent task with containerId scanned
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/start-descent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task container id`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-id.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start descent task with palletId scanned
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/start-descent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task pallet id`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-pallet-id.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start descent task with palletId scanned for order
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/order-start-descent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task pallet id for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/order-start-descent-pallet-id.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start descent task with palletId scanned for order
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/withdrawal-start-descent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task pallet id for withdrawal`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/withdrawal-start-descent-pallet-id.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start lift task with containerId scanned
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/start/lift/before.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/lift/after-start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start lift task container id`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-lift-id.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start lift task with containerId scanned
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/start/lift/before.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/lift/order-after-start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start lift task container id for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/order-start-lift-id.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start descent task with serialNumber scanned
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/start-descent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task serial number`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-serial.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start lift task with scanned containerId in loc, but not from the task
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/start/lift/before.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/lift/after-start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start lift task container id not from task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-lift-id-not-from-task.json",
            null,
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Start descent task with scanned containerId in loc, but not from the task
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task container id not from task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-id-not-from-task.json",
            "controller/replenishment-task/move/response/start/start-descent-id-not-from-task.json",
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isUnprocessableEntity
        )
    }

    /**
     * Start descent task with correct containerId scanned in other loc
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/before/id-in-wrong-loc.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/after/id-in-wrong-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task container id in wrong loc`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-id.json",
            "controller/replenishment-task/move/response/start/start-descent-id-in-wrong-loc.json",
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isBadRequest
        )
    }

    /**
     * Start descent task for order with correct containerId scanned in other loc
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/before/id-in-wrong-loc-ord.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/after/id-in-wrong-loc-ord.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent order task container id in wrong loc`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-id.json",
            "controller/replenishment-task/move/response/start/start-descent-id-in-wrong-loc.json",
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().isBadRequest
        )
    }

    /**
     * Start descent task with wrong serialNumber scanned
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task wrong serial number`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-serial-wrong.json",
            "controller/replenishment-task/move/response/start/start-descent-serial-wrong.json",
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Start descent task with wrong containerId scanned
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task wrong container id`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-id-wrong.json",
            "controller/replenishment-task/move/response/start/start-descent-id-wrong.json",
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Start descent task with wrong user
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db-negative-tests.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/db-negative-tests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task wrong user`() {
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-wrong-user.json",
            "controller/replenishment-task/move/response/start/start-descent-wrong-user.json",
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Start descent task with wrong status
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/start/descent/db-negative-tests.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/start/descent/db-negative-tests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `start descent task wrong status`() {
        // wmsErrorCode используется на фронте, чтобы выкидывать пользователя в меню выбора задания,
        // если статус задания не подходит для текущего действия
        assertApiCall(
            "controller/replenishment-task/move/request/start/start-descent-wrong-status.json",
            "controller/replenishment-task/move/response/start/start-descent-wrong-status.json",
            MockMvcRequestBuilders.post("/move/start"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Finish descent task happy
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/after/finish-descent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent.json",
            null,
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Finish descent task for order happy
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/after/order-finish-descent-rep-buf.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/order-finish-descent-rep-buf.json",
            null,
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Finish descent task for order happy
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/after/order-finish-descent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task for order to rep buf loc type`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/order-finish-descent.json",
            null,
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Finish lift task happy
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/lift/before/finish.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/lift/after/finish.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish lift task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-lift.json",
            null,
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Finish lift task happy for order
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/lift/before/finish.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/lift/after/order-finish.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish lift task for order`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/order-finish-lift.json",
            null,
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Finish descent task with id in multiple locs. Items should be moved as usual.
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db-id-in-two-locs.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/after/db-id-in-two-locs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task id in multiple locs`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent.json",
            null,
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Finish descent task with wrong loc type
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task wrong loc type`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent-wrong-loc-type.json",
            "controller/replenishment-task/move/response/finish/finish-descent-wrong-loc-type.json",
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Finish descent task with REP_BUF_O loc type which only for order replenishment
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task replenishment buffer for order loc type`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent-rep-buf-order-loc-type.json",
            "controller/replenishment-task/move/response/finish/finish-descent-rep-buf-order-loc-type.json",
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Finish descent task with wrong loc type
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests-order.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests-order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task for order wrong loc type`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent-wrong-loc-type.json",
            "controller/replenishment-task/move/response/finish/finish-descent-order-wrong-loc-type.json",
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Finish lift task with wrong loc type
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/lift/before/finish.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/lift/before/finish.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish lift task wrong loc type`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-lift-wrong-loc-type.json",
            "controller/replenishment-task/move/response/finish/finish-lift-wrong-loc-type.json",
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Finish descent task with not empty loc
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task loc not empty`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent-loc-not-empty.json",
            "controller/replenishment-task/move/response/finish/finish-descent-loc-not-empty.json",
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Finish descent task with wrong status
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/before/db-negative-tests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task wrong status`() {
        // wmsErrorCode используется на фронте, чтобы выкидывать пользователя в меню выбора задания,
        // если статус задания не подходит для текущего действия
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent-wrong-status.json",
            "controller/replenishment-task/move/response/finish/finish-descent-wrong-status.json",
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    /**
     * Finish descent task with non closed tasks on empty loc
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"), DatabaseSetup(
            "/controller/replenishment-task/move/db/finish/descent/before/" +
                "db-non-closed-tasks-on-empty-loc.xml"
        )
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/finish/descent/after/" +
            "finish-descent-non-closed-tasks-on-empty-loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish descent task with non closed tasks on empty loc`() {
        assertApiCall(
            "controller/replenishment-task/move/request/finish/finish-descent.json",
            null,
            MockMvcRequestBuilders.post("/move/finish"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Lost in descent task
     */
    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/lost/descent/before.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/lost/descent/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `lost in descent task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/lost/descent.json",
            null,
            MockMvcRequestBuilders.post("/move/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    /**
     * Lost in lift task
     */
    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/lost/lift/before.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/lost/lift/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `lost loc in lift task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/lost/lift.json",
            null,
            MockMvcRequestBuilders.post("/move/shortage"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/get-down-order-task.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/get-down-order-task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get down order task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-down-order-task.json",
            "controller/replenishment-task/move/response/get-task/get-down-order-task.json",
            MockMvcRequestBuilders.put("/move/current/down"),
            MockMvcResultMatchers.status().isOk
        )    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/get-down-order-task.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/after/get-down-order-task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get down any task`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-down-any-task.json",
            "controller/replenishment-task/move/response/get-task/get-down-order-task.json",
            MockMvcRequestBuilders.put("/move/current/down"),
            MockMvcResultMatchers.status().isOk
        )    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/replenishment-task/sku-loc-common.xml"),
        DatabaseSetup("/controller/replenishment-task/move/db/get-task/before/get-down-no-task.xml")
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/get-task/before/get-down-no-task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get down task no tasks`() {
        assertApiCall(
            "controller/replenishment-task/move/request/get-task/get-down-order-task.json",
            null,
            MockMvcRequestBuilders.put("/move/current/down"),
            MockMvcResultMatchers.status().isNoContent
        )    }

    private fun assertApiCall(
        requestFile: String?, responseFile: String?, request: MockHttpServletRequestBuilder, status: ResultMatcher
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
