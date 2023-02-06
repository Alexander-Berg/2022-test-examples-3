package ru.yandex.market.wms.dropping.controller

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.settings.HttpClientSettings
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.client.configuration.CoreWebClientConfig
import ru.yandex.market.wms.dropping.HttpAssert

class DroppingControllerWithdrawalTest : IntegrationTest() {

    @Autowired
    @MockBean
    protected lateinit var coreClient: CoreClient

    @MockBean(name = "coreHttpClientSettings")
    @Qualifier(CoreWebClientConfig.CORE_CLIENT)
    private lateinit var coreHttpClientSettings: HttpClientSettings

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/dropping/withdrawal/db-common.xml")
    @ExpectedDatabase(value = "/controller/dropping/withdrawal/db-common.xml", assertionMode = NON_STRICT_UNORDERED)
    fun dropInfoOk() {
        httpAssert.assertApiCallOk(
            post("/get-drop-info"),
            "controller/dropping/withdrawal/get-drop-info/ok-request.json",
            "controller/dropping/withdrawal/get-drop-info/ok-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/withdrawal/db-common.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/withdrawal/put-parcel-on-drop/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelOnDropOk() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/withdrawal/put-parcel-on-drop/ok-request.json",
            "controller/dropping/withdrawal/put-parcel-on-drop/ok-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/withdrawal/db-anomaly.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/withdrawal/put-anomaly-parcel-on-drop/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putAnomalyParcelOnDropOk() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/withdrawal/put-anomaly-parcel-on-drop/ok-request.json",
            "controller/dropping/withdrawal/put-anomaly-parcel-on-drop/ok-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/withdrawal/db-anomaly.xml")
    fun anomalyParcelInfoOk() {
        httpAssert.assertApiCallOk(
            post("/get-parcel-info"),
            "controller/dropping/withdrawal/anomaly-parcel-info/ok-request.json",
            "controller/dropping/withdrawal/anomaly-parcel-info/ok-response.json",
        )
    }

    @Test
    @DatabaseSetup(
        type = DatabaseOperation.INSERT, value = [
            "/controller/dropping/withdrawal/db-common.xml",
            "/controller/dropping/withdrawal/put-parcel-on-drop/before.xml"
        ]
    )
    fun putParcelOnDropErrorOrderKey() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/withdrawal/put-parcel-on-drop/error-orderkey-request.json",
            "На дропке может находится только одно изъятие - ORD0001, а в этой посылке - ORD0300",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }
}
