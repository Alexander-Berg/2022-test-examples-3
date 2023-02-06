package ru.yandex.market.logistics.management.controller.admin;

import java.time.Instant;
import java.time.ZoneId;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.frontend.FrontPluginsCollector;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.FakePluginListFactory;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CleanDatabase
@Sql("/data/controller/admin/front/prepare_role.sql")
class FrontControllerTest extends AbstractContextualTest {

    @Autowired
    private FrontPluginsCollector frontPluginsCollector;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        Mockito.when(frontPluginsCollector.getPlugins()).thenReturn(FakePluginListFactory.getPluginList());
    }

    @Test
    @WithBlackBoxUser(login = "userName", uid = 1, authorities = {"ROLE_USER_AUTHORITY"})
    void getLogin() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/user")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/front/user.json"));
    }

    @Test
    @WithBlackBoxUser(login = "userName", uid = 1, authorities = {"ROLE_USER_AUTHORITY"})
    void testUserRole() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/plugins")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/front/test_user.json"));
    }

    @Test
    @WithBlackBoxUser(login = "userName", uid = 1, authorities = {"ROLE_ADMIN_AUTHORITY"})
    void testAdminRole() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/plugins")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/front/test_admin.json"));
    }

    @Test
    @WithBlackBoxUser(login = "userName", uid = 1, authorities = {"ROLE_NOT_EXIST"})
    void testNoExistRole() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/plugins")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/front/no_roles.json"));
    }

    @Test
    @WithBlackBoxUser(login = "userWithoutAuthorities", uid = 1, authorities = {})
    void testNoAuthorities() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/plugins")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/front/no_roles.json"));
    }

    @Test
    void anonymousLogin() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/user")
        )
            .andExpect(status().is(401));
    }

    @Test
    @DisplayName("Получение CSRF-токена авторизованным пользователем с yandexuid кукой")
    @WithBlackBoxUser(login = "userName", uid = 1, authorities = {"ROLE_USER_AUTHORITY"})
    void getCsrfTokenWithYandexuid() throws Exception {
        clock.setFixed(Instant.ofEpochMilli(1578889361000L), ZoneId.systemDefault());

        mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/csrf")
                .cookie(new Cookie("yandexuid", "100500"))
        )
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(content().string("262973488955f0becee56810a14eebe06d42990f:1578889361000"));

        clock.clearFixed();
    }

    @Test
    @DisplayName("Получение CSRF-токена авторизованным пользователем без yandexuid куки")
    @WithBlackBoxUser(login = "userName", uid = 1, authorities = {"ROLE_USER_AUTHORITY"})
    void getCsrfTokenWithoutYandexuid() throws Exception {
        clock.setFixed(Instant.ofEpochMilli(1578889361000L), ZoneId.systemDefault());

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/csrf"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Request does not contain 'yandexuid' cookie"));

        clock.clearFixed();
    }
}
