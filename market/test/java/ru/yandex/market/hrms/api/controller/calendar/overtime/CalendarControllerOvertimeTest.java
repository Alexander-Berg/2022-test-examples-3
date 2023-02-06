package ru.yandex.market.hrms.api.controller.calendar.overtime;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {
        "CalendarControllerOvertimeTest.common.csv",
        "CalendarControllerOvertimeTest.employees.csv"
})
public class CalendarControllerOvertimeTest extends AbstractApiTest {

    @BeforeEach
    public void init() {
        mockClock(Instant.parse("2021-12-01T06:00:00+03:00"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.test1.before.csv")
    public void addMultipleOvertimesAtSameDate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.notHoliday.before.csv")
    public void notHoliday() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Подработки можно создавать только на выходные дни"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.intersectScheduled.before.csv")
    public void intersectScheduled() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Подработка пересекается с рабочей сменой сотрудника 'ivanov': " +
                                "[2021-12-16T09:00..2021-12-16T21:00)"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.intersectExistingOvertime.before.csv")
    public void intersectExistingOvertime() throws  Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                        .queryParam("domainId", "1")
                        .queryParam("overtimeSlotId", "2")
                        .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Подработка пересекается с уже существующей подработкой" +
                                " сотрудника 'ivanov': [2021-12-14T20:00..2021-12-15T10:00)"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.leftAdjacent.before.csv")
    public void leftAdjacent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isOk());
    }


    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.rightAdjacent.before.csv")
    public void rightAdjacent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.limits.before.csv")
    public void limitFilteredEmpty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Сотрудник 'ivanov' не удовлетворяет наложенным на слот ограничениям"));
    }


    @Test
    @DbUnitDataSet(before = "CalendarControllerOvertimeTest.unmappedPosition.before.csv")
    public void unmappedPosition() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                        .queryParam("domainId", "1")
                        .queryParam("overtimeSlotId", "202")
                        .content("""    
                        [
                            {
                                "employeeId": 202,
                                "oebsAssignmentId": "100202-0202"
                            }
                        ]
                        """)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Сотрудник 'petrov' не удовлетворяет наложенным на слот ограничениям"));
    }

    @Test
    @DbUnitDataSet(before = {
            "CalendarControllerOvertimeTest.limits.before.csv",
            "CalendarControllerOvertimeTest.limits.exceedTotalCount.before.csv"
    })
    public void limitExceedTotalCount() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Превышено ограничение на количество сотрудников в выбранном слоте, текущий лимит: 7"));
    }


    @Test
    @DbUnitDataSet(before = {
            "CalendarControllerOvertimeTest.limits.before.csv",
            "CalendarControllerOvertimeTest.limits.exceedTotalCountWithPosition.before.csv"
    })
    public void limitExceedTotalCountWithPosition() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/overtime")
                .queryParam("domainId", "1")
                .queryParam("overtimeSlotId", "1")
                .content("""    
                        [
                            {
                                "employeeId": 1,
                                "oebsAssignmentId": "161136-4351"
                            }
                        ]
                        """)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EDITOR).getAuthorities()
                ))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Превышено ограничение на количество сотрудников в должности 'Кладовщик'" +
                                " в выбранном слоте, текущий лимит: 3"));
    }
}
