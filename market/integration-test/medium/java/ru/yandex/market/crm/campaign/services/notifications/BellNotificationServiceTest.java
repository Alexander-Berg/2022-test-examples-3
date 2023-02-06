package ru.yandex.market.crm.campaign.services.notifications;

import java.time.LocalDateTime;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.domain.notifications.Note;
import ru.yandex.market.crm.campaign.domain.notifications.NoteType;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;

/**
 * @author apershukov
 */
public class BellNotificationServiceTest extends AbstractServiceMediumTest {

    @Inject
    private NotificationDAO notificationDAO;
    @Inject
    private BellNotificationService notificationService;

    private static Note note() {
        return new Note()
                .setType(NoteType.INFO)
                .setSubject("Subject");
    }

    @Test
    public void testPurgeOutdated() {
        Note note1 = note();
        note1 = notificationDAO.save(note1);

        Note note2 = note()
                .setTo(LocalDateTime.now().plusMinutes(1));
        note2 = notificationDAO.save(note2);

        Note note3 = note()
                .setTo(LocalDateTime.now().minusMinutes(1));
        notificationDAO.save(note3);

        notificationService.purgeOutdatedNotes();

        List<Note> notes = notificationDAO.getAll();
        Assertions.assertEquals(2, notes.size());
        Assertions.assertEquals(note2.getId(), notes.get(0).getId());
        Assertions.assertEquals(note1.getId(), notes.get(1).getId());
    }

    @Test
    public void testAddNoteWithKey() {
        Note note = note()
                .setKey("iddqd");

        notificationService.addNote(note);

        List<Note> notes = notificationDAO.getActual();
        Assertions.assertEquals(1, notes.size());

        Assertions.assertEquals(note.getKey(), notes.get(0).getKey());
    }

    @Test
    public void testDeleteByKey() {
        Note note1 = note()
                .setKey("iddqd");

        notificationService.addNote(note1);

        Note note2 = note();
        note2 = notificationService.addNote(note2);

        notificationService.deleteNoteByKey(note1.getKey());

        List<Note> notes = notificationDAO.getActual();
        Assertions.assertEquals(1, notes.size());
        Assertions.assertEquals(note2.getId(), notes.get(0).getId());
    }

    @Test
    public void testReplaceNoteByKey() {
        Note oldNote = note()
                .setKey("iddqd");

        oldNote = notificationService.addNote(oldNote);

        Note newNote = note()
                .setType(NoteType.ALERT);

        notificationService.replaceNoteByKey(oldNote.getKey(), newNote);

        List<Note> notes = notificationDAO.getActual();
        Assertions.assertEquals(1, notes.size());

        newNote = notes.get(0);
        Assertions.assertEquals(oldNote.getKey(), newNote.getKey());
        Assertions.assertNotEquals(oldNote.getId(), newNote.getId());
        Assertions.assertEquals(NoteType.ALERT, newNote.getType());
    }
}
