package ru.yandex.market.tsup.core.pipeline.validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.ConstraintValidatorContext;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleType;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.PointParams;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.RouteScheduleModificationPayload;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.ScheduleInfo;

class PointParamsValidatorUnitNoBookingTest {

    private final PointParamsValidator validator = new PointParamsValidator();
    private ConstraintValidatorContext context;

    @BeforeEach
    void setup() {
        context = Mockito.mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
            Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.anyString())).thenReturn(builder);
    }

    @ParameterizedTest
    @MethodSource("scheduleParams")
    void testValidation1(RouteScheduleType scheduleType, boolean bookingEnabled, boolean singleDay) {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(1, 0, LocalTime.of(13, 0), LocalTime.of(14, 0))
        ), scheduleType, bookingEnabled, singleDay), context)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("scheduleParams")
    void testValidation2(RouteScheduleType scheduleType, boolean bookingEnabled, boolean singleDay) {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(1, 60, LocalTime.of(13, 0), LocalTime.of(14, 0))
        ), scheduleType, bookingEnabled, singleDay), context)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("scheduleParams")
    void testValidation3(RouteScheduleType scheduleType, boolean bookingEnabled, boolean singleDay) {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(1, 1440, LocalTime.of(12, 0), LocalTime.of(13, 0))
        ), scheduleType, bookingEnabled, singleDay), context)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("scheduleParams")
    void testValidation4(RouteScheduleType scheduleType, boolean bookingEnabled, boolean singleDay) {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(1, 0, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0))
        ), scheduleType, bookingEnabled, singleDay), context)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("scheduleParams")
    void testValidation5(RouteScheduleType scheduleType, boolean bookingEnabled, boolean singleDay) {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(1, 2820, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0))
        ), scheduleType, bookingEnabled, singleDay), context)).isTrue();
    }

    private PointParams pointParams(int index, Integer transitionTime, LocalTime startTime, LocalTime endTime) {
        PointParams params = new PointParams();
        params.setIndex(index);
        params.setTransitionTime(transitionTime);
        params.setArrivalStartTime(startTime);
        params.setArrivalEndTime(endTime);
        return params;
    }

    private RouteScheduleModificationPayload toRouteScheduleModificationPayload(
        List<PointParams> pointParams,
        RouteScheduleType scheduleType,
        boolean bookingEnabled,
        boolean singleDay
    ) {
        return new RouteScheduleModificationPayload()
            .setPointParams(pointParams)
            .setType(scheduleType)
            .setSlotBookingEnabled(bookingEnabled)

            .setScheduleInfo(new ScheduleInfo()
                .setStartDate(LocalDate.of(2022, 2, 18))
                .setEndDate(LocalDate.of(2022, 2, 18).plusDays(singleDay ? 0 : 1))
            );
    }

    static Stream<Arguments> scheduleParams() {
        return Stream.of(
            Arguments.of(RouteScheduleType.LINEHAUL, false, false),
            Arguments.of(RouteScheduleType.XDOC_TRANSPORT, false, false),
            Arguments.of(RouteScheduleType.LINEHAUL, true, false),
            Arguments.of(RouteScheduleType.XDOC_TRANSPORT, true, false),
            Arguments.of(RouteScheduleType.LINEHAUL, false, true),
            // Для лайнхоллов, если календаризация даже случайно будет включена,
            // валидация времени на точках всё равно будет
            Arguments.of(RouteScheduleType.LINEHAUL, true, true),
            Arguments.of(RouteScheduleType.XDOC_TRANSPORT, false, true)
        );
    }

}
