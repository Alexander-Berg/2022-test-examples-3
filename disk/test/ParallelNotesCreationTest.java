package ru.yandex.chemodan.app.notes.core.test;

import java.util.concurrent.CompletableFuture;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.notes.core.NotesManager;
import ru.yandex.chemodan.app.notes.core.NotesManagerImpl;
import ru.yandex.chemodan.app.notes.core.model.notes.Note;
import ru.yandex.chemodan.app.notes.core.model.notes.NoteCreation;
import ru.yandex.chemodan.app.notes.core.model.tag.TagType;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.web.servlet.HttpServletRequestX;
import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;

/**
 * @author yashunsky
 */
public class ParallelNotesCreationTest extends NotesWithContextAbstractTest {
    @Autowired
    private NotesManager notesManager;

    private DataApiUserId uid;
    private ListF<NoteCreation> notesData;
    private int notesCount;

    @Before
    public void setup() {
        notesCount = 10;
        notesData = Cf.range(0, notesCount).map(noteId -> new NoteCreation(
                "title " + noteId, Option.of(Instant.now()),
                Option.empty(), Cf.list(), Cf.list(), Option.empty(), Option.empty(), Option.empty()));

        uid = DataApiUserId.parse("1");
        notesManager.init(uid, Option.empty(), Option.empty(), Option.empty());

        MockHttpServletRequest inp = new ru.yandex.misc.web.servlet.mock.MockHttpServletRequest();
        HttpServletRequestX reqX = ru.yandex.misc.web.servlet.HttpServletRequestX.wrap(inp);

        notesManager.listNotes(uid).forEach(note -> notesManager.deleteNote(uid, note.id, reqX));
    }

    @Test
    public void createMultipleNotesInParallel() {
        ListF<CompletableFuture<Note>> creationResults = notesData.map(noteData -> CompletableFuture.supplyAsync(
                () -> notesManager.createNote(uid, noteData, Option.empty()).getResult()));

        await(creationResults);

        Assert.assertForAll(creationResults, cf -> !cf.isCompletedExceptionally());
        ListF<Note> notes = getNotes();
        Assert.hasSize(notesCount, notes);
    }

    @Test
    public void tryToCreateMultipleNotesInParallelWithExpectedDbRevision() {
        NotesManagerImpl nmImpl = (NotesManagerImpl) notesManager;

        long rev = nmImpl.initInternal(uid, Option.empty()).rev;

        ListF<CompletableFuture<Note>> creationResults = notesData.map(noteData -> CompletableFuture.supplyAsync(
                () -> nmImpl.createNote(uid, Option.empty(), noteData, Option.empty(), Option.of(rev)).getResult()));

        await(creationResults);

        ListF<Note> notes = getNotes();
        Assert.hasSize(1, notes);
    }

    private void await(ListF<CompletableFuture<Note>> creationResults) {
        while (true) {
            if (creationResults.map(CompletableFuture::isDone).containsTs(false)) {
                ThreadUtils.sleep(100);
            } else {
                break;
            }
        }
    }

    private ListF<Note> getNotes() {
        return notesManager.listNotes(uid).filter(note -> !note.tags.containsTs(TagType.DELETED.getPredefinedId()));
    }
}
