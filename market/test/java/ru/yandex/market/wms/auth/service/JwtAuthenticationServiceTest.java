package ru.yandex.market.wms.auth.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.core.exception.TokenException;
import ru.yandex.market.wms.auth.core.model.InforAuthentication;
import ru.yandex.market.wms.common.spring.exception.LoginException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class JwtAuthenticationServiceTest extends AuthIntegrationTest {

    @Autowired
    private JwtAuthenticationService jwtAuthenticationService;

    @Autowired
    @MockBean
    private LdapTemplate ldapTemplate;

    @Test
    @DatabaseSetup(value = "/db/service/jwt-auth/users.xml", connection = "scprdd1Connection")
    public void checkInactive() {
        Exception exception = assertThrows(LoginException.class, () -> {
            jwtAuthenticationService.generateToken("AD1", "pass");
        });
    }

    @Test
    @DatabaseSetup(value = "/db/service/jwt-auth/users.xml", connection = "scprdd1Connection")
    public void checkActive() {
        String exptected = new InforAuthentication("AD2", "token", List.of()).getUser();
        String actual = jwtAuthenticationService.generateToken("AD2", "pass").getUser();
        assertEquals(exptected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/service/jwt-auth/users.xml", connection = "scprdd1Connection")
    public void validateAndRefreshTokenMultiLoginDisable() {
        ReflectionTestUtils.setField(jwtAuthenticationService, "multiLoginIsDisable", true);
        ReflectionTestUtils.setField(jwtAuthenticationService, "tokenExpirationMinutes", 0);
        String token = jwtAuthenticationService.generateToken("AD2", "pass").getToken();
        // получаем второй токен
        String secondToken = jwtAuthenticationService.generateToken("AD2", "pass").getToken();

        // первый должен отвалиться
        Exception exception = assertThrows(
                TokenException.class,
                () -> jwtAuthenticationService.validateAndRefreshToken(token, true));

        assertTrue(exception.getMessage().contains("Multi login is disable"));

    }

    @Test
    @DatabaseSetup(value = "/db/service/jwt-auth/users.xml", connection = "scprdd1Connection")
    public void validateAndRefreshTokenMultiLoginEnable() {
        ReflectionTestUtils.setField(jwtAuthenticationService, "multiLoginIsDisable", false);
        String token = jwtAuthenticationService.generateToken("AD2", "pass").getToken();
        String secondToken = jwtAuthenticationService.generateToken("AD2", "pass").getToken();
        try {
            jwtAuthenticationService.validateAndRefreshToken(token, true);
        } catch (Exception exception) {
            fail("Should not thrown exception");
        }

        try {
            jwtAuthenticationService.validateAndRefreshToken(secondToken, true);
        } catch (Exception exception) {
            fail("Should not thrown exception");
        }
    }
}
