package ru.yandex.market.wms.dropping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.dropping.HttpAssert

class DroppingControlledMoveCancelledParcelTest : IntegrationTest() {
    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/dropping/move-cancelled-parcel/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-cancelled-parcel/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move cancelled parcel`() {
        httpAssert.assertApiCallOk(
            MockMvcRequestBuilders.post("/move-cancelled-parcel"),
            requestFile = "controller/dropping/move-cancelled-parcel/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-cancelled-parcel/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-cancelled-parcel/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `parcel not found`() {
        httpAssert.assertApiCallError(
            MockMvcRequestBuilders.post("/move-cancelled-parcel"),
            requestFile = "controller/dropping/move-cancelled-parcel/parcel-not-found-request.json",
            errorFragment = "Посылка P000003 не существует или пуста",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }
}
