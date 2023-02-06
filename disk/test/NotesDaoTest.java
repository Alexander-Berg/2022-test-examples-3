package ru.yandex.chemodan.app.notes.dao.test;

import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.notes.dao.model.NoteRecord;
import ru.yandex.misc.test.Assert;

/**
 * @author vpronto
 */
public class NotesDaoTest extends NotesJdbcDaoTestSupport {

    @Before
    public void insert() {
        notesDao.create(noteR);
    }

    @After
    public void delete() {
        boolean deleted = notesDao.softDelete(noteR.uid, noteR.id);
        Assert.isTrue(deleted);
        Option<NoteRecord> note = notesDao.findNote(noteR.uid, noteR.id);
        Assert.isTrue(!note.isPresent());

        notesDao.delete(noteR.uid, noteR.id);
        Option<NoteRecord> deletedNote = notesDao.findNote(noteR.uid, noteR.id);
        Assert.isFalse(deletedNote.isPresent());
    }

    @Test
    public void find() {
        Option<NoteRecord> note = notesDao.findNote(noteR.uid, noteR.id);
        Assert.isTrue(note.isPresent());
        Assert.equals(note.get().id, noteR.id);
        Assert.equals(note.get().uid, noteR.uid);
        Assert.equals(note.get().lastRevision, 1L);
    }

    @Test
    public void findNotes() {
        ListF<NoteRecord> notes = notesDao.findNotes(noteR.uid);
        Assert.isFalse(notes.isEmpty());
        Assert.equals(notes.get(0).id, noteR.id);
        Assert.equals(notes.get(0).uid, noteR.uid);
        Assert.equals(notes.get(0).lastRevision, 1L);
    }

    @Test
    public void update() {
        NoteRecord patch = noteR.toBuilder()
                .mtime(Instant.now())
                .lastRevision(2L)
                .build();
        notesDao.update(patch);
        Option<NoteRecord> note = notesDao.findNote(noteR.uid, noteR.id);
        Assert.isTrue(note.isPresent());
        Assert.equals(note.get().id, patch.id);
        Assert.equals(note.get().uid, patch.uid);
        Assert.equals(note.get().lastRevision, 2L);
    }
}
