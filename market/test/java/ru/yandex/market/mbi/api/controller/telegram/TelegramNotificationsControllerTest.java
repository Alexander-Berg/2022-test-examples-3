package ru.yandex.market.mbi.api.controller.telegram;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.SupplierInfo;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.api.client.entity.telegram.NotificationMetaDto;
import ru.yandex.market.mbi.api.client.entity.telegram.NotificationThemeDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.doReturn;

/**
 * Тесты для {@link ru.yandex.market.mbi.api.controller.telegram.TelegramNotificationsController}
 */
class TelegramNotificationsControllerTest extends FunctionalTest {

    private static final String TG_BOT_ID = "IAmTgBot";
    private static final long TG_ID = 192837465;

    @Autowired
    private SupplierService supplierService;


    @BeforeEach
    public void init() {
        doReturn(new SupplierInfo(10, "0011"))
                .when(supplierService).getSupplier(10);
        doReturn(new SupplierInfo(20, "0022"))
                .when(supplierService).getSupplier(20);
        doReturn(new SupplierInfo(60, "0066"))
                .when(supplierService).getSupplier(60);
    }

    @Test
    @DisplayName("Успешная отписка telegram-аккаунта пользователя от уведомления заданного типа")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribe.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribe.after.csv"
    )
    void testPostTelegramUnsubscribe() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, null);
    }

    @Test
    @DisplayName("Успешная отписка telegram-аккаунта пользователя от уведомления заданного типа по заданному " +
            "поставщику")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeWithSupplier.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeWithSupplier.after.csv"
    )
    void testPostTelegramUnsubscribeWithSupplier() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
    }

    @Test
    @DisplayName("Успешная отписка telegram-аккаунта пользователя от уведомления заданного типа по заданному магазину")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeWithShop.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeWithShop.after.csv"
    )
    void testPostTelegramUnsubscribeWithShop() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
    }

    @Test
    @DisplayName("Повторная подписка telegram-аккаунта пользователя на уведомления заданного типа")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramResubscribe.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramResubscribe.after.csv"
    )
    void testPostTelegramResubscribe() {
        mbiApiClient.resubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, null);
    }

    @Test
    @DisplayName("Повторная подписка telegram-аккаунта пользователя на уведомления заданного типа по заданному " +
            "поставщику")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramResubscribeWithSupplier.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramResubscribeWithSupplier.after.csv"
    )
    void testPostTelegramResubscribeWithSupplier() {
        mbiApiClient.resubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
    }

    @Test
    @DisplayName("Повторная подписка telegram-аккаунта пользователя на уведомления заданного типа по заданному " +
            "магазину")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramResubscribeWithShop.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramResubscribeWithShop.after.csv"
    )
    void testPostTelegramResubscribeWithShop() {
        mbiApiClient.resubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 10L);
    }

    @Test
    @DisplayName("Успешная отписка от уведомления сохраняет историю")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeAudit.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeAudit.after.csv"
    )
    void testPostTelegramUnsubscribeHistory() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, null);
    }

    @Test
    @DisplayName("Успешная отписка от уведомления сохраняет историю по заданному пщставщику")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeAuditWithSupplier.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeAuditWithSupplier.after.csv"
    )
    void testPostTelegramUnsubscribeHistoryWithSupplier() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
    }

    @Test
    @DisplayName("Успешная отписка от уведомления сохраняет историю по заданному магазину")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeAuditWithShop.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramUnsubscribeAuditWithShop.after.csv"
    )
    void testPostTelegramUnsubscribeHistoryWithShop() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
    }

    @Test
    @DisplayName("Успешная переподписка на уведомления сохраняет историю")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramResubscribeAudit.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramResubscribeAudit.after.csv"
    )
    void testPostTelegramResubscribeHistory() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, null);
        mbiApiClient.resubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, null);
    }

    @Test
    @DisplayName("Успешная переподписка на уведомления сохраняет историю")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramResubscribeAuditWithSupplier.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramResubscribeAuditWithSupplier.after.csv"
    )
    void testPostTelegramResubscribeHistoryWithSupplier() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
        mbiApiClient.resubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
    }

    @Test
    @DisplayName("Успешная переподписка на уведомления сохраняет историю")
    @DbUnitDataSet(
            before = "TelegramNotificationsControllerTest.testPostTelegramResubscribeAuditWithShop.before.csv",
            after = "TelegramNotificationsControllerTest.testPostTelegramResubscribeAuditWithShop.after.csv"
    )
    void testPostTelegramResubscribeHistoryWithShop() {
        mbiApiClient.unsubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
        mbiApiClient.resubscribeTelegramNotification(TG_BOT_ID, TG_ID, 140, 20L);
    }

    @Test
    void testGetSubscriptionManagedThemes() {
        List<NotificationThemeDto> themes = mbiApiClient.getSubscriptionManagedThemes();
        boolean isQualityChecksContained = themes.stream().anyMatch(theme -> theme.getId() == 5);
        Assertions.assertFalse(isQualityChecksContained);
    }

    @Test
    @DisplayName("Получение информации о доступных для управления уведомлениях")
    @DbUnitDataSet(before = "TelegramNotificationsControllerTest.testGetTelegramNotifications.csv")
    void testGetTelegramNotifications() {
        Map<Long, NotificationMetaDto> notifications = mbiApiClient.getTelegramNotifications(100, 1, false, 50)
                .stream()
                .collect(Collectors.toMap(NotificationMetaDto::getId, Function.identity()));
        Assertions.assertFalse(notifications.isEmpty());
        NotificationMetaDto combined = notifications.get(1612872409L);
        Assertions.assertTrue(combined.isActive());

        notifications = mbiApiClient.getTelegramNotifications(110, 1, false, 50)
                .stream()
                .collect(Collectors.toMap(NotificationMetaDto::getId, Function.identity()));
        Assertions.assertFalse(notifications.isEmpty());
        NotificationMetaDto notActive = notifications.get(1612872409L);
        Assertions.assertFalse(notActive.isActive());

        notifications = mbiApiClient.getTelegramNotifications(120, 1, false, 50)
                .stream()
                .collect(Collectors.toMap(NotificationMetaDto::getId, Function.identity()));
        Assertions.assertFalse(notifications.isEmpty());
        NotificationMetaDto notInSubscriptionsTable = notifications.get(1612872409L);
        Assertions.assertTrue(notInSubscriptionsTable.isActive());
    }

    @Test
    @DbUnitDataSet(before = "TelegramNotificationsControllerTest.testGetTelegramNotifications.csv")
    @DisplayName("Получение информации о доступных для управления уведомлениях с разбивкой по партнеру")
    void testGetTelegramNotificationsWithDivideByPartner() {
        Map<Long, List<NotificationMetaDto>> notifications =
                mbiApiClient.getTelegramNotifications(100, 1, true, 50)
                        .stream()
                        .collect(groupingBy(NotificationMetaDto::getId, toList()));
        Assertions.assertFalse(notifications.isEmpty());
        Assertions.assertEquals(4, notifications.get(1612872409L).size());
        Map<Long, NotificationMetaDto> partnerNotifications = notifications.get(1612872409L).stream()
                .filter(n -> n.getPartnerId() != null)
                .collect(Collectors.toMap(NotificationMetaDto::getPartnerId, Function.identity()));
        NotificationMetaDto id10 = partnerNotifications.get(10L);
        Assertions.assertTrue(id10.isActive());

        NotificationMetaDto id60 = partnerNotifications.get(60L);
        Assertions.assertFalse(id60.isActive());

        notifications = mbiApiClient.getTelegramNotifications(110, 1, true, 50)
                .stream()
                .collect(groupingBy(NotificationMetaDto::getId, toList()));
        Assertions.assertFalse(notifications.isEmpty());
        //1 дропшип-партнер, нет сплита
        Assertions.assertEquals(1, notifications.get(1612872409L).size());

        NotificationMetaDto notActive = notifications.get(1612872409L).get(0);
        Assertions.assertFalse(notActive.isActive());

        notifications = mbiApiClient.getTelegramNotifications(120, 1, true, 50)
                .stream()
                .collect(groupingBy(NotificationMetaDto::getId, toList()));
        Assertions.assertFalse(notifications.isEmpty());
        Assertions.assertEquals(3, notifications.get(1612872409L).size());
        partnerNotifications = notifications.get(1612872409L).stream()
                .filter(n -> n.getPartnerId() != null)
                .collect(Collectors.toMap(NotificationMetaDto::getPartnerId, Function.identity()));
        NotificationMetaDto id30 = partnerNotifications.get(30L);
        Assertions.assertTrue(id30.isActive());

        NotificationMetaDto id70 = partnerNotifications.get(70L);
        Assertions.assertTrue(id70.isActive());
    }
}
