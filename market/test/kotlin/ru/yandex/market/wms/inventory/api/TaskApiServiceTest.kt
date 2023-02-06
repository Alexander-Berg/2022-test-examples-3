package ru.yandex.market.wms.inventory.api

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.MockitoAnnotations
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.inventory.model.dto.BalanceOfLoc
import ru.yandex.market.wms.inventory.model.dto.Loc
import ru.yandex.market.wms.inventory.service.BalanceFetcherWms
import kotlin.test.Test

class TaskApiServiceTest(
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val balanceFetcher: BalanceFetcherWms
) : AbstractApiTest() {

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        jdbcTemplate.execute("alter sequence task_id_seq restart with 1;")
        Mockito
            .`when`(balanceFetcher.fetch(any()))
            .thenReturn(BalanceOfLoc(loc = Loc("whs172", "2-01"), emptySet()))
    }

    // 1) loc = null На юзера есть задание в статусе in_progress -> return задание task parent!=null
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/find/user-in-progress/before.xml"),
    )
    @ExpectedDatabase(
        "/json/api/task/find/user-in-progress/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun userInProgress() {
        assertApiCall(
            "json/api/task/find/user-in-progress/request.json",
            "json/api/task/find/user-in-progress/response.json",
            MockMvcRequestBuilders.post("/task/find"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //loc = null На юзера есть задание в статусе in_progress -> return задание task parent!=null
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/find/user-in-progress-2/before.xml"),
    )
    @ExpectedDatabase(
        "/json/api/task/find/user-in-progress-2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun userInProgressTaskParentNull() {
        assertApiCall(
            "json/api/task/find/user-in-progress-2/request.json",
            "json/api/task/find/user-in-progress-2/response.json",
            MockMvcRequestBuilders.post("/task/find"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    // 2) loc = null На юзера нет задания в статусе in_progress -> return empty
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/find/user-not-found/before.xml"),
    )
    @ExpectedDatabase(
        "/json/api/task/find/user-not-found/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun userNotFound() {
        assertApiCall(
            "json/api/task/find/user-not-found/request.json",
            "json/api/task/find/user-not-found/response.json",
            MockMvcRequestBuilders.post("/task/find"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //3) loc! = null Задания нет
    @Test
    fun taskNotFound() {
        assertApiCallClientError(
            "json/api/task/find/task-not-found/request.json",
            MockMvcRequestBuilders.post("/task/find"),
            "INVENTORY_TASK_NOT_FOUND"
        )
    }

    //4) loc! = null Задание назначено на другого юзера
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/find/task-not-found-for-user/before.xml"),
    )
    fun taskNotFoundForUser() {
        assertApiCallClientError(
            "json/api/task/find/task-not-found-for-user/request.json",
            MockMvcRequestBuilders.post("/task/find"),
            "INVENTORY_TASK_NOT_FOUND"
        )
    }

    //5) loc! = null Задание есть, его апдейтим (проверяем статус=IN_PROGRESS и юзера)
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/find/success/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/find/success/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun taskFoundSuccess() {

        assertApiCall(
            "json/api/task/find/success/request.json",
            "json/api/task/find/success/response.json",
            MockMvcRequestBuilders.post("/task/find"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    // loc! = null берем самое свежее, остальные отменять
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/find/last-success/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/find/last-success/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun taskFoundLastSuccess() {

        assertApiCall(
            "json/api/task/find/last-success/request.json",
            "json/api/task/find/last-success/response.json",
            MockMvcRequestBuilders.post("/task/find"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    // loc! = null берем самое свежее задание на шорт, остальные отменяем
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/find/last-short-success/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/find/last-short-success/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun taskFoundLastShortSuccess() {

        assertApiCall(
            "json/api/task/find/last-short-success/request.json",
            "json/api/task/find/last-short-success/response.json",
            MockMvcRequestBuilders.post("/task/find"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //loc in work by another user
    @Test
    @DatabaseSetup("/json/api/task/find/loc-for-another-user/before.xml")
    @ExpectedDatabase(
        "/json/api/task/find/loc-for-another-user/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun locInWorkByAnotherUser() {
        assertApiCallClientError(
            "json/api/task/find/loc-for-another-user/request.json",
            MockMvcRequestBuilders.post("/task/find"),
            "INVENTORY_TASK_NOT_FOUND"
        )
    }

    //loc in work by another user, but short exists -> accept task
    @Test
    @DatabaseSetup("/json/api/task/find/loc-for-another-user-short/before.xml")
    @ExpectedDatabase(
        value = "/json/api/task/find/loc-for-another-user-short/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun locInWorkByAnotherUserShort() {
        assertApiCall(
            "json/api/task/find/loc-for-another-user-short/request.json",
            "json/api/task/find/loc-for-another-user-short/response.json",
            MockMvcRequestBuilders.post("/task/find"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //loc!=null; but user has already another task in work
    @Test
    @DatabaseSetup("/json/api/task/find/different-loc-in-progress/before.xml")
    @ExpectedDatabase(
        "/json/api/task/find/different-loc-in-progress/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun anotherTaskInWork() {
        assertApiCallClientError(
            "json/api/task/find/different-loc-in-progress/request.json",
            MockMvcRequestBuilders.post("/task/find"),
            "UNEXPECTED_LOC"
        )
    }

    @Test
    @DatabaseSetup("/json/api/task/create/locs-exist/before.xml")
    @ExpectedDatabase(
        value = "/json/api/task/create/locs-exist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createTasksWhenLocsAlreadyExist() {
        assertApiCall(
            requestFile = "/json/api/task/create/locs-exist/request.json",
            responseFile = "/json/api/task/create/locs-exist/response.json",
            request = MockMvcRequestBuilders.post("/tasks"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    @Test
    @DatabaseSetup("/json/api/task/create/locs-not-exist/before.xml")
    @ExpectedDatabase(
        value = "/json/api/task/create/locs-not-exist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createTasksWhenLocsNotExist() {
        assertApiCall(
            requestFile = "/json/api/task/create/locs-not-exist/request.json",
            responseFile = "/json/api/task/create/locs-not-exist/response.json",
            request = MockMvcRequestBuilders.post("/tasks"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    @Test
    @DatabaseSetup("/json/api/task/create/logistic-point-not-exist/before.xml")
    @ExpectedDatabase(
        value = "/json/api/task/create/logistic-point-not-exist/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createTasksWhenLogisticPointNotExist() {
        assertApiCallClientError(
            requestFile = "/json/api/task/create/locs-not-exist/request.json",
            request = MockMvcRequestBuilders.post("/tasks"),
            errorInfo = "UNKNOWN_LOGISTIC_POINT"
        )
    }

    @Test
    @DatabaseSetup("/json/api/task/update/before.xml")
    @ExpectedDatabase(
        value = "/json/api/task/update/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateTasksStatuses() {
        assertApiCall(
            requestFile = "/json/api/task/update/request.json",
            responseFile = "/json/api/task/update/response.json",
            request = MockMvcRequestBuilders.put("/tasks"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //1) with discrepancies by qty -> has_discrepancies
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/finish/has-discrepancies-in-qty/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/finish/has-discrepancies-in-qty/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun hasDiscrepanciesInQty() {
        assertApiCall(
            "json/api/task/finish/has-discrepancies-in-qty/request.json",
            "json/api/task/finish/has-discrepancies-in-qty/response.json",
            MockMvcRequestBuilders.put("/task/finish"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //1.1) with discrepancies (в большую сторону) by qty -> не расхождения
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/finish/has-discrepancies-in-qty-bigger/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/finish/has-discrepancies-in-qty-bigger/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun hasDiscrepanciesInQtyBigger() {
        assertApiCall(
            "json/api/task/finish/has-discrepancies-in-qty-bigger/request.json",
            "json/api/task/finish/has-discrepancies-in-qty-bigger/response.json",
            MockMvcRequestBuilders.put("/task/finish"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //2) with discrepancies by sku -> has_discrepancies
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/finish/has-discrepancies-in-sku/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/finish/has-discrepancies-in-sku/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun hasDiscrepanciesInSku() {
        assertApiCall(
            "json/api/task/finish/has-discrepancies-in-sku/request.json",
            "json/api/task/finish/has-discrepancies-in-sku/response.json",
            MockMvcRequestBuilders.put("/task/finish"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //3) finish task (Status->FINISHED)
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/finish/success/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/finish/success/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun taskFinishSuccess() {
        assertApiCall(
            "json/api/task/finish/success/request.json",
            "json/api/task/finish/success/response.json",
            MockMvcRequestBuilders.put("/task/finish"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //3) with discrepancies + force = 'true'-> (Status->FINISHED)
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/task/finish/force-true/before.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/json/api/task/finish/force-true/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun taskFinishWithForce() {
        assertApiCall(
            "json/api/task/finish/force-true/request.json",
            "json/api/task/finish/force-true/response.json",
            MockMvcRequestBuilders.put("/task/finish"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }
}
