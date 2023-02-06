package ru.yandex.chemodan.app.notes.core.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiPublicUserId;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.notes.core.NotesContentManager;
import ru.yandex.chemodan.app.notes.core.NotesManager;
import ru.yandex.chemodan.app.notes.core.NotesManagerImpl;
import ru.yandex.chemodan.app.notes.core.model.notes.Note;
import ru.yandex.chemodan.app.notes.dao.NotesDao;
import ru.yandex.chemodan.app.notes.dao.NotesRevisionsDao;
import ru.yandex.chemodan.app.notes.dao.model.RevisionRecord;
import ru.yandex.commune.dynproperties.DynamicPropertyManager;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.io.InputStreamX;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.web.servlet.HttpServletRequestX;

/**
 * @author yashunsky
 */
public class NotesWithContentTest extends NotesWithContextAbstractTest {
    private HttpServletRequestX request;

    @Autowired
    private NotesManager notesManager;
    @Autowired
    private DynamicPropertyManager dynamicPropertyManager;

    @Before
    public void setup() {
        request = Mockito.mock(HttpServletRequestX.class);
        Mockito.when(request.getHeaderO(Mockito.any())).thenReturn(Option.empty());
        dynamicPropertyManager.setValue(((NotesManagerImpl) notesManager).createInitialNote, true);
    }

    @Test
    public void createPublicNoteUseItAsInitialAndClone() {
        DataApiUserId publicUid = new DataApiPublicUserId();
        notesManager.init(publicUid, Option.empty(), Option.empty(), Option.empty());
        Note publicNote =
                notesManager.createPublicNote(Language.ENGLISH, "some title", "some snippet", "some content").result;
        Assert.equals("some title", publicNote.title);
        Assert.some(publicNote.snippet, "some snippet");
        Assert.equals("some content", getContent(publicUid, publicNote.id));

        DataApiUserId uid = DataApiUserId.parse("8461");
        notesManager.init(uid, Option.empty(), Option.empty(), Option.empty());

        ListF<Note> allNotes = notesManager.listNotes(uid);
        Assert.hasSize(1, allNotes);

        Note initialNote = allNotes.single();
        Assert.equals("some title", initialNote.title);
        Assert.equals("some content", getContent(uid, initialNote.id));

        String extendedContent = "{\"snippet\":\"something\",\"content\":\"new content\"}";

        Mockito.when(request.getInputStreamX()).thenReturn(stringToStream(extendedContent));

        Note clonedNote = notesManager.createNoteWithContent(
                uid, Option.of(initialNote.id), Option.empty(), Instant.now(), request).result;
        Assert.some(clonedNote.getSnippet(), "something");
        Assert.equals("some title", initialNote.title);
        Assert.some(initialNote.snippet, "something");
        Assert.equals("new content", getContent(uid, initialNote.id));
    }

    private InputStreamX stringToStream(String value) {
        return new InputStreamX(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Autowired
    private NotesDao notesDao;
    @Autowired
    private NotesRevisionsDao notesRevisionsDao;
    @Autowired
    private NotesContentManager notesContentManager;

    private String getContent(DataApiUserId uid, String noteId) {
        OutputStream outputStream = new ByteArrayOutputStream();
        notesDao.findNote(uid, noteId)
                .filterMap(note -> notesRevisionsDao.findRevision(note.uid, note.id, note.lastRevision))
                .map(RevisionRecord::getSnapshotMdsKey).ifPresent(key -> notesContentManager.get(key, outputStream));

        return outputStream.toString();
    }
}
