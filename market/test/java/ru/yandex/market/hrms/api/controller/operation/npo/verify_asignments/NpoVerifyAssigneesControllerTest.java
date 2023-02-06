package ru.yandex.market.hrms.api.controller.operation.npo.verify_asignments;

import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "NpoVerifyAssigneesControllerTest.before.csv")
public class NpoVerifyAssigneesControllerTest extends AbstractApiTest {

    public static LocalDateTime FIRST_DATE = LocalDateTime.parse("2022-06-05T14:00:01");
    public static LocalDateTime SECOND_DATE = LocalDateTime.parse("2022-06-05T17:59:59");

    @Test
    void empty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/non-production-operations/verify-assignees")
                        .queryParam("domainId", "37")
                        .queryParam("startDateTime", FIRST_DATE.toString())
                        .queryParam("endDateTime", SECOND_DATE.toString())
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));
    }


    @Test
    @DbUnitDataSet(before = "NpoVerifyAssigneesControllerTest.assignees.csv")
    void happyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/non-production-operations/verify-assignees")
                        .queryParam("domainId", "37")
                        .queryParam("employeeIds", "1", "2")
                        .queryParam("outstaffIds", "3", "4")
                        .queryParam("startDateTime", FIRST_DATE.toString())
                        .queryParam("endDateTime", SECOND_DATE.toString())
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                            { "assignee": { "type": "EMPLOYEE", "id": 1 }, "hasWmsLogs": true},
                            { "assignee": { "type": "EMPLOYEE", "id": 2 }, "hasWmsLogs": false},
                            { "assignee": { "type": "OUTSTAFF", "id": 3 }, "hasWmsLogs": true},
                            { "assignee": { "type": "OUTSTAFF", "id": 4 }, "hasWmsLogs": false}
                        ]
                        """, true));
    }

    @Test
    void shouldNotFailWhenAssigneeNotExists() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/non-production-operations/verify-assignees")
                        .queryParam("domainId", "37")
                        .queryParam("employeeIds", "1001")
                        .queryParam("outstaffIds", "1001")
                        .queryParam("startDateTime", FIRST_DATE.toString())
                        .queryParam("endDateTime", SECOND_DATE.toString())
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                            { "assignee": { "type": "EMPLOYEE", "id": 1001 }, "hasWmsLogs": false},
                            { "assignee": { "type": "OUTSTAFF", "id": 1001 }, "hasWmsLogs": false}
                        ]
                        """, true));
    }


    @Test
    void shouldNotFailWhenAssigneeIsOutstaffAndSingleDatePresent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/non-production-operations/verify-assignees")
                        .queryParam("domainId", "37")
                        .queryParam("outstaffIds", "1")
                        .queryParam("startDateTime", FIRST_DATE.toString())
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                            { "assignee": { "type": "OUTSTAFF", "id": 1 }, "hasWmsLogs": false}
                        ]
                        """, true));
    }

    @Test
    void shouldReturnNullsWhenWmsNotEnabledForDomain() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/non-production-operations/verify-assignees")
                        .queryParam("domainId", "43")
                        .queryParam("employeeIds", "5")
                        .queryParam("outstaffIds", "6")
                        .queryParam("startDateTime", FIRST_DATE.toString())
                        .queryParam("endDateTime", SECOND_DATE.toString())
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                            { "assignee": { "type": "EMPLOYEE", "id": 5 }},
                            { "assignee": { "type": "OUTSTAFF", "id": 6 }}
                        ]
                        """, true));
    }
}
