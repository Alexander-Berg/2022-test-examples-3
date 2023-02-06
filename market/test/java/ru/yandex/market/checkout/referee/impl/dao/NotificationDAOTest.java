package ru.yandex.market.checkout.referee.impl.dao;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObjectType;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.NoteType;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.criteria.NoteSearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class NotificationDAOTest extends ConversationObjectDAOTest {
    private static final Instant EVENT_TS = Instant.now();

    @Test
    public void testNoteObject() {
        List<Conversation> convs = Stream.of(
                ConversationObjectDAOTest.genOrderConv(),
                ConversationObjectDAOTest.genOrderItemConv()
        ).map(this::addAndGetConv)
                .collect(Collectors.toList());

        for (Conversation conv : convs) {
            checkoutRefereeService.insertNote(generateNote(conv, NoteType.NOTIFY_SHOP));
            assertCount(conv, new NoteSearch(), 1);
        }
    }

    @Test
    public void testNoteSearch() {
        Conversation conv = addAndGetConv(ConversationObjectDAOTest.genOrderItemConv());

        List<Note> notes = Arrays.asList(
                generateNote(conv, NoteType.NOTIFY_SHOP),
                generateNote(conv, NoteType.NOTIFY_SHOP, Color.RED),
                generateNote(conv, NoteType.NOTIFY_USER_WARNING),
                generateNote(conv, NoteType.NOTIFY_USER_REFUND_REQUEST)
        );
        notes.forEach(n -> checkoutRefereeService.insertNote(n));

        NoteSearch noteSearch = new NoteSearch();
        noteSearch.setTypes(EnumSet.of(NoteType.NOTIFY_SHOP, NoteType.NOTIFY_USER_WARNING));
        assertCount(conv, noteSearch, 3);

        noteSearch.setRgbs(EnumSet.of(Color.BLUE));
        assertCount(conv, noteSearch, 2);

        noteSearch.setIgnoreTypes(EnumSet.of(NoteType.NOTIFY_SHOP));
        noteSearch.setTypes(null);
        noteSearch.setRgbs(EnumSet.of(Color.BLUE));
        assertCount(conv, noteSearch, 2);
    }

    private static Note generateNote(Conversation conv, NoteType type) {
        return generateNote(conv, type, Color.BLUE);
    }

    private static Note generateNote(Conversation conv, NoteType type, Color rgb) {
        Note note = new Note();
        note.setConversationId(conv.getId());
        if (ConversationObjectType.aboutOrder(conv.getObject().getObjectType())) {
            note.setOrderId(conv.getObject().getOrderId());
            note.setUserName(conv.getOrder().getName());
            note.setUserEmail(conv.getOrder().getEmail());
        }
        note.setShopId(conv.getShopId());
        note.setUid(conv.getUid());
        note.setAuthorRole(RefereeRole.USER);
        note.setConvStatusAfter(ConversationStatus.ISSUE);
        note.setRgb(rgb);
        note.setType(type);
        note.setEventTs(EVENT_TS);
        return note;
    }

    private void assertCount(Conversation conv, NoteSearch noteSearch, int size) {
        List<Note> notes = checkoutRefereeService.getNotifications(noteSearch).stream()
                .filter(n -> conv.getObject().equals(n.getObject()))
                .collect(Collectors.toList());
        for (Note note : notes) {
            assertEquals(EVENT_TS, note.getEventTs());
            assertEquals(conv.getUid(), Long.valueOf(note.getUid()));
            if (ConversationObjectType.aboutOrder(conv.getObject().getObjectType())) {
                assertEquals(conv.getOrder().getName(), note.getUserName());
                assertEquals(conv.getOrder().getEmail(), note.getUserEmail());
            }
        }
        assertEquals(size, notes.size(),
                notes.stream()
                        .map(n -> n.getType() + " " + n.getRgb() + " " + n.getAuthorRole())
                        .collect(Collectors.joining("\n")));
    }

}
