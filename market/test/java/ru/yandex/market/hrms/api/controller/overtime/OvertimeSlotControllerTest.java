package ru.yandex.market.hrms.api.controller.overtime;

import java.time.Instant;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.overtime.ticket.OvertimeTicketFactory;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.startrek.client.model.Issue;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "OvertimeSlotControllerTest.common.csv")
public class OvertimeSlotControllerTest extends AbstractApiTest {

    @MockBean
    private OvertimeTicketFactory overtimeTicketFactory;

    @AfterEach
    public void clearMocks() {
        Mockito.clearInvocations(overtimeTicketFactory);
        Mockito.reset(overtimeTicketFactory);
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource({"1,Europe/Moscow", "37,Europe/Samara", "4,Asia/Yekaterinburg", "38,Asia/Novosibirsk"})
    void createSlotInDifferentTimezones(long domainId, String displayName) throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        mockMvc.perform(
                post("/lms/overtime-slots")
                        .queryParam("domainId", String.valueOf(domainId))
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "date": "2022-01-30",
                                    "startedAt": "2022-01-30T12:00:00",
                                    "endedAt": "2022-01-30T18:00:00",
                                    "reason": "PROCESS_URGENT_TASKS"
                                }
                                 """)
        ).andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                        	"overtimeSlotId": 1,
                        	"state": "CREATED",
                            "date": "2022-01-30",
                            "startedAt": "2022-01-30T12:00:00",
                            "endedAt": "2022-01-30T18:00:00",
                            "reason": "PROCESS_URGENT_TASKS",
                            "approvalRequired": false,
                            "locked": false
                        }
                        """, true));
    }

    @Test
    @DbUnitDataSet(after = "OvertimeSlotControllerTest.withLimits.after.csv")
    void createSlotWithLimits() throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        mockMvc.perform(
                post("/lms/overtime-slots")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "date": "2022-01-30",
                                    "startedAt": "2022-01-30T12:00:00",
                                    "endedAt": "2022-01-30T18:00:00",
                                    "reason": "PROCESS_URGENT_TASKS",
                                    "limits": [
                                        {
                                            "count": 10,
                                            "positionId": 2
                                        },
                                        {
                                            "count": 2,
                                            "positionId": 1
                                        }
                                    ]
                                }
                                 """)
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "OvertimeSlotControllerTest.noLimits.after.csv")
    void createSlotWithNoLimits() throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        mockMvc.perform(
                post("/lms/overtime-slots")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "date": "2022-01-30",
                                    "startedAt": "2022-01-30T12:00:00",
                                    "endedAt": "2022-01-30T18:00:00",
                                    "reason": "PROCESS_URGENT_TASKS"
                                }
                                 """)
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "OvertimeSlotControllerTest.nightHours.after.csv")
    void createSlotWithValidNightHours() throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        mockMvc.perform(
                post("/lms/overtime-slots")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "date": "2022-01-30",
                                    "startedAt": "2022-01-30T22:00:00",
                                    "endedAt": "2022-01-30T23:00:00",
                                    "reason": "PROCESS_URGENT_TASKS"
                                }
                                 """)
        ).andExpect(status().isOk());
    }

    @Test
    void createSlotWithLimitOnNotAllowedPosition() throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        mockMvc.perform(
                post("/lms/overtime-slots")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "date": "2022-01-30",
                                    "startedAt": "2022-01-30T12:00:00",
                                    "endedAt": "2022-01-30T18:00:00",
                                    "reason": "PROCESS_URGENT_TASKS",
                                    "limits": [
                                        {
                                            "count": 1,
                                            "positionId": 3
                                        }
                                    ]
                                }
                                 """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Для должности 'кладовщик 2 категории' подработки не разрешены")));
    }

    @Test
    @DbUnitDataSet(before = "OvertimeSlotControllerCloseTest.common.csv")
    public void closeNoParticipants() throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        mockMvc.perform(
                post("/lms/overtime-slots/1/close")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "dev.1"))
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                        ))
        ).andExpect(status().isOk());
        Mockito.verifyZeroInteractions(overtimeTicketFactory);
    }

    @Test
    @DbUnitDataSet(before = "OvertimeSlotControllerCloseTest.common.csv")
    @DbUnitDataSet(
            before= "OvertimeSlotControllerLockTest.happyPath.before.csv",
            after = "OvertimeSlotControllerLockTest.happyPath.after.csv")
    public void lockHappyPath() throws Exception {
        mockClock(Instant.parse("2022-01-10T12:42:44+03:00"));

        var issueKey = "HRMSOVTAPR-1";
        var issue = Mockito.mock(Issue.class);
        when(issue.getKey()).thenReturn(issueKey);
        var ticket =  new StartrekTicket(issue, null, null);
        when(overtimeTicketFactory.createApprovalTicket(any(), any(), any())).thenReturn(ticket);

        mockMvc.perform(
                        post("/lms/overtime-slots/1/lock")
                                .queryParam("domainId", "1")
                                .cookie(new Cookie("yandex_login", "dev.1"))
                                .header("X-Admin-Roles", String.join(",",
                                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                                ))
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("APPROVAL_REQUESTED")));
    }

    @Test
    @DbUnitDataSet(before = "OvertimeSlotControllerCloseTest.common.csv")
    @DbUnitDataSet(
            before= "OvertimeSlotControllerCloseTest.happyPath.before.csv",
            after = "OvertimeSlotControllerCloseTest.happyPath.after.csv")
    public void closeHappyPath() throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        var issueKey = "SALARY-1";
        var issue = Mockito.mock(Issue.class);
        when(issue.getKey()).thenReturn(issueKey);
        var ticket =  new StartrekTicket(issue, null, null);
        when(overtimeTicketFactory.createSalaryTicket(any(), any(), any(), any())).thenReturn(ticket);

        mockMvc.perform(
                post("/lms/overtime-slots/1/close")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "dev.1"))
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                        ))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("CLOSED")));
    }

    @Test
    @DbUnitDataSet(before = "OvertimeSlotControllerCloseTest.common.csv")
    @DbUnitDataSet(
            before= "OvertimeSlotControllerCloseTest.multipleSlots.firstRejected.before.csv",
            after = "OvertimeSlotControllerCloseTest.multipleSlots.firstRejected.after.csv")
    public void closeMultipleSlotsWithFirstRejected() throws Exception {
        testClosingMultipleSlots();
        Mockito.verify(overtimeTicketFactory)
                .createSalaryTicket(any(), any(), any(), any());
    }

    @Test
    @DbUnitDataSet(before = "OvertimeSlotControllerCloseTest.common.csv")
    @DbUnitDataSet(
            before= "OvertimeSlotControllerCloseTest.multipleSlots.firstConfirmed.before.csv",
            after = "OvertimeSlotControllerCloseTest.multipleSlots.firstConfirmed.after.csv")
    public void closeMultipleSlotsWithFirstConfirmed() throws Exception {
        testClosingMultipleSlots();
        Mockito.verify(overtimeTicketFactory, times(2))
                .createSalaryTicket(any(), any(), any(), any());
    }

    @Test
    @DbUnitDataSet(
            before = "OvertimeSlotControllerReplaceParticipantTest.common.csv",
            after = "OvertimeSlotControllerReplaceParticipantTest.happyPath.after.csv"
    )
    public void replaceParticipantHappyPath() throws Exception {
        mockClock(Instant.parse("2022-01-10T12:42:44+03:00"));
        mockMvc.perform(
                put("/lms/overtime-slots/1/participants/1")
                        .queryParam("domainId", "1")
                        .queryParam("newEmployeeId", "2")
                        .cookie(new Cookie("yandex_login", "first"))
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                        ))
        ).andExpect(status().isOk());
    }

    private void testClosingMultipleSlots() throws Exception {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));

        var issue = Mockito.mock(Issue.class);
        when(issue.getKey()).thenReturn("SALARY-1", "SALARY-2");
        var ticket =  new StartrekTicket(issue, null, null);
        when(overtimeTicketFactory.createSalaryTicket(any(), any(), any(), any())).thenReturn(ticket);

        mockMvc.perform(
                post("/lms/overtime-slots/1/close")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "first"))
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                        ))
        ).andExpect(status().isOk());

        Mockito.verifyNoMoreInteractions(overtimeTicketFactory);

        mockMvc.perform(
                post("/lms/overtime-slots/2/close")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login", "second"))
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                        ))
        ).andExpect(status().isOk());
    }
}
