package ru.yandex.market.hrms.api.controller.structure.transfer;

import javax.servlet.http.Cookie;

import com.google.common.base.Strings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "StructureControllerTransferRoleTest.before.csv")
public class StructureControllerTransferRoleTest extends AbstractApiTest {
    @ParameterizedTest
    @CsvSource({
            ",403",
            "USER,403",
            HrmsPlugin.AUTHORITY_PAGE_STRUCTURE_EDIT_HIGH_ACCESS+",200",
            HrmsPlugin.AUTHORITY_PAGE_STRUCTURE_EDIT_LOW_ACCESS+",403"
    })
    void shouldNotAllowToTransferAssignedEmployeeWithoutRole(String role, int status) throws Exception {
        mockMvc.perform(transferRequest("{"
                        + "  \"groupId\": 12,"
                        + "  \"employeesStaffLogins\": ["
                        + "    \"antipov93\""
                        + "  ],"
                        + "  \"applyDate\": \"2021-02-01\""
                        + "}", Strings.nullToEmpty(role)))
                .andExpect(status().is(status));
    }

    @ParameterizedTest
    @CsvSource({
            ",403",
            "USER,403",
            HrmsPlugin.AUTHORITY_PAGE_STRUCTURE_EDIT_HIGH_ACCESS+",200",
            HrmsPlugin.AUTHORITY_PAGE_STRUCTURE_EDIT_LOW_ACCESS+",200"
    })
    void shouldNotAllowToTransferUnassignedEmployeeWithoutRole(String role, int status) throws Exception {
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 12,"
                + "  \"employeesStaffLogins\": ["
                + "    \"timursha\""
                + "  ],"
                + "  \"applyDate\": \"2021-02-01\""
                + "}", Strings.nullToEmpty(role)))
                .andExpect(status().is(status));
    }

    @CsvSource({
            ",403",
            "USER,403",
            HrmsPlugin.AUTHORITY_PAGE_STRUCTURE_EDIT_HIGH_ACCESS+",200",
            HrmsPlugin.AUTHORITY_PAGE_STRUCTURE_EDIT_LOW_ACCESS+",403"
    })
    void shouldNotAllowToTransferAssignedEmployeeToAnotherShiftWithoutRole(String role, int status) throws Exception {
        mockMvc.perform(transferRequest("{"
                + "  \"groupId\": 15,"
                + "  \"employeesStaffLogins\": ["
                + "    \"antipov93\""
                + "  ],"
                + "  \"applyDate\": \"2021-02-01\""
                + "}", Strings.nullToEmpty(role)))
                .andExpect(status().is(status));
    }


    private static MockHttpServletRequestBuilder transferRequest(String body, String role) {
        return MockMvcRequestBuilders.post("/lms/structure/transfer")
                .cookie(new Cookie("yandex_login", "123"))
                .param("domainId", "1")
                .header("X-Admin-Roles", role)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
