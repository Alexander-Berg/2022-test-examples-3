package ru.yandex.market.pers.qa.admin;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.ComplaintService;
import ru.yandex.market.pers.qa.service.QuestionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ModerationControllerTest extends PersQaAdminTest {

    private final long UID = 12345;
    private final String UID_STR = String.valueOf(UID);

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ComplaintService complaintService;
    @Autowired
    private ModerationMvcMocks moderationMvcMocks;
    @Autowired
    private CommentService commentService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerService answerService;

    @Test
    void testModerateComplaintOnCommentVideo() throws Exception {
        long commentIdForBan = createComment(CommentProject.VIDEO, 1);
        long commentIdForPub = createComment(CommentProject.VIDEO, 2);

        moderateCommentTest(commentIdForBan, commentIdForPub, QaEntityType.COMMENT_VIDEO);
    }

    @Test
    void testModerateComplaintOnCommentPost() throws Exception {
        Question questionRaw = Question.buildInterestPost(UID, UUID.randomUUID().toString(), "title", 1);
        Question question = questionService.createQuestion(questionRaw, new SecurityData());
        long commentIdForBan = createComment(CommentProject.POST, question.getId());

        questionRaw = Question.buildInterestPost(UID + 1, UUID.randomUUID().toString(), "title" + 1, 1);
        question = questionService.createQuestion(questionRaw, new SecurityData());
        long commentIdForPub = createComment(CommentProject.POST, question.getId());

        moderateCommentTest(commentIdForBan, commentIdForPub, QaEntityType.COMMENT_POST);
    }

    @Test
    void testModerateComplaintOnCommentQa() throws Exception {
        Question questionRaw = Question.buildInterestPost(UID, UUID.randomUUID().toString(), "title", 1);
        Question question = questionService.createQuestion(questionRaw, new SecurityData());
        Answer answer = answerService.createAnswer(UID + 1, UUID.randomUUID().toString(), question.getId());
        long commentIdForBan = createComment(CommentProject.QA, answer.getId());

        answer = answerService.createAnswer(UID + 2, UUID.randomUUID().toString(), question.getId());
        long commentIdForPub = createComment(CommentProject.QA, answer.getId());

        moderateCommentTest(commentIdForBan, commentIdForPub, QaEntityType.COMMENT);
    }

    @Test
    void testModerateComplaintOnCommentGrade() throws Exception {
        long gradeId = 1324;
        long commentIdForBan = createComment(CommentProject.GRADE, gradeId);

        long commentIdForPub = createComment(CommentProject.GRADE, gradeId);

        moderateCommentTest(commentIdForBan, commentIdForPub, QaEntityType.COMMENT_GRADE);
    }

    @Test
    void testModerateComplaintOnCommentVersus() throws Exception {
        long gradeId = 1324;
        long commentIdForBan = createComment(CommentProject.VERSUS, gradeId);

        long commentIdForPub = createComment(CommentProject.VERSUS, gradeId);

        moderateCommentTest(commentIdForBan, commentIdForPub, QaEntityType.COMMENT_VERSUS);
    }

    @Test
    void testModerateComplaintOnCommentArticle() throws Exception {
        long gradeId = 1324;
        long commentIdForBan = createComment(CommentProject.ARTICLE, gradeId);

        long commentIdForPub = createComment(CommentProject.ARTICLE, gradeId);

        moderateCommentTest(commentIdForBan, commentIdForPub, QaEntityType.COMMENT_ARTICLE);
    }

    private void moderateCommentTest(long commentIdForBan, long commentIdForPub, QaEntityType qaEntityType) throws Exception {
        complaintService.createComplaint(UserType.UID, UID_STR, qaEntityType, commentIdForBan, 1, "");
        complaintService.createComplaint(UserType.UID, UID_STR, qaEntityType, commentIdForPub, 1, "");

        Long complaintIdForBanComment = jdbcTemplate.queryForObject("select id from qa.complaint where entity_id = ?", Long.class,
            String.valueOf(commentIdForBan));
        Long complaintIdForPubComment = jdbcTemplate.queryForObject("select id from qa.complaint where entity_id = ?", Long.class,
            String.valueOf(commentIdForPub));

        moderationMvcMocks.moderateComplaintBanComment(complaintIdForBanComment, status().is2xxSuccessful());
        moderationMvcMocks.moderateComplaintPublishComment(complaintIdForPubComment, status().is2xxSuccessful());
        Long count = jdbcTemplate.queryForObject("select count(*) from qa.moderation_billing_log", Long.class);
        assertEquals(count.longValue(), 2);
    }


    private long createComment(CommentProject commentProject, long rootId) {
        Comment.Builder commentBuilder = Comment
            .builder(
                commentProject,
                rootId,
                UUID.randomUUID().toString(),
                UID);

        return commentService.createComment(commentBuilder);
    }

}
