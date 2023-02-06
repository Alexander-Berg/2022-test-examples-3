package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.getFileContent
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate

class BookingControllerConsolidatedBookSlotTest(@Autowired var geobaseProviderApi: GeobaseProviderApi) : AbstractContextualTest() {

    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/consolidated-book-slot/1/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/consolidated-book-slot/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingCreatedSuccessfully() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/1/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.INBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfGetQuotas()

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/consolidated")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/1/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/consolidated-book-slot/2/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/consolidated-book-slot/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun oneBookingExists() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/2/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.INBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfGetQuotas()

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/consolidated")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/2/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/consolidated-book-slot/3/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/consolidated-book-slot/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun bothBookingsExist() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/3/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.INBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfGetQuotas()

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/consolidated")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/3/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/consolidated-book-slot/4/before.xml"])
    fun noFreeSlot() {
        val requestJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/4/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.INBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfGetQuotas()

        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/consolidated")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        ).andExpect(MockMvcResultMatchers.content()
            .string("{\"message\":\"Time slot was already booked.\",\"type\":\"SLOT_ALREADY_BOOKED\"}")).andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/consolidated-book-slot/5/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/consolidated-book-slot/5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingCreatedWithDestinationServiceIdSuccessfully() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/5/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.INBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfGetQuotas()

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/consolidated")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/consolidated-book-slot/5/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }
}
