package ru.yandex.market.hrms.api.controller.structure.transfer;

import java.time.LocalDate;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "StructureControllerTransferTest.before.csv")
public class StructureControllerTransferTest extends AbstractApiTest {

    @Test
    @DisplayName("Успешный перевод сотрудников в будущем")
    @DbUnitDataSet(after = "StructureControllerTransferTest.transferFuture.after.csv")
    void transferFuture() throws Exception {
        mockClock(LocalDate.of(2021, 1, 25));
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 13,"
                + "  \"employeesStaffLogins\": ["
                + "    \"antipov93\","
                + "    \"timursha\""
                + "  ],"
                + "  \"applyDate\": \"2021-02-01\""
                + "}"
        )).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешный перевод сотрудника задним числом")
    @DbUnitDataSet(after = "StructureControllerTransferTest.transferRetroactively.after.csv")
    void transferRetroactively() throws Exception {
        mockClock(LocalDate.of(2021, 1, 25));
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 13,"
                + "  \"employeesStaffLogins\": ["
                + "    \"antipov93\""
                + "  ],"
                + "  \"applyDate\": \"2021-01-14\""
                + "}"
        )).andExpect(status().isOk());
    }


    @Test
    @DisplayName("Успешный перевод сотрудника до его первого назначения")
    @DbUnitDataSet(after = "StructureControllerTransferTest.transferBeforeFiredDate.after.csv")
    void transferBeforeFiredDate() throws Exception {
        mockClock(LocalDate.of(2021, 12, 25));
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 13,"
                + "  \"employeesStaffLogins\": ["
                + "    \"antipov93\""
                + "  ],"
                + "  \"applyDate\": \"2020-12-25\""
                + "}"
        )).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешный перевод терминатора до первого назначения")
    @DbUnitDataSet(after = "StructureControllerTransferTest.transferTerminator.after.csv")
    void transferTerminatorOnHiredDate() throws Exception {
        mockClock(LocalDate.of(2024, 4, 1));
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 13,"
                + "  \"employeesStaffLogins\": ["
                + "    \"arnold\""
                + "  ],"
                + "  \"applyDate\": \"2021-04-01\""
                + "}"
        )).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Попытка перевода неизвестного сотрудника")
    @DbUnitDataSet(after = "StructureControllerTransferTest.before.csv")
    void transferUnknownEmployee() throws Exception {
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 13,"
                + "  \"employeesStaffLogins\": ["
                + "    \"antipov92\""
                + "  ],"
                + "  \"applyDate\": \"2021-01-14\""
                + "}")
        ).andExpect(status().is(NOT_FOUND.value()));
    }

    @Test
    @DisplayName("Попытка перевода неизвестного сотрудника")
    @DbUnitDataSet(after = "StructureControllerTransferTest.before.csv")
    void transferToUnknownGroup() throws Exception {
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 1111,"
                + "  \"employeesStaffLogins\": ["
                + "    \"antipov93\""
                + "  ],"
                + "  \"applyDate\": \"2021-01-14\""
                + "}")
        ).andExpect(status().is(NOT_FOUND.value()));
    }

    @Test
    @DisplayName("Попытка перевода сотрудника, для которого уже задана новая группа после планируемой даты перевода")
    @DbUnitDataSet(after = "StructureControllerTransferTest.before.csv")
    void transferBeforeNextGroupJoining() throws Exception {
        mockClock(LocalDate.of(2021, 1, 15));
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 13,"
                + "  \"employeesStaffLogins\": ["
                + "    \"ogonek\""
                + "  ],"
                + "  \"applyDate\": \"2021-01-14\""
                + "}")
        ).andExpect(status().is(BAD_REQUEST.value()));
    }

    @Test
    @DisplayName("Попытка перевода сотрудника в ту же самую группу")
    @DbUnitDataSet(after = "StructureControllerTransferTest.before.csv")
    void transferToSameGroup() throws Exception {
        mockClock(LocalDate.of(2021, 1, 15));
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 11,"
                + "  \"employeesStaffLogins\": ["
                + "    \"ogonek\""
                + "  ],"
                + "  \"applyDate\": \"2021-02-14\""
                + "}")
        ).andExpect(status().is(BAD_REQUEST.value()));
    }

    private static MockHttpServletRequestBuilder transferRequest(String body) {
        return MockMvcRequestBuilders.post("/lms/structure/transfer")
                .cookie(new Cookie("yandex_login", "antipov93"))
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.HR_MANAGER).getAuthorities()
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
