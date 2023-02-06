package ru.yandex.vendor.blackbox;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import ru.yandex.vendor.User;
import ru.yandex.vendor.util.IRestClient;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlackboxClientTest {
    private static final String GET_USERINFO_PATH = "/blackbox/";

    private IRestClient restClient;
    private IBlackboxClientResponseParser parser;
    private BlackboxClient blackboxClient;

    public BlackboxClientTest() {
        this.parser = new BlackboxClientResponseParser();
        this.restClient = mock(IRestClient.class);
        this.blackboxClient = new BlackboxClient(parser, restClient);
    }

    @ParameterizedTest
    @DisplayName("Получение user по фильтру login")
    @CsvSource(value = {
            "empty_response.json, vasya, , , , , , , ",
            "full_user_response.json, vasya, vasya, 100500, vasya, Vasily, Pupkin, 1/2-3, vasya@yandex.ru"
    })
    void testUserByLogin(String filename, String filter, String expectedLogin, Long expectedUid,
                         String expectedDisplayName, String expectedFirstName, String expectedLastName,
                         String expectedAvatarId, String expectedDefaultEmail) {
        setRestClientResponse(filename);

        Optional<User> user = blackboxClient.getUserByLogin(filter);
        String actualLogin = user.map(User::getLogin).orElse(null);
        Long actualUid = user.map(User::getUid).orElse(null);
        String actualDisplayName = user.map(User::getDisplayName).orElse(null);
        String actualFirstName = user.map(User::getFirstName).orElse(null);
        String actualLastName = user.map(User::getLastName).orElse(null);
        String actualAvatarId = user.map(User::getAvatarId).orElse(null);
        String actualDefaultEmail = user.map(User::getDefaultEmail).orElse(null);

        assertThat(actualUid, is(expectedUid));
        assertThat(actualLogin, is(expectedLogin));
        assertThat(actualDisplayName, is(expectedDisplayName));
        assertThat(actualFirstName, is(expectedFirstName));
        assertThat(actualLastName, is(expectedLastName));
        assertThat(actualAvatarId, is(expectedAvatarId));
        assertThat(actualDefaultEmail, is(expectedDefaultEmail));
    }

    @ParameterizedTest
    @DisplayName("Получение user по фильтру uid")
    @CsvSource(value = {
            "empty_response.json, 100500, , , , , , , ",
            "full_user_response.json, 100500, vasya, 100500, vasya, Vasily, Pupkin, 1/2-3, vasya@yandex.ru"
    })
    void testUserByUid(String filename, Long filter, String expectedLogin, Long expectedUid,
                       String expectedDisplayName, String expectedFirstName, String expectedLastName,
                       String expectedAvatarId, String expectedDefaultEmail) {
        setRestClientResponse(filename);

        Optional<User> user = blackboxClient.getUserByUid(filter);
        String actualLogin = user.map(User::getLogin).orElse(null);
        Long actualUid = user.map(User::getUid).orElse(null);
        String actualDisplayName = user.map(User::getDisplayName).orElse(null);
        String actualFirstName = user.map(User::getFirstName).orElse(null);
        String actualLastName = user.map(User::getLastName).orElse(null);
        String actualAvatarId = user.map(User::getAvatarId).orElse(null);
        String actualDefaultEmail = user.map(User::getDefaultEmail).orElse(null);

        assertThat(actualUid, is(expectedUid));
        assertThat(actualLogin, is(expectedLogin));
        assertThat(actualDisplayName, is(expectedDisplayName));
        assertThat(actualFirstName, is(expectedFirstName));
        assertThat(actualLastName, is(expectedLastName));
        assertThat(actualAvatarId, is(expectedAvatarId));
        assertThat(actualDefaultEmail, is(expectedDefaultEmail));
    }

    @Test
    @DisplayName("Пользователь отсутствует, если для него из blackbox не возвращается uid")
    void testUserEmptyWithoutUid() {
        setRestClientResponse("empty_response.json");
        Optional<User> user = blackboxClient.getUserByUid(100500);
        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("Получение user по фильтру списку uid")
    void testMultipleUserByLogin() {
        setRestClientResponse("multiple_full_users_response.json");

        final Set<User> expectedUsers = Sets.newHashSet(
                new User()
                        .setUid(100500L)
                        .setLogin("vasya")
                        .setDisplayName("vasya")
                        .setFirstName("Vasily")
                        .setLastName("Pupkin")
                        .setAvatarId("1/2-3")
                        .setDefaultEmail("vasya@yandex.ru"),
                new User()
                        .setUid(100501L)
                        .setLogin("petya")
                        .setDisplayName("petya")
                        .setFirstName("Petya")
                        .setLastName("Vasilev")
                        .setAvatarId("1/2-3")
                        .setDefaultEmail("petya@yandex.ru")
        );

        final Set<User> actualUsers = blackboxClient.getUsersByUids(Sets.newHashSet(100500L, 100501L));

        assertEquals(expectedUsers, actualUsers);
    }


    private void setRestClientResponse(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        Resource response = ofNullable(classLoader.getResource("ru/yandex/vendor/blackbox/" + filename))
                .map(url -> {
                    try {
                        return url.openStream();
                    } catch (IOException e) {
                        fail("Cannot get input file " + filename);
                    }
                    return null;
                })
                .map(InputStreamResource::new)
                .orElseThrow(() -> new AssertionError("Can't get " + filename + " file."));

        when(restClient.getForObject(eq(GET_USERINFO_PATH), any(), any())).thenReturn(response);
    }
}
