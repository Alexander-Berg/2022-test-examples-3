package ru.yandex.market.mbi.api.controller.telegram;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.telegram.TelegramAccountDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;
import ru.yandex.market.notification.telegram.bot.model.address.TelegramIdAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link TelegramBotController}.
 */
class TelegramBotControllerTest extends FunctionalTest {

    private static final String BOT_ID = "IAmRobot";
    private static final long TG_ID = 192837465;
    private static final long ANOTHER_TG_ID = 12345678;
    private static final long SHOP_ID = 1;
    private static final long ORDER_ID = 10;
    private static final long CAMPAIGN_ID = 100;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private PartnerBotRestClient partnerBotRestClient;

    @Test
    @DisplayName("Успешная генерация приглашения для привязки telegram-аккаунта пользователя")
    @DbUnitDataSet(
            after = "TelegramBotControllerTest.testPostTelegramSendInvitations.after.csv"
    )
    void testPostTelegramSendInvitations() {
        TelegramAccountDto account = createTelegramAccount();
        mbiApiClient.sendTelegramInvitation(account);
    }

    @Test
    @DisplayName("Успешная генерация приглашения для привязки telegram-аккаунта пользователя (хэш уже есть в БД)")
    @DbUnitDataSet(
            before = "TelegramBotControllerTest.testPostTelegramSendInvitationsHashExists.before.csv",
            after = "TelegramBotControllerTest.testPostTelegramSendInvitationsHashExists.after.csv"
    )
    void testPostTelegramSendInvitationsHashExists() {
        TelegramAccountDto account = createTelegramAccount();
        mbiApiClient.sendTelegramInvitation(account);
    }

    @Test
    @DisplayName("Проверка существования телеграм аккаунта с указанием botId")
    @DbUnitDataSet(
            before = "TelegramBotControllerTest.testGetAccountExists.before.csv"
    )
    void testGetAccountExists() {
        assertTrue(mbiApiClient.telegramAccountExists(BOT_ID, 12345678));
        assertFalse(mbiApiClient.telegramAccountExists(BOT_ID, 20000));
    }

    @Test
    @DisplayName("Проверка существования телеграм аккаунта без указания botId")
    @DbUnitDataSet(
            before = "TelegramBotControllerTest.testGetAccountExists.before.csv"
    )
    void testGetAccountExistsWithoutBotId() {
        assertTrue(mbiApiClient.telegramAccountExists(null, 12345678));
        assertFalse(mbiApiClient.telegramAccountExists(null, 20000));
    }

    @Test
    @DisplayName("Активация телеграм аккаунта с указанием botId")
    @DbUnitDataSet(
            before = "TelegramBotControllerTest.testPatchActivateAccount.before.csv",
            after = "TelegramBotControllerTest.testPatchActivateAccount.after.csv"
    )
    void testPatchActivateAccount() {
        mbiApiClient.activateTelegramAccount(BOT_ID, ANOTHER_TG_ID);
    }

    @Test
    @DisplayName("Активация телеграм аккаунта без указания botId")
    @DbUnitDataSet(
            before = "TelegramBotControllerTest.testPatchActivateAccount.before.csv",
            after = "TelegramBotControllerTest.testPatchActivateAccount.after.csv"
    )
    void testPatchActivateAccountWithoutBotId() {
        mbiApiClient.activateTelegramAccount(null, ANOTHER_TG_ID);
    }

    @Test
    @DisplayName("Деактивация телеграм аккаунта с указанием botId")
    @DbUnitDataSet(
            before = "TelegramBotControllerTest.testPatchDeactivateAccount.before.csv",
            after = "TelegramBotControllerTest.testPatchDeactivateAccount.after.csv"
    )
    void testPatchDeactivateAccount() {
        mbiApiClient.deactivateTelegramAccount(BOT_ID, ANOTHER_TG_ID);
    }

    @Test
    @DisplayName("Деактивация телеграм аккаунта без указания botId")
    @DbUnitDataSet(
            before = "TelegramBotControllerTest.testPatchDeactivateAccount.before.csv",
            after = "TelegramBotControllerTest.testPatchDeactivateAccount.after.csv"
    )
    void testPatchDeactivateAccountWithoutBotId() {
        mbiApiClient.deactivateTelegramAccount(null, ANOTHER_TG_ID);
    }

    @Test
    @DbUnitDataSet(before = "TelegramBotControllerTest.testAcceptOrder.before.csv")
    void testAcceptOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        Mockito.when(checkouterAPI.getOrder(
                Mockito.any(),
                Mockito.any()
        )).thenReturn(order);
        mbiApiClient.acceptOrderViaTelegram(BOT_ID, TG_ID, ORDER_ID, CAMPAIGN_ID);
        Mockito.verify(checkouterAPI).updateOrderStatus(
                Mockito.eq(ORDER_ID), Mockito.eq(ClientRole.SHOP),
                Mockito.eq(SHOP_ID), Mockito.eq(SHOP_ID),
                Mockito.eq(OrderStatus.PROCESSING), Mockito.eq(OrderSubstatus.STARTED)
        );
        Mockito.verify(partnerBotRestClient).sendMessage(
                Mockito.eq(Collections.singletonList(TelegramIdAddress.create(BOT_ID, TG_ID))),
                Mockito.any()
        );
    }

    @Test
    @DbUnitDataSet(before = "TelegramBotControllerTest.testNoAccessedCampaignsFoundAcceptOrder.before.csv")
    void testNoAccessedCampaignsAcceptOrder() {
        mbiApiClient.acceptOrderViaTelegram(BOT_ID, TG_ID, ORDER_ID, CAMPAIGN_ID);
        Mockito.verify(checkouterAPI, Mockito.never()).updateOrderStatus(
                Mockito.anyLong(), Mockito.any(),
                Mockito.anyLong(), Mockito.anyLong(),
                Mockito.any(), Mockito.any()
        );
        Mockito.verify(partnerBotRestClient).sendMessage(
                Mockito.eq(Collections.singletonList(TelegramIdAddress.create(BOT_ID, TG_ID))),
                Mockito.any()
        );
    }

    @Test
    @DbUnitDataSet(before = "TelegramBotControllerTest.testAcceptOrder.before.csv")
    void testCheckouterDidNotAllowAcceptOrder() {
        Mockito.when(checkouterAPI.updateOrderStatus(
                Mockito.eq(ORDER_ID), Mockito.eq(ClientRole.SHOP),
                Mockito.eq(SHOP_ID), Mockito.eq(SHOP_ID),
                Mockito.eq(OrderStatus.PROCESSING), Mockito.eq(OrderSubstatus.STARTED)
        )).thenThrow(
                new OrderStatusNotAllowedException(
                        OrderStatusNotAllowedException.NOT_ALLOWED_CODE,
                        "not allowed",
                        403)
        );
        Order order = new Order();
        order.setStatus(OrderStatus.RESERVED);
        Mockito.when(checkouterAPI.getOrder(
                Mockito.any(),
                Mockito.any()
        )).thenReturn(order);

        mbiApiClient.acceptOrderViaTelegram(BOT_ID, TG_ID, ORDER_ID, CAMPAIGN_ID);
        Mockito.verify(partnerBotRestClient).sendMessage(
                Mockito.eq(Collections.singletonList(TelegramIdAddress.create(BOT_ID, TG_ID))),
                Mockito.any()
        );
    }

    @Test
    @DbUnitDataSet(before = "TelegramBotControllerTest.testAcceptOrder.before.csv")
    void testOrderIsAlreadyAccepted() {
        Mockito.when(checkouterAPI.updateOrderStatus(
                Mockito.eq(ORDER_ID), Mockito.eq(ClientRole.SHOP),
                Mockito.eq(SHOP_ID), Mockito.eq(SHOP_ID),
                Mockito.eq(OrderStatus.PROCESSING), Mockito.eq(OrderSubstatus.STARTED)
        )).thenThrow(
                new OrderStatusNotAllowedException(
                        OrderStatusNotAllowedException.NOT_ALLOWED_CODE,
                        "not allowed",
                        400,
                        OrderStatus.PROCESSING,
                        OrderSubstatus.STARTED
                )
        );
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);
        Mockito.when(checkouterAPI.getOrder(
                Mockito.any(),
                Mockito.any()
        )).thenReturn(order);

        mbiApiClient.acceptOrderViaTelegram(BOT_ID, TG_ID, ORDER_ID, CAMPAIGN_ID);
        Mockito.verify(partnerBotRestClient).sendMessage(
                Mockito.eq(Collections.singletonList(TelegramIdAddress.create(BOT_ID, TG_ID))),
                Mockito.any()
        );
    }

    private TelegramAccountDto createTelegramAccount() {
        TelegramAccountDto account = new TelegramAccountDto();
        account.setTgId(TG_ID);
        account.setBotId(BOT_ID);
        account.setFirstName("Petr");
        account.setUsername("petya");
        account.setPhotoUrl("https://t.me/i/userpic/320/somekindofpicture.jpg");
        return account;
    }

}
