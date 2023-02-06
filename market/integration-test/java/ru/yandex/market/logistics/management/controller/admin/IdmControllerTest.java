package ru.yandex.market.logistics.management.controller.admin;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.IdmController;
import ru.yandex.market.logistics.management.domain.entity.idm.UserRole;
import ru.yandex.market.logistics.management.repository.IdmRepository;
import ru.yandex.market.logistics.management.service.frontend.FrontPluginsCollector;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.FakePluginListFactory;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@CleanDatabase
@Sql("/data/controller/idm/prepare_role.sql")
@SuppressWarnings({"checkstyle:MagicNumber"})
class IdmControllerTest extends AbstractContextualTest {
    private static final String NOT_EXIST_LOGIN = "test";
    private static final String PROJECT = "test";
    private static final String ROLE = "USER";

    @Autowired
    private IdmController idmController;

    @Autowired
    private IdmRepository idmRepository;

    @Autowired
    private FrontPluginsCollector frontPluginsCollector;

    @BeforeEach
    void setup() {
        Mockito.when(frontPluginsCollector.getPlugins()).thenReturn(FakePluginListFactory.getPluginList());
    }

    @Test
    void infoHandleTest() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/idm/info/")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/idm/info.json"));
    }

    @Test
    void addRoleTest() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/idm/add-role/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .content("login=" + NOT_EXIST_LOGIN + "&path=/project/" + PROJECT + "/role/" + ROLE + "/")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/idm/ok.json"));

        List<UserRole> userRoles = idmRepository.findAllByLogin(NOT_EXIST_LOGIN);
        softly.assertThat(userRoles.size()).isEqualTo(1);
        UserRole actualUserRole = userRoles.get(0);

        softly.assertThat(actualUserRole.getProject()).isEqualTo(PROJECT);
        softly.assertThat(actualUserRole.getRole()).isEqualTo(ROLE);
    }

    @Test
    void addRoleWithInvalidPath() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/idm/add-role/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .content("login=" + NOT_EXIST_LOGIN + "&path=/")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/idm/500.json"));
    }

    @Test
    void deleteExistRole() throws Exception {
        String loginToDelete = "sql";

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/idm/remove-role/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .content("login=" + loginToDelete + "&path=/project/" + PROJECT + "/role/" + ROLE + "/")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/idm/ok.json"));

        softly.assertThat(idmRepository.findAll().size()).isEqualTo(2);
        softly.assertThat(idmRepository.findAllByLogin(loginToDelete).size()).isEqualTo(2);
        softly.assertThat(idmRepository.findByLoginAndProjectAndRole(loginToDelete, PROJECT, ROLE)).isEmpty();
    }

    @Test
    void deleteNotExistRole() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/idm/remove-role/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .content("login=" + NOT_EXIST_LOGIN + "&path=/project/" + PROJECT + "/role/" + ROLE + "/")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/idm/ok.json"));
    }

    @Test
    void getAllRoles() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/idm/get-all-roles/")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/idm/get_all.json"));
    }
}
