package ru.yandex.chemodan.app.notes.core.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.notes.api.AddRevisionInterceptor;
import ru.yandex.chemodan.app.notes.core.model.notes.AttachmentCreation;
import ru.yandex.chemodan.app.notes.core.model.notes.Note;
import ru.yandex.chemodan.app.notes.core.model.notes.NoteCreation;
import ru.yandex.chemodan.app.notes.core.model.notes.NotePatch;
import ru.yandex.chemodan.app.notes.core.model.notes.NoteTag;
import ru.yandex.chemodan.app.notes.core.model.notes.NoteWithMdsKey;
import ru.yandex.chemodan.app.notes.core.model.tag.TagBase;
import ru.yandex.chemodan.app.notes.core.model.tag.TagPatch;
import ru.yandex.chemodan.app.notes.core.model.tag.TagType;
import ru.yandex.chemodan.test.A3TestSupport;
import ru.yandex.chemodan.util.bender.ISOInstantUnmarshaller;
import ru.yandex.misc.bender.Bender;
import ru.yandex.misc.bender.MembersToBind;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.config.CustomMarshallerUnmarshallerFactoryBuilder;
import ru.yandex.misc.bender.custom.ReadableInstantConfigurableMarshaller;
import ru.yandex.misc.bender.parse.BenderParser;
import ru.yandex.misc.bender.serialize.BenderSerializer;
import ru.yandex.misc.io.http.HttpHeaderNames;
import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG;
import static ru.yandex.chemodan.app.notes.dao.test.ActivateNotesEmbeddedPg.NOTES_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author vpronto
 * TODO: use mapper everywhere for model coverage
 */
@ActiveProfiles({EMBEDDED_PG, NOTES_EMBEDDED_PG, DATAAPI_EMBEDDED_PG})
@ContextConfiguration(classes = NotesTestContextConfiguration.class)
public class NotesActionsTest extends A3TestSupport {

    private static BenderConfiguration benderConfiguration = BenderConfiguration.cons(
            MembersToBind.WITH_ANNOTATIONS, false,
            CustomMarshallerUnmarshallerFactoryBuilder.cons()
                    .add(Instant.class, new ReadableInstantConfigurableMarshaller(ISODateTimeFormat.dateTime()))
                    .add(Instant.class, new ISOInstantUnmarshaller())
                    .build()
    );
    private static BenderParser<Note> noteParser = Bender.parser(Note.class, benderConfiguration);
    private static BenderParser<NoteWithMdsKey> noteWithMdsKeyBenderParser =
            Bender.parser(NoteWithMdsKey.class, benderConfiguration);
    private static BenderSerializer<NotePatch> notePatchSerializer = Bender.serializer(NotePatch.class, benderConfiguration);
    private static BenderSerializer<NoteCreation> createNoteSerializer =
            Bender.serializer(NoteCreation.class, benderConfiguration);
    private static BenderSerializer<AttachmentCreation> attachementsCreationSerializer =
            Bender.serializer(AttachmentCreation.class, benderConfiguration);
    private static BenderSerializer<TagPatch> tagPatchSerializer =
            Bender.serializer(TagPatch.class, benderConfiguration);
    private static BenderSerializer<TagBase> tagBaseSerializer =
            Bender.serializer(TagBase.class, benderConfiguration);


    @Before
    public void prepare() {
        HttpResponse response = put("/api/init?__uid=1", null);
        Assert.equals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
    }

    @Test
    public void parseLocaleFromRequest() {
        HttpResponse response = put("/api/init?__uid=784&locale=tr", null);
        Assert.equals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
        HttpResponse badResponse = put("/api/init?__uid=784&locale=zz", null);
        Assert.equals(HttpStatus.SC_NO_CONTENT, badResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void createNoteWithTags() throws IOException {

        String noteCreation = new String(createNoteSerializer.serializeJson(
                new NoteCreation("title", Option.empty(), Option.empty(), Cf.list(), Cf.list(),
                        Option.of("snippet"), Option.empty(), Option.empty())));
        Note createNoteResult = createNote(noteCreation);

        String noteId = createNoteResult.id;

        String noteUrl = "/api/notes/" + noteId + "?__uid=1";
        String noteContentUrl = "/api/notes/" + noteId + "/content?__uid=1";
        String noteAttachmentsUrl = "/api/notes/" + noteId + "/attachments?__uid=1";
        String noteAttachUrl = "/api/notes/" + noteId + "/attachments/test_123?__uid=1";

        // again, should be skipped
        Note createNoteResult2 = createNote(new String(createNoteSerializer.serializeJson(
                new NoteCreation("skipped", Option.empty(), Option.empty(), Cf.list(), Cf.list(),
                        Option.of("skipped"), Option.empty(), Option.empty()))));

        Assert.assertNotEquals(createNoteResult2.title, "skipped");

        getNote(noteUrl, createNoteResult);

        patchNote(noteUrl);

        addTag();
        patchTag();
        listTags("tag321");
        deleteTag();

        getContent(noteContentUrl); //getEmptyContent
        putContent(noteContentUrl);
        String rev = getContent(noteContentUrl); //getSameContent
        getContent(noteContentUrl, rev);

        addAttachments(noteAttachmentsUrl);
        deleteAttachments(noteAttachUrl, noteAttachmentsUrl);

        String responseWithKey = getResult(getRawContent(noteUrl + "&mdsKey=true"));
        String mdsKey = noteWithMdsKeyBenderParser.parseJson(responseWithKey).getContentMdsKey().get().serialize();
        Assert.equals("1/notes-snapsot-1", mdsKey);

        getNotes();

        Note note;

        deleteNoteWithRevision(noteUrl, 0); //try to delete note with outdated content revision

        note = getResultNote(get(noteUrl));
        Assert.notNull(note);
        Assert.notIn(TagType.DELETED.getPredefinedId(), note.tagsWithMeta.map(noteTag -> noteTag.id));

        deleteNoteWithRevision(noteUrl, note.contentRevision.get()); //delete note with actual revision

        note = getResultNote(get(noteUrl));
        Assert.notNull(note);
        Assert.assertContains(note.tagsWithMeta.map(noteTag -> noteTag.id), TagType.DELETED.getPredefinedId());
        listTags("deleted");

        getContentAfterDelete(noteContentUrl);

        deleteNote(noteUrl);
    }

    private void getNote(String noteUrl, Note createNoteResult) throws IOException {
        // assert we get same data
        HttpResponse getResponse = get(noteUrl);
        Assert.equals(createNoteResult, getResultNote(getResponse));
    }

    private Note getResultNote(HttpResponse getResponse) throws IOException {
        return noteParser.parseJson(getResult(getResponse));
    }

    private void getNotes() {

        HttpResponse getResponse = get("/api/notes?__uid=1");
        String result = getResult(getResponse);
        Assert.notEmpty(result);
        Assert.assertContains(result, "items");
        Assert.assertContains(result, "title");
    }

    private void addAttachments(String noteAttachmentsUrl) throws JsonProcessingException {
        AttachmentCreation attachmentCreation = new AttachmentCreation("test_123", "name");
        String addAttachmentsJson = new String(attachementsCreationSerializer.serializeJson(attachmentCreation));
        HttpResponse put = put(noteAttachmentsUrl, addAttachmentsJson);
        Assert.equals(HttpStatus.SC_OK, put.getStatusLine().getStatusCode());
        String result = getResult(put);
        Assert.notEmpty(result);

        HttpResponse get = get(noteAttachmentsUrl);
        Assert.equals(HttpStatus.SC_OK, get.getStatusLine().getStatusCode());
        String getR = getResult(get);

    }

    private void deleteAttachments(String noteAttachUrl, String noteAttachmentsUrl) {
        HttpResponse delete = delete(noteAttachUrl);
        Assert.equals(HttpStatus.SC_NO_CONTENT, delete.getStatusLine().getStatusCode());

        HttpResponse delete2 = delete(noteAttachUrl);
        Assert.equals(HttpStatus.SC_NO_CONTENT, delete2.getStatusLine().getStatusCode());

        HttpResponse get = get(noteAttachmentsUrl);
        Assert.equals(HttpStatus.SC_OK, get.getStatusLine().getStatusCode());
        String getR = getResult(get);
        Assert.assertContains(getR, "[]");
    }

    private void putContent(String noteContentUrl) throws UnsupportedEncodingException {
        HttpResponse put = put(noteContentUrl,
                "{\"name\":\"$root\",\"children\":[{\"name\":\"paragraph\",\"children\":[{\"data\":\"Hello!\"}]}]}");
        Assert.notNull(put.getFirstHeader(AddRevisionInterceptor.X_ACTUAL_REVISION));
        Assert.equals(HttpStatus.SC_NO_CONTENT, put.getStatusLine().getStatusCode());

        HttpEntityEnclosingRequestBase confReq = new HttpPut(buildUrl(noteContentUrl));
        confReq.setHeader(HttpHeaderNames.IF_MATCH, "0");
        HttpResponse confResp = execute(confReq, new StringEntity("{}"));
        Assert.equals(HttpStatus.SC_CONFLICT, confResp.getStatusLine().getStatusCode());
        Header firstHeader = confResp.getFirstHeader(AddRevisionInterceptor.X_ACTUAL_REVISION);
        Assert.notNull(firstHeader);
        Assert.equals(firstHeader.getValue(), "1");
        getResult(confResp);

        HttpEntityEnclosingRequestBase newRevReq = new HttpPut(buildUrl(noteContentUrl));
        String revision = put.getFirstHeader(AddRevisionInterceptor.X_ACTUAL_REVISION).getValue();
        newRevReq.setHeader(HttpHeaderNames.IF_MATCH, revision);
        String reqId = UUID.randomUUID().toString();
        newRevReq.setHeader("X-Request-Id", reqId);
        HttpResponse newRevResp = execute(newRevReq, new StringEntity("{}"));
        Assert.equals(HttpStatus.SC_NO_CONTENT, newRevResp.getStatusLine().getStatusCode());

        HttpEntityEnclosingRequestBase sameRevReq = new HttpPut(buildUrl(noteContentUrl));
        sameRevReq.setHeader(HttpHeaderNames.IF_MATCH, revision);
        sameRevReq.setHeader("X-Request-Id", reqId);
        HttpResponse sameRevResp = execute(sameRevReq, new StringEntity("{}"));
        Assert.equals(HttpStatus.SC_NO_CONTENT, sameRevResp.getStatusLine().getStatusCode());

    }

    private String getContent(String noteContentUrl) {
        HttpResponse get = getRawContent(noteContentUrl);
        Header rev = get.getFirstHeader(AddRevisionInterceptor.X_ACTUAL_REVISION);
        Assert.notNull(rev);
        Assert.equals(HttpStatus.SC_OK, get.getStatusLine().getStatusCode());
        Assert.notNull(getResult(get));
        return rev.getValue();
    }

    private HttpResponse getRawContent(String noteContentUrl) {
        return get(noteContentUrl);
    }

    private void getContent(String noteContentUrl, String rev) {
        HttpGet request = new HttpGet(buildUrl(noteContentUrl));
        request.setHeader(HttpHeaderNames.IF_NONE_MATCH, rev);
        HttpResponse get = execute(request, null);
        Assert.equals(HttpStatus.SC_NOT_MODIFIED, get.getStatusLine().getStatusCode());
    }

    private void deleteNote(String noteUrl) {
        HttpResponse delete = delete(noteUrl);
        Assert.equals(HttpStatus.SC_NO_CONTENT, delete.getStatusLine().getStatusCode());
    }

    private void deleteNoteWithRevision(String noteUrl, long rev) {
        HttpRequestBase request = new HttpDelete(buildUrl(noteUrl));
        request.addHeader(HttpHeaderNames.IF_MATCH, String.valueOf(rev));
        HttpResponse delete = execute(request, null);
        Assert.equals(HttpStatus.SC_NO_CONTENT, delete.getStatusLine().getStatusCode());
    }

    private void deleteTag() {
        HttpResponse deleteTag = delete("/api/tags/104?__uid=1");
        Assert.equals(HttpStatus.SC_NO_CONTENT, deleteTag.getStatusLine().getStatusCode());

        HttpResponse deleteSysTag = delete("/api/tags/1?__uid=1");
        Assert.equals(HttpStatus.SC_BAD_REQUEST, deleteSysTag.getStatusLine().getStatusCode());
        getResult(deleteSysTag);

    }

    private void patchTag() throws JsonProcessingException {
        String tagPatch = new String(tagPatchSerializer.serializeJson(new TagPatch(Option.of("tag321"))));
        HttpResponse patchTag = patch("/api/tags/104?__uid=1", tagPatch);
        Assert.equals(HttpStatus.SC_OK, patchTag.getStatusLine().getStatusCode());
        Assert.assertContains(getResult(patchTag), "tag321");

    }

    private void listTags(String tagName) {
        HttpResponse getTags = get("/api/tags?__uid=1");
        String result = getResult(getTags);
        Assert.assertContains(result, tagName);
    }

    private void addTag() throws JsonProcessingException {
        String createTagJson = new String(tagBaseSerializer.serializeJson(new TagBase(TagType.TEXT, Option.of("tag123"))));
        HttpResponse createTag = put("/api/tags?__uid=1", createTagJson);
        Assert.equals(HttpStatus.SC_CREATED, createTag.getStatusLine().getStatusCode());
        String result = getResult(createTag);
        Assert.assertContains(result, "tag123");
    }

    private void patchNote(String noteUrl) throws IOException {

        Option<Instant> instants = Option.of(Instant.parse("2017-09-08T09:18:06.161Z"));
        NoteTag tag = NoteTag.builder().id(3L).mtime(instants).build();
        NotePatch notePatch = NotePatch.builder()
                .title(Option.of("newValue"))
                .mtime(instants)
                .addTags(Cf.set())
                .addTagsWithMeta(Cf.set(tag))
                .removeTags(Cf.set(1L))
                .snippet(Option.empty())
                .contentRevision(Option.empty())
                .build();

        HttpEntityEnclosingRequestBase patchRequest = new HttpPatch(buildUrl(noteUrl));
        patchRequest.setHeader("X-Request-Id", "1234");
        HttpResponse patch = execute(patchRequest, convert(new String(notePatchSerializer.serializeJson(notePatch))));

        Assert.equals(HttpStatus.SC_OK, patch.getStatusLine().getStatusCode());
        Note resultPatch = getResultNote(patch);
        Assert.assertEquals(resultPatch.title, "newValue");
        Assert.assertEquals(resultPatch.mtime, instants.get());
        Assert.assertEquals(resultPatch.tagsWithMeta.get(0), tag);

        HttpEntityEnclosingRequestBase patchRequestAgain = new HttpPatch(buildUrl(noteUrl));
        patchRequestAgain.setHeader("X-Request-Id", "1234");
        String newPatch = new String(notePatchSerializer.serializeJson(NotePatch.builder()
                .title(Option.of("newValue"))
                .mtime(Option.of(Instant.parse("2017-09-08T09:18:06.161Z")))
                .addTags(Cf.set())
                .addTagsWithMeta(Cf.set())
                .removeTags(Cf.set())
                .snippet(Option.empty())
                .contentRevision(Option.empty())
                .build()));
        HttpResponse patchAgain = execute(patchRequestAgain, convert(newPatch));
        Assert.equals(HttpStatus.SC_NO_CONTENT, patchAgain.getStatusLine().getStatusCode());

        HttpResponse getResponseAfterPatch = get(noteUrl);
        String result = getResult(getResponseAfterPatch);
        Assert.notNull(result);
        Assert.assertContains(result, "newValue");
    }

    private Note createNote(String data) throws IOException {
        HttpEntityEnclosingRequestBase request = new HttpPost(buildUrl("/api/notes?__uid=1"));
        request.setHeader("X-Request-Id", "123");
        HttpResponse create = execute(request, convert(data));
        Assert.equals(HttpStatus.SC_CREATED, create.getStatusLine().getStatusCode());
        String result = getResult(create);
        Assert.notNull(result);
        Note note = noteParser.parseJson(result);
        return note;
    }

    public void getContentAfterDelete(String noteContentUrl) {
        HttpResponse get = getRawContent(noteContentUrl);
        Assert.equals(HttpStatus.SC_NOT_FOUND, get.getStatusLine().getStatusCode());
        Assert.notNull(getResult(get));
    }
}
