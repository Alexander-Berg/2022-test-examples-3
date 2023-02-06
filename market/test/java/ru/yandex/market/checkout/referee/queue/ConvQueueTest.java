package ru.yandex.market.checkout.referee.queue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.MessageRequest;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.impl.CheckoutRefereeService;
import ru.yandex.market.checkout.referee.impl.PublishConvService;
import ru.yandex.market.checkout.referee.test.BaseConversationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.referee.test.BaseTest.getConvTitle;
import static ru.yandex.market.checkout.referee.test.BaseTest.getText;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;

/**
 * @author kukabara
 */
public class ConvQueueTest extends BaseConversationTest {
    private static final long SHOP_ID = newUID();

    @Autowired
    private PublishConvService publishConvService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Override
    @BeforeEach
    public void init() {
        this.client = checkoutRefereeJsonClient;
    }

    @AfterEach
    public void tearDown() throws Exception {
        pgJdbcTemplate.update("delete from a_attachment");
        pgJdbcTemplate.update("update a_attachment_group set message_id = null");
        pgJdbcTemplate.update("delete from a_message");
        pgJdbcTemplate.update("delete from a_attachment_group");
        pgJdbcTemplate.update("delete from a_conversation");
    }

    @Test
    public void testPublishJob() {
        int count = 10;
        assertEquals(count, generateConv(count).size());

        checkUnpublishedExists(10);
    }

    /**
     * Сбрасываем флаг публикации при обновлении. См. {@link CheckoutRefereeService#updateConversation}.
     * Изменение флагов прочитанности, архивации игнорируем.
     */
    @Test
    public void testUnpublishWhenUpdate() {
        Conversation conv = generateConv(1).get(0);
        checkUnpublishedExists();

        sendMessage(conv);
        checkUnpublishedExists();
    }

    private void sendMessage(Conversation conv) {
        client.sendMessage(new MessageRequest.Builder(conv.getId(), 1L, RefereeRole.SHOP)
                .withShopId(conv.getShopId())
                .withText("text").build());
    }

    @Test
    public void testNoConvUpdatesTillPublish() throws Exception {
        Conversation conv = generateConv(1).get(0);

        assertEquals(0, client.conversationUpdates(conv.getShopId(), null, null, null).size());

        publishConvService.publishConversationUpdates();
        assertEquals(1, client.conversationUpdates(conv.getShopId(), null, null, null).size());
    }

    /**
     * Пусть обновления идут в следующем порядке:
     * conv1
     * conv2
     * conv3
     * conv1
     * <p>
     * События будут опубликованы в следующем порядке:
     * conv, published_id
     * -----------------
     * conv1, 3
     * conv2, 1
     * conv3, 2
     */
    @Test
    public void testSeveralConvUpdates() throws Exception {
        // generate updates & publish
        List<Conversation> convs = generateConv(3);
        assertEquals(3, publishConvService.publishConversationUpdates().size());

        sendMessage(convs.get(0));
        assertEquals(1, publishConvService.publishConversationUpdates().size());

        // check updates order
        List<Conversation> updates = client.conversationUpdates(SHOP_ID, null, null, null);
        assertEquals(3, updates.size());
        assertEquals(convs.get(1).getId(), updates.get(0).getId());
        assertEquals(convs.get(2).getId(), updates.get(1).getId());
        assertEquals(convs.get(0).getId(), updates.get(2).getId());

        sendMessage(convs.get(1));
        publishConvService.publishConversationUpdates();
        updates = client.conversationUpdates(SHOP_ID, null, null, null);
        assertEquals(convs.get(2).getId(), updates.get(0).getId());
        assertEquals(convs.get(0).getId(), updates.get(1).getId());
        assertEquals(convs.get(1).getId(), updates.get(2).getId());
    }

    /**
     * conv, published_id, published_ts
     * -------------------------------
     * conv1, 1, ts1
     * conv2, 2, ts2
     * conv3, 3, ts2
     * <p>
     * При публикации published_ts выставляется в now() - время начала транзакции.
     * Т.к. тест выполняется в одной транзакции, а мы хотим получить разные published_ts используем Propagation.NEVER.
     */
    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void testPublishedTsAndId() throws Exception {
        // generate updates
        generateAndPublish(1);
        TimeUnit.MILLISECONDS.sleep(1L);
        generateAndPublish(2);

        // check updates order
        List<Conversation> updates = client.conversationUpdates(SHOP_ID, null, null, null);
        assertEquals(3, updates.size());

        // filter by published_ts
        assertNotEquals(updates.get(0).getPublishedTs(), updates.get(1).getPublishedTs());
        assertEquals(2, client.conversationUpdates(SHOP_ID, updates.get(1).getPublishedTs(), null, null).size());

        // filter by published_ts + published_id
        assertEquals(updates.get(1).getPublishedTs(), updates.get(2).getPublishedTs());
        assertEquals(1, client.conversationUpdates(SHOP_ID,
                updates.get(1).getPublishedTs(), updates.get(1).getPublishedId(), null).size());
    }

    private void checkUnpublishedExists() {
        checkUnpublishedExists(1);
    }

    private void checkUnpublishedExists(int count) {
        assertEquals(count, publishConvService.publishConversationUpdates().size());
        assertEquals(0, publishConvService.publishConversationUpdates().size());
    }

    private List<Conversation> generateConv(int count) {
        return IntStream.range(0, count)
                .mapToObj((i) -> client.startConversation(
                        new ConversationRequest.Builder(
                                newUID(), RefereeRole.SHOP, ConversationObject.fromOrder(newOrderId()), getText())
                                .withShopId(SHOP_ID)
                                .withTitle(getConvTitle()).build()))
                .collect(Collectors.toList());
    }

    private void generateAndPublish(int count) {
        List<Conversation> convs = generateConv(count);
        List<Long> publishedConvIds = publishConvService.publishConversationUpdates()
                .stream().map(PublishConvService.PublishConversation::getConversationId).collect(Collectors.toList());
        assertTrue(publishedConvIds.size() >= count);
        assertTrue(convs.stream().allMatch(c -> publishedConvIds.contains(c.getId())));
    }
}
