package ru.yandex.market.checkout.referee;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.RefereeRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author kukabara
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({
        "classpath:checkout-referee-test.xml"
})
@Disabled
public class CheckoutRefereeClientTest {
    @Autowired
    CheckoutRefereeClient checkoutRefereeClient;

    /*
    curl -X POST \
     'http://checkouter.tst.vs.market.yandex.net:39001/get-orders?clientRole=SYSTEM&clientId=1' \
     -H 'content-type: application/json' \
     -d '{
     "paymentMethod": "YANDEX",
     "fake": false,
     "statuses" : ["DELIVERY"]
     } '
     */
    @Test
    public void startConversation() throws Exception {
        Conversation conversation = null;
        try {
            String title = "Вопрос о доставке";
            String text = "Длинный текст\nRussian language";
            long orderId = 1711316L;
            long userId = 4004661923L;
            conversation = checkoutRefereeClient.startConversation(new ConversationRequest.Builder(
                    userId, RefereeRole.USER, ConversationObject.fromOrder(orderId), text)
                    .withTitle(title).build());
            assertNotNull(conversation);
            assertEquals(conversation.getTitle(), title);
            assertEquals(1, conversation.getUpdatedMessages().size());
        } finally {
            if (conversation != null) {
                checkoutRefereeClient.deleteConversation(conversation.getId());
            }
        }
    }

}
