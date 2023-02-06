package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.reporter.ReporterClient
import ru.yandex.market.wms.reporter.dto.ReportTaskDto
import ru.yandex.market.wms.reporter.dto.response.GetReportTasksResponse
import ru.yandex.market.wms.reporter.enums.ReportCode
import ru.yandex.market.wms.reporter.enums.ReportTaskStatus
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerGetTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @MockBean
    @Autowired
    private lateinit var reporterClient: ReporterClient

    @BeforeEach
    fun reset() {
        Mockito.reset(reporterClient)
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/get/common.xml")
    fun `GET shipments empty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments"),
            responseFile = "controller/shipping/shipments/get/shipments_empty_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments`() {
        Mockito.`when`(
            reporterClient
                .getReportTasks(
                    Mockito.eq(ReportCode.RECEIPTOUTBOUNDSACT),
                    Mockito.eq(listOf("5"))
                )
        ).thenReturn(
            GetReportTasksResponse.builder()
                .tasks(listOf(
                    ReportTaskDto.builder()
                        .uid("5")
                        .status(ReportTaskStatus.READY)
                        .build()
                ))
                .build()
        )

        Mockito.`when`(
            reporterClient
                .getReportTasks(
                    Mockito.eq(ReportCode.OUTBOUNDSACT),
                    Mockito.eq(listOf("4"))
                )
        ).thenReturn(
            GetReportTasksResponse.builder()
                .tasks(listOf(
                    ReportTaskDto.builder()
                        .uid("4")
                        .status(ReportTaskStatus.FAILED)
                        .build()
                ))
                .build()
        )

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments"),
            responseFile = "controller/shipping/shipments/get/shipments_default_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments with desc order and limit`() {
        reporterClientEmptyMock()

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?order=ASC&limit=2"),
            responseFile = "controller/shipping/shipments/get/shipments_asc_and_limit_response.json",
            compareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments with desc order limit and offset`() {

        Mockito.`when`(
            reporterClient
                .getReportTasks(
                    Mockito.any(ReportCode::class.java),
                    Mockito.argThat { it.contains("4") || it.contains("5") }
                )
        ).thenReturn(
            GetReportTasksResponse.builder()
                .tasks(listOf(
                    ReportTaskDto.builder()
                        .uid("4")
                        .status(ReportTaskStatus.IN_PROGRESS)
                        .build(),
                    ReportTaskDto.builder()
                        .uid("5")
                        .status(ReportTaskStatus.READY)
                        .build()
                ))
                .build()
        )

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?order=DESC&limit=3&offset=1"),
            responseFile = "controller/shipping/shipments/get/shipments_desc_limit_and_offset_response.json",
            compareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments with replacing sorting`() {
        reporterClientEmptyMock()

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?sort=carrierCodes"),
            responseFile = "controller/shipping/shipments/get/shipments_sort_by_shipment_id_response.json",
            compareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments with filter`() {
        reporterClientEmptyMock()

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?filter=shipmentId==4;vehicle==4WMS777;" +
                "type==STANDARD;door=='DOOR-4';status=='SHIPPED';" +
                "startTime==\"2021-01-01 03:00:00\";finishTime=ge=\"2021-01-01 03:00:00\";" +
                "carrierCodes=='CARRIER-01'"),
            responseFile = "controller/shipping/shipments/get/shipments_filter_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments with filter by carrier codes`() {
        reporterClientEmptyMock()

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?filter=status=='NEW';" +
                "(carrierCodes=='CARRIER-01',carrierCodes=='CARRIER-02')"),
            responseFile = "controller/shipping/shipments/get/shipments_filter_by_carrier_codes_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments with filter by old format carrier codes`() {
        reporterClientEmptyMock()

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?filter=status=='NEW';" +
                "(carrierCode=='CARRIER-01',carrierCode=='CARRIER-02')"),
            responseFile = "controller/shipping/shipments/get/shipments_filter_by_carrier_codes_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill_multi_carriers.xml"
    )
    fun `GET shipments with filter by multi carrier codes`() {
        reporterClientEmptyMock()

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?filter=" +
                "(carrierCodes=='CARRIER-01',carrierCodes=='CARRIER-02')"),
            responseFile = "controller/shipping/shipments/get/shipments_filter_by_multi_carrier_codes_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill.xml"
    )
    fun `GET shipments with withdrawal ids`() {
        reporterClientEmptyMock()

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments?filter=" +
                "(withdrawalIds==1,withdrawalIds==2,withdrawalIds==3,withdrawalIds==61,withdrawalIds==99)"),
            responseFile = "controller/shipping/shipments/get/shipments_withdrawal_ids_response.json"
        )
    }

    private fun reporterClientEmptyMock() {
        Mockito.`when`(
            reporterClient
                .getReportTasks(
                    Mockito.any(ReportCode::class.java),
                    Mockito.anyList()
                )
        )
            .then { throw Exception("Panic!") }
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill_cached_statuses.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/get/after_fill_cached_statuses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `GET shipments report statuses from cache`() {
        Mockito.`when`(
            reporterClient
                .getReportTasks(
                    Mockito.any(ReportCode::class.java),
                    Mockito.argThat { it.contains("3") || it.contains("5") }
                )
        ).thenThrow(AssertionError("Reports 3 and 5 has terminal cached status and should not be requested"))

        Mockito.`when`(
            reporterClient
                .getReportTasks(
                    Mockito.any(ReportCode::class.java),
                    Mockito.argThat { it.contains("2") || it.contains("4") }
                )
        ).thenReturn(
            GetReportTasksResponse.builder()
                .tasks(
                    listOf(
                        ReportTaskDto.builder()
                            .uid("2")
                            .status(ReportTaskStatus.IN_PROGRESS)
                            .build(),
                        ReportTaskDto.builder()
                            .uid("4")
                            .status(ReportTaskStatus.READY)
                            .build(),
                    )
                )
                .build()
        )

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments"),
            responseFile = "controller/shipping/shipments/get/shipments_cached_statuses_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/before_fill_cached_statuses.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/get/before_fill_cached_statuses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `GET shipments get cached report status when reporter failed`() {

        Mockito.`when`(
            reporterClient
                .getReportTasks(
                    Mockito.any(ReportCode::class.java),
                    Mockito.anyList()
                )
        )
            .then { throw Exception("Panic!") }

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments"),
            responseFile = "controller/shipping/shipments/get/shipments_cached_statuses_wo_reporter_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/get/common.xml",
        "/controller/shipping/shipments/get/two_drops_one_withdrawal.xml"
    )
    fun `GET shipment with two drops for one withdrawal id`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments"),
            responseFile = "controller/shipping/shipments/get/one_withdrawal_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/get/failed_reports.xml")
    fun `GET shipment with failed reports without reportId`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments"),
            responseFile = "controller/shipping/shipments/get/failed_reports_response.json"
        )
    }
}
