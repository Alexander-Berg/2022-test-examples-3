package ru.yandex.chemodan.app.notes.dao.test;

import java.util.UUID;

import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.notes.core.test.NotesAbstractTest;
import ru.yandex.chemodan.app.notes.dao.NotesDao;
import ru.yandex.chemodan.app.notes.dao.NotesRequestsDao;
import ru.yandex.chemodan.app.notes.dao.NotesRevisionsDao;
import ru.yandex.chemodan.app.notes.dao.model.AttachmentRecord;
import ru.yandex.chemodan.app.notes.dao.model.NoteRecord;
import ru.yandex.chemodan.app.notes.dao.model.RequestRecord;
import ru.yandex.chemodan.app.notes.dao.model.RevisionRecord;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.inside.mds.MdsFileKey;

/**
 * @author vpronto
 */
@YaIgnore
@ContextConfiguration(classes = NotesEmbeddedPgConfiguration.class)
@ActivateNotesEmbeddedPg
public abstract class NotesJdbcDaoTestSupport extends NotesAbstractTest {

    @Autowired
    protected NotesRevisionsDao revisionsDao;

    @Autowired
    protected NotesDao notesDao;

    @Autowired
    protected NotesRequestsDao requestsDao;

    protected final DataApiUserId uid = DataApiUserId.parse("123");

    protected final NoteRecord noteR = NoteRecord.builder()
            .id(UUID.randomUUID().toString())
            .uid(uid)
            .mtime(Instant.now())
            .lastRevision(1L)
            .build();

    protected final RevisionRecord revisionR = RevisionRecord.builder()
            .uid(noteR.uid)
            .noteId(noteR.id)
            .revision(1L)
            .clientTime(Instant.now())
            .authorUid(DataApiUserId.parse("321"))
            .delta("{  \"key\": \"value\" }")
            .deltaMdsKey(MdsFileKey.parse("1/notes-delta"))
            .snapshotMdsKey(MdsFileKey.parse("2/notes-snapsot"))
            .build();

    protected final RequestRecord requestR = RequestRecord.builder()
            .uid(noteR.uid)
            .requestId("requestId")
            .entityId("entityId")
            .revision(1L)
            .build();

    protected final AttachmentRecord attachmentR = AttachmentRecord.builder()
            .uid(noteR.uid)
            .noteId(noteR.id)
            .createRevision(revisionR.revision)
            .path("path")
            .build();
}
