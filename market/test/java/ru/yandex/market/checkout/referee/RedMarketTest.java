package ru.yandex.market.checkout.referee;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.Page;
import ru.yandex.market.checkout.entity.ArbitrageCheckType;
import ru.yandex.market.checkout.entity.ClaimType;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.IssueType;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.MessageRequest;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.NoteType;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeErrorCode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.impl.ReplyCode;
import ru.yandex.market.checkout.referee.test.BaseConversationTest;
import ru.yandex.market.checkout.referee.test.State;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.referee.test.BaseTest.getConvTitle;
import static ru.yandex.market.checkout.referee.test.BaseTest.getText;
import static ru.yandex.market.checkout.referee.test.BaseTest.newRedOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newRedOrderIdWithoutTrack;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;

/**
 * Сценарии:
 * 1. Переписка от пользователя
 * 2. Переписка от магазина
 * 3. Претензия -> арбитраж по "длинному флоу" -> решение -> апелляция -> решение
 * 4. Претензия -> арбитраж по "длинному флоу" -> закрыт -> претензия
 * 4. Арбитраж по "короткому флоу"
 * 6. Перевести "короткий" флоу в "длинный"
 * 7. Чат-бот
 *
 * @author kukabara
 */
@Disabled("RED is DEAD")
public class RedMarketTest extends BaseConversationTest {
    private static final State RED_OPEN = new State.StateBuilder()
            .withStatus(ConversationStatus.OPEN)
            .withCanRaiseIssue(true)
            .build();
    private static final State RED_ISSUE = new State.StateBuilder()
            .withStatus(ConversationStatus.ISSUE)
            .withCanEscalateAfterNotNull(true)
            .build();

    private static final State RED_ISSUE_CAN_ESCALATE = new State.StateBuilder()
            .withStatus(ConversationStatus.ISSUE)
            .withCanEscalate(true)
            .build();

    private static final State RED_CLOSED = new State.StateBuilder()
            .withStatus(ConversationStatus.CLOSED)
            .withCanRaiseIssue(false)
            .build();

    @Override
    @BeforeEach
    public void init() {
        this.client = checkoutRefereeJsonClient;
    }

    private static ConversationObject genOrderItem() {
        return ConversationObject.fromOrderItem(newRedOrderId(), newUID());
    }

    /**
     * Пользователь общается с чатботом.
     * Бекэнд КМ будет добавлять сообщения от чат-бота (от роли {@link RefereeRole#SYSTEM}, а выбранное пользователем
     * от роли {@link RefereeRole#USER}. Эти сообщения не должны быть видны магазину.
     */
    @Test
    public void testChatBot() {
        long user = newUID();
        ConversationObject item = genOrderItem();

        String userCode = "USER_ANSWER_1";
        Conversation conv = client.startConversation(
                new ConversationRequest.Builder(user, RefereeRole.USER, item, "У меня проблема")
                        .withCode(userCode)
                        .withPrivacyMode(PrivacyMode.PM_TO_USER)
                        .withTitle("title").build());
        assertNotNull(conv);
        Message userMessage1 = conv.getUpdatedMessages().get(0);
        assertEquals(userCode, userMessage1.getCode());
        assertEquals(PrivacyMode.PM_TO_USER, userMessage1.getPrivacyMode());

        long systemUid = 1L;
        String botCode = "BOT_ANSWER_1";
        Message botMessage1 = client.sendMessage(new MessageRequest.Builder(conv.getId(), systemUid, RefereeRole.SYSTEM)
                .withText("Привет, мы решим любую проблему с заказом. Пожалуйста, ответь на несколько простых вопросов.\n" +
                        "Ты получил заказ?")
                .withCode(botCode)
                .withPrivacy(PrivacyMode.PM_TO_USER).build());
        assertEquals(botCode, botMessage1.getCode());
        assertEquals(PrivacyMode.PM_TO_USER, botMessage1.getPrivacyMode());

        String userCode2 = "USER_ANSWER_2";
        Message userMessage2 = client.sendMessage(new MessageRequest.Builder(conv.getId(), user, RefereeRole.USER)
                .withText("Ещё не получил.")
                .withCode(userCode2)
                .withPrivacy(PrivacyMode.PM_TO_USER).build());
        assertEquals(userCode2, userMessage2.getCode());
        assertEquals(PrivacyMode.PM_TO_USER, userMessage2.getPrivacyMode());

        // Магазин не должен получать уведомления о разговорах пользователя с ботом
        testShopNotes(conv.getId(), 0);
        Page<Message> messages = client.getMessages(conv.getId(), 1L, RefereeRole.SHOP, conv.getShopId(), null, null);
        assertTrue(messages.getItems().isEmpty());

        testSearchByShop(conv, true);

        String text = "Real message";
        Conversation issue = client.raiseIssue(conv.getId(), user, RefereeRole.USER,
                EnumSet.of(IssueType.DELIVERY_DELAY), ClaimType.REFUND,
                null, text);
        assertEquals(null, issue.getUpdatedMessages().get(0).getPrivacyMode());
        testSearchByShop(conv, false);
    }

    @Test
    public void testConvUser() {
        long user = newUID();
        ConversationObject item1 = genOrderItem();
        Conversation convItem1 = restart(user, newUID(), getConvTitle(),
                item1, getText(), null, RED_OPEN);
        testCanNotStart(convItem1);
        assertNull(convItem1.getLastLabel());
        List<Note> shopNotes = testShopNotes(convItem1.getId(), 1);
        shopNotes.forEach(note -> {
            assertEquals(item1.getOrderId(), Long.valueOf(note.getOrderId()));
            assertEquals(item1, note.getObject());
        });
        testSearchByObject(convItem1);

        ConversationObject item2 = ConversationObject.fromOrderItem(item1.getOrderId(), newUID());
        Conversation convItem2 = restart(user, newUID(), getConvTitle(),
                item2, getText(), null, RED_OPEN);
        assertNotEquals(convItem2.getId(), convItem1.getId(), "Можем создать разные переписки на разные item-ы в заказе");
    }

    private void testSearchByObject(Conversation conv) {
        SearchTerms searchTerms = SearchTerms.SearchTermsBuilder
                .byUid(conv.getUid())
                .withObject(conv.getObject())
                .build();

        Page<Conversation> found = client.searchConversations(searchTerms);
        assertNotNull(found);
        assertEquals(1, found.getItems().size());
    }

    @Test
    public void testConvShop() {
        Conversation conv1 = client.startConversation(new ConversationRequest.Builder(
                newUID(), RefereeRole.SHOP, ConversationObject.fromOrderItem(newRedOrderId(), newUID()), getText())
                .withShopId(newUID())
                .withTitle(getConvTitle()).build());

        assertEquals(ConversationStatus.OPEN, conv1.getLastStatus());
        assertTrue(conv1.isCanRaiseIssue());
        assertFalse(conv1.isCanEscalate());
        assertNull(conv1.getLastLabel());

        assertEquals(1, conv1.getUpdatedMessages().size(), "Должно быть 1 сообщение от магазина");

        long user = conv1.getOrder().getUid();
        Conversation conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertTrue(conv2.isCanRaiseIssue());
    }

    /**
     * -> ISSUE
     */
    @ParameterizedTest
    @CsvSource({"10001, 222", ","})
    public void testIssue(Long orderId, Long itemId) {
        ConversationRequest request = initRequest(orderId, itemId);

        Conversation conv = redIssue(request, RED_ISSUE);
        testCanNotStart(conv);

        try {
            client.raiseIssue(request);
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
        testCanNotRaiseIssue(request);

        testMessagesCount(conv.getId(), request.getUid(), RefereeRole.USER, 1);
        testShopNotes(conv.getId(), 0);
    }

    private ConversationRequest initRequest(Long orderId, Long itemId) {
        long user = newUID();

        ConversationObject conversationObject;
        if (orderId != null && itemId != null) {
            conversationObject = ConversationObject.fromOrderItem(orderId, itemId);
        } else {
            conversationObject = ConversationObject.fromSku("feed group id hash",
                    List.of(new FeedOfferId("offerId", 42L)));
        }

        return redIssueRequest(user, conversationObject);
    }

    private List<Note> testShopNotes(long convId, int cnt) {
        List<Note> shopNotes;
        shopNotes = client.getNotifications(
                EnumSet.of(NoteType.NOTIFY_SHOP), null, EnumSet.of(Color.RED)
        ).getNotes().stream()
                .filter(n -> n.getConversationId() == convId)
                .collect(Collectors.toList());
        assertEquals(cnt, shopNotes.size(),
                shopNotes.stream()
                        .map(n -> n.getConvStatusBefore() + " -> " + n.getConvStatusAfter() + " by " + n.getAuthorRole())
                        .collect(Collectors.joining("\n")));
        return shopNotes;
    }

    private void testSearchByShop(Conversation conv, boolean hidden) {
        SearchTerms searchTerms = SearchTerms.SearchTermsBuilder
                .byShopId(1L, conv.getShopId())
                .withRgbs(Sets.newHashSet(Color.RED))
                .withObject(conv.getObject())
                .build();

        Page<Conversation> found = client.searchConversations(searchTerms);
        assertNotNull(found);
        assertEquals(hidden, found.getItems().isEmpty());
    }

    private static void testReply(Conversation conv, ReplyCode code) {
        assertEquals(2, conv.getUpdatedMessages().size(), "Должно быть 2 сообщения: от пользователя и автоответ");
        assertTrue(conv.getUpdatedMessages().stream()
                        .anyMatch(m -> m.getAuthorRole() == RefereeRole.SYSTEM &&
                                m.getText().equals(code.name())),
                "Не найдено " + code
        );
    }

    /**
     * -> ISSUE -> ESCALATED -> ARBITRAGE -> CLOSED -> [ESCALATED -> ARBITRAGE -> CLOSED]
     */
    @Test
    @Disabled("not formulated yet")
    public void testIssueEscalateArbitrageResolveTwice() {
        long user = newUID();

        Conversation conv = redIssue(user, newRedOrderId(), newUID(), RED_ISSUE);
        Conversation escalated = escalate(conv);
        Conversation arbitrage = startArbitrage(escalated);
        Conversation resolved = resolveArbiter(arbitrage);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 5);
        testShopNotes(conv.getId(), 3);

        testCanNotRaiseIssue(resolved);

        Conversation reopen = reopen(resolved);
        reopen = startArbitrageSecondTime(reopen);
        Conversation resolved2 = resolveArbiter2(reopen);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 8);
        testShopNotes(conv.getId(), 5);

        testCanNotRaiseIssue(resolved2);
        testCanNotAppeal(resolved2);
    }

    /**
     * ISSUE -> CLOSED
     */
    @Test
    void testCloseIssue() {
        long user = newUID();
        long orderId = newRedOrderId();
        long itemId = newUID();

        Conversation conv = redIssue(user, orderId, itemId, RED_ISSUE);
        conv = close(conv, RED_CLOSED);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 2);
    }

    /**
     * ISSUE -> CLOSED
     * ESCALATED -> CLOSED
     * ARBITRAGE -> CLOSED
     */
    @Test
    @Disabled("ESCALATED -> CLOSED & ARBITRAGE -> CLOSED not formulated yet")
    public void testClose() {
        long user = newUID();
        long orderId = newRedOrderId();
        long itemId = newUID();

        Conversation conv = redIssue(user, orderId, itemId, RED_ISSUE);
        conv = close(conv, RED_CLOSED);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 2);

        conv = redIssue(user, orderId, itemId, RED_ISSUE);
        conv = escalate(conv);
        conv = close(conv, RED_CLOSED);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 6);

        conv = redIssue(user, orderId, itemId, RED_ISSUE_CAN_ESCALATE);
        assertTrue(conv.isCanEscalate());
        conv = escalate(conv);
        conv = startArbitrage(conv);
        conv = close(conv, RED_CLOSED);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 11);

        conv = redIssue(user, orderId, itemId, RED_ISSUE_CAN_ESCALATE);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 12);
    }

    /**
     * Создаём арбитраж по короткому флоу
     * Проверяем:
     * - автоответы
     * - уведомления магазину
     * - видимость для магазина
     */
    @Test
    @Disabled("not formulated yet")
    public void testAutoArbitrage() {
        Conversation conv = createAutoArbitrage();
        close(conv, RED_CLOSED);

        conv = client.redEscalate(conv.getUid(), conv.getObject().getOrderId(), conv.getObject().getItemId());
        testMessagesCount(conv.getId(), conv.getUid(), RefereeRole.USER, 5);

        conv = startArbitrage(conv);
        testSearchByShop(conv, true);

        conv = resolveArbiter(conv);
        testSearchByShop(conv, false);
    }

    private Conversation createAutoArbitrage() {
        long user = newUID();
        Conversation conv = client.redEscalate(user, newRedOrderIdWithoutTrack(), newUID());
        testReply(conv, ReplyCode.RED_NO_DELIVERY);
        testMessagesCount(conv.getId(), user, RefereeRole.USER, 2);

        assertEquals(ConversationStatus.ESCALATED, conv.getLastStatus());
        assertEquals(ArbitrageCheckType.AUTO, conv.getCheckType());

        testShopNotes(conv.getId(), 0);
        testSearchByShop(conv, true);
        return conv;
    }

    @Test
    public void testAutoArbitrageTrackExists() {
        Conversation conv = client.redEscalate(newUID(), newRedOrderId(), newUID());
        testReply(conv, ReplyCode.RED_NO_DELIVERY);
        testShopNotes(conv.getId(), 1);
        testSearchByShop(conv, false);

        assertEquals(ConversationStatus.ISSUE, conv.getLastStatus());
        assertEquals(ArbitrageCheckType.MANUAL, conv.getCheckType());
    }

    /**
     * При изменении способа проверки арбитража
     * - переписка становится видна магазину
     * - отправляем уведомление
     */
    @Test
    public void testUpdateCheckType() {
        Conversation conv = createAutoArbitrage();

        Conversation manualConv = client.updateCheckType(conv.getId(), 1L, RefereeRole.ARBITER,
                ArbitrageCheckType.MANUAL);
        assertEquals(ArbitrageCheckType.MANUAL, manualConv.getCheckType());
        testShopNotes(conv.getId(), 1);
        testSearchByShop(conv, false);

        Conversation saved = client.getConversation(conv.getId(), 1L, RefereeRole.ARBITER, null);
        assertEquals(saved, manualConv);
    }
}
