package ru.yandex.market.tsup.core.pipeline.cube.quick_trip;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.cube.QuickTripScheduleConverterCube;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.CourierId;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.QuickTripInitPayload;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.QuickTripScheduleConverterInput;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.TransportId;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleCourierData;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleModificationConvertedData;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleStatus;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleSubtype;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleType;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.PointParams;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.TransportInfo;
import ru.yandex.market.tsup.domain.entity.tsup_properties.TsupProperty;
import ru.yandex.market.tsup.domain.entity.tsup_properties.TsupPropertyKey;
import ru.yandex.market.tsup.repository.mappers.TsupPropertyMapper;

public class QuickTripScheduleConverterCubeTest extends AbstractContextualTest {
    @Autowired
    private QuickTripScheduleConverterCube cube;
    @Autowired
    private TsupPropertyMapper tsupPropertyMapper;

    @Test
    void execute() {
        List<PointParams> points = List.of(
            new PointParams()
                .setIndex(0)
                .setArrivalStartTime(LocalTime.of(10, 0))
                .setArrivalEndTime(LocalTime.of(11, 0)),
            new PointParams()
                .setIndex(1)
                .setArrivalStartTime(LocalTime.of(13, 0))
                .setArrivalEndTime(LocalTime.of(15, 0)));

        clock.setFixed(Instant.parse("2022-03-05T10:00:00Z"), ZoneOffset.UTC);
        QuickTripScheduleConverterInput input = new QuickTripScheduleConverterInput()
            .setCourierId(new CourierId(1L))
            .setTransportId(new TransportId(5L))
            .setPayload(new QuickTripInitPayload()
                .setRouteId(11L)
                .setTransportInfo(new TransportInfo(55L, 10, null, 2000L))
                .setType(RouteScheduleType.LINEHAUL)
                .setPointParams(points)
            );

        var result = cube.execute(input);
        softly.assertThat(result).isEqualTo(
            RouteScheduleModificationConvertedData.builder()
                .daysOfWeek(List.of(DayOfWeek.SATURDAY))
                .scheduleStartDate(LocalDate.now(clock))
                .scheduleEndDate(LocalDate.now(clock))
                .movingPartnerId(55L)
                .points(points)
                .routeId(11L)
                .maxPallet(10)
                .price(200000L)
                .status(RouteScheduleStatus.ACTIVE)
                .type(RouteScheduleType.LINEHAUL)
                .subtype(RouteScheduleSubtype.UNSCHEDULED)
                .courier(new RouteScheduleCourierData(1L, 5L))
                .build()
        );
    }

    @Test
    void executeWithLag() {
        tsupPropertyMapper.insert(new TsupProperty(
            111111L, TsupPropertyKey.QUICK_TRIP_NEXT_DAY_BACKWARD_LAG_MINUTES, "15")
        );
        List<PointParams> points = List.of(
            new PointParams()
                .setIndex(0)
                .setArrivalStartTime(LocalTime.of(10, 0))
                .setArrivalEndTime(LocalTime.of(11, 0)),
            new PointParams()
                .setIndex(1)
                .setArrivalStartTime(LocalTime.of(13, 0))
                .setArrivalEndTime(LocalTime.of(15, 0)));

        clock.setFixed(Instant.parse("2022-03-05T10:14:00Z"), ZoneOffset.UTC);
        QuickTripScheduleConverterInput input = new QuickTripScheduleConverterInput()
            .setCourierId(new CourierId(1L))
            .setTransportId(new TransportId(5L))
            .setPayload(new QuickTripInitPayload()
                .setRouteId(11L)
                .setTransportInfo(new TransportInfo(55L, 10, null, 2000L))
                .setType(RouteScheduleType.LINEHAUL)
                .setPointParams(points)
            );

        var result = cube.execute(input);
        softly.assertThat(result).isEqualTo(
            RouteScheduleModificationConvertedData.builder()
                .daysOfWeek(List.of(DayOfWeek.SATURDAY))
                .scheduleStartDate(LocalDate.now(clock))
                .scheduleEndDate(LocalDate.now(clock))
                .movingPartnerId(55L)
                .points(points)
                .routeId(11L)
                .maxPallet(10)
                .price(200000L)
                .status(RouteScheduleStatus.ACTIVE)
                .type(RouteScheduleType.LINEHAUL)
                .subtype(RouteScheduleSubtype.UNSCHEDULED)
                .courier(new RouteScheduleCourierData(1L, 5L))
                .build()
        );
    }

    @Test
    void executeNextDay() {
        List<PointParams> points = List.of(
            new PointParams()
                .setIndex(0)
                .setArrivalStartTime(LocalTime.of(9, 0))
                .setArrivalEndTime(LocalTime.of(10, 0)),
            new PointParams()
                .setIndex(1)
                .setArrivalStartTime(LocalTime.of(13, 0))
                .setArrivalEndTime(LocalTime.of(15, 0)));

        clock.setFixed(Instant.parse("2022-03-05T10:00:00Z"), ZoneOffset.UTC);
        QuickTripScheduleConverterInput input = new QuickTripScheduleConverterInput()
            .setCourierId(new CourierId(1L))
            .setTransportId(new TransportId(5L))
            .setPayload(new QuickTripInitPayload()
                .setRouteId(11L)
                .setTransportInfo(new TransportInfo(55L, 10, null, 2000L))
                .setType(RouteScheduleType.LINEHAUL)
                .setPointParams(points)
            );

        var result = cube.execute(input);
        softly.assertThat(result).isEqualTo(
            RouteScheduleModificationConvertedData.builder()
                .daysOfWeek(List.of(DayOfWeek.SUNDAY))
                .scheduleStartDate(LocalDate.now(clock).plusDays(1))
                .scheduleEndDate(LocalDate.now(clock).plusDays(1))
                .movingPartnerId(55L)
                .points(points)
                .routeId(11L)
                .maxPallet(10)
                .price(200000L)
                .status(RouteScheduleStatus.ACTIVE)
                .type(RouteScheduleType.LINEHAUL)
                .subtype(RouteScheduleSubtype.UNSCHEDULED)
                .courier(new RouteScheduleCourierData(1L, 5L))
                .build()
        );
    }

    @Test
    void executeCurrentDayNearMidnight() {
        List<PointParams> points = List.of(
            new PointParams()
                .setIndex(0)
                .setArrivalStartTime(LocalTime.of(9, 0))
                .setArrivalEndTime(LocalTime.of(10, 0)),
            new PointParams()
                .setIndex(1)
                .setArrivalStartTime(LocalTime.of(13, 0))
                .setArrivalEndTime(LocalTime.of(15, 0)));

        clock.setFixed(Instant.parse("2022-03-05T00:20:00Z"), ZoneOffset.UTC);
        QuickTripScheduleConverterInput input = new QuickTripScheduleConverterInput()
            .setCourierId(new CourierId(1L))
            .setTransportId(new TransportId(5L))
            .setPayload(new QuickTripInitPayload()
                .setRouteId(11L)
                .setTransportInfo(new TransportInfo(55L, 10, null, 2000L))
                .setType(RouteScheduleType.LINEHAUL)
                .setPointParams(points)
            );

        var result = cube.execute(input);
        softly.assertThat(result).isEqualTo(
            RouteScheduleModificationConvertedData.builder()
                .daysOfWeek(List.of(DayOfWeek.SATURDAY))
                .scheduleStartDate(LocalDate.now(clock))
                .scheduleEndDate(LocalDate.now(clock))
                .movingPartnerId(55L)
                .points(points)
                .routeId(11L)
                .maxPallet(10)
                .price(200000L)
                .status(RouteScheduleStatus.ACTIVE)
                .type(RouteScheduleType.LINEHAUL)
                .subtype(RouteScheduleSubtype.UNSCHEDULED)
                .courier(new RouteScheduleCourierData(1L, 5L))
                .build()
        );
    }

    @Test
    void executeForDutyRun() {
        List<PointParams> points = List.of(
            new PointParams()
                .setIndex(0)
                .setArrivalStartTime(LocalTime.of(10, 0))
                .setArrivalEndTime(LocalTime.of(11, 0)),
            new PointParams()
                .setIndex(1)
                .setArrivalStartTime(LocalTime.of(13, 0))
                .setArrivalEndTime(LocalTime.of(15, 0)));

        clock.setFixed(Instant.parse("2022-03-05T10:00:00Z"), ZoneOffset.UTC);
        QuickTripScheduleConverterInput input = new QuickTripScheduleConverterInput()
            .setCourierId(new CourierId(1L))
            .setTransportId(new TransportId(5L))
            .setPayload(new QuickTripInitPayload()
                .setRouteId(11L)
                .setTransportInfo(new TransportInfo(55L, 10, null, 2000L))
                .setType(RouteScheduleType.LINEHAUL)
                .setPointParams(points)
                .setRunId(10001L)
                .setSubtype(RouteScheduleSubtype.DUTY)
            );

        var result = cube.execute(input);
        softly.assertThat(result).isEqualTo(
            RouteScheduleModificationConvertedData.builder()
                .daysOfWeek(List.of(DayOfWeek.SATURDAY))
                .scheduleStartDate(LocalDate.now(clock))
                .scheduleEndDate(LocalDate.now(clock))
                .movingPartnerId(55L)
                .points(points)
                .routeId(11L)
                .maxPallet(10)
                .price(200000L)
                .status(RouteScheduleStatus.ACTIVE)
                .type(RouteScheduleType.LINEHAUL)
                .subtype(RouteScheduleSubtype.DUTY)
                .runId(10001L)
                .courier(new RouteScheduleCourierData(1L, 5L))
                .build()
        );
    }

}
