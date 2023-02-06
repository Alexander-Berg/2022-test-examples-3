package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.ff.client.dto.quota.QuotaDTO
import ru.yandex.market.ff.client.dto.quota.QuotaInfoDTO
import ru.yandex.market.ff.client.dto.quota.QuotaInfosDTO
import ru.yandex.market.ff.client.enums.DailyLimitsType
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.config.idm.IdmRoleSecurityConfigurationAdapter.Companion.USER_LOGIN_HEADER
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateCustomScheduleResponse
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class CalendaringInfoControllerTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) : AbstractContextualTest() {

    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
    }

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/controller/calendaring-info/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun testGetSuccessful() {
        val testDate = LocalDate.of(2021, 5, 17)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/INBOUND")
                .header(USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent("fixtures/controller/calendaring-info/response_with_time_zone.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    fun testBadRequestIfLmsResponseEmpty() {
        val testDate = LocalDate.of(2021, 5, 18)
        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/WITHDRAW_LIMITS")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/controller/calendaring-info/before-diff-zone.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun testGetSuccessfulDifferentTimeZone() {
        val testDate = LocalDate.of(2021, 5, 18)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/OUTBOUND")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .header(USER_LOGIN_HEADER, "test_login")
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent("fixtures/controller/calendaring-info/response_without_time_zone.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/controller/calendaring-info/before-with-different-gate-types-and-bookings.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun testSuccessfulWithDifferentGateAndBookingsOnGatesTypes() {
        val testDate = LocalDate.of(2021, 5, 17)
        Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(any(), any(), any())).thenReturn(
            MockParametersHelper.mockGatesScheduleResponse(
                listOf(
                    createLogisticPointGateCustomResponse(1, EnumSet.of(GateTypeResponse.INBOUND, GateTypeResponse.OUTBOUND), testDate),
                    createLogisticPointGateCustomResponse(2, EnumSet.of(GateTypeResponse.INBOUND, GateTypeResponse.OUTBOUND), testDate),
                    createLogisticPointGateCustomResponse(3, EnumSet.of(GateTypeResponse.OUTBOUND), testDate),
                    createLogisticPointGateCustomResponse(4, EnumSet.of(GateTypeResponse.INBOUND), testDate),
                )
            )
        )

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) +
                "/OUTBOUND?timezone=Europe/Moscow")
                .header(USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent("fixtures/controller/calendaring-info/response_with_different_gates_bookings.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/controller/calendaring-info/connected-bookings/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun testSuccessfulWithConnectedBookings() {
        val testDate = LocalDate.of(2021, 5, 17)
        Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(any(), any(), any())).thenReturn(
            MockParametersHelper.mockGatesScheduleResponse(
                listOf(
                    createLogisticPointGateCustomResponse(2, EnumSet.of(GateTypeResponse.INBOUND, GateTypeResponse.OUTBOUND), testDate),
                    createLogisticPointGateCustomResponse(4, EnumSet.of(GateTypeResponse.INBOUND), testDate),
                )
            )
        )


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) +
                "/OUTBOUND?timezone=Europe/Moscow")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .header(USER_LOGIN_HEADER, "test_login")
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent("fixtures/controller/calendaring-info/connected-bookings/response.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml")
    fun testSuccessfulGetDailyQuota() {

        Mockito.`when`(ffwfClientApi!!.getQuotaInfo(
            123,
            LocalDate.of(2021, 5, 18),
            LocalDate.of(2021, 5, 19),
            listOf(DailyLimitsType.SUPPLY, DailyLimitsType.MOVEMENT_SUPPLY, DailyLimitsType.XDOCK_TRANSPORT_SUPPLY)
        )).thenReturn(QuotaInfosDTO.builder()
            .quotaInfos(listOf(
                getQuotaInfoDTO(DailyLimitsType.SUPPLY, LocalDate.of(2021, 5, 18), null, null, 10, 20, 1, 2, 0, 0),
                getQuotaInfoDTO(DailyLimitsType.MOVEMENT_SUPPLY, LocalDate.of(2021, 5, 18), -5, null, null, null, 5, 2, 1, 1),
                getQuotaInfoDTO(DailyLimitsType.XDOCK_TRANSPORT_SUPPLY, LocalDate.of(2021, 5, 18), 2, null, 1, null, 0, 0, 0, 0),
                getQuotaInfoDTO(DailyLimitsType.SUPPLY, LocalDate.of(2021, 5, 19), null, null, null, null, 0, 0, 0, 0),
                getQuotaInfoDTO(DailyLimitsType.MOVEMENT_SUPPLY, LocalDate.of(2021, 5, 19), 5, 6, 7, 8, 9, 10, 11, 12),
                getQuotaInfoDTO(DailyLimitsType.XDOCK_TRANSPORT_SUPPLY, LocalDate.of(2021, 5, 19), 2, null, 1, null, 0, 0, 0, 0)
            ))
            .build()
        )

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/INBOUND/123/quota/2021-05-18/2021-05-19")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .header(USER_LOGIN_HEADER, "test_login")
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent("fixtures/controller/calendaring-info/response_get_quota_info.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verify(ffwfClientApi!!).getQuotaInfo(any(), any(), any(), any())
    }


    @Test
    @DatabaseSetup(
        "classpath:fixtures/controller/calendaring-info/booked-slot/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun findSlotByRequestExternalId() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/booked-slot")
                .header(USER_LOGIN_HEADER, "test_login")
                .param("id", "562358")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent(
                "fixtures/controller/calendaring-info/booked-slot/response-request-external.json"
            )

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, false)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/controller/calendaring-info/booked-slot/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun findSlotByServiceId() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/booked-slot")
                .header(USER_LOGIN_HEADER, "test_login")
                .param("id", "100500")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent(
                "fixtures/controller/calendaring-info/booked-slot/response-service-id.json"
            )

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, false)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/controller/calendaring-info/booked-slot/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun findSlotByFfwfId() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/booked-slot")
                .header(USER_LOGIN_HEADER, "test_login")
                .param("id", "111")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent(
                "fixtures/controller/calendaring-info/booked-slot/response-ffwf.json"
            )

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, false)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/controller/calendaring-info/booked-slot/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun findSlotByBookingExternalId() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/booked-slot")
                .header(USER_LOGIN_HEADER, "test_login")
                .param("id", "ID1")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent(
                "fixtures/controller/calendaring-info/booked-slot/response-booking-external.json"
            )

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, false)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/controller/calendaring-info/booked-slot/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun findSlotByBookingId() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/booked-slot")
                .header(USER_LOGIN_HEADER, "test_login")
                .param("id", "1")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent(
                "fixtures/controller/calendaring-info/booked-slot/response-booking-id.json"
            )

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, false)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/controller/calendaring-info/add-parent-slot/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml"
    )
    fun findParentSlot() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/booked-slot")
                .header(USER_LOGIN_HEADER, "test_login")
                .param("id", "112")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent(
                "fixtures/controller/calendaring-info/add-parent-slot/response.json"
            )

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, true)
    }

    private fun setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate: LocalDate) {
        Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(any(), any(), any())).thenReturn(
            MockParametersHelper.mockGatesScheduleResponse(
                MockParametersHelper.mockAvailableGatesResponse(
                    setOf(1L, 2L), EnumSet.of(GateTypeResponse.INBOUND),
                    MockParametersHelper.mockGatesSchedules(
                        LocalTime.of(9, 0),
                        LocalTime.of(20, 0),
                        testDate,
                    )
                ),
            )
        )
    }

    private fun getQuotaInfoDTO(type: DailyLimitsType,
                                date: LocalDate,
                                firstPartyItemsQuota: Long?,
                                thirdPartyItemsQuota: Long?,
                                firstPartyPalletsQuota: Long?,
                                thirdPartyPalletsQuota: Long?,
                                firstPartyUnavailableItemsCount: Long,
                                thirdPartyUnavailableItemsCount: Long,
                                firstPartyUnavailablePalletsCount: Long,
                                thirdPartyUnavailablePalletsCount: Long
    ): QuotaInfoDTO {
        return QuotaInfoDTO.builder()
            .type(type)
            .date(date)
            .firstPartyQuotas(getQuotaDTO(firstPartyItemsQuota, firstPartyPalletsQuota,
                firstPartyUnavailableItemsCount, firstPartyUnavailablePalletsCount))
            .thirdPartyQuotas(getQuotaDTO(thirdPartyItemsQuota, thirdPartyPalletsQuota,
                thirdPartyUnavailableItemsCount, thirdPartyUnavailablePalletsCount))
            .build()
    }

    private fun getQuotaDTO(
        itemsQuota: Long?,
        palletsQuota: Long?,
        unavailableItemsCount: Long,
        unavailablePalletsCount: Long
    ): QuotaDTO {
        return QuotaDTO.builder()
            .itemsQuota(itemsQuota)
            .palletsQuota(palletsQuota)
            .unavailableItemsCount(unavailableItemsCount)
            .unavailablePalletsCount(unavailablePalletsCount)
            .build()
    }

    private fun createLogisticPointGateCustomResponse(
        id: Long,
        types: EnumSet<GateTypeResponse>,
        testDate: LocalDate
    ): LogisticsPointGateCustomScheduleResponse {
        return LogisticsPointGateCustomScheduleResponse.newBuilder()
            .enabled(true)
            .gateNumber(id.toString())
            .types(types)
            .schedule(
                MockParametersHelper.mockGatesSchedules(
                    LocalTime.of(9, 0),
                    LocalTime.of(20, 0),
                    testDate,
                )
            )
            .id(id).build()
    }

}
