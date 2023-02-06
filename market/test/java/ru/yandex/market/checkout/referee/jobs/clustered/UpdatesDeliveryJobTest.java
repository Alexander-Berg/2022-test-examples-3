package ru.yandex.market.checkout.referee.jobs.clustered;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.NoteType;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.criteria.NoteSearch;
import ru.yandex.market.checkout.referee.impl.CheckoutRefereeService;
import ru.yandex.market.checkout.referee.impl.RefereeManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 */
public class UpdatesDeliveryJobTest extends EmptyTest {

    @Autowired
    private UpdatesDeliveryJob updatesDeliveryJob;

    @Autowired
    private CheckoutRefereeService checkoutRefereeService;

    @Autowired
    private RefereeManager refereeManager;

    private long convId;

    @BeforeEach
    public void setUp() {
        long uid = 1L;
        long shopId = 1L;
        long orderId = Math.abs(RND.nextInt());

        refereeManager.getOrder(orderId, uid, RefereeRole.USER, shopId);
        Conversation conv = refereeManager.start(
                new ConversationRequest.Builder(uid, RefereeRole.USER, ConversationObject.fromOrder(orderId), "text")
                        .withTitle("title")
                        .build(), ConversationStatus.OPEN);
        convId = conv.getId();

        Message message = new Message.Builder(conv.getId(), 1L, RefereeRole.SHOP)
                .withText("text")
                .withMessageTs(new Date())
                .withConversation(conv)
                .build();
        refereeManager.send(conv, message, null, RefereeManager.UpdateConvFunction.EMPTY);
    }

    @Test
    public void testDoRealJob() throws Exception {
        updatesDeliveryJob.setJobName("jobName");
        updatesDeliveryJob.doJob(null);

        Conversation conversation = checkoutRefereeService.getConversation(convId);
        assertTrue(conversation.isReadBy(RefereeRole.SYSTEM));

        List<Note> notes = getNotes();
        assertEquals(1, notes.size());
        assertEquals(notes.get(0).getConversationId(), convId);

        checkoutRefereeService.deleteNotifications(notes.stream().map(Note::getId).collect(Collectors.toList()));
        assertTrue(getNotes().isEmpty());

        updatesDeliveryJob.doRealJob(null);
        assertTrue(getNotes().isEmpty());
    }

    private List<Note> getNotes() {
        return checkoutRefereeService.getNotifications(new NoteSearch()).stream()
                .filter(n -> n.getType() == NoteType.NOTIFY_USER_NEW_MESSAGE)
                .collect(Collectors.toList());
    }
}
