package ru.yandex.market.wms.auth.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.CollectionUtils;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class SkillsControllerTest extends AuthIntegrationTest {

    @Test
    @DatabaseSetup(value = "/db/controller/skills/before.xml", connection = "scprdd1Connection")
    public void listEmployeeSkills() throws Exception {
        assertHttpCall(
                MockMvcRequestBuilders.get("/admin-user-skills/skills/PICKING"),
                MockMvcResultMatchers.status().isOk(),
                Collections.emptyMap(),
                "controller/skills/list-response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/controller/skills/before.xml", connection = "scprdd1Connection")
    public void listEmployeeSkillsWithFilters() throws Exception {
        assertHttpCall(
                MockMvcRequestBuilders.get("/admin-user-skills/skills/PICKING"),
                MockMvcResultMatchers.status().isOk(),
                Map.of(
                        "filter", "userId==User2 or USERID==User1",
                        "limit", "1",
                        "offset", "1",
                        "sort", "userId",
                        "order", "DESC"
                ),
                "controller/skills/list-response-filtered.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/controller/skills/before.xml", connection = "scprdd1Connection")
    public void listEmployeeSkillsWithFilteringBySkills() throws Exception {
        assertHttpCall(
                MockMvcRequestBuilders.get("/admin-user-skills/skills/PICKING"),
                MockMvcResultMatchers.status().isOk(),
                Map.of(
                        "filter", "(SKILLS=='MEZ1',skills=='MEZ2')",
                        "sort", "USERID",
                        "order", "DESC"
                ),
                "controller/skills/list-response-filtered-by-skills.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/controller/skills/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/controller/skills/after-update.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void updateEmployeeSkills() throws Exception {
        assertHttpCall(
                MockMvcRequestBuilders.put("/admin-user-skills/skills/PICKING"),
                MockMvcResultMatchers.status().isOk(),
                "controller/skills/update-request.json",
                "controller/skills/update-response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/controller/skills/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/controller/skills/after-set-skills-to-users.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void setSkillsToUsers() throws Exception {
        assertHttpCall(
                MockMvcRequestBuilders.post("/admin-user-skills/set-skills-to-users/PICKING"),
                MockMvcResultMatchers.status().isOk(),
                "controller/skills/set-skills-to-users-request.json",
                "controller/skills/set-skills-to-users-response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/controller/skills/before.xml", connection = "scprdd1Connection")
    public void skillsList() throws Exception {
        assertHttpCall(
                MockMvcRequestBuilders.get("/admin-user-skills/skills-list"),
                MockMvcResultMatchers.status().isOk(),
                Collections.emptyMap(),
                "controller/skills/skills-list-response.json"
        );
    }

    private ResultActions assertHttpCall(MockHttpServletRequestBuilder requestBuilder,
                                         ResultMatcher status,
                                         Map<String, String> params,
                                         String responseFile) throws Exception {
        ResultActions result = mockMvc.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .params(CollectionUtils.toMultiValueMap(
                        params.entrySet().stream().collect(
                                Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue()))
                        ))))
                .andExpect(status);
        if (responseFile != null) {
            String response = getFileContent(responseFile);
            result.andExpect(content().json(response, false));
        }
        return result;
    }

    protected ResultActions assertHttpCall(MockHttpServletRequestBuilder requestBuilder,
                                           ResultMatcher status,
                                           String requestFile,
                                           String responseFile) throws Exception {
        ResultActions result = mockMvc.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(requestFile)))
                .andExpect(status);
        if (responseFile != null) {
            String response = getFileContent(responseFile);
            result.andExpect(content().json(response, false));
        }
        return result;
    }

}
