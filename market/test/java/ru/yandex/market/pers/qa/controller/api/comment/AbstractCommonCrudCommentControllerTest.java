package ru.yandex.market.pers.qa.controller.api.comment;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.controller.dto.CommentResultDto;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.IS_REPLY_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.MAX_BATCH_SIZE;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PARENT_ID_KEY;

/**
 * @author varvara
 * 20.03.2019
 */
public abstract class AbstractCommonCrudCommentControllerTest extends AbstractCommonCommentControllerTest {
    protected abstract CommentProject getProject();

    protected long buildTestEntity() {
        return mvc.createEntity();
    }

    protected boolean canRemapId() {
        return false;
    }

    protected long remapId(long id) {
        return id;
    }

    @Test
    public void testCreateComment() throws Exception {
        long entityId = buildTestEntity();
        long projectId = getProject().getProjectId();

        String textComment1 = "это текст комментария первого уровня";
        String textComment2 = "это текст комментария второго уровня"; // child of comment1
        String textComment3 = "это текст еще одного комментария второго уровня"; // child of comment1
        String textComment4 = "это текст комментария третьего уровня"; // child of comment2

        CommentDto comment1 = createCommentDto(entityId, UID, null, getBody(textComment1));
        CommentDto comment2 = createCommentDto(entityId, UID, comment1.getId(), getBody(textComment2));
        CommentDto comment3 = createCommentDto(entityId, UID, comment1.getId(), getBody(textComment3));
        CommentDto comment4 = createCommentDto(entityId, UID, comment2.getId(), getBody(textComment4));

        long[] id = new long[]{
            0, // to start with 1
            comment1.getId(),
            comment2.getId(),
            comment3.getId(),
            comment4.getId(),
        };

        checkCommentDto(comment1, entityId, projectId, null, 0, 2, 3);
        checkCommentDto(comment2, entityId, projectId, id[1], 1, 1, 1, id[1]);
        checkCommentDto(comment3, entityId, projectId, id[1], 1, 0, 0, id[1]);
        checkCommentDto(comment4, entityId, projectId, id[2], 2, 0, 0, id[1], id[2]);

        checkEntityChildCount(entityId, projectId, 4, 1);
    }

    @Test
    public void testCreateDuplicateCommentAfterDeletedBySpamFilter() throws Exception {
        long entityId = buildTestEntity();
        final String body = getBody(UUID.randomUUID().toString());

        // создали коммент
        long commentId1 = createComment(entityId, UID, null, body);
        // удалили коммент модератором
        commentService.banCommentByManager(commentId1);
        // попытались создать еще один - но слишком рано и ничего не вышло
        final String response = mvc.createComment4xx(entityId, UID, body);
        Assertions.assertTrue(response.contains("Comment already exists"));

        final long delta = 5 * CommentService.MIN_WRITE_DELAY_SECONDS; // in seconds
        qaJdbcTemplate.update("update com.comment set upd_time = now() - make_interval(secs:=?) where id = ?",
            delta,
            commentId1);

        // создали еще коммент после долго молчания - все ок
        long commentId2 = createComment(entityId, UID, null, body);
    }

    @Test
    public void testCreateDuplicateCommentAfterDeletedByUser() throws Exception {
        long entityId = buildTestEntity();
        final String body = getBody(UUID.randomUUID().toString());

        // создали коммент
        long commentId1 = createComment(entityId, UID, null, body);
        // удалили коммент пользователем
        deleteComment(commentId1);
        // создали еще коммент - все ок
        long commentId2 = createComment(entityId, UID, null, body);
    }

    @Test
    public void testCreateDuplicateCommentWithDelta() throws Exception {
        long entityId = buildTestEntity();
        final String body = getBody(UUID.randomUUID().toString());

        // создали коммент
        long commentId1 = createComment(entityId, UID, null, body);

        long delta = 5 * CommentService.MIN_WRITE_DELAY_SECONDS; // in seconds
        qaJdbcTemplate.update("update com.comment set upd_time = now() - make_interval(secs:=?) where id = ?",
            delta,
            commentId1);
        // создали еще коммент после долго молчания - все ок
        long commentId2 = createComment(entityId, UID, null, body);

        // попытались создать еще один - но слишком рано и ничего не вышло

        String response = mvc.createComment4xx(entityId, UID, body);
        Assertions.assertTrue(response.contains("Comment already exists"));

        delta = CommentService.MIN_WRITE_DELAY_SECONDS - 120; // in seconds
        qaJdbcTemplate.update("update com.comment set upd_time = now() - make_interval(secs:=?) where id = ?",
            delta,
            commentId1);
        // создали еще коммент после долго молчания - все ок
        // попытались создать еще один - но слишком рано и ничего не вышло
        response = mvc.createComment4xx(entityId, UID, body);
        Assertions.assertTrue(response.contains("Comment already exists"));

        final Long count = qaJdbcTemplate.queryForObject("select count(*) from com.comment where user_id = ?",
            Long.class, String.valueOf(UID));
        assertNotNull(count);
        assertEquals(2, (long) count);
    }

    @Test
    void testCreateWithUserReply() throws Exception {
        long entityId = buildTestEntity();

        CommentDto parent = createComment(entityId, UID, getAnyBody(), x -> x);
        long parentId = parent.getId();

        CommentDto childWithReply = createComment(entityId, UID + 1, getAnyBody(), x ->
            x.param(PARENT_ID_KEY, String.valueOf(parentId))
                .param(IS_REPLY_KEY, "true"));
        long childWithReplyId = childWithReply.getId();

        CommentDto childWithoutReply = createComment(entityId, UID + 2, getAnyBody(), x ->
            x.param(PARENT_ID_KEY, String.valueOf(parentId))
                .param(IS_REPLY_KEY, "false"));
        long childWithoutReplyId = childWithoutReply.getId();

        CommentDto childWithoutReplyDef = createComment(entityId, UID + 3, getAnyBody(), x -> x);
        long childWithoutReplyDefId = childWithoutReplyDef.getId();

        // check dto after creation
        Map<Long, CommentDto> commentsMap = ListUtils.toMap(Arrays.asList(
            parent,
            childWithReply,
            childWithoutReply,
            childWithoutReplyDef
        ), CommentDto::getId);

        checkUserReplyCommentsMap(parentId, parent.getAuthor(),
            childWithReplyId, childWithoutReplyId, childWithoutReplyDefId, commentsMap);

        // check comments with getter
        List<CommentDto> comments = mvc.getAllComments(entityId);
        commentsMap = ListUtils.toMap(comments, CommentDto::getId);

        assertEquals(4, comments.size());
        checkUserReplyCommentsMap(parentId, parent.getAuthor(),
            childWithReplyId, childWithoutReplyId, childWithoutReplyDefId, commentsMap);
    }

    @Test
    void testCreateWithUserReplyToShopComment() throws Exception {
        long entityId = buildTestEntity();
        long shopId = 666999;

        long parentId = commentService.createShopComment(getProject(), UID, getAnyBody(), entityId, shopId, null);
        CommentDto parent = commentHelper.getCommentByIdFull(parentId, getProject(), UserInfo.uid(UID));

        CommentDto childWithReply = createComment(entityId, UID + 1, getAnyBody(), x ->
            x.param(PARENT_ID_KEY, String.valueOf(parentId))
                .param(IS_REPLY_KEY, "true"));
        long childWithReplyId = childWithReply.getId();

        CommentDto childWithoutReply = createComment(entityId, UID + 2, getAnyBody(), x ->
            x.param(PARENT_ID_KEY, String.valueOf(parentId))
                .param(IS_REPLY_KEY, "false"));
        long childWithoutReplyId = childWithoutReply.getId();

        CommentDto childWithoutReplyDef = createComment(entityId, UID + 3, getAnyBody(), x -> x);
        long childWithoutReplyDefId = childWithoutReplyDef.getId();

        // check dto after creation
        Map<Long, CommentDto> commentsMap = ListUtils.toMap(Arrays.asList(
            parent,
            childWithReply,
            childWithoutReply,
            childWithoutReplyDef
        ), CommentDto::getId);

        checkUserReplyCommentsMap(parentId, parent.getAuthor(),
            childWithReplyId, childWithoutReplyId, childWithoutReplyDefId, commentsMap);

        // check comments with getter
        List<CommentDto> comments = mvc.getAllComments(entityId);
        commentsMap = ListUtils.toMap(comments, CommentDto::getId);

        assertEquals(4, comments.size());
        checkUserReplyCommentsMap(parentId, parent.getAuthor(),
            childWithReplyId, childWithoutReplyId, childWithoutReplyDefId, commentsMap);
    }

    protected void checkUserReplyCommentsMap(long parentId,
                                             AuthorIdDto parentAuthor,
                                             long childWithReplyId,
                                             long childWithoutReplyId,
                                             long childWithoutReplyDefId,
                                             Map<Long, CommentDto> commentsMap) {
        CommentDto parentDto = commentsMap.get(parentId);
        assertAuthor(AuthorIdDto.USER, UID_STR, parentDto.getUser());
        assertAuthor(parentAuthor.getEntity(), parentAuthor.getId(), parentDto.getAuthor());
        assertNull(parentDto.getReplyTo());

        CommentDto childWithReplyDto = commentsMap.get(childWithReplyId);
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 1), childWithReplyDto.getUser());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 1), childWithReplyDto.getAuthor());
        assertNotNull(childWithReplyDto.getReplyTo());
        assertAuthor(parentAuthor.getEntity(), parentAuthor.getId(), childWithReplyDto.getReplyTo());

        CommentDto childWithoutReplyDto = commentsMap.get(childWithoutReplyId);
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 2), childWithoutReplyDto.getUser());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 2), childWithoutReplyDto.getAuthor());
        assertNull(childWithoutReplyDto.getReplyTo());

        CommentDto childWithoutReplyDefDto = commentsMap.get(childWithoutReplyDefId);
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 3), childWithoutReplyDefDto.getUser());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 3), childWithoutReplyDefDto.getAuthor());
        assertNull(childWithoutReplyDefDto.getReplyTo());
    }

    @Test
    void testEditOwnComment() throws Exception {
        long entityId = buildTestEntity();

        CommentDto comment = createComment(entityId, UID, getBody("test text"), x -> x);

        List<CommentDto> comments = mvc.getAllComments(entityId);
        assertEquals(1, comments.size());

        CommentDto commentDto = comments.get(0);
        assertEquals("test text", commentDto.getText());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getAuthor());
        assertNull(commentDto.getReplyTo());

        editComment(comment.getId(), UID,
            getBody("edited text (test)"),
            status().is2xxSuccessful(), x -> x);

        comments = mvc.getAllComments(entityId);
        assertEquals(1, comments.size());

        commentDto = comments.get(0);
        assertEquals("edited text (test)", commentDto.getText());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getAuthor());
        assertNull(commentDto.getReplyTo());
    }

    @Test
    void testEditSameText() throws Exception {
        long entityId = buildTestEntity();

        CommentDto comment = createComment(entityId, UID, getBody("test text"), x -> x);

        // edit comment - should work fine
        editComment(comment.getId(), UID,
            getBody("test text"),
            status().is2xxSuccessful(), x -> x);

        // check text is not changed
        List<CommentDto> comments = mvc.getAllComments(entityId);
        assertEquals(1, comments.size());

        CommentDto commentDto = comments.get(0);
        assertEquals("test text", commentDto.getText());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getAuthor());
        assertNull(commentDto.getReplyTo());
    }

    @Test
    void testEditOwnCommentWithReply() throws Exception {
        long entityId = buildTestEntity();

        CommentDto parent = createComment(entityId, UID + 1, getBody("test parent"), x -> x);
        CommentDto comment = createComment(entityId, UID, getBody("test text"), x -> x
            .param(PARENT_ID_KEY, String.valueOf(parent.getId()))
            .param(IS_REPLY_KEY, "true"));

        List<CommentDto> comments = mvc.getParentComments(entityId, parent.getId());
        assertEquals(1, comments.size());

        // check reply is ok
        CommentDto commentDto = comments.get(0);
        assertEquals("test text", commentDto.getText());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getUser());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getAuthor());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 1), commentDto.getReplyTo());

        // check reply removed after editing
        editComment(comment.getId(), UID,
            getBody("edited text (no reply)"),
            status().is2xxSuccessful(), x -> x
                .param(IS_REPLY_KEY, "false"));

        comments = mvc.getParentComments(entityId, parent.getId());
        assertEquals(1, comments.size());

        commentDto = comments.get(0);
        assertEquals("edited text (no reply)", commentDto.getText());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getUser());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getAuthor());
        assertNull(commentDto.getReplyTo());

        // check reply is back
        editComment(comment.getId(), UID,
            getBody("edited text (with reply again)"),
            status().is2xxSuccessful(), x -> x
                .param(IS_REPLY_KEY, "true"));

        comments = mvc.getParentComments(entityId, parent.getId());
        assertEquals(1, comments.size());

        commentDto = comments.get(0);
        assertEquals("edited text (with reply again)", commentDto.getText());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getUser());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getAuthor());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 1), commentDto.getReplyTo());

        // nothing changes on simple edit
        editComment(comment.getId(), UID,
            getBody("edited text (basic)"),
            status().is2xxSuccessful(), x -> x);

        comments = mvc.getParentComments(entityId, parent.getId());
        assertEquals(1, comments.size());

        commentDto = comments.get(0);
        assertEquals("edited text (basic)", commentDto.getText());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getUser());
        assertAuthor(AuthorIdDto.USER, UID_STR, commentDto.getAuthor());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 1), commentDto.getReplyTo());
    }

    @Test
    void testEditOtherComment() throws Exception {
        long entityId = buildTestEntity();

        CommentDto comment = createComment(entityId, UID, getBody("test text"), x -> x);

        String result = editComment(comment.getId(), UID + 1,
            getBody("edited text (test)"),
            status().is4xxClientError(), x -> x);

        assertTrue(result.contains("hasn't rights to edit comment"));
    }

    @Test
    void testEditVendorComment() throws Exception {
        long entityId = buildTestEntity();

        long commentId = commentService.createVendorComment(
            getProject(),
            UID,
            "text",
            entityId,
            123456
        );

        String result = editComment(commentId, UID,
            getBody("edited text (test)"),
            status().is4xxClientError(), x -> x);

        assertTrue(result.contains("hasn't rights to edit comment"));
    }

    @Test
    void testEditedFlag() throws Exception {
        long entityId = buildTestEntity();

        CommentDto comment = createComment(entityId, UID, getBody("test text"), x -> x);

        assertFalse(comment.isChanged());

        jdbcTemplate.update(
            "update com.comment\n" +
                "set upd_time = now() + interval '1' minute \n" +
                "where id = ?",
            comment.getId()
        );

        List<CommentDto> comments = mvc.getCommentsByUid(entityId, UID);
        assertEquals(1, comments.size());

        assertTrue(comments.get(0).isChanged());
    }

    @Test
    void testEditWithDuplicate() throws Exception {
        long entityId = buildTestEntity();

        CommentDto comment = createComment(entityId, UID, getBody("test text"), x -> x);
        CommentDto commentNext = createComment(entityId, UID, getBody("test text next"), x -> x);

        // try to edit with duplicate text
        String result = editComment(comment.getId(), UID,
            getBody("test text next"),
            status().is4xxClientError(), x -> x);

        assertTrue(result.contains("Comment already exists, please wait"));

        // change comment times
        jdbcTemplate.update(
            "update com.comment\n" +
                "set upd_time = now() - interval '10' day"
        );

        // should be fine
        editComment(comment.getId(), UID,
            getBody(commentNext.getText()),
            status().is2xxSuccessful(), x -> x);

        List<CommentDto> comments = mvc.getCommentsByUid(entityId, UID);
        assertEquals(2, comments.size());

        assertEquals(commentNext.getText(), comments.get(0).getText());
        assertEquals(commentNext.getText(), comments.get(1).getText());
    }

    @Test
    public void testLimitInvalid4xx() throws Exception {
        final String text = UUID.randomUUID().toString();
        long commentId = createComment((long) ENTITY_ID, UID, null, getBody(text));
        String response = mvc.getCommentsResponse4xx(ENTITY_ID, UID, commentId - 1, 0);
        response = mvc.getCommentsResponse4xx(ENTITY_ID, UID, commentId - 1, -10);
    }

    @Test
    public void testNoLimitOk() throws Exception {
        final String text = UUID.randomUUID().toString();
        long commentId = createComment((long) ENTITY_ID, UID, null, getBody(text));

        final String response = mvc.getCommentsResponseNoLimit(ENTITY_ID, UID, commentId - 1);
    }

    @Test
    public void testCommentFormat() throws Exception {
        final long entityId = ENTITY_ID;
        final String text = UUID.randomUUID().toString();

        List<CommentDto> comments = mvc.getCommentsByUid(entityId, UID);
        assertEquals(0, comments.size());

        long commentId = createComment(entityId, UID, null, getBody(text));
        long updateTime = qaJdbcTemplate
            .queryForObject("select upd_time from com.comment where id = ?", Timestamp.class, commentId).getTime();

        final String response = mvc.getCommentsResponseForFormat(entityId, UID);

        String body = String.format(
            "{ \"data\" : [" +
                "{\"text\" : \"%s\",\n" +
                "\"id\":%s,\n" +
                "\"entityId\":%s,\n" +
                "\"state\":\"%s\",\n" +
                "\"updateTime\":%s,\n" +
                "\"author\": {\"entity\":\"user\", \"id\":\"%s\"},\n" +
                "\"changed\":false," +
                "\"entity\":\"commentary\"," +
                "\"firstLevelChildCount\":0}\n" +
                "],\n" +
                "\"tree\": {\"0\" : [%s]}\n" +
                "}",
            text, commentId, entityId, CommentState.NEW.getName(), updateTime, UID, commentId);

        JSONAssert.assertEquals(body, response, false);
    }

    @Test
    public void testDeleteCommentByUser() throws Exception {
        testDeleteComment(CommentState.DELETED, (id) -> {
            try {
                deleteComment(id);
            } catch (Exception e) {
                System.out.println("Can't delete comment");
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testDeleteCommentByManager() throws Exception {
        testDeleteComment(CommentState.BANNED, (id) -> commentService.banCommentByManager(id));
    }

    @Test
    public void testDeleteCommentByAutoFilters() throws Exception {
        testDeleteComment(CommentState.BANNED,
            (id) -> commentService.banCommentsByRegExp(Collections.singletonList(id)));
    }

    private void testDeleteComment(CommentState expectedState,
                                   Consumer<Long> deleteAction) throws Exception {
        long entityId = buildTestEntity();

        long comment1 = createComment(entityId, UID, null, getAnyBody());
        long comment2 = createComment(entityId, UID, comment1, getAnyBody());

        mvc.deleteComment4xx(comment1, UID + 1);
        List<CommentDto> comments = mvc.getCommentsByYandexUid(entityId, YANDEXUID);
        assertEquals(1, comments.size());
        assertEquals(CommentState.NEW.getName(), comments.get(0).getState());

        deleteAction.accept(comment1);
        comments = mvc.getCommentsByYandexUid(entityId, YANDEXUID);
        // because still has non-deleted child
        assertEquals(1, comments.size());
        assertEquals(expectedState.getName(), comments.get(0).getState());

        deleteAction.accept(comment2);
        comments = mvc.getCommentsByYandexUid(entityId, YANDEXUID);
        assertEquals(0, comments.size());
    }

    @Test
    public void testDeleteCommentOnDiffLevels() throws Exception {
        long entityId = buildTestEntity();
        long projectId = getProject().getProjectId();

        String textComment1 = "это текст комментария первого уровня";
        String textComment2 = "это текст комментария второго уровня"; // child of comment1
        String textComment3 = "это текст еще одного комментария второго уровня"; // child of comment1
        String textComment4 = "это текст комментария третьего уровня"; // child of comment2

        CommentDto comment1 = createCommentDto(entityId, UID, null, getBody(textComment1));
        CommentDto comment2 = createCommentDto(entityId, UID, comment1.getId(), getBody(textComment2));
        CommentDto comment3 = createCommentDto(entityId, UID, comment1.getId(), getBody(textComment3));
        CommentDto comment4 = createCommentDto(entityId, UID, comment2.getId(), getBody(textComment4));

        long[] c = {0,
            comment1.getId(),
            comment2.getId(),
            comment3.getId(),
            comment4.getId(),
        };

        long levelComment1 = 0;
        long firstLevelComment1 = 2;
        long childContComment1 = 3;

        long levelComment2 = 1;
        long firstLevelComment2 = 1;
        long childContComment2 = 1;

        long levelComment3 = 1;
        long firstLevelComment3 = 0;
        long childContComment3 = 0;

        long levelComment4 = 2;
        long firstLevelComment4 = 0;
        long childContComment4 = 0;

        checkCommentDto(comment1, entityId, projectId, null, levelComment1, firstLevelComment1, childContComment1);
        checkCommentDto(comment2,
            entityId,
            projectId,
            c[1],
            levelComment2,
            firstLevelComment2,
            childContComment2,
            c[1]);
        checkCommentDto(comment3,
            entityId,
            projectId,
            c[1],
            levelComment3,
            firstLevelComment3,
            childContComment3,
            c[1]);
        checkCommentDto(comment4,
            entityId,
            projectId,
            c[2],
            levelComment4,
            firstLevelComment4,
            childContComment4,
            c[1],
            c[2]);
        checkEntityChildCount(entityId, projectId, 4, 1);

        deleteComment(c[3]);
        firstLevelComment1--;
        childContComment1--;
        checkCountAndParents(comment1, firstLevelComment1, childContComment1);
        checkCountAndParents(comment2, firstLevelComment2, childContComment2, c[1]);
        checkCountAndParents(comment4, firstLevelComment4, childContComment4, c[1], c[2]);
        checkEntityChildCount(entityId, projectId, 3, 1);

        deleteComment(c[4]);
        childContComment1--;
        firstLevelComment2--;
        childContComment2--;
        checkCountAndParents(comment1, firstLevelComment1, childContComment1);
        checkCountAndParents(comment2, firstLevelComment2, childContComment2, c[1]);
        checkEntityChildCount(entityId, projectId, 2, 1);

        deleteComment(c[1]);
        checkCountAndParents(comment2, firstLevelComment2, childContComment2, c[1]);
        checkEntityChildCount(entityId, projectId, 1, 0);
    }

    @Test
    public void testGetCommentsByUid() throws Exception {
        checkGetComments(false);
    }

    @Test
    public void testGetCommentsByYandexUid() throws Exception {
        checkGetComments(true);
    }

    @Test
    public void testBulkCommentsCountMax() throws Exception {
        mvc.getCommentsBulkCountWithoutMapping(LongStream.range(0, MAX_BATCH_SIZE + 1).toArray(),
            status().isBadRequest());
    }

    @Test
    public void testBulkCommentsCount() throws Exception {
        createComment(ANOTHER_ENTITY_ID, UID, null, getAnyBody());
        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID, comment1, getAnyBody());
        createComment(ENTITY_ID, UID, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID, comment2, getAnyBody());
        long comment5 = createComment(ENTITY_ID, UID, comment4, getAnyBody());
        deleteComment(comment2);
        deleteComment(comment5);
        Map<Long, CountDto> countMap = mvc
            .getCommentsBulkCount(new long[]{ENTITY_ID, ANOTHER_ENTITY_ID, FAKE_ENTITY_ID},
                status().is2xxSuccessful());
        Assertions.assertEquals(3, countMap.get(ENTITY_ID).getCount());
        Assertions.assertEquals(1, countMap.get(ANOTHER_ENTITY_ID).getCount());
        Assertions.assertEquals(0, countMap.get(FAKE_ENTITY_ID).getCount());

        // should fail in empty set
        mvc.getCommentsBulkCountWithoutMapping(new long[]{}, status().is4xxClientError());
    }

    //     c1       c6
//    |  \
//    c2  c3
//    |
//    c4
//    |
//    c5
    private void checkGetComments(boolean isYandexUid) throws Exception {
        final Function<Long, CommentResultDto> getComments = (parentId) -> {
            try {
                if (isYandexUid) {
                    return mvc.getResponseDtoByYandexUid(ENTITY_ID, YANDEXUID, parentId);
                } else {
                    return mvc.getResponseDtoByUid(ENTITY_ID, UID, parentId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new CommentResultDto();
        };

        CommentResultDto dto;
        dto = mvc.getResponseDtoByUid(ENTITY_ID, UID);
        assertEquals(0, dto.getData().size());
        assertEquals(2, dto.getTree().size());
        assertTrue(dto.getTree().get("0").isEmpty());

        dto = mvc.getResponseDtoByYandexUid(ENTITY_ID, YANDEXUID);
        assertEquals(0, dto.getData().size());
        assertEquals(2, dto.getTree().size());
        assertTrue(dto.getTree().get("0").isEmpty());

        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID + 1, comment1, getAnyBody());
        long comment3 = createComment(ENTITY_ID, UID + 2, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID + 3, comment2, getAnyBody());
        long comment5 = createComment(ENTITY_ID, UID + 4, comment4, getAnyBody());
        long comment6 = createComment(ENTITY_ID, UID + 5, null, getAnyBody());

        setUpdateTime(comment1, comment2, comment3, comment4, comment5, comment6);

        // 1th level for entity: comment1, comment6
        dto = getComments.apply(null);
        checkData(dto, comment1, comment6);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment6);

        // 1th level for comment1: comment2, comment3
        dto = getComments.apply(comment1);
        checkData(dto, comment2, comment3);
        checkBranch(dto, comment1, COMMENT_CMP, comment2, comment3);

        // 1th level for comment2 : comment4
        dto = getComments.apply(comment2);
        checkData(dto, comment4);
        checkBranch(dto, comment2, COMMENT_CMP, comment4);

        // 1th level for comment3 : none
        dto = getComments.apply(comment3);
        checkData(dto);

        // 1th level for comment4 : comment5
        dto = getComments.apply(comment4);
        checkData(dto, comment5);
        checkBranch(dto, comment4, COMMENT_CMP, comment5);

        // 1th level for comment6 : none
        dto = getComments.apply(comment6);
        checkData(dto);
    }

    @Test
    void testCreateWithVendorReply() throws Exception {
        long entityId = buildTestEntity();

        long parentId = createComment(entityId, UID, getAnyBody(), x -> x).getId();

        // make comment to be from vendor
        String vendorId = "221415";
        jdbcTemplate.update(
            "update com.comment\n" +
                "set brand_id = ?::bigint,\n" +
                "    text = ?\n" +
                "where id = ?",
            vendorId,
            "Some text",
            parentId
        );
        CommentDto parent = mvc.getCommentsByUid(entityId, UID).get(0);

        CommentDto childWithReply = createComment(entityId, UID + 1, getAnyBody(), x ->
            x.param(PARENT_ID_KEY, String.valueOf(parentId))
                .param(IS_REPLY_KEY, "true"));
        long childWithReplyId = childWithReply.getId();

        CommentDto childWithoutReply = createComment(entityId, UID + 2, getAnyBody(), x ->
            x.param(PARENT_ID_KEY, String.valueOf(parentId))
                .param(IS_REPLY_KEY, "false"));
        long childWithoutReplyId = childWithoutReply.getId();

        CommentDto childWithoutReplyDef = createComment(entityId, UID + 3, getAnyBody(), x -> x);
        long childWithoutReplyDefId = childWithoutReplyDef.getId();

        // check dto after creation
        Map<Long, CommentDto> commentsMap = ListUtils.toMap(Arrays.asList(
            parent,
            childWithReply,
            childWithoutReply,
            childWithoutReplyDef
        ), CommentDto::getId);

        checkVendorReply(vendorId,
            parentId,
            childWithReplyId,
            childWithoutReplyId,
            childWithoutReplyDefId,
            commentsMap);

        List<CommentDto> comments = mvc.getAllComments(entityId);
        commentsMap = ListUtils.toMap(comments, CommentDto::getId);

        assertEquals(4, comments.size());

        checkVendorReply(vendorId,
            parentId,
            childWithReplyId,
            childWithoutReplyId,
            childWithoutReplyDefId,
            commentsMap);
    }

    private void checkVendorReply(String vendorId,
                                  long parentId,
                                  long childWithReplyId,
                                  long childWithoutReplyId,
                                  long childWithoutReplyDefId,
                                  Map<Long, CommentDto> commentsMap) {
        CommentDto parentDto = commentsMap.get(parentId);
        assertAuthor(AuthorIdDto.USER, UID_STR, parentDto.getUser());
        assertAuthor(AuthorIdDto.VENDOR, vendorId, parentDto.getAuthor());
        assertNull(parentDto.getReplyTo());

        CommentDto childWithReplyDto = commentsMap.get(childWithReplyId);
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 1), childWithReplyDto.getUser());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 1), childWithReplyDto.getAuthor());
        assertNotNull(childWithReplyDto.getReplyTo());
        assertAuthor(AuthorIdDto.VENDOR, vendorId, childWithReplyDto.getReplyTo());

        CommentDto childWithoutReplyDto = commentsMap.get(childWithoutReplyId);
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 2), childWithoutReplyDto.getUser());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 2), childWithoutReplyDto.getAuthor());
        assertNull(childWithoutReplyDto.getReplyTo());

        CommentDto childWithoutReplyDefDto = commentsMap.get(childWithoutReplyDefId);
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 3), childWithoutReplyDefDto.getUser());
        assertAuthor(AuthorIdDto.USER, String.valueOf(UID + 3), childWithoutReplyDefDto.getAuthor());
        assertNull(childWithoutReplyDefDto.getReplyTo());
    }

    protected void checkEntityChildCount(long entityId,
                                         long projectId,
                                         long childCount,
                                         long firstLevelCount) throws Exception {
        checkEntityChildCountExactInDb(remapId(entityId), projectId, childCount, firstLevelCount);
    }

    protected void checkCommentDto(CommentDto comment,
                                   long entityId,
                                   long projectId,
                                   Long parentId,
                                   long level,
                                   long firstLevelChildCount,
                                   long childCount,
                                   Long... parents) {
        super.checkCommentDto(comment, entityId, projectId, parentId, level, firstLevelChildCount, childCount, parents);
        if (!canRemapId()) {
            long id = comment.getId();
            checkCommentInDb(id, entityId, projectId, parentId, level, firstLevelChildCount, childCount, parents);
        }
    }

}
