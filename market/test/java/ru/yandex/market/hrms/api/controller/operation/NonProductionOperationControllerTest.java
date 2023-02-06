package ru.yandex.market.hrms.api.controller.operation;

import java.time.Instant;
import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import one.util.streamex.EntryStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Deprecated
@DbUnitDataSet(before = "CreateOperations.environment.before.csv")
public class NonProductionOperationControllerTest extends AbstractApiTest {

    @BeforeEach
    public void init() {
        mockClock(LocalDateTime.of(2021, 6, 24, 4, 38, 28, 888471000));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "CreateOperationForSc.before.csv",
                    "CreateOperation.withExistingOvertime.before.csv"
            },
            after = "CreateOperationWithFullShift.after.csv"
    )
    public void createWithFullShiftTestForScByEmployeeIds() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-05T21:00:00",
                            "isFullShift": true,
                            "employeeIds": [5317, 5319]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "CreateOperationForRW.before.csv",
                    "CreateOperation.withExistingOvertime.before.csv"
            },
            after = "CreateOperationWithFullShiftRw.after.csv"
    )
    public void createWithFullShiftTestForScByEmployeeIdsInRw() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/non-production-operations")
                .queryParam("domainId", "52")
                .cookie(new Cookie("yandex_login", "any"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                                {
                                    "operationType": "IDLE_TIME",
                                    "startDateTime": "2021-06-05T10:00:00",
                                    "isFullShift": true,
                                    "employeeIds": [5317]
                                }
                                """
                )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "CreateOperationForRW.before.csv",
                    "CreateOperation.withExistingOvertime.before.csv"
            },
            after = "CreateOperationOutstaffRw.after.csv"
    )
    public void createWithFullShiftTestForScByOutstaffIdsInRw() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/non-production-operations")
                .queryParam("domainId", "52")
                .cookie(new Cookie("yandex_login", "any"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                                {
                                    "operationType": "IDLE_TIME",
                                    "startDateTime": "2021-06-05T14:00:05",
                                    "endDateTime": "2021-06-05T16:00:05",
                                    "outstaffIds": [100]
                                }
                                """
                )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "NonProductionOperationControllerTest.before.csv",
                    "CreateOperation.before.csv",
                    "CreateOperation.withExistingOvertime.before.csv",
            },
            after = "CreateOperationWithFullShift.after.csv")
    public void createWithFullShiftTestWithIds() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-05T21:00:00",
                            "isFullShift": true,
                            "employeeIds": [5317, 5319]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "NonProductionOperationControllerTest.differentTimezones.before.csv",
            after = "NonProductionOperationControllerTest.differentTimezones.after.csv"
    )
    public void createInDifferentTimezones() throws Exception {
        mockClock(LocalDateTime.of(2021, 6,24, 14, 0, 0));
        var requestBody = """
                {
                    "operationGroupId": 1,
                    "startDateTime": "2021-06-24T09:00:00",
                    "endDateTime": "2021-06-24T21:00:00",
                    "employeeIds": [%d]
                }
                """;
        var requests = EntryStream.of(1, 1, 4, 4)
                .mapKeyValue((domainId, wmsLogin) -> createRequest(requestBody.formatted(wmsLogin), "any", domainId))
                .toList();
        for (var r : requests) {
            mockMvc.perform(r).andExpect(status().isOk());
        }
    }


    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.differentTimezones.before.csv")
    public void createInDifferentTimezonesForFuture() throws Exception {
        mockClock(LocalDateTime.of(2021, 6,24, 14, 0, 0));
        var requestBody = """
                {
                    "operationGroupId": 1,
                    "startDateTime": "2021-06-25T21:00:00",
                    "endDateTime": "2021-06-26T09:00:00",
                    "employeeIds": ["%d"]
                }
                """;
        var requests = EntryStream.of(1, 1, 4, 4)
                .mapKeyValue((domainId, wmsLogin) -> createRequest(requestBody.formatted(wmsLogin), "any", domainId))
                .toList();
        for (var r : requests) {
            mockMvc.perform(r).andExpect(status().is4xxClientError());
        }
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv", after = "CreateOperationWithShiftEndInaccuracy.after.csv")
    public void createForShiftWithEndSecondsInaccuracyTestWithIds() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-05T21:00:05",
                            "endDateTime": "2021-06-06T09:00:10",
                            "isFullShift": false,
                            "employeeIds": ["5317"]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv")
    public void cannotCreateNpoStartsBeforeShiftWithIdsTest() throws Exception {
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-05T20:00:05",
                            "endDateTime": "2021-06-06T08:00:10",
                            "isFullShift": false,
                            "employeeIds": [5317]
                        }
                        """,
                "any"
        )).andExpect(status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv",
            after = "CreateOperation.ffc.outstaff.after.csv")
    public void createNpoTestForOutstaffInFfcWithIds() throws Exception {
        mockClock(LocalDateTime.of(2021, 6, 25, 18, 0));
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-25T20:00:00",
                            "endDateTime": "2021-06-25T22:00:00",
                            "isFullShift": false,
                            "outstaffIds": [100, 101]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "CreateOperationForSc.before.csv",
            after = "CreateOperation.sc.outstaff.after.csv")
    public void createNpoTestForOutstaffByIds() throws Exception {
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-05T20:00:00",
                            "endDateTime": "2021-06-05T22:00:00",
                            "isFullShift": false,
                            "outstaffIds": [100, 101]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }


    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv", after = "CreateOperationWithExplicitInterval.after.csv")
    public void createWithExplicitIntervalByEmployeeId() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-05T14:00:00",
                            "endDateTime": "2021-06-05T16:00:00",
                            "employeeIds": [5318]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv")
    public void createWithExplicitIntervalWhenStartedBeforeShiftTest() throws Exception {
        mockMvc.perform(createRequest(
                        """
                                {
                                    "operationType": "IDLE_TIME",
                                    "startDateTime": "2021-06-05T19:59:59",
                                    "endDateTime": "2021-06-05T22:00:00",
                                    "employeeIds": [5317]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Не найдена рабочая смена для сотрудника Иванов Иван"
                        }
                        """, false));
    }

    @Test
    @DbUnitDataSet(before = {
            "NonProductionOperationControllerTest.before.csv",
            "CreateOperation.before.csv"
    })
    public void createAndRegisterNewOvertimeTest() throws Exception {
        mockMvc.perform(createRequest(
                        """
                                {
                                    "operationType": "IDLE_TIME",
                                    "startDateTime": "2021-06-05T22:00:00",
                                    "endDateTime": "2021-06-05T23:00:00",
                                    "employeeIds": [5319]
                                }
                                """,
                        "any"
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Не найдена рабочая смена для сотрудника Александров Александр"));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "NonProductionOperationControllerTest.before.csv",
                    "CreateOperation.before.csv",
                    "CreateOperation.withExistingOvertime.before.csv"
            },
            after = "CreateOperationWithExistingOvertime.after.csv"
    )
    public void createWithExistingOvertimeTest() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "IDLE_TIME",
                            "startDateTime": "2021-06-05T22:00:00",
                            "endDateTime": "2021-06-05T23:00:00",
                            "employeeIds": [5319]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(
            before = "CreateOperationOnSameShift.before.csv",
            after = "CreateOperationOnSameShift.after.csv"
    )
    public void createOnSameShiftTest() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                            "operationType": "OTHER_WORK",
                            "startDateTime": "2021-06-05T23:00:00",
                            "endDateTime": "2021-06-06T00:00:00",
                            "employeeIds": [5317, 5319]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperationOnSameShift.before.csv")
    public void createOverlappedTest() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                    "operationType": "OTHER_WORK",
                                    "startDateTime": "2021-06-05T22:30:00",
                                    "endDateTime": "2021-06-06T00:00:00",
                                    "employeeIds": [5317]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Для сотрудника sof-test1 уже существует НПО в запрошенном временном интервале"
                        }
                        """, false));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv")
    public void createWithUnknownWmsLogin() throws Exception {
        mockMvc.perform(createRequest(
                        """
                                {
                                   "operationType": "IDLE_TIME",
                                   "startDateTime": "2021-06-05T23:59:59",
                                   "isFullShift": true,
                                    "employeeIds": [5317,5318,53120]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Не найдены сотрудники staff ids: [53120], outstaff ids: []"
                        }
                        """, false));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv")
    public void createWithRightBoundViolation() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                   "operationType": "IDLE_TIME",
                                   "startDateTime": "2021-06-06T08:00:00Z",
                                   "endDateTime": "2021-06-06T10:00:01",
                                    "employeeIds": [5317]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "НПО выходит за пределы смены сотрудника sof-test1 (2021-06-05 21:00 - 2021-06-06 09:00)"
                        }
                        """, false));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv")
    public void createWithRightBoundViolationIds() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                        """
                                {
                                   "operationType": "IDLE_TIME",
                                   "startDateTime": "2021-06-06T08:00:00Z",
                                   "endDateTime": "2021-06-06T10:00:01",
                                    "employeeIds": ["5317"]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "НПО выходит за пределы смены сотрудника sof-test1 (2021-06-05 21:00 - 2021-06-06 09:00)"
                        }
                        """, false));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "CreateOperation.before.csv")
    public void createWithOverfilledShiftWithIds() throws Exception {
        mockClock(Instant.parse("2021-06-07T15:00:00Z"));
        mockMvc.perform(createRequest(
                """
                        {
                           "operationType": "IDLE_TIME",
                           "startDateTime": "2021-06-05T21:15:00Z",
                           "endDateTime": "2021-06-05T22:00:00Z",
                            "employeeIds": [5317]
                        }
                        """,
                "any"
        )).andExpect(status().isOk());

        mockMvc.perform(createRequest(
                        """
                                {
                                   "operationType": "IDLE_TIME",
                                   "startDateTime": "2021-06-06T08:15:00Z",
                                   "endDateTime": "2021-06-06T09:15:01",
                                    "employeeIds": [5317]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "НПО выходит за пределы смены сотрудника sof-test1 (2021-06-05 21:00 - 2021-06-06 09:00)"
                        }
                        """, false));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    public void createWithInvalidIntervalTest() throws Exception {
        mockMvc.perform(createRequest(
                """
                        {
                           "operationType": "IDLE_TIME",
                           "startDateTime": "2021-06-06T02:00:00Z",
                           "endDateTime": "2021-06-05T20:00:00Z",
                           "employeeIds": [5317]
                        }
                        """,
                "any"
        )).andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "EmployeesWithMultipleAssignments.csv")
    public void createWithFullShiftOnMultipleAssignmentsWithIds() throws Exception {
        mockClock(LocalDateTime.of(2021, 7, 14, 12, 0, 0));
        mockMvc.perform(createRequest(
                        """
                                {
                                    "operationType": "IDLE_TIME",
                                    "startDateTime": "2021-07-15T20:00:00",
                                    "isFullShift": true,
                                    "employeeIds": [16144, 16145]
                                }
                                """,
                        "any"
                ))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", is("Для сотрудников с совмещенными должностями " +
                        "нельзя создавать НПО на всю смену")));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "LoadOutstaffOperations.before.csv")
    @DbUnitDataSet(before = "LoadOutstaffOperations.withExisted.before.csv")
    public void createForOutstaffWhenOverlapped() throws Exception {
        mockMvc.perform(createRequest(
                        """
                                {
                                   "operationType": "IDLE_TIME",
                                   "startDateTime": "2021-04-28T08:59:59",
                                   "endDateTime": "2021-04-28T09:59:59",
                                    "outstaffIds": [100]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Не найдены сотрудники staff ids: [], outstaff ids: [100]"
                        }
                        """, false));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "LoadOutstaffOperations.before.csv")
    @DbUnitDataSet(before = "LoadOutstaffOperations.withExisted.before.csv")
    public void createForOutstaffWhenOverlappedWithIds() throws Exception {
        mockMvc.perform(createRequest(
                        """
                                {
                                   "operationType": "IDLE_TIME",
                                   "startDateTime": "2021-04-28T08:59:59",
                                   "endDateTime": "2021-04-28T09:59:59",
                                    "outstaffIds": [9876]
                                }
                                """,
                        "any"
                )).andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                            "message": "Для Outstaff[9876] уже существует НПО в запрошенном временном интервале"
                        }
                        """, false));
    }





    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "EditOperation.before.csv", after = "EditOperation.before.csv")
    public void editOperationForTooLongDurationWithIds() throws Exception {
        mockClock(LocalDateTime.of(2021, 7, 14, 12, 0, 0));
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/lms/non-production-operations/4")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                                .content("""
                                        {
                                            "operationType": "MENTORING",
                                            "startDateTime": "2021-04-28T00:00:00",
                                            "endDateTime": "2021-04-28T12:01:00",
                                            "createdAt": "2021-04-28T12:00:00",
                                            "outstaffIds": [9876],
                                            "isFullShift": true,
                                            "shiftNumber": "1"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }



    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "EditOperation.before.csv", after = "EditOperation.duringAnotherOperation.csv")
    public void editOperationDuringAnotherOperationWithIds() throws Exception {
        mockClock(LocalDateTime.of(2021, 7, 14, 12, 0, 0));
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/lms/non-production-operations/3")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                                .content("""
                                        {
                                            "operationType": "OTHER_WORK",
                                            "startDateTime": "2021-04-28T10:00:00",
                                            "endDateTime": "2021-04-28T21:01:00",
                                            "createdAt": "2021-04-26T12:00:00",
                                            "employeeIds": [5317],
                                            "isFullShift": "false",
                                            "shiftNumber": "4"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "LoadOperations.before.csv")
    public void loadOperationsTest() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/lms/non-production-operations")
                                .queryParam("date", "2021-04-28")
                                .queryParam("groupId", "31")
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("LoadOperationTest.json")));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "LoadOperations.before.csv")
    public void loadOperationsSecondPageTest() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/lms/non-production-operations")
                                .queryParam("date", "2021-04-28")
                                .queryParam("groupId", "31")
                                .queryParam("page", "1")
                                .queryParam("size", "2")
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("LoadOperationSecondPageTest.json")));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "LoadOutstaffOperations.before.csv")
    public void loadOutstaffOperations() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/lms/non-production-operations")
                                .queryParam("date", "2021-04-28")
                                .queryParam("groupId", "30")
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("LoadOutstaffOperations.json")));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "LoadOperations.before.csv")
    public void showOutstaffOperationsWithStaffAfterSearchFilters() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/lms/non-production-operations")
                                .queryParam("date", "2021-04-28")
                                .queryParam("groupId", "30")
                                .queryParam("employeeName", "test-l")
                ).andExpect(status().isOk())
                .andExpect(content().json(
                        loadFromFile("ShowOutstaffOperationsWithStaffAfterSearchFilters.json")));
    }

    @Test
    @DbUnitDataSet(before = "NonProductionOperationControllerTest.before.csv")
    @DbUnitDataSet(before = "LoadOperations.before.csv")
    public void showOutstaffOperationsWithStaffAfterSearchFilters2() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/lms/non-production-operations")
                                .queryParam("date", "2021-04-28")
                                .queryParam("groupId", "30")
                                .queryParam("employeeName", "test")
                ).andExpect(status().isOk())
                .andExpect(content().json(
                        loadFromFile("ShowOutstaffOperationsWithStaffAfterSearchFilters2.json")));
    }

    private static MockHttpServletRequestBuilder createRequest(String body, String by) {
        return createRequest(body, by, 1L);
    }

    private static MockHttpServletRequestBuilder createRequest(String body, String by, long domainId) {
        return MockMvcRequestBuilders.post("/lms/non-production-operations")
                .queryParam("domainId", String.valueOf(domainId))
                .cookie(new Cookie("yandex_login", by))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
