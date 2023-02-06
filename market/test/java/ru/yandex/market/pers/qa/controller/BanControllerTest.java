package ru.yandex.market.pers.qa.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.client.dto.UserBanInfoDto;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.mock.mvc.BanMvcMocks;

import java.time.Instant;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author varvara
 * 12.09.2019
 */
public class BanControllerTest extends ControllerTest {

    public static final long MODERATOR_ID = 123;

    @Autowired
     private BanMvcMocks banMvcMocks;

    @Test
    public void testBanDifferentUserTypes() throws Exception {
        String userId = String.valueOf(1324);
        String bodyBan = createBodyRequest(null, "в черный список");
        String bodyTrust = createBodyRequest(Instant.now().plus(1, DAYS), "в белый список");

        UserType userType = UserType.UID;
        UserType userTypeAnother = UserType.SHOP;

        banMvcMocks.banUser(MODERATOR_ID, userType, userId, bodyTrust, status().is2xxSuccessful());
        UserBanInfoDto info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isBanned());
        assertFalse(info.isTrusted());

        banMvcMocks.trust(MODERATOR_ID, userTypeAnother, userId, bodyBan, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userTypeAnother, userId, status().is2xxSuccessful());
        assertFalse(info.isBanned());
        assertTrue(info.isTrusted());

        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isBanned());
        assertFalse(info.isTrusted());
    }

    @ParameterizedTest
    @MethodSource("getUserTypes")
    public void testBanTrusted(UserType userType) throws Exception {
        String userId = String.valueOf(1324);
        String bodyBan = createBodyRequest(null, "в черный список");
        String bodyTrust = createBodyRequest(Instant.now().plus(1, DAYS), "в белый список");

        // trust добавляет в белый список
        banMvcMocks.trust(MODERATOR_ID, userType, userId, bodyTrust, status().is2xxSuccessful());
        UserBanInfoDto info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertFalse(info.isBanned());
        assertTrue(info.isTrusted());

        // ban добавляет в черный список, удаляет из белого
        banMvcMocks.banUser(MODERATOR_ID, userType, userId, bodyBan, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isBanned());
        assertFalse(info.isTrusted());
    }

    @ParameterizedTest
    @MethodSource("getUserTypes")
    public void testTrustBanned(UserType userType) throws Exception {
        String userId = String.valueOf(1324);
        String bodyBan = createBodyRequest(null, "в черный список");
        String bodyTrust = createBodyRequest(Instant.now().plus(1, DAYS), "в белый список");

        // ban добавляет в черный список
        banMvcMocks.banUser(MODERATOR_ID, userType, userId, bodyBan, status().is2xxSuccessful());
        UserBanInfoDto info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isBanned());
        assertFalse(info.isTrusted());

        // trust добавляет в белый список и удаляет из черного
        banMvcMocks.trust(MODERATOR_ID, userType, userId, bodyTrust, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertFalse(info.isBanned());
        assertTrue(info.isTrusted());

        // ban добавляет в черный список и удаляет из белого
        banMvcMocks.banUser(MODERATOR_ID, userType, userId, bodyBan, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isBanned());
        assertFalse(info.isTrusted());
    }

    @ParameterizedTest
    @MethodSource("getUserTypes")
    public void testUserTrustNotForever(UserType userType) throws Exception {
        String userId = String.valueOf(1324);
        String body = createBodyRequest(Instant.now().plus(1, DAYS), "в белый список");
        banMvcMocks.trust(MODERATOR_ID, userType, userId, body, status().is2xxSuccessful());

        UserBanInfoDto info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isTrusted());
        assertFalse(info.isBanned());

        banMvcMocks.mistrust(MODERATOR_ID, userType, userId, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertFalse(info.isTrusted());
        assertFalse(info.isBanned());
    }

    @ParameterizedTest
    @MethodSource("getUserTypes")
    public void testUserTrustForever(UserType userType) throws Exception {
        String userId = String.valueOf(1324);
        String body = createBodyRequest(null, "в белый список");
        banMvcMocks.trust(MODERATOR_ID, userType, userId, body, status().is2xxSuccessful());

        UserBanInfoDto info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isTrusted());
        assertFalse(info.isBanned());

        banMvcMocks.mistrust(MODERATOR_ID, userType, userId, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertFalse(info.isTrusted());
        assertFalse(info.isBanned());
    }

    @ParameterizedTest
    @MethodSource("getUserTypes")
    public void testBanUserForever(UserType userType) throws Exception {
        String userId = String.valueOf(1324);
        String body = createBodyRequest(null, "баним в тесте навсегда");
        banMvcMocks.banUser(MODERATOR_ID, userType, userId, body, status().is2xxSuccessful());

        UserBanInfoDto info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isBanned());

        banMvcMocks.unbanUser(MODERATOR_ID, userType, userId, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertFalse(info.isBanned());
        assertFalse(info.isTrusted());
    }

    @ParameterizedTest
    @MethodSource("getUserTypes")
    public void testBanUserNotForever(UserType userType) throws Exception {
        String userId = String.valueOf(1324);
        String body = createBodyRequest(Instant.now().plus(1, DAYS), "баним в тесте не навсегда");
        banMvcMocks.banUser(MODERATOR_ID, userType, userId, body, status().is2xxSuccessful());

        UserBanInfoDto info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertTrue(info.isBanned());
        assertFalse(info.isTrusted());

        banMvcMocks.unbanUser(MODERATOR_ID, userType, userId, status().is2xxSuccessful());
        info = banMvcMocks.getUserInfo(userType, userId, status().is2xxSuccessful());
        assertFalse(info.isBanned());
        assertFalse(info.isTrusted());
    }

    private String createBodyRequest(Instant dateTo, String description) {
        if (dateTo != null) {
            String template = "{\n" +
                "\"dateTo\" : \"%s\",\n" +
                "\"moderatorId\" : \"%s\",\n" +
                "\"description\" : \"%s\"\n" +
                "}";
            String date = convertToIsoLocalDateTime(dateTo);
            return String.format(template, date, MODERATOR_ID, description);
        } else {
            String template = "{\n" +
                "\"moderatorId\" : \"%s\",\n" +
                "\"description\" : \"%s\"\n" +
                "}";
            return String.format(template, MODERATOR_ID, description);
        }
    }

    private static Stream<Arguments> getUserTypes() {
        return Stream.of(UserType.values())
            .map(Arguments::of);
    }

}
