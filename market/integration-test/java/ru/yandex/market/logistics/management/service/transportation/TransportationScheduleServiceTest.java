package ru.yandex.market.logistics.management.service.transportation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.yt.PartnerCutoffData;
import ru.yandex.market.logistics.management.domain.entity.yt.TransportationSchedule;
import ru.yandex.market.logistics.management.domain.entity.yt.TransportationType;
import ru.yandex.market.logistics.management.domain.entity.yt.YtDimensionsClass;
import ru.yandex.market.logistics.management.domain.entity.yt.YtHoliday;
import ru.yandex.market.logistics.management.domain.entity.yt.YtRoutingConfig;
import ru.yandex.market.logistics.management.domain.entity.yt.YtSchedule;
import ru.yandex.market.logistics.management.service.client.TransportationScheduleService;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@Sql("/data/service/transportation/transportation.sql")
class TransportationScheduleServiceTest extends AbstractContextualTest {

    @Autowired
    private TransportationScheduleService transportationScheduleService;

    @Test
    void drophipSelfExportToSc() {
        var groupedSchedules = transportationScheduleService.getTransportationSchedules();
        TransportationSchedule firstTransportation = groupedSchedules.get(0);
        softly.assertThat(firstTransportation)
            .usingRecursiveComparison()
            .ignoringCollectionOrderInFields("transportationSchedule", "transportationHolidays")
            .isEqualTo(
                TransportationSchedule.builder()
                    .outboundPartnerId(1L)
                    .outboundLogisticsPointId(10L)
                    .movingPartnerId(1L)
                    .movementSegmentId(2L)
                    .inboundPartnerId(2L)
                    .inboundLogisticsPointId(20L)
                    .transportationSchedule(getWeekDaySchedule())
                    .transportationHolidays(getSecondCalendar())
                    .volume(2000)
                    .weight(200)
                    .duration(24)
                    .transportationType(TransportationType.ORDERS_OPERATION)
                    .build()
            );
    }

    @Test
    void drophipsWithdrawBySc() {
        var groupedSchedules = transportationScheduleService.getTransportationSchedules();

        softly.assertThat(groupedSchedules.get(2)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(3L)
                .outboundLogisticsPointId(30L)
                .movingPartnerId(2L)
                .movementSegmentId(6L)
                .inboundPartnerId(2L)
                .inboundLogisticsPointId(20L)
                .transportationSchedule(getWeekEndSchedule())
                .transportationHolidays(getFirstCalendar())
                .duration(24)
                .transportationType(TransportationType.ORDERS_OPERATION)
                .build()
        );

        softly.assertThat(groupedSchedules.get(3)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(4L)
                .outboundLogisticsPointId(40L)
                .movingPartnerId(2L)
                .movementSegmentId(6L)
                .inboundPartnerId(2L)
                .inboundLogisticsPointId(20L)
                .transportationSchedule(getWeekEndSchedule())
                .transportationHolidays(getFirstCalendar())
                .duration(24)
                .transportationType(TransportationType.ORDERS_OPERATION)
                .build()
        );

    }

    @Test
    void dropship3rdParty() {
        var groupedSchedules = transportationScheduleService.getTransportationSchedules();

        softly.assertThat(groupedSchedules.get(4))
                .usingRecursiveComparison()
                .ignoringCollectionOrderInFields("transportationSchedule", "transportationHolidays")
                .isEqualTo(
                        TransportationSchedule.builder()
                                .outboundPartnerId(5L)
                                .outboundLogisticsPointId(50L)
                                .movingPartnerId(6L)
                                .movementSegmentId(9L)
                                .inboundPartnerId(2L)
                                .inboundLogisticsPointId(20L)
                                .transportationSchedule(getWeekDaySchedule())
                                .transportationHolidays(getFirstCalendar())
                                .duration(24)
                                .transportationType(TransportationType.ORDERS_OPERATION)
                                .routingConfig(new YtRoutingConfig(
                                        true,
                                        YtDimensionsClass.REGULAR_CARGO,
                                        new BigDecimal("1.0"),
                                        1.0d,
                                        true,
                                        "location-group-tag"
                                ))
                                .build()

                );
    }

    @Test
    void deliveryServiceReturnToFf() {
        var groupedSchedules = transportationScheduleService.getTransportationSchedules();

        softly.assertThat(groupedSchedules.get(1))
            .usingRecursiveComparison()
            .ignoringCollectionOrderInFields("transportationSchedule", "transportationHolidays")
            .isEqualTo(
                TransportationSchedule.builder()
                    .outboundPartnerId(1L)
                    .outboundLogisticsPointId(10L)
                    .movingPartnerId(1L)
                    .movementSegmentId(7L)
                    .inboundPartnerId(2L)
                    .inboundLogisticsPointId(20L)
                    .transportationSchedule(getWeekEndSchedule())
                    .transportationHolidays(getSecondCalendar())
                    .duration(24)
                    .transportationType(TransportationType.ORDERS_RETURN)
                    .build()
            );
    }

    @Test
    @Sql({"/data/service/transportation/transportation.sql", "/data/service/transportation/transportation_faulty.sql"})
    void drophipSelfExportToScSkippedFaulty() {

        var groupedSchedules = transportationScheduleService.getTransportationSchedules();
        softly.assertThat(groupedSchedules).hasSize(2);
    }

    @Test
    @Sql({"/data/service/transportation/transportation.sql",
        "/data/service/transportation/transportation_disabled.sql"})
    void drophipSelfExportToScSkippedInactive() {

        var groupedSchedules = transportationScheduleService.getTransportationSchedules();
        softly.assertThat(groupedSchedules).hasSize(3);
    }

    @Test
    @Sql({
        "/data/service/transportation/transportation.sql",
        "/data/service/transportation/transportation_faulty.sql",
        "/data/service/transportation/interwarehouse_transportation.sql"
    })
    void withInterwarehouseTransportations() {
        var groupedSchedules = transportationScheduleService.getTransportationSchedules();

        groupedSchedules.sort(
            Comparator.comparing(TransportationSchedule::getOutboundLogisticsPointId)
                .thenComparing(TransportationSchedule::getInboundLogisticsPointId)
                .thenComparing(TransportationSchedule::getTransportationType)
        );

        softly.assertThat(groupedSchedules).hasSize(7);

        softly.assertThat(groupedSchedules.get(5)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(7L)
                .outboundLogisticsPointId(60L)
                .inboundPartnerId(8L)
                .inboundLogisticsPointId(70L)
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(10, 0), LocalTime.of(15, 0), true, 15, 1L),
                    new YtSchedule(2L, 2, LocalTime.of(17, 0), LocalTime.of(19, 0), true, 5, null),
                    new YtSchedule(3L, 2, LocalTime.of(12, 0), LocalTime.of(14, 0), true, 5, null)
                ))
                .transportationHolidays(List.of())
                .transportationType(TransportationType.XDOC_TRANSPORT)
                .build()
        );

        softly.assertThat(groupedSchedules.get(6)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(7L)
                .outboundLogisticsPointId(60L)
                .inboundPartnerId(8L)
                .inboundLogisticsPointId(70L)
                .transportationSchedule(List.of(
                    new YtSchedule(4L, 2, LocalTime.of(17, 0), LocalTime.of(19, 0), true, 5, 5L)
                ))
                .transportationHolidays(List.of())
                .transportationType(TransportationType.LINEHAUL)
                .build()
        );

        softly.assertThat(groupedSchedules.get(3)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(2L)
                .outboundLogisticsPointId(20L)
                .inboundPartnerId(8L)
                .inboundLogisticsPointId(80L)
                .transportationSchedule(List.of(
                    new YtSchedule(7L, 7, LocalTime.of(13, 0), LocalTime.of(20, 0), true, 10, 2L)
                ))
                .transportationHolidays(getFirstCalendar())
                .transportationType(TransportationType.XDOC_TRANSPORT)
                .build()
        );

        softly.assertThat(groupedSchedules.get(1)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(2L)
                .outboundLogisticsPointId(20L)
                .inboundPartnerId(7L)
                .inboundLogisticsPointId(60L)
                .transportationSchedule(List.of(
                    new YtSchedule(5L, 5, LocalTime.of(16, 0), LocalTime.of(19, 0), true, 19, 6L)
                ))
                .transportationHolidays(getFirstCalendar())
                .transportationType(TransportationType.LINEHAUL)
                .build()
        );
    }

    @Test
    @Sql({
        "/data/service/transportation/transportation.sql",
        "/data/service/transportation/dropoff_transportation.sql"
    })
    void dropoffMovement() {
        var groupedSchedules = transportationScheduleService.getTransportationSchedules();

        groupedSchedules.sort(
            Comparator.comparing(TransportationSchedule::getOutboundLogisticsPointId)
                .thenComparing(TransportationSchedule::getInboundLogisticsPointId)
                .thenComparing(TransportationSchedule::getTransportationType)
        );

        softly.assertThat(groupedSchedules).hasSize(8);

        softly.assertThat(groupedSchedules.get(2)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(1L)
                .outboundLogisticsPointId(10L)
                .movingPartnerId(1L)
                .movementSegmentId(12L)
                .inboundPartnerId(4L)
                .inboundLogisticsPointId(100L)
                .transportationSchedule(getWeekDaySchedule())
                .transportationHolidays(List.of(
                    new YtHoliday(LocalDate.of(2021, 5, 2), 1L),
                    new YtHoliday(LocalDate.of(2021, 5, 7), 1L),
                    new YtHoliday(LocalDate.of(2021, 5, 2), 4L),
                    new YtHoliday(LocalDate.of(2021, 5, 7), 4L)
                ))
                .duration(24)
                .transportationType(TransportationType.ORDERS_OPERATION)
                .build()
        );

        softly.assertThat(groupedSchedules.get(6)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(4L)
                .outboundLogisticsPointId(100L)
                .movingPartnerId(1L)
                .movementSegmentId(11L)
                .inboundPartnerId(2L)
                .inboundLogisticsPointId(20L)
                .transportationSchedule(getWeekDaySchedule())
                .transportationHolidays(getThirdCalendar())
                .duration(24)
                .transportationType(TransportationType.ORDERS_OPERATION)
                .build()
        );

        softly.assertThat(groupedSchedules.get(7)).isEqualTo(
            TransportationSchedule.builder()
                .outboundPartnerId(4L)
                .outboundLogisticsPointId(110L)
                .movingPartnerId(1L)
                .movementSegmentId(15L)
                .inboundPartnerId(2L)
                .inboundLogisticsPointId(20L)
                .transportationSchedule(getWeekDaySchedule())
                .transportationHolidays(List.of(
                    new YtHoliday(LocalDate.of(2021, 5, 2), 4L),
                    new YtHoliday(LocalDate.of(2021, 5, 7), 4L),
                    new YtHoliday(LocalDate.of(2021, 5, 2), 2L),
                    new YtHoliday(LocalDate.of(2021, 5, 9), 2L),
                    new YtHoliday(LocalDate.of(2021, 5, 10), 2L)
                ))
                .duration(24)
                .transportationType(TransportationType.ORDERS_OPERATION)
                .build()
        );
    }

    @Test
    @Sql("/data/service/transportation/dropship_cutoff.sql")
    void dropshipCutoff() {
        var schedules = transportationScheduleService.getTransportationSchedules();

        PartnerCutoffData cutoffData = new PartnerCutoffData();
        cutoffData.setCutoffTime(LocalTime.of(18, 0));
        cutoffData.setWarehouseOffsetSeconds(3 * 60 * 60);
        cutoffData.setHandlingTimeDays(1);

        softly.assertThat(schedules)
            .extracting(TransportationSchedule::getPartnerCutoffData)
            .containsExactly(cutoffData);
    }

    private List<YtSchedule> getWeekDaySchedule() {
        return List.of(
            new YtSchedule(1L, 1, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(2L, 2, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(3L, 3, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(4L, 4, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(5L, 5, LocalTime.of(10, 0), LocalTime.of(18, 0), false)
        );
    }

    private List<YtSchedule> getWeekEndSchedule() {
        return List.of(
            new YtSchedule(6L, 6, LocalTime.of(15, 0), LocalTime.of(20, 0), false),
            new YtSchedule(7L, 7, LocalTime.of(15, 0), LocalTime.of(20, 0), false)
        );
    }

    private List<YtHoliday> getFirstCalendar() {
        return List.of(
            new YtHoliday(LocalDate.of(2021, 5, 2), 2L),
            new YtHoliday(LocalDate.of(2021, 5, 9), 2L),
            new YtHoliday(LocalDate.of(2021, 5, 10), 2L)
        );
    }

    private List<YtHoliday> getSecondCalendar() {
        return List.of(
            new YtHoliday(LocalDate.of(2021, 5, 2), 1L),
            new YtHoliday(LocalDate.of(2021, 5, 7), 1L),
            new YtHoliday(LocalDate.of(2021, 5, 2), 2L),
            new YtHoliday(LocalDate.of(2021, 5, 9), 2L),
            new YtHoliday(LocalDate.of(2021, 5, 10), 2L)
        );
    }

    private List<YtHoliday> getThirdCalendar() {
        return List.of(
            new YtHoliday(LocalDate.of(2021, 5, 2), 4L),
            new YtHoliday(LocalDate.of(2021, 5, 7), 4L),
            new YtHoliday(LocalDate.of(2021, 5, 2), 2L),
            new YtHoliday(LocalDate.of(2021, 5, 9), 2L),
            new YtHoliday(LocalDate.of(2021, 5, 10), 2L)
        );
    }
}
