package ru.yandex.market.hrms.api.controller.calendar.filters;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@DbUnitDataSet(before = "CalendarControllerFiltersTest.before.csv", schema = "public")
public class CalendarControllerFiltersTest extends AbstractApiTest {
    @Test
    void shouldNotShowFiltersForRootCategory() throws Exception {
        mockMvc.perform(get("/lms/calendar/filters")
                .queryParam("groupId", "1")
                .queryParam("domainId", "1")
        ).andExpect(jsonPath("$.showFilters", CoreMatchers.is(false)));
    }

    @Test
    void shouldShowFiltersForMinRankCategory() throws Exception {
        mockMvc.perform(get("/lms/calendar/filters")
                .queryParam("groupId", "2")
                .queryParam("domainId", "1")
        ).andExpect(jsonPath("$.showFilters", CoreMatchers.is(true)));
    }
}
