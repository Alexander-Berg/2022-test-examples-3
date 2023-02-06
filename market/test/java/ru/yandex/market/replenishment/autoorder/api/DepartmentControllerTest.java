package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class DepartmentControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "DepartmentControllerTest.before.csv")
    public void testGetDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.id==1)].name").value("TEST_DEPARTMENT_1"))
            .andExpect(jsonPath("$[?(@.id==2)].name").value("TEST_DEPARTMENT_2"));
    }
}
