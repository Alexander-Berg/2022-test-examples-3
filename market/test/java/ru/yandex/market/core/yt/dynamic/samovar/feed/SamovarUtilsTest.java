package ru.yandex.market.core.yt.dynamic.samovar.feed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Date: 10.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarUtilsTest {

    @DisplayName("Парсинг некорректной строки auth возращает null")
    @ParameterizedTest(name = "auth = {0}")
    @CsvSource({
            "''",
            "login",
            "login:password:id",
            "login:",
            ":password"
    })
    void parseAuth_incorrectAuth_nullCredentials(String auth) {
        assertNull(SamovarUtils.parseAuth(auth));
    }

    @DisplayName("Парсинг null строки auth возращает null")
    @Test
    void parseAuth_nullAuth_nullCredentials() {
        assertNull(SamovarUtils.parseAuth(null));
    }

    @DisplayName("Парсинг корректной строки auth возращает null")
    @ParameterizedTest(name = "auth = {0}, login = {1}, password = {2}")
    @CsvSource({
            "nick:qwerty,nick,qwerty",
            "lgn:eto,lgn,eto",
            "login:password,login,password"
    })
    void parseAuth_correctAuth_credentials(String auth, String login, String password) {
        ResourceAccessCredentials credentials = SamovarUtils.parseAuth(auth);
        assertNotNull(credentials);
        assertEquals(login, credentials.login());
        assertEquals(password, credentials.password());
    }
}
