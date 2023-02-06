package ru.yandex.market.hrms.api.controller.operation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.Cookie;

import one.util.streamex.EntryStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"NPOV2Test.schedules.csv", "NPOV2Test.before.csv"})
public class NonProductionOperationControllerV2Test extends AbstractApiTest {

    @BeforeEach
    public void init() {
        mockClock(LocalDateTime.of(2021, 12, 24, 4, 38, 28, 888471000));
    }


    @Test
    public void createInDifferentTimezonesForFuture() throws Exception {
        mockClock(LocalDateTime.of(2021, 6,24, 14, 0, 0));
        var requestBody = """
                {
                    "startDateTime": "2021-12-24T10:00:00",
                    "endDateTime": "2021-12-24T19:00:00",
                    "employeeIds": [%d]
                }
                """;
        var requests = EntryStream.of(1L, 1, 2L,5)
                .mapKeyValue((domainId, id) -> createRequest(requestBody.formatted(id), "any", domainId))
                .toList();
        for (var r : requests) {
            mockMvc.perform(r).andExpect(status().is4xxClientError());
        }
    }


    @Test
    @DbUnitDataSet(after = "NPOV2Test.ScCreateFullShift.after.csv")
    public void createWithFullShiftTestForSc() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "startDateTime": "2021-12-06T14:00:00",
                            "isFullShift": true,
                            "employeeIds": [3]
                        }
                        """,
                "gymboss", 41L
        )).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("fullShiftNpoSc.json")));
    }

    @Test
    @DbUnitDataSet(after = "NPOV2Test.FfcCreateFullShift2Employees.after.csv")
    public void createWithFullShiftTestFor2FfcEmployee() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                    "startDateTime": "2021-12-06T14:00:00",
                                    "isFullShift": true,
                                    "employeeIds": [1, 6]
                                }
                                """,
                        "gymboss", 1L
                )).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("NPOV2Test.two_employee_npo.json")));
    }

    @Test
    @DbUnitDataSet(after = "NPOV2Test.FfcCreateFullShift.after.csv")
    public void createWithFullShiftTestForFfc() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "startDateTime": "2021-12-06T14:00:00",
                            "isFullShift": true,
                            "employeeIds": [1]
                        }
                        """,
                "gymboss", 1L
        )).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("ffcFullShiftNpo.json")));
    }

    @Test
    @DbUnitDataSet(after = "NPOV2Test.FfcCreateWithSecondInaccurancy.after.csv")
    public void createForShiftWithEndSecondsInaccuracyTest() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "startDateTime": "2021-12-06T10:00:05",
                            "endDateTime": "2021-12-06T19:00:54",
                            "isFullShift": false,
                            "employeeIds": [1]
                        }
                        """,
                "gymboss", 1L
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "NPOV2Test.empty_npo_table.csv")
    public void cannotCreateNpoStartsBeforeShiftTest() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "startDateTime": "2021-12-06T09:00:05",
                            "endDateTime": "2021-12-06T12:15:10",
                            "isFullShift": false,
                            "employeeIds": [1]
                        }
                        """,
                "gymboss", 1L
        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Не найдена рабочая смена для сотрудника Полина Гагарина"));
    }

    @Test
    @DbUnitDataSet(after = "NPOV2Test.FfcCreateWithInterval.after.csv")
    public void createWithExplicitIntervalTest() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                         {
                            "startDateTime": "2021-12-06T11:00:05",
                            "endDateTime": "2021-12-06T14:15:10",
                            "isFullShift": false,
                            "employeeIds": [1]
                        }
                        """,
                "gymboss", 1L
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "NPOV2Test.empty_npo_table.csv")
    public void createWithExplicitIntervalWhenStartedBeforeShiftTest() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                    "startDateTime": "2021-12-05T09:59:59",
                                    "endDateTime": "2021-12-05T12:00:00",
                                    "isFullShift": false,
                                    "employeeIds": [1]
                                }
                                """,
                        "gymboss", 1L
                ))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Не найдена рабочая смена для сотрудника Полина Гагарина"));
    }

    @Test
    public void createAndRegisterNewOvertimeTest() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                    "startDateTime": "2021-12-05T03:00:00",
                                    "endDateTime": "2021-12-05T05:00:00",
                                    "employeeIds": [1]
                                }
                                """,
                        "gymboss", 1L
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Не найдена рабочая смена для сотрудника Полина Гагарина"));
    }

    @Test
    @DbUnitDataSet(
            before = "NPOV2Test.FfcCreateWithExistedOvertime.before.csv",
            after = "NPOV2Test.FfcCreateWithExistedOvertime.after.csv"
    )
    public void createWithExistingOvertimeTest() throws Exception {
        mockClock(Instant.parse("2021-12-06T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "startDateTime": "2021-12-04T22:00:00",
                            "endDateTime": "2021-12-04T23:00:00",
                            "employeeIds": [1]
                        }
                        """,
                "gymboss", 1L
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.FfcCreateOnSameShift.before.csv",
            after = "NPOV2Test.FfcCreateOnSameShift.after.csv")
    public void createOnSameShiftTest() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "startDateTime": "2021-12-06T15:00:05",
                            "endDateTime": "2021-12-06T16:00:10",
                            "isFullShift": false,
                            "employeeIds": [1]
                        }
                        """,
                "gymboss", 1L
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.FfcCreateOnSameShift.before.csv")
    public void createOverlappedTest() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                   "startDateTime": "2021-12-06T11:00:05",
                                   "endDateTime": "2021-12-06T16:00:10",
                                   "isFullShift": false,
                                   "employeeIds": [1]
                                }
                                """,
                        "gymboss", 1L
                ))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("Для сотрудника sof-user1 уже существует НПО в запрошенном временном интервале"));
    }

    @Test
    public void createWithUnknownWmsLogin() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                   "startDateTime": "2021-12-05T23:59:59",
                                   "isFullShift": true,
                                    "employeeIds": [98765]
                                }
                                """,
                        "gymboss", 1L
                ))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Не найдены сотрудники staff ids: [98765], outstaff ids: []"));
    }

    @Test
    public void createWithRightBoundViolation() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                   "startDateTime": "2021-12-06T18:00:00Z",
                                   "endDateTime": "2021-12-06T22:00:01",
                                    "employeeIds": [1]
                                }
                                """,
                        "gymboss", 1L
                ))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                     .value("НПО выходит за пределы смены сотрудника sof-user1 (2021-12-06 10:00 - 2021-12-06 19:00)"));
    }

    @Test
    public void createWithInvalidIntervalTest() throws Exception {
        mockMvc.perform(createRequest(
                """
                        {
                           "startDateTime": "2021-12-06T02:00:00Z",
                           "endDateTime": "2021-12-05T20:00:00Z",
                            "employeeIds": [1]
                        }
                        """,
                "gymboss", 1L
        ))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Некорректный интервал"));
    }

    @Test
    public void createNpoInPastWithAuthority() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                    "startDateTime": "2021-12-03T18:00:00",
                                    "endDateTime": "2021-12-03T19:00:00",
                                    "isFullShift": false,
                                    "employeeIds": [1]
                                }
                                """,
                        "levpolyakoff", 1L
                ).with(SecurityMockMvcRequestPostProcessors
                        .user("levpolyakoff")
                        .authorities(List.of(
                                new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL),
                                new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_NPO_EDIT_EXTENDED)))
                ))
                .andExpect(status().isOk());
    }

    @Test
    public void createNpoInPastWithoutAuthority() throws Exception {
        mockClock(Instant.parse("2021-12-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                    "startDateTime": "2021-12-03T18:00:00",
                                    "endDateTime": "2021-12-03T19:00:00",
                                    "isFullShift": false,
                                    "employeeIds": [1]
                                }
                                """,
                        "levpolyakoff", 1L
                ))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("Нельзя создавать НПО на смену, которая закончилась ранее, чем 2 суток назад"));
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.editNpo.before.csv",
            after = "NPOV2Test.editNpo.after.csv")
    public void editNpoTest() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 24, 14, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.put("/lms/non-production-operations/1")
                .queryParam("domainId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("yandex_login", "alex-fill"))
                .content("""
                        {
                            "operationType": "MENTORING",
                            "startDateTime": "2021-12-24T11:00:00",
                            "endDateTime": "2021-12-24T16:00:00",
                            "isFullShift": true,
                            "employeeIds": [1]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("editedNpo.json")));
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.FfcCreateOnSameShift.before.csv")
    public void editNpoAfterShiftEndTest() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 7, 10, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.put("/lms/non-production-operations/1")
                        .queryParam("domainId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("yandex_login", "alex-fill"))
                        .content("""
                                {
                                    "operationType": "MENTORING",
                                    "startDateTime": "2021-12-06T11:00:00",
                                    "endDateTime": "2021-12-06T16:00:00",
                                    "isFullShift": true,
                                    "employeeIds": [1]
                                }
                                """))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Редактировать операции возможно только во время смены"
                        }
                        """));
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.deleteNpo.before.csv",
            after = "NPOV2Test.deleteNpo.after.csv")
    public void deleteNpoTest() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 24, 14, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.delete("/lms/non-production-operations/1")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.deleteNpo.before.csv")
    public void deleteNpoAfterShiftTest() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 25, 14, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.delete("/lms/non-production-operations/1")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Редактировать операции возможно только во время смены"
                        }
                        """));
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.EditWhileOvertimeShift.before.csv",
            after = "NPOV2Test.EditWhileOvertimeShift.after.csv")
    public void editNpoTestWhileOvertimeShift() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 4, 23, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.put("/lms/non-production-operations/1")
                .queryParam("domainId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("yandex_login", "alex-fill"))
                .content("""
                        {
                            "operationType": "MENTORING",
                            "startDateTime": "2021-12-04T22:00:00",
                            "endDateTime": "2021-12-04T23:30:00",
                            "isFullShift": true,
                            "employeeIds": [1]
                        }
                        """)).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("editedNpoOvertime.json")));
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.EditWhileOvertimeShift.before.csv")
    public void editNpoAfterOvertimeShiftEndTest() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 6, 10, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.put("/lms/non-production-operations/1")
                        .queryParam("domainId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("yandex_login", "alex-fill"))
                        .content("""
                                {
                                    "operationType": "MENTORING",
                                    "startDateTime": "2021-12-04T22:00:00",
                                    "endDateTime": "2021-12-04T23:30:00",
                                    "isFullShift": true,
                                    "employeeIds": [1]
                                }
                                """))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Редактировать операции возможно только во время смены"
                        }
                        """));
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.EditWhileOvertimeShift.before.csv")
    public void editNpoAfterOvertimeShiftEndTestWithAuthority() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 6, 10, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.put("/lms/non-production-operations/1")
                        .queryParam("domainId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("yandex_login", "alex-fill"))
                        .with(SecurityMockMvcRequestPostProcessors
                                .user("alex-fill")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                        .content("""
                                {
                                    "operationType": "MENTORING",
                                    "startDateTime": "2021-12-04T22:00:00",
                                    "endDateTime": "2021-12-04T23:30:00",
                                    "isFullShift": true,
                                    "employeeIds": [1]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("editedEndedNpoWithAuthority.json")));
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.deleteNpoOvertimeShift.before.csv",
            after = "NPOV2Test.deleteNpoOvertimeShift.after.csv")
    public void deleteNpoWhileOvertimeShiftTest() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 4, 23, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.delete("/lms/non-production-operations/1")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NPOV2Test.deleteNpoOvertimeShift.before.csv")
    public void deleteNpoAfterOvertimeShiftTest() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 25, 14, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.delete("/lms/non-production-operations/1")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Редактировать операции возможно только во время смены"
                        }
                        """, false));
    }

    @Test
    public void minThresholdTest() throws Exception {
        mockMvc.perform(createRequest(
                        """
                                {
                                    "startDateTime": "2021-12-06T10:00:05",
                                    "endDateTime": "2021-12-06T10:10:54",
                                    "isFullShift": false,
                                    "employeeIds": [1]
                                }
                                """,
                        "gymboss", 1L
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Некорректный интервал"
                        }
                        """));
    }

    private MockHttpServletRequestBuilder createRequest(String body, String by, Long domainId) {
        return MockMvcRequestBuilders.post("/lms/non-production-operations")
                .cookie(new Cookie("yandex_login", by))
                .param("domainId", String.valueOf(domainId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

}
