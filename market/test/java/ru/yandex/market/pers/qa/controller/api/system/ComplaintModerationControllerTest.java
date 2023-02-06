package ru.yandex.market.pers.qa.controller.api.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.dto.complaint.ArticleCommentComplaintDto;
import ru.yandex.market.pers.qa.client.dto.complaint.CommentComplaintDto;
import ru.yandex.market.pers.qa.client.dto.complaint.QaCommentComplaintDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.ComplaintState;
import ru.yandex.market.pers.qa.controller.api.ComplaintControllerTest;
import ru.yandex.market.pers.qa.model.CommentStatus;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.service.CommentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ComplaintModerationControllerTest extends ComplaintControllerTest {

    @Autowired
    private CommentService commentService;

    private static final int REASON_ID = -10;
    private static final String REASON_NAME = "Другое";
    private static final String COMPAINT_BODY = "{\n" +
        "  \"id\": %s,\n" +
        "  \"state\": \"%s\"\n" +
        "}";

    @BeforeEach
    public void setUp() {
        qaJdbcTemplate.update("insert into qa.complaint_reason(id, name) values(?, ?)", REASON_ID, REASON_NAME);
    }

    @Test
    public void testQaAndArticleInboxForSameUser() throws Exception {
        final int complaintsCount = 5;
        createQaComplaints(complaintsCount);
        createArticleComplaints(complaintsCount);

        QAPager<QaCommentComplaintDto> qaComlaints = getQaCommentComplaint(UID);
        assertEquals(complaintsCount, qaComlaints.getData().size());
        assertEquals(getCommentComplaintCount().getCount(), qaComlaints.getPager().getCount());
        assertEquals(complaintsCount, qaComlaints.getPager().getCount());

        qaComlaints = getQaCommentComplaint(UID);
        assertEquals(0, qaComlaints.getData().size());

        QAPager<ArticleCommentComplaintDto> articleComplaints = getArticleCommentComplaint(UID);
        assertEquals(complaintsCount, articleComplaints.getData().size());
        assertEquals(getCommentComplaintCount().getCount(), articleComplaints.getPager().getCount());
        assertEquals(complaintsCount, articleComplaints.getPager().getCount());

        articleComplaints = getArticleCommentComplaint(UID);
        assertEquals(0, articleComplaints.getData().size());
    }

    @Test
    public void testQaInboxForSameUser() throws Exception {
        final int complaintsCount = 10;
        createQaComplaints(complaintsCount);

        QAPager<QaCommentComplaintDto> complaints = getQaCommentComplaint(UID);
        assertEquals(complaintsCount, complaints.getData().size());
        assertEquals(getCommentComplaintCount().getCount(), complaints.getPager().getCount());
        assertEquals(complaintsCount, complaints.getPager().getCount());

        complaints = getQaCommentComplaint(UID);
        assertEquals(0, complaints.getData().size());
    }

    @Test
    public void testQaInboxForAnotherUser() throws Exception {
        final int complaintsCount = 10;
        createQaComplaints(complaintsCount);

        QAPager<QaCommentComplaintDto> complaints = getQaCommentComplaint(UID);
        assertEquals(complaintsCount, complaints.getData().size());
        assertEquals(getCommentComplaintCount().getCount(), complaints.getPager().getCount());
        assertEquals(complaintsCount, complaints.getPager().getCount());

        complaints = getQaCommentComplaint(UID + 1);
        assertEquals(0, complaints.getData().size());
    }

    @Test
    public void testArticleInboxForSameUser() throws Exception {
        final int complaintsCount = 10;
        createArticleComplaints(complaintsCount);

        QAPager<ArticleCommentComplaintDto> complaints = getArticleCommentComplaint(UID);
        assertEquals(complaintsCount, complaints.getData().size());
        assertEquals(getCommentComplaintCount(QaEntityType.COMMENT_ARTICLE).getCount(), complaints.getPager().getCount());
        assertEquals(complaintsCount, complaints.getPager().getCount());

        complaints = getArticleCommentComplaint(UID);
        assertEquals(0, complaints.getData().size());
    }

    @Test
    public void testArticleInboxForAnotherUser() throws Exception {
        final int complaintsCount = 10;
        createArticleComplaints(complaintsCount);

        QAPager<ArticleCommentComplaintDto> complaints = getArticleCommentComplaint(UID);
        assertEquals(complaintsCount, complaints.getData().size());
        assertEquals(getCommentComplaintCount(QaEntityType.COMMENT_ARTICLE).getCount(), complaints.getPager().getCount());
        assertEquals(complaintsCount, complaints.getPager().getCount());

        complaints = getArticleCommentComplaint(UID + 1);
        assertEquals(0, complaints.getData().size());
    }

    @Test
    public void testQaModeration() throws Exception {
        final int complaintsCount = 10;
        createQaComplaints(complaintsCount);

        QAPager<QaCommentComplaintDto> complaints = getQaCommentComplaint(UID);
        assertEquals(complaintsCount, complaints.getData().size());
        assertEquals(getCommentComplaintCount().getCount(), complaints.getPager().getCount());
        assertEquals(complaintsCount, complaints.getPager().getCount());

        final Map<Long, ComplaintState> map = moderateComplaints(complaints, UID);
        checkComplaints(map);
    }

    @Test
    public void testArticleModeration() throws Exception {
        final int complaintsCount = 10;
        createArticleComplaints(complaintsCount);

        QAPager<ArticleCommentComplaintDto> complaints = getArticleCommentComplaint(UID);
        assertEquals(complaintsCount, complaints.getData().size());
        assertEquals(getCommentComplaintCount(QaEntityType.COMMENT_ARTICLE).getCount(), complaints.getPager().getCount());
        assertEquals(complaintsCount, complaints.getPager().getCount());

        final Map<Long, ComplaintState> map = moderateComplaints(complaints, UID);
        checkComplaints(map);
    }

    @Test
    public void testCommentWithParentComplaint() throws Exception {
        String commentText = "commentText";
        String parentCommentText = "parentCommentText";
        long articleId = ThreadLocalRandom.current().nextLong();
        String parentComplainText = "жалоба на родителя";
        String complainText = "жалоба на коммент";
        long parentCommentId = commentService.createComment(CommentProject.ARTICLE, UID, parentCommentText, articleId);
        long commentId = commentService.createComment(CommentProject.ARTICLE,
            UID,
            commentText,
            articleId,
            parentCommentId);
        createComplaintByUid(QaEntityType.COMMENT_ARTICLE, "child-0-" + parentCommentId, REASON_ID, parentComplainText);
        createComplaintByUid(QaEntityType.COMMENT_ARTICLE, "child-0-" + commentId, REASON_ID, complainText);

        QAPager<ArticleCommentComplaintDto> complaints = getArticleCommentComplaint(UID);
        assertEquals(2, complaints.getData().size());

        ArticleCommentComplaintDto commentComplaintDto = complaints.getData().get(0);
        ArticleCommentComplaintDto parentCommentComplaintDto = complaints.getData().get(1);
        if (commentComplaintDto.getCommentText().equals(parentCommentText)) {
            ArticleCommentComplaintDto t = commentComplaintDto;
            commentComplaintDto = parentCommentComplaintDto;
            parentCommentComplaintDto = t;
        }
        assertNull(parentCommentComplaintDto.getParentCommentText());
        assertEquals(parentCommentText, parentCommentComplaintDto.getCommentText());
        assertTrue(parentCommentComplaintDto.getComplaintText().endsWith(parentComplainText));

        assertEquals(parentCommentText, commentComplaintDto.getParentCommentText());
        assertEquals(commentText, commentComplaintDto.getCommentText());
        assertTrue(commentComplaintDto.getComplaintText().endsWith(complainText));
    }

    @Test
    public void testQaGetComplaint() throws Exception {
        String questionText = UUID.randomUUID().toString();
        String answerText = UUID.randomUUID().toString();
        String commentText = UUID.randomUUID().toString();
        String complaintText = UUID.randomUUID().toString();
        createQaComplaint(questionText, answerText, commentText, complaintText, REASON_ID);
        QAPager<QaCommentComplaintDto> complaints = getQaCommentComplaint(UID);
        assertEquals(1, complaints.getData().size());
        checkComplaint(complaints.getData().get(0), complaintText, commentText, answerText, questionText);
    }

    @Test
    public void testQaGetComplaintForUnpublishedEntity() throws Exception {
        long questionId = createQuestion(UUID.randomUUID().toString());
        qaJdbcTemplate.update("update qa.question set mod_state = ? where id = ?", ModState.TOLOKA_REJECTED.getValue(), questionId);
        long answerId = createAnswer(questionId, UUID.randomUUID().toString());
        qaJdbcTemplate.update("update qa.answer set mod_state = ? where id = ?", ModState.TOLOKA_REJECTED.getValue(), answerId);
        String commentId = createComment(UUID.randomUUID().toString(), answerId);
        createComplaintByUid(QaEntityType.COMMENT, commentId, REASON_ID, UUID.randomUUID().toString());
        QAPager<QaCommentComplaintDto> complaints = getQaCommentComplaint(UID);
        assertEquals(1, complaints.getData().size());
        QaCommentComplaintDto qaCommentComplaintDto = complaints.getData().get(0);
        Assert.notNull(qaCommentComplaintDto.getQuestionEntityId());
        Assert.notNull(qaCommentComplaintDto.getQuestionText());
        Assert.notNull(qaCommentComplaintDto.getAnswerText());
    }

    @Test
    public void testGetSomeQaComplaints() throws Exception {
        int n = 10;
        for (int i = 0; i < n; i++) {
            createQaComplaint();
        }
        QAPager<QaCommentComplaintDto> complaints = getQaCommentComplaint(UID);
        assertEquals(n, complaints.getData().size());
    }

    @Test
    public void testGetOnlyNewQaComplaints() throws Exception {
        int n = 3;
        createQaComplaints(n);
        qaJdbcTemplate.update("update qa.complaint set STATE = 1"); // approved
        createQaComplaints(n);
        qaJdbcTemplate.update("update qa.complaint set STATE = 2 where state != 1"); // rejected
        createQaComplaints(n); // new
        final QAPager<QaCommentComplaintDto> complaints = getQaCommentComplaint(UID);
        assertEquals(n, complaints.getData().size());
    }

    @Test
    public void testGetOnlyNewArticleComplaints() throws Exception {
        int n = 3;
        createArticleComplaints(n);
        qaJdbcTemplate.update("update qa.complaint set STATE = 1"); // approved
        createArticleComplaints(n);
        qaJdbcTemplate.update("update qa.complaint set STATE = 2 where state != 1"); // rejected
        createArticleComplaints(n); // new
        QAPager<ArticleCommentComplaintDto> complaints = getArticleCommentComplaint(UID);
        assertEquals(n, complaints.getData().size());
    }


    /**
     * Четные - APPROVE, нечтеные - REJECT.
     */
    private Function<CommentComplaintDto, ComplaintState> generateComplaintState() {
        return it -> {
            switch ((int) (it.getId() % 2)) {
                case 0:
                    return ComplaintState.APPROVE;
                case 1:
                    return ComplaintState.REJECT;
            }
            return ComplaintState.NEW;
        };
    }

    private String getModerationComplaintsBody(Map<Long, ComplaintState> complaints) {
        List<String> str = new ArrayList<>();
        complaints.forEach((id, state) -> {
            str.add(String.format(COMPAINT_BODY, id, state.getValue()));
        });
        return "{\n" +
            " \"complaints\": [\n" +
            str.stream().collect(Collectors.joining(",\n")) +
            "\n]\n" +
            "}\n";
    }

    private void createArticleComplaints(int n) throws Exception {
        for (int i = 0; i < n; i++) {
            String commentText = UUID.randomUUID().toString();
            long articleId = ThreadLocalRandom.current().nextLong();
            String complainText = UUID.randomUUID().toString();
            long commentId = commentService.createComment(CommentProject.ARTICLE, UID, commentText, articleId);
            createComplaintByUid(QaEntityType.COMMENT_ARTICLE, "child-0-" + commentId, REASON_ID, complainText);
        }
    }

    private void createQaComplaints(int n) throws Exception {
        for (int i = 0; i < n; i++) {
            createQaComplaint();
        }
    }

    private void createQaComplaint() throws Exception {
        String questionText = UUID.randomUUID().toString();
        String answerText = UUID.randomUUID().toString();
        String commentText = UUID.randomUUID().toString();
        String complainText = UUID.randomUUID().toString();
        createQaComplaint(questionText, answerText, commentText, complainText, REASON_ID);
    }

    private void createQaComplaint(String questionText, String answerText, String commentText, String complaintText, int reasonId)
        throws Exception {
        long questionId = createQuestion(questionText);
        long answerId = createAnswer(questionId, answerText);
        String commentId = createComment(commentText, answerId);
        createComplaintByUid(QaEntityType.COMMENT, commentId, reasonId, complaintText);
    }

    private void checkComplaint(QaCommentComplaintDto dto, String complainText, String commentText, String answerText,
                                String questionText) {
        assertEquals(REASON_NAME + ": " + complainText, dto.getComplaintText());
        assertEquals(answerText, dto.getAnswerText());
        assertEquals(questionText, dto.getQuestionText());
        assertEquals(commentText, dto.getCommentText());
        assertEquals(String.valueOf(UID), dto.getCommentAuthorId());
    }

    private void checkComplaintState(long id, ComplaintState state) {
      List<ComplaintState> states =  qaJdbcTemplate.query("select state from qa.complaint where id = ?",
          (rs, rowNum) -> ComplaintState.valueOf(rs.getInt("state")), id);
      assertEquals(1, states.size());
      assertEquals(state, states.get(0));
        if (state == ComplaintState.APPROVE) {
            List<Integer> deletedList = qaJdbcTemplate.queryForList(
                "select mod_state from com.comment where id in ( " +
                    "  select entity_id::bigint from qa.complaint where id = ?)",
                Integer.class, id);
            assertEquals(1, deletedList.size());
            assertEquals(CommentStatus.REJECTED_BY_MANAGER.getId(), (int) deletedList.get(0));
        }
    }

    private String createComment(String commentText, long answerId) {
        long id = commentService.createAnswerComment(UID, commentText, answerId);
        return "child-0-" + id;
    }

    private QAPager<QaCommentComplaintDto> getQaCommentComplaint(Long uid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/moderation/complaint/comment")
                .param("userId", String.valueOf(uid)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<QaCommentComplaintDto>>() {
        });
    }


    private QAPager<ArticleCommentComplaintDto> getArticleCommentComplaint(Long uid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/moderation/complaint/comment")
                .param("complaintCommentType", String.valueOf(QaEntityType.COMMENT_ARTICLE.getValue()))
                .param("userId", String.valueOf(uid)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<ArticleCommentComplaintDto>>() {
        });
    }

    private CountDto getCommentComplaintCount(QaEntityType qaEntityType) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/moderation/complaint/comment/count")
                .param("complaintCommentType", String.valueOf(qaEntityType.getValue())),
            status().is2xxSuccessful()
        ), new TypeReference<CountDto>() {
        });
    }

    private CountDto getCommentComplaintCount() throws Exception {
        return getCommentComplaintCount(QaEntityType.COMMENT);
    }

    private void moderateComplaints(Map<Long, ComplaintState> complaints, Long uid) throws Exception {
        final String response = invokeAndRetrieveResponse(
            post("/moderation/complaint/comment")
                .param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getModerationComplaintsBody(complaints))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    private Map<Long, ComplaintState> moderateComplaints(QAPager<? extends CommentComplaintDto> complaints, Long uid) throws Exception {
        Map<Long, ComplaintState> map = complaints.getData().stream().collect(Collectors.toMap(
            CommentComplaintDto::getId,
            generateComplaintState()
        ));

        moderateComplaints(map, uid);
        return map;
    }

    private void checkComplaints(Map<Long, ComplaintState> complaints) {
        complaints.forEach(this::checkComplaintState);
    }

}
