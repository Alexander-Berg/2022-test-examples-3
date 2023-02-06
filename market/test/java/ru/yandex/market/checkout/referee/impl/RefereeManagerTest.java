package ru.yandex.market.checkout.referee.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.entity.ArbitrageCheckType;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.PrivacyMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
public class RefereeManagerTest {
    private Conversation conv;
    private Message message;

    @BeforeEach
    public void setUp() throws Exception {
        conv = mock(Conversation.class);
        when(conv.isVisibleForShop()).thenReturn(true);
        when(conv.getCheckType()).thenReturn(ArbitrageCheckType.MANUAL);

        message = mock(Message.class);
    }

    @Test
    public void testCommon() {
        when(conv.getLastStatus()).thenReturn(ConversationStatus.OPEN);
        when(conv.getCheckType()).thenReturn(ArbitrageCheckType.MANUAL);
        when(message.getConvStatusBefore()).thenReturn(null);
        when(message.getPrivacyMode()).thenReturn(null);

        checkVisibility("Начало обычной переписки", true);
        when(message.getConvStatusBefore()).thenReturn(ConversationStatus.OPEN);

        checkVisibility("Продолжение переписки", true);

        when(message.getPrivacyMode()).thenReturn(PrivacyMode.PM_TO_USER);
        checkVisibility("Даже если пишут личные сообщения, то видимость не меняем", true);
    }

    @Test
    public void testAutoArbitrage() {
        startAutoArbitrage();

        when(message.getConvStatusBefore()).thenReturn(ConversationStatus.ARBITRAGE);
        when(message.getConvStatusAfter()).thenReturn(ConversationStatus.CLOSED);
        when(conv.getLastStatus()).thenReturn(ConversationStatus.CLOSED);
        checkVisibility("Вынесли вердикт", true);
    }

    private void startAutoArbitrage() {
        when(conv.getLastStatus()).thenReturn(ConversationStatus.ESCALATED);
        when(conv.getCheckType()).thenReturn(ArbitrageCheckType.AUTO);
        when(message.getConvStatusBefore()).thenReturn(null);
        when(message.getPrivacyMode()).thenReturn(null);

        checkVisibility("Начало автоарбитража", false);

        when(message.getConvStatusBefore()).thenReturn(ConversationStatus.ESCALATED);
        when(message.getPrivacyMode()).thenReturn(PrivacyMode.PM_TO_USER);
        checkVisibility("Пользователь что-то пишет", false);

        when(message.getConvStatusAfter()).thenReturn(ConversationStatus.ARBITRAGE);
        when(conv.getLastStatus()).thenReturn(ConversationStatus.ARBITRAGE);
        checkVisibility("Автоарбитраж взят в работу", false);
    }

    @Test
    public void testAutoArbitrageToManual() {
        startAutoArbitrage();

        when(message.getConvStatusBefore()).thenReturn(ConversationStatus.ARBITRAGE);
        when(message.getConvStatusAfter()).thenReturn(ConversationStatus.ARBITRAGE);
        when(conv.getCheckType()).thenReturn(ArbitrageCheckType.MANUAL);

        checkVisibility("Перевели арбитраж в ручной режим", true);

        when(message.getConvStatusBefore()).thenReturn(ConversationStatus.ARBITRAGE);
        when(message.getPrivacyMode()).thenReturn(PrivacyMode.PM_TO_USER);
        checkVisibility("Пользователь что-то пишет", true);
    }

    @Test
    public void testChatBot() {
        when(conv.getLastStatus()).thenReturn(ConversationStatus.OPEN);
        when(conv.getCheckType()).thenReturn(ArbitrageCheckType.MANUAL);
        when(message.getConvStatusBefore()).thenReturn(null);
        when(message.getPrivacyMode()).thenReturn(PrivacyMode.PM_TO_USER);

        checkVisibility("Начало общения с чат-ботом", false);

        when(message.getConvStatusBefore()).thenReturn(ConversationStatus.OPEN);
        checkVisibility("Продолжение переписки с чат-ботом", false);

        when(message.getPrivacyMode()).thenReturn(null);
        checkVisibility("Первое публичное сообщение", true);
    }

    private void checkVisibility(String comment, boolean expectedVisibility) {
        assertEquals(expectedVisibility, RefereeManager.resolveVisibilityForShop(conv, message), comment);
        when(conv.isVisibleForShop()).thenReturn(expectedVisibility);
    }
}
