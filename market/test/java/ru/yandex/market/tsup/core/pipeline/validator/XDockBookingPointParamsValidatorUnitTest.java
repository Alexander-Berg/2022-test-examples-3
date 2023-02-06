package ru.yandex.market.tsup.core.pipeline.validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.validation.ConstraintValidatorContext;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleType;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.PointParams;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.RouteScheduleModificationPayload;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.ScheduleInfo;

class XDockBookingPointParamsValidatorUnitTest {

    private final PointParamsValidator validator = new PointParamsValidator();
    private ConstraintValidatorContext context;

    @BeforeEach
    void setup() {
        context = Mockito.mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
            Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.anyString())).thenReturn(builder);
    }

    @Test
    void testValidation1() {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(1, 0, LocalTime.of(13, 0), LocalTime.of(14, 0))
        )), context)).isTrue();
    }

    @Test
    void testValidation2() {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(1, 60, LocalTime.of(13, 0), LocalTime.of(14, 0))
        )), context)).isTrue();
    }

    @Test
    void testValidation3() {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(1, 1440, LocalTime.of(12, 0), LocalTime.of(13, 0))
        )), context)).isTrue();
    }

    @Test
    void testValidation4() {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(1, 0, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0))
        )), context)).isTrue();
    }

    @Test
    void testValidation5() {
        Assertions.assertThat(validator.isValid(toRouteScheduleModificationPayload(List.of(
            pointParams(1, 2820, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            pointParams(0, null, LocalTime.of(12, 0), LocalTime.of(13, 0))
        )), context)).isTrue();
    }

    private PointParams pointParams(int index, Integer transitionTime, LocalTime startTime, LocalTime endTime) {
        PointParams params = new PointParams();
        params.setIndex(index);
        params.setTransitionTime(transitionTime);
        params.setArrivalStartTime(startTime);
        params.setArrivalEndTime(endTime);
        return params;
    }

    private RouteScheduleModificationPayload toRouteScheduleModificationPayload(List<PointParams> pointParams) {
        return new RouteScheduleModificationPayload()
            .setPointParams(pointParams)
            .setType(RouteScheduleType.XDOC_TRANSPORT)
            .setSlotBookingEnabled(true)
            .setScheduleInfo(new ScheduleInfo()
                .setStartDate(LocalDate.of(2022,2,18))
                .setEndDate(LocalDate.of(2022,2,18))
            );
    }

}
