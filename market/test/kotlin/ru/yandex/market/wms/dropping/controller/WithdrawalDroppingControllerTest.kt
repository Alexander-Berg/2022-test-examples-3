package ru.yandex.market.wms.dropping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.dropping.HttpAssert

class WithdrawalDroppingControllerTest : IntegrationTest() {
    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/dropping/withdrawal/packed-not-dropped/setup.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/withdrawal/packed-not-dropped/setup.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun getLocsOfDropableOrderParcels() {
        httpAssert.assertApiCallOk(
            get("/withdrawal/dropable-with-locs"),
            null,
            "controller/dropping/withdrawal/packed-not-dropped/ok-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/withdrawal/packed-not-dropped/setup.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/withdrawal/packed-not-dropped/setup.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun getLocsOfDropableOrderParcelsLimit2() {
        httpAssert.assertApiCallOk(
            get("/withdrawal/dropable-with-locs?limit=2"),
            null,
            "controller/dropping/withdrawal/packed-not-dropped/ok-response_limit2.json"
        )
    }

    @Test
    fun getLocsOfDropableOrderParcelsNoData() {
        httpAssert.assertApiCallOk(
            get("/withdrawal/dropable-with-locs"),
            null,
            "controller/dropping/withdrawal/packed-not-dropped/ok-response_nodata.json"
        )
    }

}
