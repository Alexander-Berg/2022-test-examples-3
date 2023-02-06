package ru.yandex.market.replenishment.autoorder.config.security.tvm;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.User;
import ru.yandex.market.replenishment.autoorder.repository.postgres.UserRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.security.WithMockTvm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests of {@link TvmAuthenticationProvider}.
 */
public class TvmAuthenticationProviderTest extends ControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockLogin("sergey")
    public void createUserIfItPassedInRequestHeader() throws Exception {
        // вызываем любую ручку, чтобы сработал код в TvmAuthenticationProvider
        mockMvc.perform(get("/idm/get-all-roles/"))
            .andExpect(status().isOk());

        var users = userRepository.findAll();
        Assertions.assertThat(users)
            .extracting(User::getLogin)
            .containsExactlyInAnyOrder("sergey");
    }

    @Test
    @WithMockTvm
    public void dontCreateUserIfItDidPassInRequestHeader() throws Exception {
        // вызываем любую ручку, чтобы сработал код в TvmAuthenticationProvider
        mockMvc.perform(get("/idm/get-all-roles/"))
            .andExpect(status().isOk());

        var users = userRepository.findAll();
        Assertions.assertThat(users).isEmpty();
    }
}
