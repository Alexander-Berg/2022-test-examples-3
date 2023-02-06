package ru.yandex.market.mbi.api.servlets;

import java.util.List;

import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import ru.market.partner.notification.client.PartnerNotificationSendException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.core.tanker.dao.CachedTankerDaoImpl;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты на ручку /send-message-to-shop.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "SendShopMessageFunctionalTest.before.csv")
class SendShopMessageFunctionalTest extends FunctionalTest {
    @Autowired
    private CachedTankerDaoImpl tankerDao;

    /**
     * Обычно запись в танкер происходит через кэш, записи в кэше при этом инвалидируются.
     * В тестах запись в танкеровые таблицы происходит из дб юнита, и кэш находится в невалидном состоянии.
     * Чтобы избежать этого, он чистится перед каждым тестом.
     */
    @BeforeEach
    public void cleanCaches() {
        tankerDao.cleanUpCache();
    }

    /**
     * Отправляем сообщение несуществущему магазину. Ожидаем 400.
     */
    @Test
    void sendToNonExistedShop() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> mbiApiClient.sendMessageToShop(-1L, 105, ""))
                .satisfies(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    /**
     * Отправляем несуществующий шаблон. Ожидаем 400.
     */
    @Test
    void sendNonExistedTemplate() {
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient, () -> {
            throw new PartnerNotificationSendException("Notification type not found: -1", 400, List.of());
        });

        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> mbiApiClient.sendMessageToShop(1L, -1, ""))
                .satisfies(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    /**
     * Отправляем письмо магазину.
     */
    @Test
    void sendMessageToShop() {
        long shopId = 1;
        int notificationTypeId = 105;

        String response = mbiApiClient.sendMessageToShop(shopId, notificationTypeId,
                "<abo-info><error-description>error</error-description></abo-info>");
        assertThat(response).contains("MessageId = ");

        verifySentNotificationType(partnerNotificationClient, 1, notificationTypeId);
    }

    /**
     * Отправляем письмо о новом заказе ДБС магазину.
     * Проверяем, что нет падений при генерации сообщения.
     */
    @Test
    void sendNewOrderMessageToDBSShop() {
        long shopId = 1;
        int notificationTypeId = 1631026286;

        String response = mbiApiClient.sendMessageToShop(shopId, notificationTypeId,
                "<abo-info order-id=\"32828492\" order-sum=\"2619\" order-date=\"16.09.2021\" " +
                        "dbs-confirmation-time=\"16:16\" dbs-confirmation-datetime=\"16:16 16 сентября\">\n" +
                        "  <offers>\n" +
                        "    <offer title=\"Nokia 150 DS Red 2020 16GMNR01A02\" count=\"1\" />\n" +
                        "  </offers>\n" +
                        "</abo-info>");
        assertThat(response).contains("MessageId = ");
    }

    /**
     * Отправляем письмо с напоминанием о новом заказе ДБС магазину.
     * Проверяем, что нет падений при генерации сообщения.
     */
    @Test
    void sendNewOrderReminderMessageToDBSShop() {
        long shopId = 1;
        int notificationTypeId = 1563377944;

        String response = mbiApiClient.sendMessageToShop(shopId, notificationTypeId,
                "<abo-info order-id=\"111\" dbs-confirmation-time=\"19:00\" order-sum=\"5000\"/>");
        assertThat(response).contains("MessageId = ");
    }

    @Test
    @DisplayName("Посылаем общий темлейт о новом заказе ДБСу")
    void sendCommonNewOrderTemplateToDBS() {
        long shopId = 1;
        int notificationTypeId = 1612872409;

        String response = mbiApiClient.sendMessageToShop(
                shopId,
                notificationTypeId,
                StringTestUtil.getString(getClass(), "newOrderNotification.request.xml")
        );
        assertThat(response).contains("MessageId = ");
    }
}
