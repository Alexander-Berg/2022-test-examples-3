package ru.yandex.market.hrms.api.controller.calendar.page;

import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.employee.calendar.CalendarHROperationTypeFilter;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "CalendarControllerPageFiltersTest.before.csv", schema = "public")
public class CalendarControllerPageFiltersTest extends AbstractApiTest {

    @CsvSource(value = {
            "WORKING,antipov93",
            "ABSENCE,timursha",
            "WEEKEND,kukabara",
            "VACATION,mkasumov",
            "SICK,imelnikov",
            "BUSINESS_TRIP,robot",
            "MATERNITY,terminator",
            "OTHER_ABSENCE,automaton"
    })
    @ParameterizedTest()
    void shouldFilterCalendar(CalendarHROperationTypeFilter filter, String login) throws Exception {
        mockClock(LocalDate.of(2021, 4, 1));

        mockMvc.perform(get("/lms/calendar")
                .param("filters", filter.name())
                .param("date", "2021-04")
                .param("groupId", "2")
                .param("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                ))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].employee.staff").value(login))
        ;
    }

}
