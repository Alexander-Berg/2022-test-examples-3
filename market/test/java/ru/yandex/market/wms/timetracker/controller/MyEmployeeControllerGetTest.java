package ru.yandex.market.wms.timetracker.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.timetracker.config.TtsTestBase;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class MyEmployeeControllerGetTest extends TtsTestBase {

    private static final String API = "/manager-to-employee/SOF";

    private static final String MANAGER1 = "test-manager";

    /**
     * Тест на happy path. Один сотрудник одному бригу
     */
    @Test
    @WithMockUser(MANAGER1)
    @DatabaseSetup("/repository/manager-to-employee/get/1/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/get/1/initialDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() {
        perform("repository/manager-to-employee/get/1/expected.json");
    }

    /**
     * Два сотрудник одному бригу
     */
    @Test
    @WithMockUser(MANAGER1)
    @DatabaseSetup("/repository/manager-to-employee/get/2/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/get/2/initialDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test2() {
        perform("repository/manager-to-employee/get/2/expected.json");
    }

    /**
     * Ни одного сотрудника на бриге
     */
    @Test
    @WithMockUser(MANAGER1)
    @DatabaseSetup("/repository/manager-to-employee/get/3/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/get/3/initialDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test3() {
        perform("repository/manager-to-employee/get/3/expected.json");
    }

    @SneakyThrows
    private void perform(String expectedFileName) {
        mockMvc.perform(MockMvcRequestBuilders.get(API))
               .andExpect(status().isOk())
               .andExpect(content().json(FileContentUtils.getFileContent(expectedFileName)))
               .andDo(print());
    }
}
