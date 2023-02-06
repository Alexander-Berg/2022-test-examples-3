package ru.yandex.market.sc.internal.controller.partner;

import java.util.Optional;

import javax.annotation.Nullable;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.dto.user.UserRequestDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxOAuthException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@ScIntControllerTest
class PartnerUserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    UserRepository userRepository;
    @SpyBean(name = "blackboxClient")
    BlackboxClient blackboxClient;
    @SpyBean(name = "innerBlackboxClient")
    BlackboxClient innerBlackboxClient;

    @Test
    void getSeniorStockman() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L, UserRole.SENIOR_STOCKMAN);
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"user\":" +
                                        userJson(sortingCenter, user.getName(), user.getEmail(), false, user.getId(),
                                                UserRole.SENIOR_STOCKMAN, null) + "}",
                                true
                        ));
    }

    @Test
    void getSeniorStockmanWithStaffLogin() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L, UserRole.SENIOR_STOCKMAN, "vasya");
        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"user\":" +
                                        userJson(sortingCenter, user.getName(), user.getEmail(), false, user.getId(),
                                                UserRole.SENIOR_STOCKMAN, user.getStaffLogin()) + "}",
                                true
                        ));
    }

    @Test
    void cantGetAdmin() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L, UserRole.ADMIN);
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
        )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cantCreateUserWithAdminRole() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                        .header("Content-Type", "application/json")
                        .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"ADMIN\"}")
        )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cantCreateUserWithSupportRole() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                                .header("Content-Type", "application/json")
                                .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"SUPPORT\"}")
                )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cantUpdateUserToAdminRole() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);
        mockMvc.perform(
                put(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
                        .header("Content-Type", "application/json")
                        .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"ADMIN\"}")
        )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getUserRoles() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId() + "/users/roles")
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{" +
                                        "\"roles\": [\"STOCKMAN\", \"SENIOR_STOCKMAN\", \"NEWBIE_STOCKMAN\" , \"MASTER_STOCKMAN\"]" +
                                        "}",
                                true
                        ));
    }

    @Test
    void clientErrorOnBlackboxAuthExceptionOnCreateUser() throws Exception {
        doThrow(BlackboxOAuthException.class).when(blackboxClient).getUidForLogin(eq("vasya"));
        var sortingCenter = testFactory.storedSortingCenter();
        mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                        .header("Content-Type", "application/json")
                        .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"}")
        )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createUser() throws Exception {
        doReturn(1L).when(blackboxClient).getUidForLogin(eq("vasya"));
        var sortingCenter = testFactory.storedSortingCenter();
        mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                        .header("Content-Type", "application/json")
                        .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"}")
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"user\":" + userJson(sortingCenter, "Вася", "vasya@yandex.ru") + "}",
                                false
                        ));
    }

    @Test
    void createUserWithStaffLogin() throws Exception {
        doReturn(1L).when(blackboxClient).getUidForLogin(eq("vasya"));
        doReturn(1L).when(innerBlackboxClient).getUidForLogin(eq("vasya"));
        var sortingCenter = testFactory.storedSortingCenter();
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                                .header("Content-Type", "application/json")
                                .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"," +
                                        "\"staffLogin\":\"vasya\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"user\":" + userJson(sortingCenter, "Вася", "vasya@yandex.ru", false, null,
                                        UserRole.STOCKMAN, "vasya") + "}",
                                false
                        ));
    }

    @Test
    void createUserStaffLoginPatternValidation() throws Exception {
        doReturn(1L).when(blackboxClient).getUidForLogin(any());
        doReturn(1L).when(innerBlackboxClient).getUidForLogin(any());
        var sortingCenter = testFactory.storedSortingCenter();
        String[] validStaffLogins = {"a", "aa", "a-a", "1", "11", "1-1", "a_", "a".repeat(40)};
        String[] invalidStaffLogins = {"", "-", "-a", "a-", "--", "-a-", "a%a", ".a", "a".repeat(41)};
        for (String staffLogin : validStaffLogins) {
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                    .header("Content-Type", "application/json")
                    .content(String.format("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"," +
                            "\"staffLogin\":\"%s\"}", staffLogin))
            ).andExpect(status().isOk());
            Optional<User> userOptional = userRepository.findByUid(1L);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                assertThat(user.getStaffLogin()).isEqualTo(staffLogin);
                userRepository.deleteById(user.getId());
            } else {
                throw new IllegalStateException("User must have been created, but it had not");
            }
        }
        for (String staffLogin : invalidStaffLogins) {
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                    .header("Content-Type", "application/json")
                    .content(String.format("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"," +
                            "\"staffLogin\":\"%s\"}", staffLogin))
            ).andExpect(status().is4xxClientError());
        }
    }

    @Test
    void createUserWithUnknownStaffLogin() throws Exception {
        String unknownStaffLogin = "unknown-staff-login";
        doThrow(new NullPointerException("Unknown login")).when(innerBlackboxClient).getUidForLogin(unknownStaffLogin);
        var sortingCenter = testFactory.storedSortingCenter();
        mockMvc.perform(MockMvcRequestBuilders
                .post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                .header("Content-Type", "application/json")
                .content(String.format("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"," +
                        "\"staffLogin\":\"%s\"}", unknownStaffLogin))
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void clientErrorOnBlackboxAuthExceptionOnUpdateUser() throws Exception {
        doThrow(BlackboxOAuthException.class).when(blackboxClient).getUidForLogin(eq("vasya"));
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);
        mockMvc.perform(
                put(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
                        .header("Content-Type", "application/json")
                        .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"}")
        )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateUser() throws Exception {
        doReturn(1L).when(blackboxClient).getUidForLogin(eq("petya"));
        doReturn(1L).when(innerBlackboxClient).getUidForLogin(any());
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);
        mockMvc.perform(
                put(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
                        .header("Content-Type", "application/json")
                        .content("{\"name\":\"Петя\",\"email\":\"petya@yandex.ru\",\"role\":\"STOCKMAN\"," +
                                "\"staffLogin\":\"petya-staff\"}")
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"user\":" +
                                        userJson(sortingCenter, "Петя", "petya@yandex.ru", false,
                                                user.getId(), UserRole.STOCKMAN, "petya-staff") + "}",
                                true
                        ));
    }

    @Test
    void getUser() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"user\":" +
                                        userJson(sortingCenter, user.getName(), user.getEmail(), user.getId()) + "}",
                                true
                        ));
    }

    @Test
    void getUsers() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users")
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"users\":[" +
                                        userJson(sortingCenter, user.getName(), user.getEmail(), user.getId()) + "]}",
                                true
                        ));
    }

    @Test
    void deleteUser() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);
        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/users/" + user.getId())
        )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(
                                "{\"user\":" +
                                        userJson(sortingCenter, user.getName(),
                                                user.getEmail(), true, user.getId(), UserRole.STOCKMAN, null) + "}",
                                true
                        ));
        assertThat(userRepository.findByIdOrThrow(user.getId()).isDeleted()).isTrue();
    }

    @Test
    void restoreDeletedUserOnOtherSc() throws Exception {
        doReturn(1L).when(blackboxClient).getUidForLogin(eq("vasya"));

        SortingCenter sortingCenter = testFactory.storedSortingCenter();
        SortingCenter wrongSortingCenter = testFactory.storedSortingCenter(666);
        User user = testFactory.storedUser(wrongSortingCenter, 1L);

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(
                                "/internal/partners/" + wrongSortingCenter.getPartnerId() + "/users/" + user.getId())
                )
                .andExpect(status().isOk());

        assertThat(userRepository.findByIdOrThrow(user.getId()).isDeleted()).isTrue();

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/users")
                                .header("Content-Type", "application/json")
                                .content("{\"name\":\"Вася\",\"email\":\"vasya@yandex.ru\",\"role\":\"STOCKMAN\"}")
                )
                .andExpect(status().isOk());

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getSortingCenter()).isEqualTo(sortingCenter);
    }

    @SuppressWarnings("SameParameterValue")
    private String userJson(SortingCenter sortingCenter, String name, String email) {
        return userJson(sortingCenter, name, email, null);
    }

    private String userJson(SortingCenter sortingCenter, String name, String email, @Nullable Long id) {
        return userJson(sortingCenter, name, email, false, id, UserRole.STOCKMAN, null);
    }

    private String userJson(
            SortingCenter sortingCenter, String name,
            String email, boolean deleted, @Nullable Long id, UserRole role, String staffLogin
    ) {
        return "{\"sortingCenterId\":" + sortingCenter.getId() + "," +
                "\"name\":\"" + name + "\"" + (id == null ? "" : (",\"id\":" + id)) + ", \"email\":\"" + email + "\"," +
                "\"role\":\"" + role + "\",\"deleted\":" + deleted +
                (staffLogin == null ? "" : (",\"staffLogin\":" + staffLogin)) +
                "}";
    }

    @Test
    @SneakyThrows
    @DisplayName("При включенном hrms флаге разрешаем изменять роль из ПИ")
    void updateRoleWhenHermesFlagEnabled() {
        doReturn(16L).when(blackboxClient).getUidForLogin(any());
        doReturn(16L).when(innerBlackboxClient).getUidForLogin(any());

        var sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.HERMES_USER_MANAGEMENT, Boolean.TRUE.toString());
        var user = testFactory.storedUser(sortingCenter, 16L);

        String content = "{\"name\":\"" + user.getName() + "\", \"role\":\"" + UserRole.SENIOR_STOCKMAN.name() + "\"}";
        mockMvc.perform(put("/internal/partners/{scId}/users/{userId}", sortingCenter.getPartnerId(), user.getId())
                        .header("Content-Type", "application/json")
                        .content(content))
                .andDo(print())
                .andExpect(status().isOk());

        user = testFactory.findUserByUid(user.getUid());
        assertThat(user.getRole()).isEqualTo(UserRole.SENIOR_STOCKMAN);
    }

    @Test
    void validateUserRequestDto() {
        assertThat(ScTestUtils.isValid(new UserRequestDto(
                "Evgeny", "mors741@yandex.ru", UserRole.PARTNER, "mors741"))
        ).isTrue();
        assertThat(ScTestUtils.isValid(new UserRequestDto(
                "Evgeny", "mors741@hrms-sc.ru", UserRole.PARTNER, "mors741"))
        ).isTrue();
        assertThat(ScTestUtils.isValid(new UserRequestDto(
                "", "mors741@yandex.ru", UserRole.PARTNER, "mors741"))
        ).isFalse();
        assertThat(ScTestUtils.isValid(new UserRequestDto(
                "Evgeny", "mors741@mail.ru", UserRole.PARTNER, "mors741"))
        ).isFalse();
    }
}
