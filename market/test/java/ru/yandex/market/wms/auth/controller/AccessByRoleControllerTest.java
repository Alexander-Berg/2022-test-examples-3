package ru.yandex.market.wms.auth.controller;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.dao.AccessByRoleDao;
import ru.yandex.market.wms.auth.model.dto.Screen;
import ru.yandex.market.wms.auth.model.request.AccessByRoleCreateRequest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class AccessByRoleControllerTest extends AuthIntegrationTest {

    @Autowired
    @SpyBean
    private AccessByRoleDao accessByRoleDao;

    @AfterEach
    public void reset() {
        Mockito.reset(accessByRoleDao);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    @ExpectedDatabase(
            value = "/db/dao/access-by-role/create-access-after.xml",
            connection = "authConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createScreenCodeTest() throws Exception {
        Screen request = new Screen(
                "TRANSPORT",
                "TRANSPORT",
                Set.of("ADMINISTRATOR", "ADMINISTRATOR1", "ADMINISTRATOR2"),
                "ROLES",
                "COMMENT",
                null,
                "test",
                null,
                "test");

        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(post("/access").content(content).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void createScreenCodeWhenExistTest() throws Exception {
        AccessByRoleCreateRequest request = new AccessByRoleCreateRequest(
                "INBOUND",
                "INBOUND",
                Set.of("ADMINISTRATOR", "ADMINISTRATOR1", "ADMINISTRATOR2"));

        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(
                post("/access").content(content).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/access/create-code-when-exist.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getCodesByRoleTest() throws Exception {
        mockMvc.perform(get("/access/getCodesByRole?role=ADMINISTRATOR").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/access/get-codes-by-roles.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getRolesByCodeTest() throws Exception {
        mockMvc.perform(get("/access/getRoles?code=RECEIVING").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/access/get-roles-by-code.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    @ExpectedDatabase(
            value = "/db/dao/access-by-role/delete-access-after.xml",
            connection = "authConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void deleteScreenCodeTest() throws Exception {
        mockMvc.perform(
                delete("/access/RECEIVING").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void deleteScreenCodeWhenNotFoundTest() throws Exception {
        mockMvc.perform(
                        delete("/access/RECEIVING5").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent("controller/access/delete-code-when-not-found.json")));
    }

    @Test
    public void getAllScreenCodes() throws Exception {
        mockMvc.perform(
                        get("/access/screenCodesEnum").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/access/get-screen-codes-enum.json")));
    }

    @Test
    public void getAllAccessObjectsTest() throws Exception {
        mockMvc.perform(
                        get("/access/accessObjectEnum").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/access/get-access-object-enum.json")));
    }

    @Test
    public void getAllAccessTypesTest() throws Exception {
        mockMvc.perform(
                        get("/access/accessTypeEnum").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/access/get-access-type-enum.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    @ExpectedDatabase(
            value = "/db/dao/access-by-role/update-access-after.xml",
            connection = "authConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void updateAccessTest() throws Exception {
        mockMvc.perform(
                        put("/access/RECEIVING")
                                .content(getFileContent("controller/access/update-access-request.json"))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void updateAccessWithBadRequest() throws Exception {
        mockMvc.perform(
                        put("/access/RECEIVING")
                                .content(getFileContent("controller/access/update-access-bad-request.json"))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/access/update-access-bad-response.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void listCodesTest() throws Exception {
        mockMvc.perform(
                        get("/access?filter=code==RECEIVING")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/access/list-codes-response.json")));


    }
}
