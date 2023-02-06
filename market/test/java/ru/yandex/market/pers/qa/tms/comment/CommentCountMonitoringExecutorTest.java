package ru.yandex.market.pers.qa.tms.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.QuestionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CommentCountMonitoringExecutorTest extends PersQaTmsTest {

    public static final int UID = 123124;
    public static final int MODEL_ID = 4134134;
    @Autowired
    private CommentCountMonitoringExecutor executor;

    @Autowired
    private CommentCountFixerExecutor fixExecutor;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private CommentService commentService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private TestTree prepareTree() {
        // Comments tree:
        // c0 - QA 1 - by user
        //  - c1 (child of c0) - by user
        //  - c2 (child of c0) - by shop (banned)
        //    - c3 (child of c2) - by vendor, reply to c2
        // c4 - QA 1 - by user (deleted)
        // c5 - QA 2 - by user

        long questionId = questionService.createModelQuestion(UID, "question", MODEL_ID).getId();
        long answerId = answerService.createAnswer(UID, "Answer!", questionId).getId();
        long answerId2 = answerService.createAnswer(UID, "Answer 2", questionId).getId();

        long shopId = 13123;
        long brandId = 524524;

        long parent;
        long[] c = {
            parent = commentService.createComment(CommentProject.QA, UID, "c0", answerId),
            commentService.createComment(CommentProject.QA, UID, "c1", answerId, parent),
            parent = commentService.createShopComment(CommentProject.QA, UID, "c2", answerId, shopId, parent),
            commentService.createVendorComment(CommentProject.QA, UID, "c3", answerId, brandId, parent),

            commentService.createComment(CommentProject.QA, UID, "c4", answerId),
            commentService.createComment(CommentProject.QA, UID, "c5", answerId2),
        };

        commentService.banCommentByManager(c[2]);
        commentService.markAsReplyTo(c[3], c[2]);
        commentService.deleteComment(CommentProject.QA, c[4], UserInfo.uid(UID));

        return new TestTree(c, new long[]{answerId, answerId2});
    }

    @Test
    public void testMonitoringOk() {
        TestTree testTree = prepareTree();
        long[] c = testTree.commentIds;
        long answerId = testTree.rootIds[0];
        long answerId2 = testTree.rootIds[1];

        assertEquals(3, jdbcTemplate.queryForObject(
            "select child_count from com.child_count where root_id = ?", Long.class, answerId));
        assertEquals(1, jdbcTemplate.queryForObject(
            "select first_level_child_count from com.child_count where root_id = ?", Long.class, answerId));

        assertEquals(1, jdbcTemplate.queryForObject(
            "select child_count from com.child_count where root_id = ?", Long.class, answerId2));
        assertEquals(1, jdbcTemplate.queryForObject(
            "select first_level_child_count from com.child_count where root_id = ?", Long.class, answerId2));

        // check comment counts
        assertEquals(2, jdbcTemplate.queryForObject(
            "select child_count from com.comment where id = ?", Long.class, c[0]));
        assertEquals(1, jdbcTemplate.queryForObject(
            "select first_level_child_count from com.comment where id = ?", Long.class, c[0]));

        // works fine
        executor.commentsCountCheck();


        //TODO
        // ruin root first_level_child_count
        // ruin comment child_count
        // ruin comment first_level_child_count
    }

    @Test
    public void testMonitoringBadRootChild() {
        TestTree testTree = prepareTree();
        long answerId = testTree.rootIds[0];

        executor.commentsCountCheck();

        // ruin root child_count
        jdbcTemplate.update("update com.child_count set child_count = 0 where root_id = ?", answerId);

        try {
            executor.commentsCountCheck();
            fail("Should not get here");
        } catch (Exception cause) {
            assertEquals("Comment counts diverged [count by project (1)]", cause.getMessage());
        }

        // try to fix
        fixExecutor.fixCommentsCount();

        // now works fine
        executor.commentsCountCheck();
    }

    @Test
    public void testMonitoringBadRootFirstLevelChild() {
        TestTree testTree = prepareTree();
        long answerId = testTree.rootIds[0];

        executor.commentsCountCheck();

        // ruin root child_count
        jdbcTemplate.update("update com.child_count set first_level_child_count = 0 where root_id = ?", answerId);

        try {
            executor.commentsCountCheck();
            fail("Should not get here");
        } catch (Exception cause) {
            assertEquals("Comment counts diverged [count by project (1)]", cause.getMessage());
        }

        // try to fix
        fixExecutor.fixCommentsCount();

        // now works fine
        executor.commentsCountCheck();
    }

    @Test
    public void testMonitoringBadCommentChild() {
        TestTree testTree = prepareTree();
        long[] c = testTree.commentIds;

        executor.commentsCountCheck();

        // ruin root child_count
        jdbcTemplate.update("update com.comment set child_count = -1 where id = ?", c[0]);
        jdbcTemplate.update("update com.comment set child_count = -1 where id = ?", c[2]);

        try {
            executor.commentsCountCheck();
            fail("Should not get here");
        } catch (Exception cause) {
            assertEquals("Comment counts diverged [full count by parent (2)]", cause.getMessage());
        }

        // try to fix
        fixExecutor.fixCommentsCount();

        // now works fine
        executor.commentsCountCheck();
    }

    @Test
    public void testMonitoringBadCommentFirstLevelChild() {
        TestTree testTree = prepareTree();
        long[] c = testTree.commentIds;

        executor.commentsCountCheck();

        // ruin root child_count
        jdbcTemplate.update("update com.comment set first_level_child_count = -1 where id = ?", c[0]);
        jdbcTemplate.update("update com.comment set first_level_child_count = -1 where id = ?", c[2]);

        try {
            executor.commentsCountCheck();
            fail("Should not get here");
        } catch (Exception cause) {
            assertEquals("Comment counts diverged [first level by parent (2)]", cause.getMessage());
        }

        // try to fix
        fixExecutor.fixCommentsCount();

        // now works fine
        executor.commentsCountCheck();
    }


    private static final class TestTree {
        long[] commentIds;
        long[] rootIds;

        public TestTree(long[] commentIds, long[] rootIds) {
            this.commentIds = commentIds;
            this.rootIds = rootIds;
        }
    }
}
