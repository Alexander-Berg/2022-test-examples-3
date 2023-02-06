package ru.yandex.market.logistics.calendaring.report.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.any
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.CalendaringType
import ru.yandex.market.logistics.calendaring.model.dto.ExportReportDTO
import ru.yandex.market.logistics.calendaring.report.model.Report
import ru.yandex.market.logistics.calendaring.report.model.ReportCell
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateCustomScheduleResponse
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount
import java.time.LocalDate
import java.util.*

class BookingReportServiceImplTest(
    @Autowired val service: BookingReportService,
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) : AbstractContextualTest() {

    @BeforeEach
    fun init(){
        setUpMockLmsGetLocationZone(geobaseProviderApi)
    }

    @Test
    @JpaQueriesCount(6)
    @DatabaseSetup(value = ["classpath:fixtures/report/service/map-fields/before.xml"])
    fun mapFieldsSuccessfullyTest() {

        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 18)

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, 1, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(2)

        softly.assertThat(report.rows[0].cells).contains(ReportCell("s1t1", 6))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("s1t2", 7))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("s1t3", 8))

        softly.assertThat(report.rows[1].cells).contains(ReportCell("s2t1", 6))
        softly.assertThat(report.rows[1].cells).contains(ReportCell("s2t2", 7))
        softly.assertThat(report.rows[1].cells).contains(ReportCell("s2t3", 8))

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/report/service/when-no-mapping/before.xml"])
    fun mapFieldsSuccessfullyWhenNoMappingExistsTest() {

        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 18)

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, 1, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(2)

        softly.assertThat(report.rows[0].cells).doesNotContain(ReportCell("s1t1", 6))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("s1t2", 7))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("s1t3", 8))

        softly.assertThat(report.rows[1].cells).contains(ReportCell("s2t1", 6))
        softly.assertThat(report.rows[1].cells).contains(ReportCell("s2t2", 7))
        softly.assertThat(report.rows[1].cells).doesNotContain(ReportCell("s2t2", 8))

    }


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/report/service/when-no-field/before.xml"])
    fun mapFieldsSuccessfullyWhenNoFieldExistTest() {

        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 18)

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, 1, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(2)

        softly.assertThat(report.rows[0].cells).contains(ReportCell("s1t1", 6))
        softly.assertThat(report.rows[1].cells).contains(ReportCell("s2t3", 8))

    }


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/report/service/map-fields/before.xml"])
    fun headerFilledSuccessfullyTest() {

        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 18)

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, 1, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(2)

        softly.assertThat(report.header.cells).contains(ReportCell(WORKING_DAY_BEGIN_COL_TITLE, 8))
        softly.assertThat(report.header.cells).contains(ReportCell(WORKING_DAY_END_COL_TITLE, 9))

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/report/service/map-meta-fields/before.xml"])
    fun testReportMeta() {

        val warehouseId: Long = 1
        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 18)

        setUpLmsClient()

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, warehouseId, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(1)

        softly.assertThat(report.rows[0].cells).contains(ReportCell("externalRequestId", 4))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("12345", 5))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("ffwfId", 6))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/report/service/map-meta-all-default-fields/before.xml"])
    fun testReportAllDefaultMappingMeta() {

        val warehouseId: Long = 1
        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 18)

        setUpLmsClient()

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, warehouseId, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(1)

        softly.assertThat(report.rows[0].cells).contains(ReportCell("externalRequestId", 4))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("12345", 5))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("ffwfId", 6))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/report/service/map-meta-one-default-fields/before.xml"])
    fun testReportOneDefaultMappingMeta() {

        val warehouseId: Long = 1
        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 18)

        setUpLmsClient()

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, warehouseId, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(1)

        softly.assertThat(report.rows[0].cells).contains(ReportCell("externalRequestId", 4))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("12345", 5))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("ffwfId", 6))
    }

    private fun setUpLmsClient() {

        Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(any(), any(), any())).thenReturn(
            MockParametersHelper.mockGatesScheduleResponse(

                listOf<LogisticsPointGateCustomScheduleResponse>(
                    LogisticsPointGateCustomScheduleResponse.newBuilder()
                        .id(123)
                        .enabled(true)
                        .gateNumber("12345")
                        .schedule(listOf())
                        .types(EnumSet.of(GateTypeResponse.INBOUND))
                        .build()
                )
            )
        )
    }

    companion object {
        private const val WORKING_DAY_BEGIN_COL_TITLE = "Начало работы склада"
        private const val WORKING_DAY_END_COL_TITLE = "Окончание работы склада"
    }

}
