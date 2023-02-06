package ru.yandex.chemodan.app.notes.core.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataField;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecord;
import ru.yandex.chemodan.app.dataapi.api.data.record.DataRecordId;
import ru.yandex.chemodan.app.dataapi.api.db.Database;
import ru.yandex.chemodan.app.dataapi.api.deltas.Delta;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.manager.DataApiManager;
import ru.yandex.chemodan.app.notes.core.NotesContentManager;
import ru.yandex.chemodan.app.notes.core.NotesManager;
import ru.yandex.chemodan.app.notes.core.NotesManagerImpl;
import ru.yandex.chemodan.app.notes.core.model.notes.NoteTag;
import ru.yandex.chemodan.app.notes.dao.NotesDao;
import ru.yandex.chemodan.app.notes.dao.NotesRequestsDao;
import ru.yandex.chemodan.app.notes.dao.NotesRevisionsDao;
import ru.yandex.chemodan.app.notes.dao.model.NoteRecord;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.misc.io.InputStreamX;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.web.servlet.HttpServletRequestX;
import ru.yandex.misc.web.servlet.HttpServletResponseX;

/**
 * @author yashunsky
 */
public class ExtendedContentTest {

    private DataApiUserId uid = DataApiUserId.parse("1");
    private String noteId = "1_1_000";
    private String noteContent = "serializedContent";
    private NotesManager notesManager;
    private HttpServletRequestX request;
    private HttpServletResponseX response;

    private ArgumentCaptor<byte[]> streamCaptor = ArgumentCaptor.forClass(byte[].class);
    private ArgumentCaptor<Delta> deltaCaptor = ArgumentCaptor.forClass(Delta.class);

    @Before
    public void setup() {
        Database db = Mockito.mock(Database.class);

        DataRecordId recordId = Mockito.mock(DataRecordId.class);
        DataRecord dataRecord = new DataRecord(uid, recordId, 0,
                Cf.map("ctime", Mockito.mock(DataField.class), "mtime", Mockito.mock(DataField.class)));
        Mockito.when(recordId.recordId()).thenReturn(noteId);

        DataApiManager dataApiManager = Mockito.mock(DataApiManager.class);
        Mockito.when(dataApiManager.getDatabaseO(Mockito.any())).thenReturn(Option.of(db));
        Mockito.when(dataApiManager.getRecords(Mockito.any(), Mockito.any())).thenReturn(Cf.list(dataRecord));
        Mockito.when(dataApiManager.applyDelta(Mockito.any(), Mockito.any(), deltaCaptor.capture()))
                .thenReturn(db);

        NotesDao notesDao = Mockito.mock(NotesDao.class);
        Mockito.when(notesDao.findNote(Mockito.any(), Mockito.any())).thenReturn(Option.of(
                NoteRecord.builder().uid(uid).id(noteId).mtime(Instant.now()).lastRevision(0).build()));

        NotesRevisionsDao notesRevisionsDao = Mockito.mock(NotesRevisionsDao.class);
        NotesRequestsDao notesRequestsDao = Mockito.mock(NotesRequestsDao.class);
        NotesContentManager notesContentManager = Mockito.mock(NotesContentManager.class);

        Mockito.when(notesContentManager.put(Mockito.any(), Mockito.any(), Mockito.anyLong(), streamCaptor.capture()))
                .thenReturn(MdsFileKey.parse("0/id"));

        int retiresOnConflict = 0;
        notesManager = new NotesManagerImpl(
                dataApiManager, notesDao, notesRevisionsDao, notesRequestsDao, notesContentManager,
                null, null, null, null, Duration.ZERO, retiresOnConflict);

        request = Mockito.mock(HttpServletRequestX.class);
        Mockito.when(request.getHeaderO(Mockito.any())).thenReturn(Option.empty());

        response = Mockito.mock(HttpServletResponseX.class);
    }

    @Test
    public void putSimpleContent() {
        Mockito.when(request.getInputStreamX()).thenReturn(stringToStream(noteContent));
        notesManager.putContent(uid, noteId, Option.empty(), Instant.now(), false, request, response);

        Assert.equals(Cf.set("mtime", "content_revision"), getChangedFields().unique());
        Assert.equals(noteContent, new String(streamCaptor.getValue()));
    }

    @Test
    public void putExtendedContent() {
        String extendedContent = "{\"snippet\":\"something\",\"content\":\"" + noteContent + "\"}";

        Mockito.when(request.getInputStreamX()).thenReturn(stringToStream(extendedContent));
        notesManager.putContent(uid, noteId, Option.empty(), Instant.now(), true, request, response);

        Assert.equals(Cf.set("mtime", "snippet", "content_revision"), getChangedFields().unique());
        Assert.equals(noteContent, new String(streamCaptor.getValue()));
    }

    @Test
    public void extendedContentSetOptionsParsing () {
        String body = "{\n" +
                "    \"title\": \"something old\",\n" +
                "    \"snippet\": \"something new\",\n" +
                "    \"content\": \"something borrowed something blue\",\n" +
                "    \"attach_resource_ids\": [\"id1\", \"id2\"],\n" +
                "    \"tags_with_meta\": [{\"id\": 1}, {\"id\": 2, \"mtime\": \"2019-01-18T13:43:09.920Z\"}]," +
                "    \"add_tags_with_meta\": [{\"id\": 6}, {\"id\": 7, \"mtime\": \"2019-01-19T13:43:09.920Z\"}]," +
                "    \"add_tags\": [5],\n" +
                "    \"remove_tags\": [3]\n" +
                "}\n";
        NotesManagerImpl.ExtendedContent extendedContent = parseExtendedContent(body);
        Assert.some("something old", extendedContent.title);
        Assert.some("something new", extendedContent.snippet);
        Assert.equals("something borrowed something blue", new String(extendedContent.content));
        Assert.equals(Cf.list("id1", "id2"), extendedContent.attachResourceIds.get());

        ListF<NoteTag> expectedTagsWithMeta = Cf.list(
                NoteTag.builder().id(1L).mtime(Option.empty()).build(),
                NoteTag.builder().id(2L).mtime(Option.of(Instant.parse("2019-01-18T13:43:09.920Z"))).build()
        );

        SetF<NoteTag> expectedAddedTagsWithMeta = Cf.set(
                NoteTag.builder().id(6L).mtime(Option.empty()).build(),
                NoteTag.builder().id(7L).mtime(Option.of(Instant.parse("2019-01-19T13:43:09.920Z"))).build()
        );

        Assert.equals(expectedTagsWithMeta, extendedContent.tagsWithMeta.get());

        Assert.equals(expectedAddedTagsWithMeta, extendedContent.addTagsWithMeta);
        Assert.equals(Cf.set(5L), extendedContent.addTags);
        Assert.equals(Cf.set(3L), extendedContent.removeTags);
    }

    @Test
    public void extendedContentEmptyOptionsParsing () {
        String body = "{\"content\":\"some content\"}";
        NotesManagerImpl.ExtendedContent extendedContent = parseExtendedContent(body);
        Assert.equals("some content", new String(extendedContent.content));
    }

    private NotesManagerImpl.ExtendedContent parseExtendedContent(String body) {
        return new NotesManagerImpl.ExtendedContent(stringToStream(body), true);
    }

    private InputStreamX stringToStream(String value) {
        return new InputStreamX(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
    }

    private ListF<String> getChangedFields() {
        return deltaCaptor.getValue().changes.single().getFields().map(field -> field.fieldId).sorted();
    }
}
