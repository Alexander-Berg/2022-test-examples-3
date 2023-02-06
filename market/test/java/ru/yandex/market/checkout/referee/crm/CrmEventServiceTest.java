package ru.yandex.market.checkout.referee.crm;

import java.util.Date;
import java.util.EnumSet;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;

import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.IssueType;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.OrderInfo;
import ru.yandex.market.checkout.entity.RefereeRole;

import static org.mockito.Mockito.verify;

/**
 * @author komarovns
 * @date 14.09.18
 */
@ExtendWith(MockitoExtension.class)
public class CrmEventServiceTest {
    private static final long id = 1;
    private static final long uid = 2;
    private static final long orderId = 3;
    private static final Date timestamp = new Date();
    private static final EnumSet<IssueType> issueTypes = EnumSet.of(IssueType.MISSING_ITEMS, IssueType.DELIVERY_DELAY);
    private static final ConversationStatus status = ConversationStatus.ARBITRAGE;
    private static final RefereeRole authorRole = RefereeRole.USER;
    private static final String text = "some message";

    @Spy
    private CrmEventService crmEventService;

    @Test
    public void testChangeStatusEvent() {
        crmEventService.pushChangeConversationStatusEvent(createConversation());
        verify(crmEventService).pushEvent(createChangeStatusEvent());
    }

    @Test
    public void testMessageEvent() {
        crmEventService.pushMessageEvent(createConversation(), createMessage());
        verify(crmEventService).pushEvent(createMessageEvent());
    }

    private static Conversation createConversation() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(orderId);

        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setUid(uid);
        conversation.setOrder(orderInfo);
        conversation.setLastStatus(status);
        conversation.setLastStatusTs(timestamp);
        conversation.setIssueTypes(issueTypes);
        return conversation;
    }

    private static Message createMessage() {
        return new Message.Builder(id, uid, authorRole)
                .withText(text)
                .withMessageTs(timestamp)
                .build();
    }

    private static CrmEvent createChangeStatusEvent() {
        CrmEvent event = createEventWithoutType();
        event.setConversationStatus(status);
        event.setEventType(CrmEventType.CHANGE_STATUS);
        return event;
    }

    private static CrmEvent createMessageEvent() {
        CrmEvent event = createEventWithoutType();
        event.setText(text);
        event.setAuthorRole(authorRole);
        event.setEventType(CrmEventType.MESSAGE);
        return event;
    }

    private static CrmEvent createEventWithoutType() {
        CrmEvent event = new CrmEvent();
        event.setConversationId(id);
        event.setUid(uid);
        event.setOrderId(orderId);
        event.setTimeStamp(timestamp);
        event.setIssueTypes(issueTypes);
        return event;
    }
}
