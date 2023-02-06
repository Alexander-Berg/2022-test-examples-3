package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.ff.client.dto.quota.TakeQuotasManyBookingsResponseDto
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateCustomScheduleResponse
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class BookingControllerUpdateSlotTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi
) :
    AbstractContextualTest() {


    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
        setUpMockFfwfGetQuota()
        setUpMockFfwfTakeQuota()
    }

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/successfully/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/successfully/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun slotUpdatedSuccessfullyTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.INBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 18)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 18))
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(1, 1)
    }

    /**
     * Given
     * slotDate = 2021-05-18
     * limitDate = 2021-05-18
     *
     * Rules for PartnerType = FULFILLMENT
     * maxQuotaDateForCalendaringWithdraw = slotDate(2021-05-18) - 1 day = 2021-5-17
     * Should update when: limitDate.isAfter(maxPossibleQuotaDate) == true
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/successfully-with-quota/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/successfully-with-quota/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun slotUpdatedWithQuotaSuccessfullyForFFTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.OUTBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully-with-quota/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 16)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 16))
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 17))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully-with-quota/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(1, 1)
        verify(ffwfClientApi!!, times(2)).findByBookingId(any())
    }


    /**
     * Given
     * slotDate = 2021-05-18
     * limitDate = 2021-05-17
     *
     * Rules for PartnerType = FULFILLMENT
     * maxQuotaDateForCalendaringWithdraw = slotDate(2021-05-18) - 1 day = 2021-5-17
     * Should update when: limitDate.isAfter(maxPossibleQuotaDate) == false
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/quota-should-not-updated/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/quota-should-not-updated/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun slotUpdatedWhenQuotaShouldNotUpdatedForFFTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.OUTBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/quota-should-not-updated/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 16)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 16))

        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 16))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/quota-should-not-updated/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(1, 0)
        verify(ffwfClientApi!!, times(2)).findByBookingId(any())
    }

    /**
     * Given
     * slotDate = 2021-05-18
     * limitDate = 2021-05-19
     *
     * Rules for PartnerType = SORTING_CENTER
     * maxQuotaDateForCalendaringWithdraw = slotDate(2021-05-19)
     * Should update when: limitDate isAfter maxPossibleQuotaDate == true
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/successfully-with-quota/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/successfully-with-quota/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun slotUpdatedWithQuotaSuccessfullyForSCTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.OUTBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully-with-quota/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 17))
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 19))
        setUpMockLmsGetPartner(PartnerType.SORTING_CENTER)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully-with-quota/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(1, 1)
        verify(ffwfClientApi!!, times(2)).findByBookingId(any())
    }

    /**
     * Given
     * slotDate = 2021-05-18
     * limitDate = 2021-05-18
     *
     * Rules for PartnerType = SORTING_CENTER
     * maxQuotaDateForCalendaringWithdraw = slotDate(2021-05-18)
     * limitDate isAfter maxPossibleQuotaDate == false
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/quota-should-not-updated/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/quota-should-not-updated/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun slotUpdatedWhenQuotaShouldNotUpdatedForSCTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.OUTBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/quota-should-not-updated/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 17))
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 18))
        setUpMockLmsGetPartner(PartnerType.SORTING_CENTER)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/quota-should-not-updated/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(1, 0)
        verify(ffwfClientApi!!, times(2)).findByBookingId(any())
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/successfully-updating/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/successfully-updating/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),

        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/successfully-updating/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        ),
    )
    fun slotUpdatedWithUpdatingStatusSuccessfullyTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.INBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully-updating/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 18)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 18))
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/successfully-updating/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verify(ffwfClientApi!!).getQuota(any())
        verify(ffwfClientApi!!).takeQuota(any())

    }

    @Test
    @DatabaseSetup(
        value = ["classpath:fixtures/controller/booking/update-slot/double-updating/before.xml"]
    )
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/double-updating/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun failWhenTryingRunSecondUpdateTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.INBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/double-updating/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 18)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 18))
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isConflict)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/double-updating/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/fashion-logic/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/fashion-logic/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),

        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/fashion-logic/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        ),
    )
    fun testFashionLogic() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.OUTBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/fashion-logic/request.json")

        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 15),
                LocalDate.of(2021, 5, 16),
                LocalDate.of(2021, 5, 17),
                LocalDate.of(2021, 5, 18),
            )
        )
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 18))
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 16))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/fashion-logic/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verify(ffwfClientApi!!).getQuota(any())
        verify(ffwfClientApi!!).findByBookingId(any())
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/decrease-slot/successfully/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/decrease-slot/successfully/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/decrease-slot/successfully/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun slotDecreasedSuccessfullyTest() {

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/decrease-slot/successfully/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/decrease-slot")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/decrease-slot/successfully/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verify(ffwfClientApi!!).updateQuota(100, 10, 1, true)
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/already-booked/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/already-booked/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun conflictSlotAlreadyBookedTest() {
        setupLmsGateSchedule(
            10,
            22,
            GateTypeResponse.INBOUND,
            LocalDate.of(2021, 5, 18),
        )

        val requestJson: String = FileContentUtils.getFileContent(
            "fixtures/controller/booking/update-slot/already-booked/request.json")

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 18)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 18))
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isConflict)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/update-slot/already-booked/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verifyBasicFfwfCommunication(1, 0)
    }


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/accept-successfully/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/accept-successfully/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/accept-successfully/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun acceptUpdateWhenShouldUpdateLimitSuccessfully() {
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 17))
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/accept/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()
        verify(ffwfClientApi!!).findByBookingId(any())
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/accept-successfully/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/accept-successfully/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/" +
                "accept-successfully/dbqueue-do-not-update-limit.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun acceptUpdateShouldNotUpdateLimitSuccessfully() {
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 18))
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/accept/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()
        verify(ffwfClientApi!!).findByBookingId(any())
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/already-accepted/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/already-accepted/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun acceptUpdateAlreadyAcceptedOkTest() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/accept/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/accept-conflict/before.xml"])
    fun acceptUpdateConflict() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/accept/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isConflict)
            .andReturn()

        assertions().assertThat(result.response.contentAsString).contains("Status was already changed")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/reject-successfully/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/reject-successfully/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/reject-successfully/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun rejectUpdateWhenShouldUpdateLimitSuccessfully() {
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 17))
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/reject/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()
        verify(ffwfClientApi!!).findByBookingId(any())
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/reject-successfully/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/update-slot/reject-successfully/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/" +
                "update-slot/reject-successfully/dbqueue-do-not-update-limit.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun rejectUpdateShouldNotUpdateLimitSuccessfully() {
        setUpMockFfwfFindByBookingId(100, LocalDate.of(2021, 5, 18))
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/reject/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()
        verify(ffwfClientApi!!).findByBookingId(any())
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/already-rejected/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/update-slot/already-rejected/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun rejectUpdateAlreadyAcceptedOkTest() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/reject/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/update-slot/reject-conflict/before.xml"])
    fun rejectUpdateConflict() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/reject/101")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isConflict)
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/decrease-consolidated-slot/1/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/decrease-consolidated-slot/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/decrease-consolidated-slot/1/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun successConsolidatedDecrease() {
        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/decrease-consolidated-slot/1/request.json")

        Mockito.`when`(
            ffwfClientApi!!.takeOrUpdateConsolidatedQuotas(
                any()
            )
        ).thenReturn(
            TakeQuotasManyBookingsResponseDto(LocalDate.MIN)
        )

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/decrease-consolidated-slot")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/decrease-consolidated-slot/1/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verify(ffwfClientApi!!).takeOrUpdateConsolidatedQuotas(any())

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/decrease-consolidated-slot/2/before.xml"])
    fun failConsolidatedDecreaseDifferentPeriods() {
        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/decrease-consolidated-slot/2/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/decrease-consolidated-slot")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().is4xxClientError)
            .andReturn()

        JSONAssert.assertEquals("{\"message\":\"It should be the same slot time and gate\"}",
            result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/decrease-consolidated-slot/3/before.xml"])
    fun failConsolidatedDecreaseDifferentGates() {
        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/decrease-consolidated-slot/3/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/decrease-consolidated-slot")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().is4xxClientError)
            .andReturn()

        JSONAssert.assertEquals("{\"message\":\"It should be the same slot time and gate\"}",
            result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    private fun setupLmsGateSchedule(
        fromHour: Int,
        toHour: Int,
        gateType: GateTypeResponse,
        workingDay: LocalDate,
    ) {
        return setupLmsGateSchedule(
            listOf(1),
            LocalTime.of(fromHour, 0),
            LocalTime.of(toHour, 0),
            gateType,
            setOf(workingDay)
        )
    }

}
