package ru.yandex.market.pipelinetests.tests.lms_lom.utils;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;

import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

@UtilityClass
@ParametersAreNonnullByDefault
public class ScheduleDayCompareUtils {

    public void compareSchedules(
        SoftAssertions softly,
        List<ScheduleDayResponse> expected,
        List<ScheduleDayResponse> actual
    ) {
        softly.assertThat(actual)
            .as("Не совпадают найденные дни расписания")
            .containsExactlyInAnyOrderElementsOf(expected);
    }
}
