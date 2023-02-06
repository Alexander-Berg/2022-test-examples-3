package ru.yandex.market.mbi.bot;

import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Epic("Уведолмения")
public class NotificationsTest extends IntegrationTest {

    @Value("${mbi-partner-bot.bot-id}")
    private String botId;

    @Autowired
    private IntegrationTestClient integrationTestClient;

    @Test
    @Feature("Отписка от уведомления")
    void testUnsubscribe() {
        sendCommand("/unsubscribe 1612872409");
    }

    @Test
    @Feature("Подписка на уведомление")
    void testSubscribe() {
        sendCommand("/resubscribe 1612872409");
    }

    @Test
    @Feature("Управление подписками")
    void testManageSubscriptions() {
        sendCommand("/manageSubscriptions");
    }

    @Test
    @Feature("Управление подписками (нажатие на кнопку)")
    void testManageSubscriptionButton() {
        sendCommand("Управление подписками");
    }

    private void sendCommand(final String text) {
        final JsonObject user = IntegrationTestUtils.createUser();
        final JsonObject chat = IntegrationTestUtils.createChat();

        final JsonObject message = IntegrationTestUtils.createMessage(user, chat, text);
        final JsonObject update = new JsonObject();
        update.add("user", user);
        update.add("chat", chat);
        update.add("message", message);
        update.addProperty("text", text);
        update.addProperty("botId", botId);

        final ResponseEntity<String> response = integrationTestClient.update(update.toString());
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
