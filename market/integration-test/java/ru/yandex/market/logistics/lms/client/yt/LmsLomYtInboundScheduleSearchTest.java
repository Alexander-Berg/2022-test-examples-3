package ru.yandex.market.logistics.lms.client.yt;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Получение информации о расписании услуги 'Забор' из логистических сегментов")
class LmsLomYtInboundScheduleSearchTest extends LmsLomYtAbstractTest {

    private static final LogisticSegmentInboundScheduleFilter FILTER = new LogisticSegmentInboundScheduleFilter()
        .setFromPartnerId(1L)
        .setToPartnerId(2L)
        .setDeliveryType(DeliveryType.COURIER);

    @Test
    @DisplayName("Найдено расписание")
    @DatabaseSetup("/lms/client/yt/get_inbound_schedule_from_yt_enabled.xml")
    void scheduleFoundAndDaysNot() {
        String schedulesQuery = "schedules FROM [//home/2022-03-02T08:05:24Z/inbound_schedule_dyn] "
            + "WHERE partner_from = 1 AND partner_to = 2 AND delivery_type = 'COURIER'";

        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(1L, 2L),
            schedulesQuery
        );

        String daysQuery = "* FROM [//home/2022-03-02T08:05:24Z/schedule_days_dyn] WHERE schedule_id IN (1,2)";
        ScheduleDayResponse day = new ScheduleDayResponse(1L, 2, LocalTime.now(), LocalTime.now(), true);

        YtUtils.mockSelectRowsFromYt(
            ytTables,
            Set.of(day),
            daysQuery
        );

        softly.assertThat(lmsLomYtClient.searchInboundSchedule(FILTER))
            .containsExactly(day);

        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(ytTables, schedulesQuery);
        YtUtils.verifySelectRowsInteractions(ytTables, daysQuery);
        verify(hahnYt, times(3)).tables();
    }

    @Test
    @DisplayName("Расписание не найдено")
    @DatabaseSetup("/lms/client/yt/get_inbound_schedule_from_yt_enabled.xml")
    void scheduleNotFound() {
        String query = "schedules FROM [//home/2022-03-02T08:05:24Z/inbound_schedule_dyn] "
            + "WHERE partner_from = 1 AND partner_to = 2 AND delivery_type = 'COURIER'";

        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(),
            query
        );

        softly.assertThat(lmsLomYtClient.searchInboundSchedule(FILTER))
            .isEmpty();

        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(ytTables, query);
        verify(hahnYt, times(2)).tables();
    }

    @Test
    @DisplayName("Расписание не найдено и тип доставки null")
    @DatabaseSetup("/lms/client/yt/get_inbound_schedule_from_yt_enabled.xml")
    void scheduleNotFoundAndDeliveryTypeIsNull() {
        String query = "schedules FROM [//home/2022-03-02T08:05:24Z/inbound_schedule_dyn] "
            + "WHERE partner_from = 1 AND partner_to = 2 AND is_null(delivery_type)";

        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(),
            query
        );

        softly.assertThat(
                lmsLomYtClient.searchInboundSchedule(
                    new LogisticSegmentInboundScheduleFilter()
                        .setFromPartnerId(1L)
                        .setToPartnerId(2L)
                )
            )
            .isEmpty();

        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(ytTables, query);
        verify(hahnYt, times(2)).tables();
    }

    @Test
    @DisplayName("Пустой список, т.к. поход в YT выключен")
    void goingToYtDisabled() {
        softly.assertThat(lmsLomYtClient.searchInboundSchedule(FILTER)).isEmpty();
    }
}
