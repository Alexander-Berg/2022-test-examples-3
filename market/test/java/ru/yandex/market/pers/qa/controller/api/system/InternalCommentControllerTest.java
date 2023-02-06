package ru.yandex.market.pers.qa.controller.api.system;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.model.CommentParam;
import ru.yandex.market.pers.qa.model.CommentStatus;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.GradeCommentService;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.PersQaServiceMockFactory.mockGradeSimple;
import static ru.yandex.market.pers.qa.PersQaServiceMockFactory.mockGradeWithFixId;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.COMMENT_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.COMMENT_TYPE_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.ROOT_ID_KEY;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertComments;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkComment;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkRoot;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkText;

class InternalCommentControllerTest extends QAControllerTest {

    private static final long USER_ID = 1234;
    private static final long ROOT_ID = 13324535;
    private static final CommentProject PROJECT = CommentProject.ARTICLE;

    @Autowired
    private GradeCommentService gradeCommentService;
    @Autowired
    private GradeClient gradeClient;

    @Test
    public void testGetCommentsByUid() throws Exception {
        long userId = USER_ID;
        long commentId1 = commentService.createComment(CommentProject.ARTICLE, userId, UUID.randomUUID().toString(), ROOT_ID);
        long commentId2 = commentService.createComment(CommentProject.GRADE, userId, UUID.randomUUID().toString(), ROOT_ID + 1);
        final long questionId = createQuestion();
        final long answerId = createAnswer(questionId);
        long commentId3 = commentService.createComment(CommentProject.QA, userId, UUID.randomUUID().toString(), answerId);

        QAPager<CommentDto> comments = getInternalComments(String.format("/internal/comment/get/user/%s?pageNum=%s&pageSize=%s&sortField=%s&asc=%s",
            userId, 1, 10, "ID", "true"));
        assertEquals(3, comments.getPager().getCount());
        assertEquals(3, comments.getData().size());
        assertContains(comments, commentId1, commentId2, commentId3);
    }

    @Test
    public void testGetCommentsPaging() throws Exception {
        long userId = USER_ID;
        long commentId1 = commentService.createComment(PROJECT, userId++, UUID.randomUUID().toString(), ROOT_ID);
        long commentId2 = commentService.createComment(PROJECT, userId++, UUID.randomUUID().toString(), ROOT_ID);
        long commentId3 = commentService.createComment(PROJECT, userId++, UUID.randomUUID().toString(), ROOT_ID);
        long commentId4 = commentService.createComment(PROJECT, userId++, UUID.randomUUID().toString(), ROOT_ID + 1);
        long commentId5 = commentService.createComment(PROJECT, userId++, UUID.randomUUID().toString(), ROOT_ID + 2);

        QAPager<CommentDto> comments;
        comments = getInternalComments(PROJECT, 1, 2, "ID", "true", ROOT_ID);
        assertEquals(3, comments.getPager().getCount());
        assertEquals(2, comments.getData().size());
        assertContains(comments, commentId1, commentId2);

        comments = getInternalComments(PROJECT, 2, 2, "ID", "true", ROOT_ID);
        assertEquals(3, comments.getPager().getCount());
        assertEquals(1, comments.getData().size());
        assertContains(comments, commentId3);

        comments = getInternalComments(PROJECT, 1, 2, "ID", "true", ROOT_ID, ROOT_ID + 1);
        assertEquals(4, comments.getPager().getCount());
        assertEquals(2, comments.getData().size());
        assertContains(comments, commentId1, commentId2);

        comments = getInternalComments(PROJECT, 2, 2, "ID", "true", ROOT_ID, ROOT_ID + 1);
        assertEquals(4, comments.getPager().getCount());
        assertEquals(2, comments.getData().size());
        assertContains(comments, commentId3, commentId4);

        comments = getInternalComments(PROJECT, 1, 10, "ID", "true", ROOT_ID, ROOT_ID + 1, ROOT_ID + 2);
        assertEquals(5, comments.getPager().getCount());
        assertEquals(5, comments.getData().size());
        assertContains(comments, commentId1, commentId2, commentId3, commentId4, commentId5);
    }

    @Test
    public void testGetCommentsFixId() throws Exception {
        long shopId = 9;
        long[] gradeIds = {1, 2};

        // first grade without fix_id, second with fix_id=gradeIds[0]
        mockGradeSimple(gradeClient, gradeIds[0], UID, GradeType.MODEL_GRADE, shopId);
        mockGradeWithFixId(gradeClient, gradeIds[1], UID, GradeType.MODEL_GRADE, shopId, gradeIds[0]);

        long commentId1 = commentService.createComment(CommentProject.GRADE, USER_ID, "first comment", gradeIds[0]);
        long commentId2 = commentService.createComment(CommentProject.GRADE, USER_ID+1, "second comment", gradeIds[1]);

        QAPager<CommentDto> comments = getInternalComments(CommentProject.GRADE, 1, 10, "ID", "true", gradeIds[0], gradeIds[1]);

        // check result
        assertComments(comments.getData(),
            checkComment(commentId1,
                checkRoot(GRADE, gradeIds[1]),
                checkText("first comment")
            ),
            checkComment(commentId2,
                checkRoot(GRADE, gradeIds[1]),
                checkText("second comment")
            )
        );
    }

    @Test
    public void testGetCommentsByVendorAndShop() throws Exception {
        long shopId = 100500;
        long vendorId = 666;
        long userId = USER_ID;
        long userIdShop = userId++;
        long userIdVendor = userId++;
        long userIdHuman = userId++;
        CommentProject project = CommentProject.QA;

        long questionId = createQuestion();
        long answerId = createAnswer(questionId, userId, "test");

        long commentIdShop = commentService.createShopComment(project, userIdShop, UUID.randomUUID().toString(), answerId, shopId);
        long commentIdVendor = commentService.createVendorComment(project, userIdVendor, UUID.randomUUID().toString(), answerId, vendorId);
        long commentIdHuman = commentService.createComment(project, userIdHuman, UUID.randomUUID().toString(), answerId);

        QAPager<CommentDto> comments;
        comments = getInternalComments(project, 1, 10, "ID", "true", answerId);
        assertEquals(3, comments.getPager().getCount());
        assertEquals(3, comments.getData().size());
        assertContains(comments, commentIdShop, commentIdVendor, commentIdHuman);

        Map<Long, AuthorIdDto> commentsMap = comments.getData().stream().collect(Collectors.toMap(
            CommentDto::getId,
            CommentDto::getAuthor
        ));

        assertEquals(AuthorIdDto.SHOP, commentsMap.get(commentIdShop).getEntity());
        assertEquals(String.valueOf(shopId), commentsMap.get(commentIdShop).getId());

        assertEquals(AuthorIdDto.VENDOR, commentsMap.get(commentIdVendor).getEntity());
        assertEquals(String.valueOf(vendorId), commentsMap.get(commentIdVendor).getId());

        assertEquals(AuthorIdDto.USER, commentsMap.get(commentIdHuman).getEntity());
        assertEquals(String.valueOf(userIdHuman), commentsMap.get(commentIdHuman).getId());
    }

    @Test
    public void testDeleteExistingComment() throws Exception {
        long commentId = 100500;

        invokeAndRetrieveResponse(delete("/internal/comment/delete/" + commentId), status().is4xxClientError());
    }

    @Test
    public void testRestoreNotExistingComment() throws Exception {
        long commentId = 100500;

        invokeAndRetrieveResponse(post("/internal/comment/restore/" + commentId), status().is4xxClientError());
    }

    @Test
    public void testChangePropertiesForNotExistingComment() throws Exception {
        long commentId = 100500;
        String body = "{ \"properties\" : [\n" +
                "{\"name\":\"prop_name\",\"value\":\"prop_value\"}\n" +
                "\n]\n}";

        invokeAndRetrieveResponse(post("/internal/comment/change/" + commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body), status().is2xxSuccessful());
    }

    @Test
    public void testDeleteAndRestoreComment() throws Exception {
        long commentId = commentService.createComment(PROJECT, USER_ID, UUID.randomUUID().toString(), ROOT_ID);

        invokeAndRetrieveResponse(delete("/internal/comment/delete/" + commentId), status().is2xxSuccessful());
        assertEquals(CommentStatus.REJECTED_BY_MANAGER, getModStateField(commentId));

        invokeAndRetrieveResponse(delete("/internal/comment/delete/" + commentId), status().is2xxSuccessful());
        assertEquals(CommentStatus.REJECTED_BY_MANAGER, getModStateField(commentId));

        invokeAndRetrieveResponse(post("/internal/comment/restore/" + commentId), status().is2xxSuccessful());
        assertEquals(CommentStatus.APPROVED, getModStateField(commentId));

        invokeAndRetrieveResponse(post("/internal/comment/restore/" + commentId), status().is2xxSuccessful());
        assertEquals(CommentStatus.APPROVED, getModStateField(commentId));
    }

    @Test
    public void testChangeCommentProperties() throws Exception {
        String existingPropName = "prop_name_0";
        String existingPropValue = "0113145";

        String changedPropName = "prop_name_1";
        String changedPropDefValue = "0";
        String changedPropValue = "1";

        String createdPropName = "prop_name_2";
        String createdPropValue = "ololo";

        long commentId = commentService.createComment(PROJECT, USER_ID, UUID.randomUUID().toString(), ROOT_ID,
            new CommentParam(changedPropName, changedPropDefValue),
            new CommentParam(existingPropName, existingPropValue));

        assertEquals(2, getPropCount(commentId));
        checkExistProperty(commentId, existingPropName, existingPropValue);
        checkExistProperty(commentId, changedPropName, changedPropDefValue);

        String body = String.format(
            "{ \"properties\" : [\n" +
                "{\"name\":\"%s\",\"value\":\"%s\"},\n" +
                "{\"name\":\"%s\", \"value\":\"%s\"}" +
                "\n]\n}",
            changedPropName, changedPropValue, createdPropName, createdPropValue);

        invokeAndRetrieveResponse(post("/internal/comment/change/" + commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body), status().is2xxSuccessful());

        assertEquals(3, getPropCount(commentId));
        checkExistProperty(commentId, existingPropName, existingPropValue);
        checkExistProperty(commentId, changedPropName, changedPropValue);
        checkExistProperty(commentId, createdPropName, createdPropValue);
    }

    @Test
    public void testGetCommentById() throws Exception {
        final String commentText = UUID.randomUUID().toString();
        long commentId = commentService.createComment(PROJECT, USER_ID, commentText, ROOT_ID,
            new CommentParam("prop_name_0", "0113145"));

        long updateTime = qaJdbcTemplate.queryForObject(
            "select upd_time from com.comment where id = ?", Timestamp.class, commentId
        ).getTime();

        String body = String.format(
            "{\"text\" : \"%s\",\n" +
                "\"id\":%s,\n" +
                "\"entityId\":%s,\n" +
                "\"state\":\"%s\",\n" +
                "\"updateTime\":%s,\n" +
                "\"author\": {\"entity\":\"user\", \"id\":\"%s\"},\n" +
                "\"changed\":false," +
                "\"entity\":\"commentary\"," +
                "\"firstLevelChildCount\":0}\n",
        commentText, commentId, ROOT_ID, CommentState.NEW.getName(), updateTime, USER_ID);

        final String response = getCommentById(commentId);

        JSONAssert.assertEquals(body, response, false);
    }

    private String getCommentById(long commentId) throws Exception {
        return invokeAndRetrieveResponse(get("/internal/comment/get/" + commentId)
            .contentType(MediaType.APPLICATION_JSON), status().is2xxSuccessful());
    }

    @Test
    public void testGetCommentBulk() throws Exception {
        long[] rootIds = {ROOT_ID, ROOT_ID + 1};
        long vendorId = 1;
        long shopId = 2;

        final String commentText = UUID.randomUUID().toString();
        long commentId = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[0]);
        long commentId2 = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[1]);

        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION, true);
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_SHOP, true);
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_VENDOR, true);

        long commentIdShop = commentService.createShopComment(PROJECT, USER_ID+1, commentText, rootIds[0], shopId);
        long commentIdVendor = commentService.createVendorComment(PROJECT, USER_ID+1, commentText, rootIds[0], vendorId);
        long commentIdUserPremod = commentService.createComment(PROJECT, USER_ID+1, commentText, rootIds[0]);

        String[] allCommentsToRequest = Stream.of(
                commentId, commentId2,
                commentIdShop, commentIdVendor, commentIdUserPremod,
                -1)
            .map(Object::toString)
            .toArray(String[]::new);

        DtoList<CommentDto> result = objectMapper.readValue(
            invokeAndRetrieveResponse(get("/internal/comment/get/bulk")
                .param(COMMENT_TYPE_KEY, String.valueOf(PROJECT.getId()))
                .param(COMMENT_ID_KEY, allCommentsToRequest)
                .contentType(MediaType.APPLICATION_JSON), status().is2xxSuccessful()),
            new TypeReference<DtoList<CommentDto>>() {
            });

        assertEquals(Set.of(commentId, commentId2, commentIdShop, commentIdVendor, commentIdUserPremod),
            ListUtils.toSet(result.getData(), CommentDto::getId));

        for (CommentDto item : result.getData()) {
            if (item.getAuthor().getUserType() == UserType.UID) {
                continue;
            }

            assertTrue(item.isPublished());
        }
    }

    @Test
    public void testGetLastCommentBulk() throws Exception {
        long[] rootIds = {ROOT_ID, ROOT_ID + 1};

        final String commentText = UUID.randomUUID().toString();
        long commentId = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[0],
            new CommentParam("prop_name_0", "0113145"));
        long commentId2 = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[1],
            new CommentParam("prop_name_0", "0113145"));
        long commentId3 = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[1],
            new CommentParam("prop_name_0", "0113145"));

        commentService.deleteComment(PROJECT, commentId3, UserInfo.uid(USER_ID));

        DtoList<CommentDto> result = getLastCommentsBulk(rootIds);

        assertTrue(ListUtils.toList(result.getData(), CommentDto::getId)
            .containsAll(Arrays.asList(commentId, commentId2)));
        assertEquals(2, result.getData().size());
    }


    @Test
    public void testGetLastCommentBulkPremod() throws Exception {
        long[] rootIds = {ROOT_ID, ROOT_ID + 1, ROOT_ID+2};
        long vendorId = 1;
        long shopId = 2;

        final String commentText = UUID.randomUUID().toString();

        long commentId = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[0]);
        long commentId2 = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[1]);
        long commentId3 = commentService.createComment(PROJECT, USER_ID, commentText, rootIds[2]);

        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION, true);
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_SHOP, true);
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_VENDOR, true);

        long commentIdShop = commentService.createShopComment(PROJECT, USER_ID+1, commentText, rootIds[0], shopId);
        long commentId2Vendor = commentService.createVendorComment(PROJECT, USER_ID+1, commentText, rootIds[1], vendorId);
        long commentId3User = commentService.createComment(PROJECT, USER_ID+1, commentText, rootIds[2]);

        DtoList<CommentDto> result = getLastCommentsBulk(rootIds);

        assertEquals(Set.of(commentIdShop, commentId2Vendor, commentId3),
            ListUtils.toSet(result.getData(), CommentDto::getId));
        assertEquals(3, result.getData().size());

        // all items are published
        for (CommentDto item : result.getData()) {
            assertTrue(item.isPublished());
        }

    }

    private DtoList<CommentDto> getLastCommentsBulk(long[] rootIds) throws Exception {
        return objectMapper.readValue(
            invokeAndRetrieveResponse(get("/internal/comment/get/last/bulk")
                .param(COMMENT_TYPE_KEY, String.valueOf(PROJECT.getId()))
                .param(ROOT_ID_KEY, Arrays.stream(rootIds).mapToObj(String::valueOf).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON), status().is2xxSuccessful()),
            new TypeReference<DtoList<CommentDto>>() {
            });
    }

    @Test
    public void testGetNotExistingCommentByid() throws Exception {
        long commentId = System.currentTimeMillis();

        invokeAndRetrieveResponse(get("/internal/comment/get/" + commentId)
            .contentType(MediaType.APPLICATION_JSON), status().is4xxClientError());
    }

    @Test
    public void testDeleteGradeComment() throws Exception {
        int batchSize = 10;

        long commentId1 = commentService.createComment(CommentProject.GRADE, USER_ID, UUID.randomUUID().toString(), ROOT_ID);
        long commentId2 = commentService.createComment(CommentProject.GRADE, USER_ID + 1, UUID.randomUUID().toString(), ROOT_ID);

        // 2 grades created
        assertEquals(2, gradeCommentService.getCommentsToSynch(batchSize).size());
        gradeCommentService.cleanSignalsAfterSynch();

        // expect no signals here
        assertEquals(0, gradeCommentService.getCommentsToSynch(batchSize).size());

        // delete last comment
        invokeAndRetrieveResponse(delete("/internal/comment/delete/" + commentId2), status().is2xxSuccessful());
        assertEquals(CommentStatus.REJECTED_BY_MANAGER, getModStateField(commentId2));
        assertEquals(1, gradeCommentService.getCommentsToSynch(batchSize).size());
        assertEquals(commentId2, gradeCommentService.getCommentsToSynch(batchSize).get(0).longValue());
        gradeCommentService.cleanSignalsAfterSynch();

        // expeact no signals here
        assertEquals(0, gradeCommentService.getCommentsToSynch(batchSize).size());

        // delete first comment
        invokeAndRetrieveResponse(delete("/internal/comment/delete/" + commentId1), status().is2xxSuccessful());
        assertEquals(CommentStatus.REJECTED_BY_MANAGER, getModStateField(commentId1));
        assertEquals(1, gradeCommentService.getCommentsToSynch(batchSize).size());
        assertEquals(commentId1, gradeCommentService.getCommentsToSynch(batchSize).get(0).longValue());
        gradeCommentService.cleanSignalsAfterSynch();

        // expect no signals here
        assertEquals(0, gradeCommentService.getCommentsToSynch(batchSize).size());

        // try to delete comment again - should generate no signals
        invokeAndRetrieveResponse(delete("/internal/comment/delete/" + commentId1), status().is2xxSuccessful());
        assertEquals(0, gradeCommentService.getCommentsToSynch(batchSize).size());
    }

    private CommentStatus getModStateField(long commentId) {
        return Optional.ofNullable(qaJdbcTemplate.queryForObject(
            "SELECT mod_state FROM com.comment where id = ?\n",
            Integer.class, commentId))
            .map(CommentStatus::getById)
            .orElse(CommentStatus.READY);
    }

    private int getPropCount(long commentId) {
        return Optional.ofNullable(qaJdbcTemplate.queryForObject(
            "SELECT count(*) FROM com.property where comment_id = ?",
            Integer.class, commentId)).orElse(0);
    }

    private void checkExistProperty(long commentId, String propName, String propValue) {
        Integer createdProp = qaJdbcTemplate.queryForObject(
            "SELECT count(*) FROM com.property where comment_id = ? and name = ? and value = ?",
            Integer.class, commentId, propName, propValue);
        assertEquals(1, (int) createdProp);
    }

    private QAPager<CommentDto> getInternalComments(String url) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get(url).contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()),
            new TypeReference<QAPager<CommentDto>>() {
            });
    }

    private QAPager<CommentDto> getInternalComments(CommentProject project,
                                                    int pageNum, int pageSize,
                                                    String sortFiled, String ascSort,
                                                    Long... rootIds) throws Exception {
        String rootIdsStr = Stream.of(rootIds).map(it -> "rootId=" + it).collect(Collectors.joining("&"));
        return getInternalComments(String.format("/internal/comment/get?commentType=%s&pageNum=%s&pageSize=%s&sortField=%s&asc=%s&%s",
            project.getProjectId(), pageNum, pageSize, sortFiled, ascSort, rootIdsStr));
    }

    private void assertContains(QAPager<CommentDto> comments, Long... commentDtos) {
        final List<Long> expected = Arrays.asList(commentDtos);
        final List<Long> commentIds = comments.getData().stream().map(CommentDto::getId).collect(Collectors.toList());
        assertTrue(commentIds.containsAll(expected),
            commentIds + " contains " + expected);

    }

}
