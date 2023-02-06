package ru.yandex.market.pipelinetests.tests.lms_lom.yt;

import io.qameta.allure.Epic;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractLmsLomTest;

import static toolkit.FileUtil.bodyStringFromFile;
import static toolkit.Mapper.mapLmsResponse;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в YT")
public class GetScheduleDayByIdTest extends AbstractLmsLomTest {

    private static final ScheduleDayResponse EXPECTED_LMS_FILLED_SCHEDULE_DAY = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/schedule_day/filled.json"),
        ScheduleDayResponse.class
    );
    private static final ScheduleDayResponse EXPECTED_LMS_EMPTY_SCHEDULE_DAY = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/schedule_day/empty.json"),
        ScheduleDayResponse.class
    );

    @Test
    @SneakyThrows
    @DisplayName("Получение интервала доставки по идентификатору: заполнены все поля")
    public void getScheduleDayAllFieldsFilled() {
        getScheduleDaysById(FILLED_SCHEDULE_DAY_ID, EXPECTED_LMS_FILLED_SCHEDULE_DAY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение интервала доставки по идентификатору: заполнены только обязательные поля")
    public void getScheduleDayWithRequiredFieldsOnly() {
        getScheduleDaysById(EMPTY_SCHEDULE_DAY_ID, EXPECTED_LMS_EMPTY_SCHEDULE_DAY);
    }

    private void getScheduleDaysById(Long scheduleDayId, ScheduleDayResponse expectedPreparedResponse) {
        ScheduleDayResponse lmsScheduleDay = LMS_STEPS.getScheduleDayById(scheduleDayId);
        compareScheduleDays(expectedPreparedResponse, lmsScheduleDay);
        ScheduleDayResponse ytScheduleDay = LOM_LMS_YT_STEPS.getScheduleDayById(scheduleDayId);
        compareScheduleDays(lmsScheduleDay, ytScheduleDay);
    }

    private void compareScheduleDays(ScheduleDayResponse expected, ScheduleDayResponse actual) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .as("Не совпали поля расписания")
            .isEqualTo(expected);
    }
}
