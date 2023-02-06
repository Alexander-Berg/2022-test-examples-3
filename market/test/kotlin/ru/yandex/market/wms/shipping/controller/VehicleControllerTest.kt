package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

internal class VehicleControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/shipping/vehicles/db.xml")
    fun getVehicleNumbers() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/vehicles/numbers"),
            responseFile = "controller/shipping/vehicles/numbers/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/vehicles/db.xml")
    fun getCarriersForVehicle() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/vehicles/carriers?vehicleNumber=A123BE55"),
            responseFile = "controller/shipping/vehicles/carriers/response1.json",
        )
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/vehicles/carriers?vehicleNumber=K543MH22"),
            responseFile = "controller/shipping/vehicles/carriers/response2.json",
        )
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/vehicles/carriers?vehicleNumber=O765РС00"),
            responseFile = "controller/shipping/vehicles/carriers/response3.json",
        )
    }
}
