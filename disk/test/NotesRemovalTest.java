package ru.yandex.chemodan.app.notes.core.test;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.notes.core.NotesManager;
import ru.yandex.chemodan.app.notes.core.model.notes.Note;
import ru.yandex.chemodan.app.notes.core.model.notes.NoteCreation;
import ru.yandex.chemodan.app.notes.core.model.notes.NoteTag;
import ru.yandex.chemodan.app.notes.core.model.tag.TagType;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class NotesRemovalTest extends NotesWithContextAbstractTest {
    @Autowired
    private NotesManager notesManager;
    @Autowired
    private BazingaTaskManager bazingaTaskManager;

    private DataApiUserId uid;

    @Before
    public void setup() {
        uid = DataApiUserId.parse("454");
        notesManager.init(uid, Option.empty(), Option.empty(), Option.empty());
    }

    @Test
    public void removeNotesTest() {
        Instant deadline = Instant.now().minus(Duration.standardDays(31));
        createDeletedNote("deleted a long time ago", deadline);
        createNotDeletedNote("existing");

        Assert.hasSize(2, notesManager.listNotes(uid));

        notesManager.removeDeletedNotes(uid, true);

        SetF<String> remainingNotes = notesManager.listNotes(uid).map(Note::getTitle).unique();
        Assert.hasSize(1, remainingNotes);
        Assert.assertContains(remainingNotes, "existing");
        Mockito.verify(bazingaTaskManager, Mockito.never()).schedule(Mockito.any(), Mockito.any());

        createDeletedNote("just deleted", Instant.now());

        Assert.hasSize(2, notesManager.listNotes(uid));
        notesManager.removeDeletedNotes(uid, true);

        remainingNotes = notesManager.listNotes(uid).map(Note::getTitle).unique();
        Assert.hasSize(2, remainingNotes);
        Assert.assertContains(remainingNotes, "just deleted");
        Assert.assertContains(remainingNotes, "existing");

        Mockito.verify(bazingaTaskManager, Mockito.only()).schedule(Mockito.any(), Mockito.any());
    }

    private void createDeletedNote(String title, Instant deletion) {
        createNote(title,
                Option.of(NoteTag.builder().id(TagType.DELETED.getPredefinedId()).mtime(Option.of(deletion)).build()));
    }

    private void createNotDeletedNote(String title) {
        createNote(title, Option.empty());
    }

    private void createNote(String title, Option<NoteTag> deletionTag) {
        NoteCreation base = new NoteCreation(title, Option.empty(), Option.empty(),
                deletionTag, deletionTag.map(NoteTag::getId),
                Option.empty(), Option.empty(), Option.empty());
        notesManager.createNote(uid, base, Option.empty());
    }
}
