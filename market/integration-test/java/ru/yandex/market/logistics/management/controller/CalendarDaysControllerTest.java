package ru.yandex.market.logistics.management.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.schedule.CalendarsFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("Поиск выходных")
@DatabaseSetup("/data/controller/calendar/holidays.xml")
public class CalendarDaysControllerTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Валидация фильтра")
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("filterValidationSource")
    void filterValidation(UnaryOperator<CalendarsFilter.Builder> adjuster, String message) throws Exception {
        getHolidays(adjuster.apply(defaultFilter()).build())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].codes[0]").value(message));
    }

    private static Stream<Arguments> filterValidationSource() {
        return Stream.<Pair<UnaryOperator<CalendarsFilter.Builder>, String>>of(
            Pair.of(f -> f.calendarIds(null), "NotEmpty.calendarsFilter.calendarIds"),
            Pair.of(f -> f.calendarIds(ImmutableList.of()), "NotEmpty.calendarsFilter.calendarIds"),
            Pair.of(f -> f.calendarIds(Collections.singletonList(null)), "NotNull.calendarsFilter.calendarIds[0]"),
            Pair.of(f -> f.dateFrom(null), "NotNull.calendarsFilter.dateFrom"),
            Pair.of(f -> f.dateTo(null), "NotNull.calendarsFilter.dateTo")
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Test
    @DisplayName("Правильный результат")
    void searchHolidays() throws Exception {
        getHolidays(defaultFilter().build())
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/calendar/holidays.json"));
    }

    @Test
    @DisplayName("Добавляем дни в календарь")
    @ExpectedDatabase(
        value = "/data/controller/calendar/after/holidays_after_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addHolidays() throws Exception {
        mockMvc.perform(
            post("/externalApi/calendar/1/addHolidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/calendar/holidays_for_update.json"))
            )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/calendar/holidays_after_add.json"));
    }

    @Test
    @DisplayName("Удаляем выходные дни из календаря")
    @ExpectedDatabase(
        value = "/data/controller/calendar/after/holidays_after_remove.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removeHolidays() throws Exception {
        mockMvc.perform(
            post("/externalApi/calendar/1/removeHolidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/calendar/holidays_to_delete.json"))
            )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/calendar/holidays_after_remove.json"));
    }

    private CalendarsFilter.Builder defaultFilter() {
        return CalendarsFilter.builder()
            .calendarIds(ImmutableList.of(1L, 2L))
            .dateFrom(LocalDate.of(2019, 1, 1))
            .dateTo(LocalDate.of(2020, 1, 1));
    }

    ResultActions getHolidays(CalendarsFilter filter) throws Exception {
        return mockMvc.perform(
            put("/externalApi/calendar/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(filter))
        );
    }

}
