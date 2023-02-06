package ru.yandex.market.pers.grade.core.db;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.comments.model.CommentModState;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.model.Comment;

import static org.springframework.util.ReflectionUtils.getField;

/**
 * @author dinyat
 *         20/04/2017
 */
public class GradeCommentDaoTest extends MockedTest {

    private final String comment1Id = "child-9-0-123457";
    private final String comment2Id = "child-9-0-123487";
    private final Comment comment1 = new Comment(comment1Id, 2L, 1L, null, CommentModState.UNMODERATED, false, Timestamp.valueOf("2017-04-20 03:00:00.0"), Timestamp.valueOf("2017-04-20 15:00:00.0"));
    private final Comment comment2 = new Comment(comment2Id, 3L, 3L, null, CommentModState.UNMODERATED, false, Timestamp.valueOf("2017-04-20 03:00:00.0"), Timestamp.valueOf("2017-04-20 15:00:00.0"));
    private final String testCommentId = "child-9-0-123456";
    private final Comment testComment = new Comment(testCommentId, 1L, 10L, null, CommentModState.UNMODERATED, false, new Timestamp(0L), new Timestamp(10L));

    @Autowired
    private GradeCommentDao gradeCommentDao;

    @Before
    public void  init() {
        pgJdbcTemplate.update("INSERT INTO grade_comment (COMMENT_ID, GRADE_ID, STATE, IS_DELETED, AUTHOR_ID, CREATION_TIME, UPDATE_TIME) VALUES ('child-9-0-123457', 2, 0, 0,1,to_timestamp('2017-04-20 03:00', 'yyyy-MM-dd HH24:MI'),to_timestamp('2017-04-20 15:00', 'yyyy-MM-dd HH24:MI'))");
        pgJdbcTemplate.update("INSERT INTO grade_comment (COMMENT_ID, GRADE_ID, STATE, IS_DELETED, AUTHOR_ID, CREATION_TIME, UPDATE_TIME) VALUES ('child-9-0-123458', 2, 1, 0,2,to_timestamp('2017-04-20 04:00', 'yyyy-MM-dd HH24:MI'),to_timestamp('2017-04-20 04:00', 'yyyy-MM-dd HH24:MI'))");
        pgJdbcTemplate.update("INSERT INTO grade_comment (COMMENT_ID, GRADE_ID, STATE, IS_DELETED, AUTHOR_ID, CREATION_TIME, UPDATE_TIME) VALUES ('child-9-0-123459', 3, 2, 0,3,to_timestamp('2017-04-21 09:00', 'yyyy-MM-dd HH24:MI'),to_timestamp('2017-04-21 11:00', 'yyyy-MM-dd HH24:MI'))");
        pgJdbcTemplate.update("INSERT INTO grade_comment (COMMENT_ID, GRADE_ID, STATE, IS_DELETED, AUTHOR_ID, CREATION_TIME, UPDATE_TIME) VALUES ('child-9-0-123460', 3, 2, 0,3,to_timestamp('2017-04-21 10:00', 'yyyy-MM-dd HH24:MI'),to_timestamp('2017-04-21 10:00', 'yyyy-MM-dd HH24:MI'))");
        pgJdbcTemplate.update("INSERT INTO grade_comment (COMMENT_ID, GRADE_ID, STATE, IS_DELETED, AUTHOR_ID, CREATION_TIME, UPDATE_TIME) VALUES ('child-9-0-123461', 3, 3, 0, 4,to_timestamp('2017-04-21 11:00', 'yyyy-MM-dd HH24:MI'),to_timestamp('2017-04-21 11:00', 'yyyy-MM-dd HH24:MI'))");

        pgJdbcTemplate.update("INSERT INTO GRADE (ID, STATE, MOD_STATE, AUTHOR_ID, RESOURCE_ID) VALUES (2, 0, 0, 10, 100)");
        pgJdbcTemplate.update("INSERT INTO GRADE (ID, STATE, MOD_STATE, AUTHOR_ID, RESOURCE_ID) VALUES (3, 0, 0, 11, 200)");
        pgJdbcTemplate.update("INSERT INTO GRADE (ID, STATE, MOD_STATE, AUTHOR_ID, RESOURCE_ID) VALUES (4, 0, 0, 12, 200)");
        pgJdbcTemplate.update("INSERT INTO GRADE (ID, STATE, MOD_STATE, AUTHOR_ID, RESOURCE_ID) VALUES (5, 0, 0, 12, 200)");
    }

    @Test
    public void testSave() throws Exception {
        String returnedId = gradeCommentDao.save(testComment);

        Assert.assertEquals(testCommentId, returnedId);
    }

    @Test
    public void testUpdate() throws Exception {
        comment1.setState(CommentModState.READY);
        Timestamp updateTimeBeforeSaving = testComment.getUpdateTime();

        gradeCommentDao.save(comment1);

        Comment resultComment = gradeCommentDao.get(comment1Id);
        Assert.assertNotEquals(updateTimeBeforeSaving, resultComment.getUpdateTime());
        Assert.assertEquals(comment1.getCreationTime(), resultComment.getCreationTime());
        Assert.assertEquals(comment1.getState(), resultComment.getState());
    }

    @Test
    public void testGet() throws Exception {
        Comment resultComment = gradeCommentDao.get(comment1Id);

        assertEqualFieldsDeep(comment1, resultComment);
    }

    private <T> void assertEqualFieldsDeep(T expected, T actual) {
        for (Field field : expected.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Assert.assertEquals(getField(field, expected), getField(field, actual));
        }
    }

    @Test
    public void testUpdateState() throws Exception {
        Assert.assertEquals(false, comment1.isDeleted());
        gradeCommentDao.updateState(comment1Id, CommentModState.REJECTED_BY_MANAGER, true);

        Comment updatedComment = gradeCommentDao.get(comment1Id);
        Assert.assertEquals(CommentModState.REJECTED_BY_MANAGER, updatedComment.getState());
        Assert.assertEquals(true, updatedComment.isDeleted());
        Assert.assertNotEquals(comment1.getUpdateTime(), updatedComment.getUpdateTime());

    }

    @Test
    public void testUpdateStateOnlyDeleted() throws Exception {
        Assert.assertEquals(false, comment1.isDeleted());

        gradeCommentDao.updateState(comment1Id, null, true);

        Comment updatedComment = gradeCommentDao.get(comment1Id);
        Assert.assertEquals(comment1.getState(), updatedComment.getState());
        Assert.assertEquals(true, updatedComment.isDeleted());
        Assert.assertNotEquals(comment1.getUpdateTime(), updatedComment.getUpdateTime());
    }

    @Test
    public void save() {
        String id = "id1";
        assertEqualFieldsDeep(id, gradeCommentDao.save(comment(id)));
    }

    @Test
    public void getAfterSave() {
        Comment expected = comment("id1");
        gradeCommentDao.save(expected);
        Comment actual = gradeCommentDao.get(expected.getCommentId());
        assertCommentsEquals(expected, actual);
    }

    private void assertCommentsEquals(Comment expected, Comment actual) {
        Assert.assertEquals(expected.getCommentId(), actual.getCommentId());
        Assert.assertEquals(expected.getAuthorId(), actual.getAuthorId());
        Assert.assertEquals(expected.getState(), actual.getState());
        Assert.assertEquals(expected.getGradeId(), actual.getGradeId());
        Assert.assertEquals(expected.getVendorId(), actual.getVendorId());
    }

    private Comment comment(String id) {
        return new Comment(id, 1L, 2L, 3L, CommentModState.READY, false,
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now()));
    }
}
