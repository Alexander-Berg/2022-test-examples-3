package ru.yandex.market.pers.tms.comment;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.GradeCommentDao;
import ru.yandex.market.pers.grade.core.db.model.LastCommentType;
import ru.yandex.market.pers.qa.client.QaClient;
import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.client.utils.QaApiUtils;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 24.09.2019
 */
public class SynchronizeCommentsExecutorTest extends MockedPersTmsTest {
    private static final long BATCH_SIZE = 1000;

    @Autowired
    private SynchronizeCommentsExecutor executor;
    @Autowired
    private QaClient qaClient;
    @Autowired
    private GradeCommentDao gradeCommentDao;
    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testSynch() throws Exception {
        // create grades to bind with
        long[] modelGrades = {
                gradeCreator.createModelGrade(1L, 1L),
                gradeCreator.createModelGrade(1L, 2L),
                gradeCreator.createClusterGrade(1L, 3L),
        };
        long[] shopGrades = {
                gradeCreator.createShopGrade(1L, 1L, 2),
                gradeCreator.createShopGrade(2L, 1L, 2),
        };

        // step 1 - add comments and check, that they were saved
        synchStep1AddComments(modelGrades, shopGrades);

        // step 2 - delete some comments in qa, check that they were deleted in grade correctly
        synchStep2DeleteComments(modelGrades, shopGrades);
    }

    private void synchStep1AddComments(long[] modelGrades, long[] shopGrades) throws Exception {
        List<Long> comments = Arrays.asList(1L, 2L, 3L, 4L, 6L, 7L, 9L, 15L);
        gradeCommentDao.saveSignals(comments);

        // check content to process
        List<Long> commentsToSynch = gradeCommentDao.getCommentsToSynch(BATCH_SIZE);
        assertEquals(comments.size(), commentsToSynch.size());
        assertTrue(commentsToSynch.containsAll(comments));

        // mock qa calls
        Mockito.when(qaClient.getCommentBulk(eq(CommentProject.GRADE), any()))
            .thenReturn(Arrays.asList(
                    buildComment(1, modelGrades[0], CommentState.NEW, UserType.UID),
                    buildComment(2, modelGrades[1], CommentState.NEW, UserType.UID),
                    buildComment(3, modelGrades[1], CommentState.NEW, UserType.VENDOR),
                    buildComment(4, modelGrades[2], CommentState.NEW, UserType.VENDOR),
                    buildComment(6, shopGrades[0], CommentState.NEW, UserType.SHOP),
                    buildComment(7, shopGrades[1], CommentState.NEW, UserType.SHOP),
                    buildComment(9, shopGrades[1], CommentState.NEW, UserType.UID)
            ));

        Mockito.when(qaClient.getLastCommentBulk(eq(CommentProject.GRADE), any()))
            .thenReturn(Arrays.asList(
                buildComment(1L, modelGrades[0], CommentState.NEW, UserType.UID),
                buildComment(3L, modelGrades[1], CommentState.NEW, UserType.VENDOR),
                buildComment(4L, modelGrades[2], CommentState.NEW, UserType.VENDOR),
                buildComment(6L, shopGrades[0], CommentState.NEW, UserType.SHOP),
                buildComment(9L, shopGrades[1], CommentState.NEW, UserType.UID)
            ));

        // run job
        executor.runTmsJob();

        // check queue was cleaned
        commentsToSynch = gradeCommentDao.getCommentsToSynch(BATCH_SIZE);
        assertEquals(0, commentsToSynch.size());

        // assert checks

        // check comments are saved for models
        assertTrue(isCommentsExists(1));
        assertTrue(isCommentsExists(2));
        assertTrue(isCommentsExists(3));
        assertTrue(isCommentsExists(4));
        assertFalse(isCommentsExists(6));
        assertFalse(isCommentsExists(7));
        assertFalse(isCommentsExists(9));

        assertFalse(isCommentVendor(1));
        assertFalse(isCommentVendor(2));
        assertTrue(isCommentVendor(3));
        assertTrue(isCommentVendor(4));

        // check last comments saved for shops
        assertNull(getLastCommentType(modelGrades[0]));
        assertNull(getLastCommentType(modelGrades[1]));
        assertEquals(LastCommentType.SHOP, getLastCommentType(shopGrades[0]));
        assertEquals(LastCommentType.USER, getLastCommentType(shopGrades[1]));
    }

    private void synchStep2DeleteComments(long[] modelGrades, long[] shopGrades) throws Exception {
        gradeCommentDao.saveSignals(Arrays.asList(3L, 6L, 9L));

        List<Long> commentsToSynch;// mock qa calls
        Mockito.when(qaClient.getCommentBulk(eq(CommentProject.GRADE), any()))
            .thenReturn(Arrays.asList(
                buildComment(3, modelGrades[1], CommentState.DELETED, UserType.VENDOR),
                buildComment(6, shopGrades[0], CommentState.DELETED, UserType.SHOP),
                buildComment(9, shopGrades[1], CommentState.DELETED, UserType.UID)
            ));

        Mockito.when(qaClient.getLastCommentBulk(eq(CommentProject.GRADE), any()))
            .thenReturn(Arrays.asList(
                buildComment(2L, modelGrades[1], CommentState.NEW, UserType.UID),
                buildComment(7L, shopGrades[1], CommentState.NEW, UserType.SHOP)
            ));

        // run job
        executor.runTmsJob();

        // check queue was cleaned
        commentsToSynch = gradeCommentDao.getCommentsToSynch(BATCH_SIZE);
        assertEquals(0, commentsToSynch.size());

        // assert checks

        // check comments are saved for models
        assertTrue(isCommentsExists(1));
        assertTrue(isCommentsExists(2));
        assertFalse(isCommentsExists(3));
        assertFalse(isCommentsExists(6));
        assertFalse(isCommentsExists(7));
        assertFalse(isCommentsExists(9));

        assertFalse(isCommentVendor(1));
        assertFalse(isCommentVendor(2));
        assertTrue(isCommentVendor(3));

        // check last comments saved for shops
        assertNull(getLastCommentType(modelGrades[0]));
        assertNull(getLastCommentType(modelGrades[1]));
        assertNull(getLastCommentType(shopGrades[0]));
        assertEquals(LastCommentType.SHOP, getLastCommentType(shopGrades[1]));
    }

    private CommentDto buildComment(long id, long gradeId, CommentState state, UserType author) {
        CommentDto result = new CommentDto();
        result.setId(id);
        result.setEntityId(gradeId);
        result.setProjectId(CommentProject.GRADE.getId());
        result.setStateEnum(state);
        result.setUser(new AuthorIdDto(UserType.UID, "123"));
        result.setAuthor(new AuthorIdDto(author, "123"));
        result.setCreateTime(new Date());
        result.setUpdateTime(new Date());
        return result;
    }

    private boolean isCommentsExists(long commentId) {
        return Optional.ofNullable(
            pgJdbcTemplate
                .queryForObject("select count(*) from GRADE_COMMENT where COMMENT_ID = ? and IS_DELETED = 0",
                    Long.class,
                    QaApiUtils.toCommentIdInController(commentId))
        )
            .orElse(0L) > 0;
    }

    private boolean isCommentVendor(long commentId) {
        return Optional.ofNullable(
            pgJdbcTemplate
                .queryForObject("select count(*) from GRADE_COMMENT where COMMENT_ID = ? and VENDOR_ID is not null",
                    Long.class,
                    QaApiUtils.toCommentIdInController(commentId))
        )
            .orElse(0L) > 0;
    }

    private LastCommentType getLastCommentType(long gradeId) throws Exception {
        List<Integer> result = pgJdbcTemplate.queryForList(
            "select last_comment_by from GRADE_COMMENT_MARKER where GRADE_ID = ?",
            Integer.class, gradeId);
        return result.isEmpty() ? null : LastCommentType.getById(result.get(0));
    }
}
