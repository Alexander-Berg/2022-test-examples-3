package ru.yandex.market.logistics.lms.client.yt;

import java.time.LocalTime;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Получение интервала доставки по идентификатору")
class LmsLomYtClientGetScheduleDayByIdTest extends LmsLomYtAbstractTest {

    private static final long SCHEDULE_DAY_ID = 1;
    private static final String GET_SCHEDULE_DAY_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/schedule_day_by_id_dyn] "
        + "WHERE id = " + SCHEDULE_DAY_ID;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        YtUtils.mockSelectRowsFromYt(ytTables, Optional.empty(), GET_SCHEDULE_DAY_QUERY);
    }

    @Test
    @DisplayName("Интервал доставки не найден в yt")
    @DatabaseSetup("/lms/client/yt/get_schedule_day_by_id_enabled.xml")
    void noScheduleDayInYt() {
        softly.assertThat(lmsLomYtClient.getScheduleDay(SCHEDULE_DAY_ID))
            .isEmpty();

        verifyScheduleDayYtRequest();
    }

    @Test
    @DisplayName("Успешное получение интервала доставки из yt")
    @DatabaseSetup("/lms/client/yt/get_schedule_day_by_id_enabled.xml")
    void successGetScheduleDayById() {
        YtUtils.mockSelectRowsFromYt(ytTables, scheduleDayResponse(), GET_SCHEDULE_DAY_QUERY);

        softly.assertThat(lmsLomYtClient.getScheduleDay(SCHEDULE_DAY_ID))
            .isEqualTo(scheduleDayResponse());

        verifyScheduleDayYtRequest();
    }

    @Test
    @DisplayName("Ошибка при обращении к yt")
    @DatabaseSetup("/lms/client/yt/get_schedule_day_by_id_enabled.xml")
    void errorCallingYt() {
        YtUtils.mockExceptionCallingYt(ytTables, GET_SCHEDULE_DAY_QUERY, new RuntimeException("Some yt exception"));

        softly.assertThatCode(
                () -> lmsLomYtClient.getScheduleDay(SCHEDULE_DAY_ID)
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Some yt exception");

        verifyScheduleDayYtRequest();
    }

    @Test
    @DisplayName("Флаг получения интервала доставки выключен, клиент не идет в yt")
    void goingToYtDisabled() {
        YtUtils.mockSelectRowsFromYt(ytTables, scheduleDayResponse(), GET_SCHEDULE_DAY_QUERY);

        softly.assertThat(lmsLomYtClient.getScheduleDay(SCHEDULE_DAY_ID))
            .isEmpty();
    }

    private void verifyScheduleDayYtRequest() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(ytTables, GET_SCHEDULE_DAY_QUERY);
        verify(hahnYt, times(2)).tables();

    }

    @Nonnull
    private Optional<ScheduleDayResponse> scheduleDayResponse() {
        return Optional.of(new ScheduleDayResponse(
            1L,
            2,
            LocalTime.of(10, 30),
            LocalTime.of(20, 30),
            true
        ));
    }
}
