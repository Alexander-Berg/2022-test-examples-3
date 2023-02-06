package ru.yandex.market.replenishment.autoorder.api;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.UserDto;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin("ivan")
public class UserControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "UserControllerTest.before.csv")
    public void testGetUsers() throws Exception {
        var mvcResult = mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andReturn();
        var users = readJsonList(mvcResult, UserDto.class);
        Assertions.assertThat(users)
            .extracting(UserDto::getLogin)
            .containsExactlyInAnyOrder("ivan", "pavel");
    }

    @Test
    @DbUnitDataSet(before = "UserControllerTest_getUserAuthorities.before.csv")
    public void testGetUserAuthorities() throws Exception {
        mockMvc.perform(get("/api/v1/users/authorities/ivan").contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", containsInAnyOrder("ROLE_USER", "ROLE_ADMIN")));
    }

    @Test
    @DbUnitDataSet(before = "UserControllerTest_getUserAuthorities.before.csv")
    public void testGetUserAuthorities_empty() throws Exception {
        mockMvc.perform(get("/api/v1/users/authorities/petr").contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "UserControllerTest_getUserAuthorities.before.csv")
    public void testGetUserAuthorities_unknownLogin() throws Exception {
        mockMvc.perform(get("/api/v1/users/authorities/unknownLogin").contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Пользователь 'unknownLogin' не существует"));
    }

    @Test
    @WithMockLogin("ivan")
    @DbUnitDataSet(before = "UserControllerTest_getUserAuthorities.before.csv")
    public void testGetCurrentUserAuthorities() throws Exception {
        mockMvc.perform(get("/api/v1/users/current-user/authorities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", containsInAnyOrder("ROLE_USER", "ROLE_ADMIN")));
    }
}
