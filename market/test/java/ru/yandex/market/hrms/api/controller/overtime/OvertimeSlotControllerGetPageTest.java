package ru.yandex.market.hrms.api.controller.overtime;

import java.time.Instant;

import javax.servlet.http.Cookie;

import one.util.streamex.EntryStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.model.overtime.OvertimeRejectionReason;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@DbUnitDataSet(before = "OvertimeSlotControllerTest.common.csv")
public class OvertimeSlotControllerGetPageTest extends AbstractApiTest {

    private final static String EMPLOYEES_REJECTION_REASON_JSON_PATH_TEMPLATE =
            "$.content[*].participants[?(@.employee.id=={})].rejectionReason";

    @BeforeEach
    public void beforeEach() {
        mockClock(Instant.parse("2022-01-20T12:42:44+03:00"));
    }

    @Test
    public void shouldReturnEmptyPageWhenNoSlotsExist() throws Exception {
        mockMvc.perform(
                        get("/lms/overtime-slots/page")
                                .queryParam("domainId", "1")
                                .cookie(new Cookie("yandex_login", "first"))
                                .header("X-Admin-Roles", String.join(",",
                                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("json/overtimeSlotsPage.empty.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "OvertimeSlotControllerReplaceParticipantTest.common.csv")
    public void shouldReturnValidPageWithSlotsAndOvertimes() throws Exception {
        mockMvc.perform(
                        get("/lms/overtime-slots/page")
                                .queryParam("domainId", "1")
                                .cookie(new Cookie("yandex_login", "first"))
                                .header("X-Admin-Roles", String.join(",",
                                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("json/overtimeSlotsPage.happyPath.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "OvertimeSlotControllerGetPageTest.rejectedParticipants.csv")
    public void shouldReturnRejectionReasonsWhenOvertimeRejected() throws Exception {
        var participantRejectionReasonsMatchers = EntryStream.of(
                        1, OvertimeRejectionReason.NO_ACTIVITY,
                        2, OvertimeRejectionReason.NO_WORKED_TIME,
                        3, OvertimeRejectionReason.NOT_APPROVED,
                        4, OvertimeRejectionReason.UNSPECIFIED
                )
                .mapValues(OvertimeRejectionReason::getDescription)
                .mapKeyValue((id, expected) ->
                        jsonPath(sf(EMPLOYEES_REJECTION_REASON_JSON_PATH_TEMPLATE, id)).value(expected))
                .toArray(ResultMatcher.class);

        mockMvc.perform(
                        get("/lms/overtime-slots/page")
                                .queryParam("domainId", "1")
                                .cookie(new Cookie("yandex_login", "first"))
                                .header("X-Admin-Roles", String.join(",",
                                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.WAREHOUSE_MANAGER).getAuthorities()
                                ))
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].participants[*].state", everyItem(is("REJECTED"))))
                .andExpect(jsonPath("$.content[*].participants[*].rejectionReason", everyItem(notNullValue())))
                .andExpectAll(participantRejectionReasonsMatchers);

    }
}
