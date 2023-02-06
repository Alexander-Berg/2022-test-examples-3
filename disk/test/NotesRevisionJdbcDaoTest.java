package ru.yandex.chemodan.app.notes.dao.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.notes.dao.model.RevisionRecord;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.misc.test.Assert;

/**
 * @author vpronto
 */
public class NotesRevisionJdbcDaoTest extends NotesJdbcDaoTestSupport {

    @Before
    public void insert() {
        notesDao.create(noteR);
        revisionsDao.create(revisionR);
    }

    @After
    public void delete() {
        revisionsDao.delete(revisionR.uid, revisionR.noteId, revisionR.revision);
        notesDao.delete(noteR.uid, noteR.id);
        Option<RevisionRecord> rec = revisionsDao.findRevision(revisionR.uid, revisionR.noteId, revisionR.revision);
        Assert.isFalse(rec.isPresent());
    }

    @Test
    public void find() {
        Option<RevisionRecord> note = revisionsDao.findRevision(revisionR.uid, revisionR.noteId, revisionR.revision);
        Assert.isTrue(note.isPresent());
        Assert.equals(note.get().uid, revisionR.uid);
        Assert.equals(note.get().noteId, revisionR.noteId);
        Assert.equals(note.get().revision, 1L);
    }

    @Test
    public void findAll() {
        ListF<RevisionRecord> revisions = revisionsDao.findRevisions(revisionR.uid, revisionR.noteId);
        Assert.isFalse(revisions.isEmpty());
        Assert.equals(revisions.get(0).uid, revisionR.uid);
        Assert.equals(revisions.get(0).noteId, revisionR.noteId);
        Assert.equals(revisions.get(0).revision, 1L);
    }

    @Test
    public void update() {
        RevisionRecord patch = revisionR.toBuilder()
                .snapshotMdsKey(MdsFileKey.parse("2/notes-snapsot-2"))
                .build();
        revisionsDao.update(patch);
        Option<RevisionRecord> note = revisionsDao.findRevision(revisionR.uid, revisionR.noteId, revisionR.revision);
        Assert.isTrue(note.isPresent());
        Assert.equals(note.get().uid, patch.uid);
        Assert.equals(note.get().noteId, patch.noteId);
        Assert.equals(note.get().snapshotMdsKey.serialize(), "2/notes-snapsot-2");
    }
}
