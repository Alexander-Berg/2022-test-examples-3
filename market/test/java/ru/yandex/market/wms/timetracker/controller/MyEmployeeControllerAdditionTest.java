package ru.yandex.market.wms.timetracker.controller;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.timetracker.config.TtsTestBase;
import ru.yandex.market.wms.timetracker.dto.AddMyEmployeeRequest;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;
import ru.yandex.market.wms.timetracker.utils.SecurityUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class MyEmployeeControllerAdditionTest extends TtsTestBase {

    private static final String API = "/manager-to-employee/SOF";

    private static final String MANAGER1 = "test-manager";
    private static final String MANAGER2 = "test-manager2";

    private static final String EMPLOYEE1 = "test-employee";
    private static final String EMPLOYEE2 = "test-employee2";

    /**
     * Тест на happy path. Один сотрудник одному бригу
     */
    @Test
    @WithMockUser(MANAGER1)
    @DatabaseSetup(type = DatabaseOperation.DELETE_ALL, value = "/repository/manager-to-employee/addition/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/addition/1/expectedDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() {
        perform(Collections.singletonList(EMPLOYEE1), "repository/manager-to-employee/addition/1/expected.json");
    }

    /**
     * Тест на happy path. Два сотрудника одному бригу
     */
    @Test
    @WithMockUser(MANAGER1)
    @DatabaseSetup(type = DatabaseOperation.DELETE_ALL, value = "/repository/manager-to-employee/addition/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/addition/2/expectedDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test2() {
        perform(List.of(EMPLOYEE1, EMPLOYEE2), "repository/manager-to-employee/addition/2/expected.json");
    }

    /**
     * Чуть сложнее варик. Один сотрудник перепривязывается к другому бригу
     */
    @Test
    @DatabaseSetup(type = DatabaseOperation.DELETE_ALL, value = "/repository/manager-to-employee/addition/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/addition/3/expectedDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test3() {
        perform(List.of(EMPLOYEE1), "repository/manager-to-employee/addition/3/expected1.json", MANAGER1);
        perform(List.of(EMPLOYEE1), "repository/manager-to-employee/addition/3/expected2.json", MANAGER2);
    }

    /**
     * То же, что и выше, но два сотрудника перепривязываются к другому бригу
     */
    @Test
    @DatabaseSetup(type = DatabaseOperation.DELETE_ALL, value = "/repository/manager-to-employee/addition/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/addition/4/expectedDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test4() {
        perform(
                List.of(EMPLOYEE1, EMPLOYEE2),
                "repository/manager-to-employee/addition/4/expected1.json",
                MANAGER1
        );
        perform(
                List.of(EMPLOYEE1, EMPLOYEE2),
                "repository/manager-to-employee/addition/4/expected2.json",
                MANAGER2
        );
    }

    /**
     * Чуть сложнее: первый взял двух ребят, второй отжал первого
     */
    @Test
    @DatabaseSetup(type = DatabaseOperation.DELETE_ALL, value = "/repository/manager-to-employee/addition/initialDatabase.xml")
    @ExpectedDatabase(
            value = "/repository/manager-to-employee/addition/5/expectedDatabase.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test5() {
        perform(
                List.of(EMPLOYEE1, EMPLOYEE2),
                "repository/manager-to-employee/addition/5/expected1.json",
                MANAGER1
        );
        perform(List.of(EMPLOYEE1), "repository/manager-to-employee/addition/5/expected2.json", MANAGER2);
    }

    private void perform(List<String> employees, String expectedFileName) {
        perform(employees, expectedFileName, null);
    }

    @SneakyThrows
    private void perform(List<String> employees, String expectedFileName, String username) {
        var requestBuilder = MockMvcRequestBuilders
                .post(API)
                .content(objectMapper.writeValueAsString(new AddMyEmployeeRequest(employees)))
                .contentType(MediaType.APPLICATION_JSON);

        if (username != null) {
            SecurityUtil.setSecurityContextAttribute(requestBuilder, username);
        }

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(expectedFileName)));
    }
}
