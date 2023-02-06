package ru.yandex.market.tsup.core.pipeline.validator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import javax.validation.ConstraintValidatorContext;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.PointParams;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.RouteScheduleModificationPayload;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.ScheduleInfo;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleStartValidatorTest {

    private ConstraintValidatorContext context;
    private TestableClock clock;
    private ScheduleStartValidator validator;

    @BeforeEach
    void setUp() {
        context = Mockito.mock(ConstraintValidatorContext.class);
        clock = new TestableClock();
        clock.setFixed(Instant.parse("2022-03-22T15:00:00.00Z"), ZoneId.of("Europe/Moscow"));
        validator = new ScheduleStartValidator(clock);
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(context);
    }

    @Test
    void isValidSingleDay() {
        RouteScheduleModificationPayload value = new RouteScheduleModificationPayload();
        value.setScheduleInfo(new ScheduleInfo()
            .setStartDate(LocalDate.of(2022, 3, 22))
            .setEndDate(LocalDate.of(2022, 3, 22))
        );
        value.setPointParams(List.of(
            new PointParams().setIndex(0).setArrivalStartTime(LocalTime.of(18, 40))
        ));

        Assertions
            .assertThat(validator.isValid(value, context))
            .isTrue();
    }

    @Test
    void isValidSingleDayEmptyPoints() {
        RouteScheduleModificationPayload value = new RouteScheduleModificationPayload();
        value.setScheduleInfo(new ScheduleInfo()
            .setStartDate(LocalDate.of(2022, 3, 20))
            .setEndDate(LocalDate.of(2022, 3, 20))
        );
        value.setPointParams(List.of());

        Assertions
            .assertThat(validator.isValid(value, context))
            .isTrue();
    }

    @Test
    void isValidMultiplaDays() {
        RouteScheduleModificationPayload value = new RouteScheduleModificationPayload();
        value.setScheduleInfo(new ScheduleInfo()
            .setStartDate(LocalDate.of(2022, 3, 20))
            .setEndDate(LocalDate.of(2022, 3, 25))
        );
        value.setPointParams(List.of(
            new PointParams().setIndex(0).setArrivalStartTime(LocalTime.of(18, 0))
        ));

        Assertions
            .assertThat(validator.isValid(value, context))
            .isTrue();
    }

    @Test
    void isInvalidSingleDay() {
        RouteScheduleModificationPayload value = new RouteScheduleModificationPayload();
        value.setScheduleInfo(new ScheduleInfo()
            .setStartDate(LocalDate.of(2022, 3, 22))
            .setEndDate(LocalDate.of(2022, 3, 22))
        );
        value.setPointParams(List.of(
            new PointParams().setIndex(0).setArrivalStartTime(LocalTime.of(18, 29))
        ));

        Assertions
            .assertThat(validator.isValid(value, context))
            .isFalse();

        Mockito.verify(context).buildConstraintViolationWithTemplate(
            "Разовое перемещение должно начинаться не раньше, чем через полчаса от текущего времени. "
                + "Начало в 2022-03-22 18:29 слишком рано."
        );
    }
}
