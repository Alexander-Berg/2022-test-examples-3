package ru.yandex.market.logistics.management.facade;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.CalendarDayCreateDto;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.facade.admin.AdminLogisticServiceFacade;
import ru.yandex.market.logistics.management.repository.CalendarRepository;

public class AdminLogisticServiceFacadeTest extends AbstractContextualTest {
    @Autowired
    private AdminLogisticServiceFacade adminLogisticServiceFacade;

    @Autowired
    private CalendarRepository calendarRepository;

    @Test
    @DatabaseSetup("/data/facade/logistic_service_facade/before/calendar.xml")
    @DisplayName("Создание дня в календаре")
    @ExpectedDatabase(
        value = "/data/facade/logistic_service_facade/after/create_calendar_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createCalendarDay() {
        Calendar calendar = calendarRepository.findByIdOrThrow(1L);

        adminLogisticServiceFacade.createDay(calendar, LocalDate.of(2021, 10, 10), true);
    }

    @Test
    @DatabaseSetup("/data/facade/logistic_service_facade/before/calendar.xml")
    @DisplayName("Создание нескольких дней в календаре")
    @ExpectedDatabase(
        value = "/data/facade/logistic_service_facade/after/create_calendar_days.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createCalendarDays() {
        Calendar calendar = calendarRepository.findByIdOrThrow(1L);

        CalendarDayCreateDto calendarDayCreateDto = new CalendarDayCreateDto();
        calendarDayCreateDto.setCalendarKey("1");
        calendarDayCreateDto.setStart("2021-10-10");
        calendarDayCreateDto.setEnd("2021-10-12");

        adminLogisticServiceFacade.createDays(calendar, calendarDayCreateDto, true);
    }
}
