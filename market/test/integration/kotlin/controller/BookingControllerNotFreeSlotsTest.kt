package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*


class BookingControllerNotFreeSlotsTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) :
    AbstractContextualTest() {


    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
        setUpMockFfwfGetQuota()
    }

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    fun doNotShowSlotsOutsideOfWorkingHoursTest() {

        setupLmsGateSchedule(
            from = LocalTime.of(14, 0),
            to = LocalTime.of(18, 0),
            workingDays = setOf(LocalDate.of(2021, 5, 17))
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T00:00")
        params.add("to", "2021-05-18T00:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/outside-working-hours/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    /**
     * today = 2021-05-12
     */
    @Test
    fun doNotShowSlotsInForDayAfterTomorrowForOutbound() {

        setupLmsGateSchedule(
            from = LocalTime.of(0, 0),
            to = LocalTime.of(16, 0),
            workingDays = setOf(
                LocalDate.of(2021, 5, 12),
                LocalDate.of(2021, 5, 13),
                LocalDate.of(2021, 5, 14),
            ),
            gateType = GateTypeResponse.OUTBOUND
        )
        setUpMockFfwfGetQuota(
            dates = setOf(
                LocalDate.of(2021, 5, 10),
                LocalDate.of(2021, 5, 11),
                LocalDate.of(2021, 5, 12)
            )
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("from", "2021-05-13T10:00")
        params.add("to", "2021-05-14T01:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile =
            FileContentUtils.getFileContent("fixtures/controller/booking/not-free-slots/outbound-today/fulfillment/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)
        verifyBasicFfwfCommunication(1, 0)
    }


    @Test
    fun doNotShowSlotsForTheNextDayOfQuotaFromForOutboundFF() {

        setupLmsGateSchedule(
            from = LocalTime.of(0, 0),
            to = LocalTime.of(16, 0),
            workingDays = setOf(
                LocalDate.of(2021, 5, 20),
                LocalDate.of(2021, 5, 21),
                LocalDate.of(2021, 5, 22)
            ),
            gateType = GateTypeResponse.OUTBOUND
        )
        setUpMockFfwfGetQuota(dates = setOf(
            LocalDate.of(2021, 5, 19),
            LocalDate.of(2021, 5, 20))
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("from", "2021-05-21T10:00")
        params.add("to", "2021-05-22T01:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")
        params.add("quotaFrom", "2021-05-19")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile =
            FileContentUtils.getFileContent("fixtures/controller/booking/not-free-slots/quota-from/fulfillment/response.json")

        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)
        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    fun showSlotsForTheSameDayOfQuotaFromForOutboundSC() {

        setupLmsGateSchedule(
            from = LocalTime.of(0, 0),
            to = LocalTime.of(16, 0),
            workingDays = setOf(
                LocalDate.of(2021, 5, 20),
                LocalDate.of(2021, 5, 21),
                LocalDate.of(2021, 5, 22)
            ),
            gateType = GateTypeResponse.OUTBOUND
        )
        setUpMockFfwfGetQuota(dates = setOf(
            LocalDate.of(2021, 5, 20),
            LocalDate.of(2021, 5, 21))
        )
        setUpMockLmsGetPartner(PartnerType.SORTING_CENTER)

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("from", "2021-05-20T10:00")
        params.add("to", "2021-05-22T01:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")
        params.add("quotaFrom", "2021-05-20")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile =
            FileContentUtils.getFileContent("fixtures/controller/booking/not-free-slots/quota-from/sorting-center/response.json")

        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)
        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    fun showSlotsForTodayForOutboundSC() {

        setupLmsGateSchedule(
            from = LocalTime.of(13, 0),
            to = LocalTime.of(16, 0),
            workingDays = setOf(
                LocalDate.of(2021, 5, 10),
                LocalDate.of(2021, 5, 11),
                LocalDate.of(2021, 5, 12),
            ),
            gateType = GateTypeResponse.OUTBOUND
        )
        setUpMockFfwfGetQuota(dates = setOf(
            LocalDate.of(2021, 5, 10),
            LocalDate.of(2021, 5, 11))
        )
        setUpMockLmsGetPartner(PartnerType.SORTING_CENTER)

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("from", "2021-05-10T10:00")
        params.add("to", "2021-05-12T15:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile =
            FileContentUtils.getFileContent("fixtures/controller/booking/not-free-slots/outbound-today/sorting-center/response.json")

        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)
        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    fun testVladivostok() {

        setUpMockLmsGetLocationZone(geobaseProviderApi, ZoneId.of("Asia/Vladivostok"))
        setupLmsGateSchedule(
            from = LocalTime.of(0, 0),
            to = LocalTime.of(23, 0),
            workingDays = setOf(LocalDate.of(2021, 5, 11), LocalDate.of(2021, 5, 12))
        )
        setUpMockFfwfGetQuota(dates = setOf(LocalDate.of(2021, 5, 11)))

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-11T00:00")
        params.add("to", "2021-05-12T00:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/vladivostok-check/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)
        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    fun doNotShowSlotsOnWarehouseDayOffTest() {
        setupLmsGateSchedule(
            from = LocalTime.of(14, 0),
            to = LocalTime.of(18, 0),
            workingDays = setOf(LocalDate.of(2021, 5, 18)),
        )
        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 18)))

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T02:00")
        params.add("to", "2021-05-18T20:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/warehouse-day-off/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/do-not-show-booked-slots/before.xml"])
    fun returnWithoutBookedSlotsTest() {
        setupLmsGateSchedule(
            from = LocalTime.of(9, 0),
            to = LocalTime.of(18, 0),
            workingDays = setOf(
                LocalDate.of(2021, 5, 15),
                LocalDate.of(2021, 5, 16),
                LocalDate.of(2021, 5, 17),
                LocalDate.of(2021, 5, 18)
            ),
        )
        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 15),
                LocalDate.of(2021, 5, 16),
                LocalDate.of(2021, 5, 17),
                LocalDate.of(2021, 5, 18)
            )
        )
        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T08:00")
        params.add("to", "2021-05-18T22:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")


        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/do-not-show-booked-slots/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/show-slot-when-gate-free/before.xml"])
    fun returnSlotWhenExistsFreeGatesTest() {
        setupLmsGateSchedule(
            from = LocalTime.of(9, 0),
            to = LocalTime.of(18, 0),
            workingDays = setOf(LocalDate.of(2021, 5, 17))
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T07:00")
        params.add("to", "2021-05-17T22:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")


        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/show-slot-when-gate-free/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)
        verifyBasicFfwfCommunication(1, 0)
    }


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/outbound/before.xml"])
    fun returnWithoutBookedSlotsForOutboundTest() {
        setupLmsGateSchedule(
            from = LocalTime.of(9, 0),
            to = LocalTime.of(18, 0),
            gateType = GateTypeResponse.OUTBOUND,
        )
        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 12),
                LocalDate.of(2021, 5, 13),
                LocalDate.of(2021, 5, 14),
                LocalDate.of(2021, 5, 15),
                LocalDate.of(2021, 5, 16),
                LocalDate.of(2021, 5, 17)
            )
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T08:00")
        params.add("to", "2021-05-18T12:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "102")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/outbound/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    fun emptySlotsForOutboundDueQuotaExhausted() {
        setupLmsGateSchedule(
            warehouseIds = listOf(1, 2, 3),
            from = LocalTime.of(10, 0),
            to = LocalTime.of(14, 0),
            gateType = GateTypeResponse.OUTBOUND,
        )
        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 12),
                LocalDate.of(2021, 5, 13),
                LocalDate.of(2021, 5, 14),
                LocalDate.of(2021, 5, 15),
                LocalDate.of(2021, 5, 16),
                LocalDate.of(2021, 5, 17)
            ),
            0,
            0
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("warehouseIds", "2")
        params.add("warehouseIds", "3")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T08:00")
        params.add("to", "2021-05-18T12:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/no-quota-outbound/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)


        verifyBasicFfwfCommunication(3, 0)
    }

    @Test
    fun emptySlotsForInboundDueQuotaExhausted() {
        setupLmsGateSchedule(
            warehouseIds = listOf(1, 2, 3),
            from = LocalTime.of(10, 0),
            to = LocalTime.of(14, 0),
        )
        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 17),
                LocalDate.of(2021, 5, 18)
            ),
            0,
            0
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("warehouseIds", "2")
        params.add("warehouseIds", "3")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T08:00")
        params.add("to", "2021-05-18T12:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/no-quota-inbound/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(3, 0)
    }


    /**
     * Случай с двумя гейтами и двумя занятыми слотами на двух гейтах с частичным перекрытием.
     * 1 gate ___|------|____  11:00 - 12:00
     * 2 gate _______|------|_ 11:30 - 12:30
     * ожидаем, что свободные будут распределены между началом периода и началом занятого слота на 2 гейте (11:30)
     * и концом занятого слота на 1 гейте (12:00) и концом периода.
     *
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/partly-overlapping/before.xml"])
    fun partlyOverlappingSlotsTest() {
        setupLmsGateSchedule(
            from = LocalTime.of(9, 0),
            to = LocalTime.of(14, 0),
            workingDays = setOf(LocalDate.of(2021, 5, 17))
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T09:00")
        params.add("to", "2021-05-17T14:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")


        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/partly-overlapping/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    /**
     * Кейс, когда начало и конце занятых слотов совпадают с началом и концом запрашиваемого периода.
     * 1 gate |------|________|---| 08:00 - 11:00 и 13:00 - 17:00
     * 2 gate |---|_______|-------| 08:30 - 09:30 и 12:30 - 17:30
     * ожидаем, что будет свободное окно будет
     * от конца первого занятого слота (09:30) на втором гейте до начала второго (13:00) занятого слота на первом гейте.
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/border-line/before.xml"])
    fun borderLineSlotsTest() {
        setupLmsGateSchedule(
            from = LocalTime.of(9, 0),
            to = LocalTime.of(18, 0),
            workingDays = setOf(LocalDate.of(2021, 5, 17))
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T09:00")
        params.add("to", "2021-05-17T14:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/border-line/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    /**
     *  Сейчас 2021-5-11 12:00 [UTC] по Мск это 2021-5-11 15:00
     *  Поэтому слоты с 15:00 по времени склада
     */
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/today/before.xml"])
    fun showSlotsForTodayTest() {
        setUpMockFfwfGetQuota(dates = setOf(LocalDate.of(2021, 5, 11)))
        setupLmsGateSchedule(
            from = LocalTime.of(9, 0),
            to = LocalTime.of(18, 0),
            workingDays = setOf(LocalDate.of(2021, 5, 11))
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-11T09:00")
        params.add("to", "2021-05-11T17:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/today/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/last-hour-in-day/before.xml"])
    fun showLastHourInDayTest() {
        setupLmsGateSchedule(
            warehouseIds = listOf(1),
            from = LocalTime.of(0, 0),
            to = LocalTime.of(0, 0),
        )
        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 17),
                LocalDate.of(2021, 5, 18)
            )
        )
        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T00:00")
        params.add("to", "2021-05-18T14:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/last-hour-in-day/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    fun warehouseNightWorkdayTest() {
        setupLmsGateSchedule(
            warehouseIds = listOf(1),
            from = LocalTime.of(14, 46),
            to = LocalTime.of(3, 22),
        )
        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 17),
                LocalDate.of(2021, 5, 18)
            )
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T20:00")
        params.add("to", "2021-05-18T12:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/night-workday/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/outbound-one-supplier/before.xml"])
    fun showOutboundBookedSlotsForSameSupplierTest() {

        setupLmsGateSchedule(
            warehouseIds = listOf(1),
            from = LocalTime.of(9, 0),
            to = LocalTime.of(18, 0),
            gateType = GateTypeResponse.OUTBOUND,
            gates = setOf(1L),
        )

        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 15),
                LocalDate.of(2021, 5, 16)
            )
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T09:00")
        params.add("to", "2021-05-17T12:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "К101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/outbound-one-supplier/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/free-slots/outbound-one-supplier-null-id/before.xml"])
    fun doNotShowOutboundBookedSlotsForSameSupplierWithNullIdTest() {

        setupLmsGateSchedule(
            warehouseIds = listOf(1),
            from = LocalTime.of(9, 0),
            to = LocalTime.of(18, 0),
            gateType = GateTypeResponse.OUTBOUND,
            gates = setOf(1L),
        )

        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 15),
                LocalDate.of(2021, 5, 16)
            )
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.WITHDRAW.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T09:00")
        params.add("to", "2021-05-17T12:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("takenItems", "50")
        params.add("takenPallets", "2")

        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/outbound-one-supplier-null-id/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)

        verifyBasicFfwfCommunication(1, 0)
    }

    /**
     * Доступно 2 слота, но засчет заигноренного букинга добавляется еще один
     */
    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/free-slots/ignored-bookings/before.xml")
    fun ignoredBookingsIntersection() {

        setupLmsGateSchedule(
            warehouseIds = listOf(1),
            from = LocalTime.of(10, 0),
            to = LocalTime.of(12, 0),
            gateType = GateTypeResponse.INBOUND,
            gates = setOf(1L, 2L),
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T07:00")
        params.add("to", "2021-05-17T22:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")
        params.add("ignoredBookings", "1")


        val mvcResult: MvcResult = performGetNotFreeSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/ignored-bookings/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.LENIENT)
        verifyBasicFfwfCommunication(1, 0)
    }

    private fun performGetNotFreeSlots(params: MultiValueMap<String, String>): MvcResult {
        return mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/not-free-slots-reasons")
                .contentType(MediaType.APPLICATION_JSON)
                .params(params)
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    private fun performGetFreeSlots(params: MultiValueMap<String, String>): MvcResult {
        return mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/free-slots")
                .contentType(MediaType.APPLICATION_JSON)
                .params(params)
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    private fun getJsonResponseNoFile(name: String): String {
        return FileContentUtils.getFileContent("fixtures/controller/booking/not-free-slots/$name")
    }

}
