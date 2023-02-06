package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.entity.structures.OrderWithConversations;
import ru.yandex.market.checkout.entity.structures.PagedOrderWithConversations;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

/**
 * @author kukabara
 */
public class OrderWithConversationsJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void testWrite() throws Exception {
        OrderWithConversations expectedObj = CheckoutRefereeHelper.getOrderWithConversations();
        String json = write(expectedObj);

        checkJson(json, "$.order.id", CheckoutRefereeHelper.ID);
        checkJson(json, "$.order.shopId", CheckoutRefereeHelper.SHOP_ID);
        checkJson(json, "$.order.shopOrderId", "12345");

        checkJson(json, "$.conversations.[0].id", CheckoutRefereeHelper.ID);
        checkJson(json, "$.conversations.[0].uid", CheckoutRefereeHelper.UID);
        checkJson(json, "$.conversations.[0].shopId", CheckoutRefereeHelper.SHOP_ID);
        checkJson(json, "$.conversations.[0].lastStatus", "OPEN");

        checkJson(json, "$.conversations.[1].id", CheckoutRefereeHelper.ID);
        InputStream is = new ByteArrayInputStream(json.getBytes());
        OrderWithConversations obj = read(OrderWithConversations.class, is);
    }

    @Test
    public void testWritePaged() throws Exception {
        PagedOrderWithConversations expectedObj = CheckoutRefereeHelper.getPagedOrderWithConversations();
        String json = write(expectedObj);

        checkPager(json);
        checkJson(json, "$.orders.[0].order.id", CheckoutRefereeHelper.ID);
        checkJson(json, "$.orders.[0].conversations.[0].id", CheckoutRefereeHelper.ID);
        checkJson(json, "$.orders.[0].conversations.[1].id", CheckoutRefereeHelper.ID);
    }

    private static void checkPager(String json) throws ParseException {
        checkJson(json, "$.pager.from", 1);
        checkJson(json, "$.pager.to", 2);
        checkJson(json, "$.pager.pageSize", 2);
        checkJson(json, "$.pager.page", 1);
    }

    @Test
    public void testRead() throws Exception {
        // TODO сейчас не читаем из java-клиента
        // InputStream is = new ByteArrayInputStream(json.getBytes());
        // OrderWithConversations obj = read(OrderWithConversations.class, is);
    }
}
