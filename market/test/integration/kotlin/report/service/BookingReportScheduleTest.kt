package ru.yandex.market.logistics.calendaring.report.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.CalendaringType
import ru.yandex.market.logistics.calendaring.model.dto.ExportReportDTO
import ru.yandex.market.logistics.calendaring.report.model.Report
import ru.yandex.market.logistics.calendaring.report.model.ReportCell
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseTimezoneService
import ru.yandex.market.logistics.calendaring.util.DateTimeInterval
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.management.entity.response.core.Address
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class BookingReportScheduleTest(
        @Autowired private val geobaseProviderApi: GeobaseProviderApi,
        @Autowired val service: BookingReportService,
) : AbstractContextualTest() {

    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/report/service/schedule/before.xml"])
    fun headerFilledSuccessfullyTest() {

        setupLmsGateSchedule()

        val from = LocalDate.of(2021, 5, 17)
        val to = LocalDate.of(2021, 5, 19)

        val exportReportDTO = ExportReportDTO(CalendaringType.INBOUND, 1, from, to)

        val report: Report = service.getReport(exportReportDTO)

        softly.assertThat(report.rows.size).isEqualTo(2)

        softly.assertThat(report.rows[0].cells).contains(ReportCell("12:30", 8))
        softly.assertThat(report.rows[0].cells).contains(ReportCell("20:30", 9))

        softly.assertThat(report.rows[1].cells).contains(ReportCell("09:00", 8))
        softly.assertThat(report.rows[1].cells).contains(ReportCell("18:00", 9))
    }


    private fun setupLmsGateSchedule() {
        Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(any(), any(), any())).thenReturn(
            MockParametersHelper.mockGatesScheduleResponse(
                MockParametersHelper.mockAvailableGatesResponse(
                    setOf(1L, 2L), EnumSet.of(GateTypeResponse.OUTBOUND),
                    listOf(
                        ScheduleDateTimeResponse.newBuilder().date(LocalDate.of(2021, 5, 17)).from(LocalTime.of(9, 0))
                            .to(LocalTime.of(18, 0)).build(),

                        ScheduleDateTimeResponse.newBuilder().date(LocalDate.of(2021, 5, 18)).from(LocalTime.of(12, 30))
                            .to(LocalTime.of(20, 30)).build(),
                    )
                ),
            )
        )
    }

}
