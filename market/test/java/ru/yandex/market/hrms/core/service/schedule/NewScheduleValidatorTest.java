package ru.yandex.market.hrms.core.service.schedule;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.domain.new_schedule.NewScheduleValidator;
import ru.yandex.market.hrms.core.domain.new_schedule.repo.ScheduleDayValidationResult;
import ru.yandex.market.hrms.core.service.oebs.model.Schedule;

@RequiredArgsConstructor
public class NewScheduleValidatorTest extends AbstractNewScheduleTest {

    @Autowired
    NewScheduleValidator NewScheduleServiceTest;

    @ParameterizedTest
    @CsvSource({
            "schedule.validation.good.json,SUCCESS",
            "schedule.validation.duplicates.json,HAS_DUPLICATES",
            "schedule.validation.overlaps.json,HAS_OVERLAPS"
    })
    public void shouldValidateScheduleDetailsByDaysAndReturnValidationResult(String file, ScheduleDayValidationResult expectedResult) {
        Schedule received = mapOebsScheduleFromFile(file).orElseThrow();

        ScheduleDayValidationResult result = NewScheduleServiceTest.validate(received.getScheduleDetails());

        Assertions.assertEquals(expectedResult, result);
    }

}
