package ru.yandex.market.pers.qa.tms.ban;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.CommentStatus;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.UserBanInfo;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.UserBanService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author grigor-vlad
 * 01.06.2022
 */
public class UserContentBanExecutorTest extends PersQaTmsTest {
    private static final long USER_ID = 1L;
    private static final UserInfo USER_INFO = UserInfo.uid(USER_ID);
    private static final long FAKE_MODERATOR_ID = 1L;
    private static final long MODEL_ID = 123L;

    @Autowired
    private UserContentBanExecutor userContentBanExecutor;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserBanService userBanService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private CommentService commentService;

    @BeforeEach
    public void initUserBan() {
        userBanService.banWithContent(UserBanInfo.forever(UserType.UID,
            USER_INFO.getId(),
            "Test reason",
            FAKE_MODERATOR_ID));
    }

    @Test
    public void testUserBanService() {
        assertEquals(1, jdbcTemplate.queryForObject(
            "select count(*) from qa.user_ban where user_type = ? and user_id = ?",
            Long.class,
            USER_INFO.getType().getValue(),
            USER_INFO.getId()
        ), "User was banned");

        assertEquals(1, jdbcTemplate.queryForObject(
            "select count(*) from qa.user_content_ban_queue where user_type = ? and user_id = ?",
            Long.class,
            USER_INFO.getType().getValue(),
            USER_INFO.getId()
        ), "User appears in user_content_ban_queue");
    }

    @Test
    public void testUserContentBan() {
        final Question question = questionService.createModelQuestion(USER_ID, "Test question?", MODEL_ID);
        final Answer answer = answerService.createAnswer(USER_ID, "Answer!", question.getId());

        long userCommentId = commentService.createComment(CommentProject.QA, USER_ID, "Comment", answer.getId());
        long anotherUserCommentId =
            commentService.createComment(CommentProject.QA, USER_ID + 1, "Comment", answer.getId());

        //execute ban of content
        userContentBanExecutor.userContentBan();

        //check ban question
        Question questionAfterBan = questionService.getQuestionByIdInternal(question.getId());
        assertEquals(ModState.ANTIFRAUD_REJECTED, questionAfterBan.getModState());

        //check ban answer
        Answer answerAfterBan = answerService.getAnswerByIdInternal(answer.getId());
        assertEquals(ModState.ANTIFRAUD_REJECTED, answerAfterBan.getModState());

        //check ban comment
        Comment commentAfterBan = commentService.getCommentByIdInternal(userCommentId);
        assertEquals(CommentStatus.ANTIFRAUD_REJECTED, commentAfterBan.getStatus());

        //check not ban comments of another user
        Comment anotherUserCommentAfterBan = commentService.getCommentByIdInternal(anotherUserCommentId);
        assertNotEquals(CommentStatus.ANTIFRAUD_REJECTED, anotherUserCommentAfterBan.getStatus());

        //check absence of user in user_content_ban_queue
        List<UserInfo> userContentBanQueue = jdbcTemplate.query(
            "select * from qa.user_content_ban_queue",
            (rs, rowNum) -> new UserInfo(UserType.valueOf(rs.getInt("user_type")), rs.getString("user_id"))
        );
        assertTrue(userContentBanQueue.isEmpty());
    }

}
