package ru.yandex.market.logistics.management.service.combinator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.BasicEntity;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;
import ru.yandex.market.logistics.management.repository.combinator.LogisticSegmentServiceRepository;

@DatabaseSetup({
    "/data/service/combinator/db/before/service_codes.xml",
    "/data/service/combinator/db/before/forced_working_dates.xml",
})
class ForcedWorkingDatesConverterTest extends AbstractContextualTest {
    @Autowired
    private LogisticSegmentServiceRepository logisticSegmentServiceRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private LogisticSegmentService logisticSegmentService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calendarExportsCorrectlyOnUpdateCalendarsJob() {
        calendarExportsCorrectly(ignored -> logisticSegmentService.updateYtServices());
    }

    @Test
    void calendarExportsCorrectlyOnUpdateService() {
        calendarExportsCorrectly(BasicEntity::onPreUpdate);
    }

    private void calendarExportsCorrectly(
        Consumer<ru.yandex.market.logistics.management.domain.entity.combinator.LogisticSegmentService> consumer
    ) {
        LocalDate holiday1 = localDate(1);
        LocalDate holiday2 = localDate(3);
        LocalDate workDay1 = localDate(5);
        LocalDate workDay2 = localDate(7);

        transactionTemplate.execute(status -> {
            var service = logisticSegmentServiceRepository.findById(600L).orElseThrow();

            service.getCalendar()
                .addCalendarDay(calendarDay(holiday1, true))
                .addCalendarDay(calendarDay(holiday2, true))
                .addCalendarDay(calendarDay(workDay1, false))
                .addCalendarDay(calendarDay(workDay2, false));

            consumer.accept(service);
            return null;
        });

        softly.assertThat(getCalendarDates("holiday_dates").getDates())
            .containsExactlyInAnyOrder(holiday1, holiday2);
        softly.assertThat(getCalendarDates("forced_working_dates").getDates())
            .containsExactlyInAnyOrder(workDay1, workDay2);
    }

    @Nonnull
    @SneakyThrows
    private CalendarDates getCalendarDates(String attributeName) {
        Map<String, Object> holidayDatesMap = jdbcTemplate.queryForMap(
            "select " + attributeName + "::text from yt.logistics_services where lms_id = 600"
        );
        return objectMapper.readValue((String) holidayDatesMap.get(attributeName), CalendarDates.class);
    }

    @Nonnull
    private CalendarDay calendarDay(LocalDate localDate, boolean isHoliday) {
        return new CalendarDay()
            .setDay(localDate)
            .setIsHoliday(isHoliday);
    }

    @Nonnull
    private LocalDate localDate(int addDaysCount) {
        return LocalDate.now().plusDays(addDaysCount);
    }

    @Data
    public static class CalendarDates {
        private List<LocalDate> dates;
    }
}
