package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.reporter.ReporterClient
import ru.yandex.market.wms.reporter.dto.ReportTaskDto
import ru.yandex.market.wms.reporter.dto.response.GetReportResponse
import ru.yandex.market.wms.reporter.enums.ReportTaskStatus
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerRetryReportTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @MockBean
    @Autowired
    private lateinit var reporterClient: ReporterClient

    @BeforeEach
    fun reset() {
        Mockito.reset(reporterClient)
    }

    private fun makeReporterResponse(uid: String) = GetReportResponse.builder()
        .task(ReportTaskDto.builder().uid(uid).status(ReportTaskStatus.NEW).build())
        .build()

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/happy/standard_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/happy/standard_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report standard`() {
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            ArgumentMatchers.eq("Test carrier 1"),
            ArgumentMatchers.anyList(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(makeReporterResponse("10"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/retry-report/standard_request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/happy/standard_null_reportid_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/happy/standard_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report standard null reportId`() {
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            ArgumentMatchers.eq("Test carrier 1"),
            ArgumentMatchers.anyList(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(makeReporterResponse("10"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/retry-report/standard_request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/happy/withdrawal_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/happy/withdrawal_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report withdrawal`() {
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(
            ArgumentMatchers.eq("1"),
        ))
            .thenReturn(makeReporterResponse("10"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/retry-report/withdrawal_request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/happy/withdrawal_null_reportid_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/happy/withdrawal_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report withdrawal null reportid`() {
        Mockito.`when`(
            reporterClient.makeOutboundsActByOrderKey(
                ArgumentMatchers.eq("1"),
            )
        )
            .thenReturn(makeReporterResponse("10"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/retry-report/withdrawal_request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report error shipment not found`() {
        Mockito
            .`when`(reporterClient.makeOutboundsActByOrderKey(ArgumentMatchers.any()))
            .thenThrow(AssertionError("There should not be any requests to reporter"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/retry-report/standard_request.json",
            responseFile = "controller/shipping/retry-report/error/standard_details_not_found.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/error/standard_details_not_found.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/error/standard_details_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report error standard detail not found`() {
        Mockito
            .`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyInt()
            ))
            .thenThrow(AssertionError("There should not be any requests to reporter"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/retry-report/standard_request.json",
            responseFile = "controller/shipping/retry-report/error/standard_details_not_found.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/error/standard_null_reportid_details_not_found.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/error/standard_null_reportid_details_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report error standard null reportid detail not found`() {
        Mockito
            .`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyInt()
            ))
            .thenThrow(AssertionError("There should not be any requests to reporter"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/retry-report/standard_request.json",
            responseFile = "controller/shipping/retry-report/error/standard_details_not_found.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/error/withdrawal_details_not_found.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/error/withdrawal_details_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report error withdrawal detail not found`() {
        Mockito
            .`when`(reporterClient.makeOutboundsActByOrderKey(ArgumentMatchers.any()))
            .thenThrow(AssertionError("There should not be any requests to reporter"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/retry-report/withdrawal_request.json",
            responseFile = "controller/shipping/retry-report/error/withdrawal_details_not_found.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/error/withdrawal_null_reportid_details_not_found.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/error/withdrawal_null_reportid_details_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report error withdrawal null reportid detail not found`() {
        Mockito
            .`when`(reporterClient.makeOutboundsActByOrderKey(ArgumentMatchers.any()))
            .thenThrow(AssertionError("There should not be any requests to reporter"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/retry-report/withdrawal_request.json",
            responseFile = "controller/shipping/retry-report/error/withdrawal_details_not_found.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/error/standard_reporter_fail_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/error/standard_reporter_fail_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )

    fun `retry report standard reporter fail`() {
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            ArgumentMatchers.eq("Test carrier 1"),
            ArgumentMatchers.anyList(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyInt()
        ))
            .then { throw Exception("Panic!") }

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/retry-report/standard_request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/error/withdrawal_reporter_fail_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/error/withdrawal_reporter_fail_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report withdrawal reporter fail`() {
        Mockito.`when`(reporterClient.makeOutboundsActByOrderKey(
            ArgumentMatchers.eq("1"),
        ))
            .then { throw Exception("Panic!") }

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/retry-report/withdrawal_request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/happy/standard_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/happy/standard_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report standard empty request`() {
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            ArgumentMatchers.eq("Test carrier 1"),
            ArgumentMatchers.anyList(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(makeReporterResponse("10"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/retry-report/error/standard_empty_request.json",
            responseFile = "controller/shipping/retry-report/error/standard_empty_request_response.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/retry-report/common.xml",
        "/controller/shipping/retry-report/happy/standard_before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/retry-report/happy/standard_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `retry report standard multiple request`() {
        Mockito.`when`(reporterClient.makeReceiptOutboundsActTaskByDropIds(
            ArgumentMatchers.eq("Test carrier 1"),
            ArgumentMatchers.anyList(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyInt()
        ))
            .thenReturn(makeReporterResponse("10"))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/retry-report"),
            MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/retry-report/error/standard_multiple_request.json",
            responseFile = "controller/shipping/retry-report/error/standard_multiple_request_response.json",
        )
    }
}
