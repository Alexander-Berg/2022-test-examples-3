package ru.yandex.market.logistics.calendaring.report.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport
import ru.yandex.market.logistics.calendaring.client.dto.enums.*
import ru.yandex.market.logistics.calendaring.meta.field.model.FieldMapperProvider
import ru.yandex.market.logistics.calendaring.meta.field.service.MetaFieldMapperService
import ru.yandex.market.logistics.calendaring.meta.field.service.MetaFieldService
import ru.yandex.market.logistics.calendaring.model.domain.ExternalIdentifier
import ru.yandex.market.logistics.calendaring.model.domain.GateInfo
import ru.yandex.market.logistics.calendaring.model.domain.Slot
import ru.yandex.market.logistics.calendaring.model.dto.GatesScheduleDTO
import ru.yandex.market.logistics.calendaring.model.dto.MetaInfoDTO
import ru.yandex.market.logistics.calendaring.model.dto.booking.BookingWithMetaDTO
import ru.yandex.market.logistics.calendaring.report.model.ReportCell
import ru.yandex.market.logistics.calendaring.util.DateTimeInterval
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class BookingReportServiceImplUnitTest : SoftAssertionSupport() {

    private val metaFieldMapperService: MetaFieldMapperService = mock(MetaFieldMapperService::class.java)
    private val metaFieldsService: MetaFieldService = mock(MetaFieldService::class.java)
    private val service = BookingBuildReportServiceImpl(metaFieldMapperService, metaFieldsService)

    @Test
    fun staticFieldFilledCorrectlyTest() {

        `when`(metaFieldMapperService.getFieldMapperProvider()).thenReturn(FieldMapperProvider(mapOf()))
        `when`(metaFieldsService.findAllFields()).thenReturn(listOf())


        val bookings = listOf(
            BookingWithMetaDTO(
                1L,
                1L,
                null,
                BookingType.MOVEMENT_SUPPLY,
                ExternalIdentifier("ID1", "SOURCE1"),
                null,
                Slot(
                    1L,
                    DateTimeInterval(
                        ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("UTC"))
                    )
                ),
                BookingStatus.ACTIVE,
                SupplierType.FIRST_PARTY,
                "101",
                MetaInfoDTO(1L, mapOf(), RequestStatus.PROCESSED, 1L, null, 7, null),
                CalendaringType.INBOUND,
                LocalDate.of(2021, 5, 17),
                null,
                null,
                null,
                true,
            ),
            BookingWithMetaDTO(
                2L,
                1L,
                null,
                BookingType.MOVEMENT_SUPPLY,
                ExternalIdentifier("ID2", "SOURCE2"),
                null,
                Slot(
                    2L,
                    DateTimeInterval(
                        ZonedDateTime.of(2021, 5, 18, 11, 0, 0, 0, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021, 5, 18, 12, 0, 0, 0, ZoneId.of("UTC"))
                    )
                ),
                BookingStatus.ACTIVE,
                SupplierType.FIRST_PARTY,
                "101",
                null,
                CalendaringType.INBOUND,
                LocalDate.of(2021, 5, 18),
                null,
                null,
                null,
                true,
            )
        )

        val schedule = GatesScheduleDTO(1L, listOf(
            GateInfo(1L, "gateNumber1", true, EnumSet.of(GateTypeResponse.INBOUND), emptyList()),
            GateInfo(2L, "gateNumber2", true, EnumSet.of(GateTypeResponse.INBOUND), emptyList()),
        ), emptyList())
        val headerAndData: HeaderAndRowsDTO = service.getHeaderAndRows(bookings, schedule)

        softly.assertThat(headerAndData.rows.size).isEqualTo(2)

        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("2021-05-17", 0))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("2021-05-17", 1))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("10:00", 2))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("11:00", 3))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("gateNumber1", 5))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("PROCESSED", 13))

        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("2021-05-18", 0))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("2021-05-18", 1))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("11:00", 2))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("12:00", 3))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("gateNumber2", 5))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("", 13))

    }

    @Test
    fun staticFieldFilledCorrectlyTestWithGateIndividualSchedule() {

        `when`(metaFieldMapperService.getFieldMapperProvider()).thenReturn(FieldMapperProvider(mapOf()))
        `when`(metaFieldsService.findAllFields()).thenReturn(listOf())
        val mapOf: Map<String, Any> = mapOf("requestCreatedAt" to "2021-05-16T10:00");

        val bookings = listOf(
            BookingWithMetaDTO(
                1L,
                1L,
                null,
                BookingType.MOVEMENT_SUPPLY,
                ExternalIdentifier("ID1", "SOURCE1"),
                null,
                Slot(
                    1L,
                    DateTimeInterval(
                        ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("UTC"))
                    )
                ),
                BookingStatus.ACTIVE,
                SupplierType.FIRST_PARTY,
                "101",
                MetaInfoDTO(1L, mapOf("requestCreatedAt" to "2021-05-16T10:00"), RequestStatus.PROCESSED, 1L,
                    null, 7, null),
                CalendaringType.INBOUND,
                LocalDate.of(2021, 5, 17),
                null,
                null,
                null,
                true,
            ),
            BookingWithMetaDTO(
                2L,
                1L,
                null,
                BookingType.MOVEMENT_SUPPLY,
                ExternalIdentifier("ID2", "SOURCE2"),
                null,
                Slot(
                    2L,
                    DateTimeInterval(
                        ZonedDateTime.of(2021, 5, 18, 11, 0, 0, 0, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021, 5, 18, 12, 0, 0, 0, ZoneId.of("UTC"))
                    )
                ),
                BookingStatus.ACTIVE,
                SupplierType.FIRST_PARTY,
                "101",
                null,
                CalendaringType.INBOUND,
                LocalDate.of(2021, 5, 18),
                null,
                null,
                null,
                true,
            )
        )

        val schedule = GatesScheduleDTO(1L, listOf(
            GateInfo(1L, "gateNumber1", true, EnumSet.of(GateTypeResponse.INBOUND), emptyList()),
            GateInfo(2L, "gateNumber2", true, EnumSet.of(GateTypeResponse.INBOUND), emptyList()),
        ), emptyList())
        val headerAndData: HeaderAndRowsDTO = service.getHeaderAndRows(bookings, schedule)

        softly.assertThat(headerAndData.rows.size).isEqualTo(2)

        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("2021-05-17", 0))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("2021-05-17", 1))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("10:00", 2))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("11:00", 3))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("gateNumber1", 5))
        softly.assertThat(headerAndData.rows[0].cells).contains(ReportCell("PROCESSED", 13))

        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("2021-05-18", 0))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("2021-05-18", 1))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("11:00", 2))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("12:00", 3))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("gateNumber2", 5))
        softly.assertThat(headerAndData.rows[1].cells).contains(ReportCell("", 13))

    }

}
