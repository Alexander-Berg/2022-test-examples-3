package ru.yandex.market.checkout.referee;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.structures.OrderWithConversations;
import ru.yandex.market.checkout.entity.structures.PagedOrderWithConversations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.referee.test.BaseTest.getConvTitle;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;

/**
 * @author kukabara
 */
public class ConversationJsonTest extends ConversationTest {

    @Override
    @BeforeEach
    public void init() {
        this.client = checkoutRefereeJsonClient;
    }

    @Test
    public void testSearchWithOrders() {
        long user = newUID();
        long orderId = newOrderId();

        Conversation conv = start(user, getConvTitle(), orderId);
        SearchTerms terms = SearchTerms.SearchTermsBuilder
                .byShopId(1L, conv.getShopId())
                .withOrderId(orderId)
                .withRgbs(Sets.newHashSet(Color.BLUE))
                .build();

        PagedOrderWithConversations pagedOrderWithConversations = client.searchWithOrders(terms);
        assertEquals((Integer) 1, pagedOrderWithConversations.getPager().getTotal());
        OrderWithConversations owc = pagedOrderWithConversations.getItems().iterator().next();
        assertEquals(conv.getOrder().getOrderId(), owc.getOrder().getId());
        assertEquals(1, owc.getConversations().size());
        assertEquals(conv, owc.getConversations().iterator().next());
    }
}
