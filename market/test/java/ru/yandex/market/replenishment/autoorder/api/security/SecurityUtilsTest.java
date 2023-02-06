package ru.yandex.market.replenishment.autoorder.api.security;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.config.security.SecurityUtils;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
@ActiveProfiles("tvm-testing")
public class SecurityUtilsTest extends FunctionalTest {

    @Test
    @WithMockLogin(value = "boris", sourceServiceId = 2L)
    public void testGetCurrentUserLoginReturnsLoginForCorrectLogin() {
        String login = SecurityUtils.getCurrentUserLogin();
        assertEquals("boris", login);
    }

    @Test
    public void testGetCurrentUserLoginThrowExceptionIfLoginIsAbsent() {
        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUserLogin);
    }

    @Test
    @WithMockLogin(value = "boris", sourceServiceId = 2L)
    public void testGetCurrentUserLoginOrSystemReturnsLoginForCorrectLogin() {
        String login = SecurityUtils.getCurrentUserLoginOrSystem();
        assertEquals("boris", login);
    }

    @Test
    public void testGetCurrentUserLoginOrSystemReturnsSystemIfLoginIsAbsent() {
        String login = SecurityUtils.getCurrentUserLoginOrSystem();
        assertEquals("system", login);
    }
}
