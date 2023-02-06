package ru.yandex.market.wms.dropping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.settings.HttpClientSettings
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.client.configuration.CoreWebClientConfig
import ru.yandex.market.wms.dropping.HttpAssert

class DroppingErrorControllerTest: IntegrationTest() {

    @Autowired
    @MockBean
    protected lateinit var coreClient: CoreClient

    @MockBean(name = "coreHttpClientSettings")
    @Qualifier(CoreWebClientConfig.CORE_CLIENT)
    private lateinit var coreHttpClientSettings: HttpClientSettings

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/parcel-error/get/ok-no-error/db.xml")
    @ExpectedDatabase(value = "/controller/parcel-error/get/ok-no-error/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `OK - no errors for user`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.get("/parcel-error"),
            responseFile = "controller/parcel-error/get/ok-no-error/response.json",
        )
    }

    @Test
    @DatabaseSetup(value = [
        "/controller/base.xml",
        "/controller/parcel-error/get/ok-second-ago/db.xml",
    ])
    @ExpectedDatabase(value = "/controller/parcel-error/get/ok-second-ago/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `OK - error happened few seconds ago`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.get("/parcel-error"),
            responseFile = "controller/parcel-error/get/ok-second-ago/response.json",
        )
    }

    @Test
    @DatabaseSetup(value = [
        "/controller/base.xml",
        "/controller/parcel-error/get/ok-has-error-but-parcel-shipped/before.xml",
    ])
    @ExpectedDatabase(value = "/controller/parcel-error/get/ok-has-error-but-parcel-shipped/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `OK - error happened, but parcel is already shipped`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.get("/parcel-error"),
            responseFile = "controller/parcel-error/get/ok-has-error-but-parcel-shipped/response.json",
        )
    }

    @Test
    @DatabaseSetup(value = [
        "/controller/base.xml",
        "/controller/parcel-error/get/ok-has-error-but-no-pickdetails/before.xml",
    ])
    @ExpectedDatabase(value = "/controller/parcel-error/get/ok-has-error-but-no-pickdetails/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `OK - error happened, but no pickdetails`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.get("/parcel-error"),
            responseFile = "controller/parcel-error/get/ok-has-error-but-no-pickdetails/response.json",
        )
    }


    @Test
    @DatabaseSetup(value = [
        "/controller/base.xml",
        "/controller/parcel-error/get/ok-has-error-but-empty-orderkey/before.xml",
    ])
    @ExpectedDatabase(value = "/controller/parcel-error/get/ok-has-error-but-empty-orderkey/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `OK - error happened, but empty orderkey`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.get("/parcel-error"),
            responseFile = "controller/parcel-error/get/ok-has-error-but-empty-orderkey/response.json",
        )
    }

    @Test
    @DatabaseSetup(value = [
        "/controller/base.xml",
        "/controller/parcel-error/get/error-minute-ago/db.xml",
    ])
    @ExpectedDatabase(value = "/controller/parcel-error/get/error-minute-ago/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `ERROR - error happened a minute ago`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.get("/parcel-error"),
            responseFile = "controller/parcel-error/get/error-minute-ago/response.json",
        )
    }

    @Test
    @DatabaseSetup(value = [
        "/controller/base.xml",
        "/controller/parcel-error/get/confirm/db.xml",
    ])
    @ExpectedDatabase(value = "/controller/parcel-error/get/confirm/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `CONFIRM - error fix must be confirmed`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.get("/parcel-error"),
            responseFile = "controller/parcel-error/get/confirm/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/parcel-error/confirm-fixed/ok/before.xml")
    @ExpectedDatabase(value = "/controller/parcel-error/confirm-fixed/ok/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `Error fix confirmed`() {
        httpAssert.assertApiCallOk(
            request = MockMvcRequestBuilders.post("/parcel-error/confirm-fixed"),
            requestFile = "controller/parcel-error/confirm-fixed/ok/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/parcel-error/confirm-fixed/fail-no-error/db.xml")
    @ExpectedDatabase(value = "/controller/parcel-error/confirm-fixed/fail-no-error/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `Error fix not confirmed - no error`() {
        httpAssert.assertApiCallError(
            request = MockMvcRequestBuilders.post("/parcel-error/confirm-fixed"),
            requestFile = "controller/parcel-error/confirm-fixed/fail-no-error/request.json",
            errorFragment = "Не найдено ошибок с посылкой P0002",
            resultMatcher = MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/parcel-error/confirm-fixed/fail-error-not-fixed/db.xml")
    @ExpectedDatabase(value = "/controller/parcel-error/confirm-fixed/fail-error-not-fixed/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun `Error fix not confirmed - error not fixed`() {
        httpAssert.assertApiCallError(
            request = MockMvcRequestBuilders.post("/parcel-error/confirm-fixed"),
            requestFile = "controller/parcel-error/confirm-fixed/fail-error-not-fixed/request.json",
            errorFragment = "Ошибка с посылкой P0001 не исправлена",
            resultMatcher = MockMvcResultMatchers.status().is4xxClientError
        )
    }
}
