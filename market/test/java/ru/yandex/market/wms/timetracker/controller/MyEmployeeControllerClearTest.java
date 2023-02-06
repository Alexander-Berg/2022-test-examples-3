package ru.yandex.market.wms.timetracker.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.timetracker.config.TtsTestBase;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class MyEmployeeControllerClearTest extends TtsTestBase {

    private static final String API = "/manager-to-employee/SOF";

    private static final String MANAGER1 = "test-manager";

    /**
     * Тест на happy path. Пользак в параметре
     */
    @Test
    @WithMockUser
    @DatabaseSetup("/repository/manager-to-employee/clear/1/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/clear/1/afterDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() {
        perform(MockMvcRequestBuilders.delete(API).queryParam("manager", MANAGER1));
    }

    /**
     * Тест на happy path. Пользак в аутентификации
     */
    @Test
    @WithMockUser(MANAGER1)
    @DatabaseSetup("/repository/manager-to-employee/clear/2/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/clear/2/afterDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test2() {
        perform(MockMvcRequestBuilders.delete(API));
    }

    @SneakyThrows
    private void perform(MockHttpServletRequestBuilder requestBuilder) {
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(print());
    }
}
