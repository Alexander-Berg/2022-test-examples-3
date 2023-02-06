package ru.yandex.market.partner.mvc.controller.telegram;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;
import ru.yandex.market.notification.telegram.bot.model.dto.ValidateUserAuthInfoResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TelegramControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private PartnerBotRestClient partnerBotRestClient;

    @Test
    @DisplayName("Успешная привязка telegram-аккаунта пользователя")
    @DbUnitDataSet(
            after = "TelegramControllerFunctionalTest.testPostTelegramBindSuccessful.after.csv"
    )
    void testPostTelegramBindSuccessful() {
        ValidateUserAuthInfoResponse resp = new ValidateUserAuthInfoResponse();
        resp.setBotId("IAmBot");
        when(partnerBotRestClient.validateUserAuthInfo(anyString())).thenReturn(Optional.of(resp));

        String json = "{ " +
                "    id: 192837465, " +
                "    first_name: 'Petr', " +
                "    username: 'petya', " +
                "    photo_url: 'https://t.me/i/userpic/320/somekindofpicture.jpg', " +
                "    auth_date: 1584621280, " +
                "    hash: '94da4f0fe843950cd7d4a0951eccc537bb821c8936d8874cd2996967916b3e63' " +
                "} ";

        HttpEntity request = JsonTestUtil.getJsonHttpEntity(json);

        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/telegram/bind?_user_id=100500&datasource_id=9000",
                request
        );

        assertThat(response,
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "" +
                        "{" +
                        "    \"userId\":100500," +
                        "    \"tgId\":192837465," +
                        "    \"username\":\"petya\"," +
                        "    \"firstName\":\"Petr\"," +
                        "    \"photoUrl\":\"https://t.me/i/userpic/320/somekindofpicture.jpg\"" +
                        "}")));
    }

    @Test
    @DisplayName("Не успешная привязка telegram-аккаунта пользователя")
    @DbUnitDataSet(
            after = "TelegramControllerFunctionalTest.testPostTelegramBindFailed.after.csv"
    )
    void testPostTelegramBindFailed() {
        when(partnerBotRestClient.validateUserAuthInfo(anyString())).thenReturn(Optional.empty());

        String json = "{ " +
                "    id: 192837465, " +
                "    first_name: 'Petr', " +
                "    username: 'petya', " +
                "    photo_url: 'https://t.me/i/userpic/320/somekindofpicture.jpg', " +
                "    auth_date: 1584621280, " +
                "    hash: '94da4f0fe843950cd7d4a0951eccc537bb821c8936d8874cd2996967916b3e63' " +
                "} ";

        HttpEntity request = JsonTestUtil.getJsonHttpEntity(json);

        assertThrows(
                Exception.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/telegram/bind?_user_id=100500&datasource_id=9000",
                        request
                ));
    }

    @Test
    @DisplayName("Отвязка telegram-аккаунта пользователя")
    @DbUnitDataSet(
            before = "TelegramControllerFunctionalTest.testPostTelegramUnbind.before.csv",
            after = "TelegramControllerFunctionalTest.testPostTelegramUnbind.after.csv"
    )
    void testPostTelegramUnbind() {
        FunctionalTestHelper.delete(
                baseUrl + "/telegram/bind?userId=100500&datasource_id=9000"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"some_bot_id"})
    @NullSource
    @DisplayName("Получение коллекции telegram-аккаунтов пользователя")
    @DbUnitDataSet(
            before = "TelegramControllerFunctionalTest.testGetTelegramUser.before.csv"
    )
    void testGetTelegramUser(String botId) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/telegram/users?userId=100500" + (botId != null ? "&bot_id=" + botId : "")
        );

        assertThat(response,
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "" +
                        "[{" +
                        "    \"userId\":100500," +
                        "    \"tgId\":192837465," +
                        "    \"username\":\"petya\"," +
                        "    \"firstName\":\"Petr\"," +
                        "    \"photoUrl\":\"https://t.me/i/userpic/320/somekindofpicture.jpg\"" +
                        "}]")));
    }

    @Test
    @DisplayName("Получение пустой коллекции telegram-аккаунтов пользователя, т.к. нет привязки к боту")
    @DbUnitDataSet(
            before = "TelegramControllerFunctionalTest.testGetTelegramUser.before.csv"
    )
    void testGetEmptyTelegramUser() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/telegram/users?userId=100500&bot_id=bot_123"
        );

        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "[]")));
    }

    @Test
    @DisplayName("Успешная привязка telegram-аккаунта пользователя по ссылке из приглашения")
    @DbUnitDataSet(
            before = "TelegramControllerFunctionalTest.testPostTelegramBindInvitedUser.before.csv",
            after = "TelegramControllerFunctionalTest.testPostTelegramBindInvitedUser.after.csv"
    )
    void testPostTelegramBindInvitedUser() {
        FunctionalTestHelper.post(
                baseUrl + "/telegram/bindInvitedUser?_user_id=100500&datasource_id=9000" +
                        "&hash=1592933099236_2f4ff18cbd2144c2a54890f5cf082dd6"
        );
    }

    @Test
    @DisplayName("Не успешная привязка telegram-аккаунта пользователя по ссылке из приглашения (хэш отсутствует в БД)")
    @DbUnitDataSet(
            before = "TelegramControllerFunctionalTest.testPostTelegramBindInvitedUserWithWrongHash.before.csv",
            after = "TelegramControllerFunctionalTest.testPostTelegramBindInvitedUserWithWrongHash.after.csv"
    )
    void testPostTelegramBindInvitedUserWithWrongHash() {
        HttpClientErrorException.BadRequest exception = assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/telegram/bindInvitedUser?_user_id=100500&datasource_id=9000" +
                                "&hash=100500_blablabla"
                ));

        Assertions.assertEquals("400 Bad Request", exception.getMessage());
    }
}
