package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.Label;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.entity.structures.PagedConversations;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class ConversationJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void testConversationRequest() throws Exception {
        Random rnd = new Random();
        ConversationRequest request = new ConversationRequest.Builder(
                rnd.nextLong(), RefereeRole.ARBITER, ConversationObject.fromOrder(rnd.nextLong()), "text")
                .withTitle("title")
                .withLabel(Label.PROBLEM)
                .withAuthorName("name")
                .withUserEmail("email!")
                .withRgb(Color.WHITE)
                .build();

        String json = write(request);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        ConversationRequest obj = read(ConversationRequest.class, is);

        assertEquals(request.getShopId(), obj.getShopId());
        assertEquals(request.getUid(), obj.getUid());
        assertEquals(request.getObject(), obj.getObject());
        assertEquals(request.getRole(), obj.getRole());
        assertEquals(request.getText(), obj.getText());
        assertEquals(request.getTitle(), obj.getTitle());
        assertEquals(request.getAuthorName(), obj.getAuthorName());
        assertEquals(request.getLabel(), obj.getLabel());
        assertEquals(request.getUserEmail(), obj.getUserEmail());
        assertEquals(request.getRgb(), obj.getRgb());
    }

    @Test
    public void testConversationOrderItem() throws Exception {
        Conversation expectedObj = CheckoutRefereeHelper.getConversation();
        String objectString = CheckoutRefereeHelper.getJsonConversation();

        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        Conversation obj = read(Conversation.class, is);

        assertEquals(expectedObj.getOrder(), obj.getOrder());
        assertEquals(expectedObj, obj);
        assertEquals(expectedObj.getObject(), obj.getObject());
        assertEquals(2, obj.getUpdatedMessages().size());
    }

    @Test
    public void testConversationSku() throws Exception {
        Conversation expectedObj = CheckoutRefereeHelper.getConversationSku();
        String objectString = CheckoutRefereeHelper.getJsonConversationSku();

        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        Conversation obj = read(Conversation.class, is);

        assertEquals(expectedObj, obj);
        assertEquals(expectedObj.getObject(), obj.getObject());
        assertEquals(2, obj.getUpdatedMessages().size());
    }

    @Test
    public void testPagedConversations() throws Exception {
        PagedConversations expectedObj = CheckoutRefereeHelper.getPagedConversations();
        String objectString = CheckoutRefereeHelper.getJsonPagedConversations();

        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        PagedConversations obj = read(PagedConversations.class, is);

        assertArrayEquals(expectedObj.getItems().toArray(), obj.getItems().toArray());
    }

}
