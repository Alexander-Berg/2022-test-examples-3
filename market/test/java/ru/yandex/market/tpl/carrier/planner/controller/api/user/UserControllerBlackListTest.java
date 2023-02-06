package ru.yandex.market.tpl.carrier.planner.controller.api.user;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserBlackListReason;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user_properties.UserPropertyQueryService;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.ANOTHER_PHONE;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class UserControllerBlackListTest extends BasePlannerWebTest {

    private final TestUserHelper testUserHelper;
    private final UserRepository userRepository;
    private final UserCommandService userCommandService;
    private final UserPropertyQueryService userPropertyQueryService;

    @SneakyThrows
    @Test
    void shouldBlackListUser() {
        User user = testUserHelper.findOrCreateUser(1L);
        mockMvc.perform(post("/internal/users/{id}/mark-blacklisted", user.getId())
                        .param("blackListReason", UserBlackListReason.NOT_USING_APP.name()))
                .andExpect(status().isOk());

        user = userRepository.findByIdOrThrow(user.getId());
        Assertions.assertThat(user.isBlackListed()).isTrue();
        var userProperty = userPropertyQueryService.getBlacklistedReason(user);
        Assertions.assertThat(userProperty).isEqualTo(UserBlackListReason.NOT_USING_APP);
    }


    @SneakyThrows
    @Test
    void shouldBlackListUserWithoutReason() {
        User user = testUserHelper.findOrCreateUser(1L);
        mockMvc.perform(post("/internal/users/{id}/mark-blacklisted", user.getId()))
                .andExpect(status().isOk());

        user = userRepository.findByIdOrThrow(user.getId());
        Assertions.assertThat(user.isBlackListed()).isTrue();
        var userProperty = userPropertyQueryService.getBlacklistedReason(user);
        Assertions.assertThat(userProperty).isNull();
    }

    @SneakyThrows
    @Test
    void shouldUnmarkBlackListUser() {
        User user = testUserHelper.findOrCreateUser(1L);
        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(user.getId(),
                UserBlackListReason.NOT_USING_APP));

        mockMvc.perform(post("/internal/users/{id}/unmark-blacklisted", user.getId()))
                .andExpect(status().isOk());

        user = userRepository.findByIdOrThrow(user.getId());
        Assertions.assertThat(user.isBlackListed()).isFalse();
        var userProperty = userPropertyQueryService.getBlacklistedReason(user);
        Assertions.assertThat(userProperty).isNull();
    }

    @SneakyThrows
    @Test
    void shouldFilterByBlacklisted() {
        User user1 = testUserHelper.findOrCreateUser(1);
        User user2 = testUserHelper.findOrCreateUser(2, Company.DEFAULT_COMPANY_NAME, ANOTHER_PHONE);

        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(user1.getId(),
                UserBlackListReason.NOT_USING_APP));

        mockMvc.perform(get("/internal/users"))
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.containsInAnyOrder(user1.getId().intValue(),
                        user2.getId().intValue())));

        mockMvc.perform(get("/internal/users")
                        .param("blacklisted", Boolean.toString(true))
                )
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(user1.getId()))
                .andExpect(jsonPath("$.content[0].blackListReason")
                        .value(UserBlackListReason.NOT_USING_APP.name()))
        ;

        mockMvc.perform(get("/internal/users")
                        .param("blacklisted", Boolean.toString(false))
                )
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(user2.getId()));
    }
}
