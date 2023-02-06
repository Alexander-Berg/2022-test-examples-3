package ru.yandex.market.hrms.api.controller.structure.assign;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "StructureControllerAssignTest.before.csv")
public class StructureControllerAssignTest extends AbstractApiTest {
    @Test
    void shouldReturnEmployeeAssignPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/structure/assign")
                .param("groupId", "2")
        )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("shouldReturnEmployeeAssignPage.json"), true));
    }
}
