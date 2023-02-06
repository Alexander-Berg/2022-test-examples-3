package ru.yandex.market.wms.shipping.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.service.time.WarehouseDateTimeService
import ru.yandex.market.wms.reporter.ReporterClient
import ru.yandex.market.wms.reporter.dto.ReportTaskDto
import ru.yandex.market.wms.reporter.dto.response.GetReportResponse
import ru.yandex.market.wms.reporter.enums.ReportTaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

class VehicleShippingServiceTest(
    @Autowired private val vehicleShippingService: VehicleShippingService
) : IntegrationTest() {

    @MockBean
    @Autowired
    lateinit var reporterClient: ReporterClient

    @MockBean
    @Autowired
    lateinit var warehouseDateTimeService: WarehouseDateTimeService

    companion object {
        @JvmStatic
        private val dateTime = LocalDateTime.now().minusDays(10)
    }

    @BeforeEach
    fun setUp() {
        Mockito.reset(reporterClient)
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(warehouseDateTimeService.warehouseDateTimeNow)
            .thenReturn(dateTime)

        Mockito.`when`(warehouseDateTimeService.operationalDate)
            .thenReturn(LocalDate.now())

        Mockito.`when`(warehouseDateTimeService.shiftOperationalDate)
            .thenReturn(LocalDate.now())
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship standard vehicle happy`() = assertMakeReceiptOutbound()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-before-with-waves.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-after-with-waves.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship standard vehicle happy with waves`() = assertMakeReceiptOutbound()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-part-shipped-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-part-shipped-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship partially shipped standard vehicle`() = assertMakeReceiptOutbound()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-duty-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-duty-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship duty vehicle happy`() {
        val reporterResponse1 = makeReporterResponse("1")
        val reporterResponse2 = makeReporterResponse("2")
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            eq("CARRIER01"),
            anyList(),
            eq(dateTime.toLocalDate()),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(reporterResponse1)
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            eq("CARRIER02"),
            anyList(),
            eq(dateTime.toLocalDate()),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(reporterResponse2)
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(2))
            .makeReceiptOutboundsActTaskByDropIds(
                anyString(),
                anyList(),
                eq(dateTime.toLocalDate()),
                ArgumentMatchers.anyInt()
            )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-withdrawal-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-withdrawal-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship withdrawal vehicle happy`() {
        val reporterResponse1 = makeReporterResponse("1")
        val reporterResponse2 = makeReporterResponse("2")
        val reporterResponse3 = makeReporterResponse("3")
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0001")))
            .thenReturn(reporterResponse1)
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0002")))
            .thenReturn(reporterResponse2)
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0003")))
            .thenReturn(reporterResponse3)
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(3))
            .makeOutboundsActByOrderKey(anyString())
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-withdrawal-part-shipped-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-withdrawal-part-shipped-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship partially shipped withdrawal vehicle happy`() {
        val reporterResponse1 = makeReporterResponse("1")
        val reporterResponse2 = makeReporterResponse("2")
        val reporterResponse3 = makeReporterResponse("3")
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0001")))
            .thenReturn(reporterResponse1)
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0002")))
            .thenReturn(reporterResponse2)
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0003")))
            .thenReturn(reporterResponse3)
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(2))
            .makeOutboundsActByOrderKey(anyString())
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-drop-is-its-parent-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-drop-is-its-parent-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship standard vehicle with drop that is its own parent happy`() {
        val reporterResponse = makeReporterResponse("1")
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            eq("CARRIER01"),
            anyList(),
            eq(dateTime.toLocalDate()),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(reporterResponse)
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(1))
            .makeReceiptOutboundsActTaskByDropIds(
                anyString(),
                anyList(),
                eq(dateTime.toLocalDate()),
                ArgumentMatchers.anyInt()
            )
    }

    /** Тест на работу флага SHIPPING_STATUS_VALIDATION_OFF, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-part-shipped-drop-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-part-shipped-drop-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship vehicle with partially shipped drop without validation`() = assertMakeReceiptOutbound()

    /** Тест на работу флага SHIPPING_WRITE_OFF_ALL_UITS, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-missing-pickdetails-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-missing-pickdetails-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship vehicle with missing pickDetails without validation`() = assertMakeReceiptOutbound()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-reporter-fail-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-reporter-fail-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship standard vehicle reporter fail`() {
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            eq("CARRIER01"),
            anyList(),
            eq(dateTime.toLocalDate()),
            ArgumentMatchers.anyInt()
        ))
            .then { throw Exception("Panic!") }
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(1))
            .makeReceiptOutboundsActTaskByDropIds(
                anyString(),
                anyList(),
                eq(dateTime.toLocalDate()),
                ArgumentMatchers.anyInt()
            )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-not-full-withdrawal-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-not-full-withdrawal-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship not full withdrawal vehicle happy`() {
        val reporterResponse1 = makeReporterResponse("1")
        val reporterResponse3 = makeReporterResponse("3")
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0001")))
            .thenReturn(reporterResponse1)
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(eq("ORD0003")))
            .thenReturn(reporterResponse3)
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(2))
            .makeOutboundsActByOrderKey(anyString())
    }

    /** MARKETWMS-12350 */
    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/service/vehicle-shipping/db/ship-vehicle-one-pickdetail-three-lotxiddetails-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/vehicle-shipping/db/ship-vehicle-one-pickdetail-three-lotxiddetails-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship standard vehicle with one pickdetail line and multiple lotxiddetail lines`() {
        val reporterResponse = makeReporterResponse("1")
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            eq("CARRIER01"),
            anyList(),
            eq(dateTime.toLocalDate()),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(reporterResponse)
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(1))
            .makeReceiptOutboundsActTaskByDropIds(
                anyString(),
                anyList(),
                eq(dateTime.toLocalDate()),
                ArgumentMatchers.anyInt()
            )
    }

    private fun makeReporterResponse(uid: String) = GetReportResponse.builder()
        .task(ReportTaskDto.builder().uid(uid).status(ReportTaskStatus.NEW).build())
        .build()

    private fun assertMakeReceiptOutbound() {
        val reporterResponse = makeReporterResponse("1")
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            eq("CARRIER01"),
            anyList(),
            eq(dateTime.toLocalDate()),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(reporterResponse)
        vehicleShippingService.shipVehicle("DOOR_A", "test_user")
        Mockito.verify(reporterClient, Mockito.times(1))
            .makeReceiptOutboundsActTaskByDropIds(
                anyString(),
                anyList(),
                eq(dateTime.toLocalDate()),
                ArgumentMatchers.anyInt()
            )
    }
}
