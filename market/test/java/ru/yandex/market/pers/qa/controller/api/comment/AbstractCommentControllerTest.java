package ru.yandex.market.pers.qa.controller.api.comment;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Comparators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.CommentResultDto;
import ru.yandex.market.pers.qa.controller.service.CommentHelper;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author varvara
 * 15.02.2019
 */
public abstract class AbstractCommentControllerTest extends ControllerTest {
    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    public static String getBody(String text) {
        return String.format("{\n" +
            "    \"text\" : \"%s\"\n" +
            "}", text);
    }

    protected static String getAnyBody() {
        return getBody(UUID.randomUUID().toString());
    }

    protected void setUpdateTime(long... comments) {
        final long timeSecond = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        final long delta = 1000; // in seconds
        final int n = comments.length;
        for (int i = 0, j = comments.length; i < n; i++, j--) {
            long timestampSec = timeSecond - j * delta;
            qaJdbcTemplate.update("update com.comment set upd_time = ? where id = ?",
                new Timestamp(timeSecond*1000), comments[i]);
        }
    }

    protected void checkEntityChildCountExactInDb(long entityId,
                                                  long projectId,
                                                  long childCount,
                                                  long firstLevelCount) {
        List<Integer> result = jdbcTemplate.query(
            "select child_count, first_level_child_count " +
                "from com.child_count " +
                "where root_id = ? and project = ?",
            (rs, rowNum) -> {
                assertEquals(childCount, rs.getLong("child_count"));
                assertEquals(firstLevelCount, rs.getLong("first_level_child_count"));
                return 1;
            },
            entityId, projectId
        );

        // check exactly one record found
        assertEquals(1, result.size());
    }

    protected void checkCommentDto(CommentDto comment,
                                   long entityId,
                                   long projectId,
                                   Long parentId,
                                   long level,
                                   long firstLevelChildCount,
                                   long childCount,
                                   Long... parents) {
        assertEquals(entityId, comment.getEntityId());
        assertEquals(parentId, comment.getParentId());
        assertEquals(level, commentService.getCommentLevel(comment.getId()));
        assertEquals(projectId, comment.getProjectId());
        checkCountAndParents(comment.getId(), firstLevelChildCount, childCount, parents);
    }

    protected void checkCommentInDb(long id,
                                    long entityId,
                                    long projectId,
                                    Long parentId,
                                    long level,
                                    long firstLevelChildCount,
                                    long childCount,
                                    Long... parents) {
        final Comment comment = commentService.getCommentById(id);
        assertEquals(entityId, comment.getRootId());
        assertEquals(parentId, comment.getParentId());
        assertEquals(level, commentService.getCommentLevel(id));
        assertEquals(projectId, comment.getProjectId());
        checkCountAndParents(id, firstLevelChildCount, childCount, parents);
    }

    protected void checkCountAndParents(CommentDto comment,
                                        long firstLevelChildCount,
                                        long childCount,
                                        Long... parents) {
        checkCountAndParents(comment.getId(), firstLevelChildCount, childCount, parents);
    }

    protected void checkCountAndParents(long id, long firstLevelChildCount, long childCount, Long... parents) {
        assertEquals(firstLevelChildCount,
            (long) jdbcTemplate
                .queryForObject("select first_level_child_count from com.comment where id = ?", Long.class, id));
        assertEquals(childCount,
            (long) jdbcTemplate.queryForObject("select child_count from com.comment where id = ?", Long.class, id));
        final List<Long> parentsDb = jdbcTemplate
            .queryForList("select parent_id from com.parent where comment_id = ?", Long.class, id);
        assertEquals(parents.length, parentsDb.size());
        assertTrue(parentsDb.containsAll(Arrays.asList(parents)));
    }

    protected void checkCount(CommentResultDto dto, Long comment, Long childCount) {
        List<CommentDto> comments = dto.getData();
        Long actualChildCount = comments.stream().filter(it -> it.getId() == comment).map(CommentDto::getChildCount).findFirst().get();
        assertEquals(childCount, actualChildCount);
    }

    protected void checkData(Long levelRootId, CommentResultDto dto, Long... expectedComments) {
        checkData(dto, expectedComments);
        String levelRootIdStr = String.valueOf(levelRootId);

        assertTrue(dto.getData().stream().map(CommentDto::getLevelRootId).allMatch(levelRootIdStr::equals));

        //check crutch for composed rootId
        if (levelRootId != CommentHelper.ROOT_ID) {
            assertTrue(dto.getData().stream().map(CommentDto::getComposedLevelRootId).allMatch(levelRootIdStr::equals));
        } else {
            assertTrue(dto.getData().stream().allMatch(x -> x.getComposedLevelRootId().equals("/" + x.getEntityId())));
        }
    }

    protected void checkData(CommentResultDto dto, Long... expectedComments) {
        List<CommentDto> comments = dto.getData();
        List<Long> expectedCommentsList = Arrays.asList(expectedComments);
        assertEquals(expectedComments.length, comments.size());
        assertTrue(comments.stream().map(CommentDto::getId).allMatch(expectedCommentsList::contains));
    }

    protected void checkDataExact(CommentResultDto dto, long... expectedComments) {
        List<CommentDto> comments = dto.getData();
        long[] actualCommentIds = ListUtils.toList(comments, CommentDto::getId).stream().mapToLong(x->x).toArray();
        assertEquals(expectedComments.length, comments.size());
        assertArrayEquals(expectedComments, actualCommentIds);
    }

    protected void checkBorderId(CommentResultDto dto, Long expectedBorderId) {
        assertEquals(expectedBorderId, dto.getBorderId());
    }

    protected void checkBranch(CommentResultDto dto, Long parentId, Comparator<Long> cmp, Long... expectedComments) {
        final Map<String, List<Long>> tree = dto.getTree();
        List<Long> comments = tree.get(parentId.toString());
        List<Long> expectedCommentsList = Arrays.asList(expectedComments);
        assertEquals(expectedCommentsList.size(), comments.size());
        assertTrue(comments.containsAll(expectedCommentsList), "contains " + expectedCommentsList + " but contains " + comments);
        assertTrue(Comparators.isInStrictOrder(comments, cmp));

        //check crutch for composed rootId
        if (parentId == CommentHelper.ROOT_ID) {
            assertEquals(2, tree.size());
            List<Long> comments2 = null;
            if (!dto.getData().isEmpty()) {
                CommentDto commentDto = dto.getData().get(0);
                String composedLevel = CommentHelper.buildComposedLevelRootId(
                    Long.valueOf(commentDto.getLevelRootId()),
                    commentDto.getEntityId());
                assertTrue(composedLevel.startsWith("/"));
                comments2 = tree.get(composedLevel);
            }

            if (comments2 != null) {
                assertArrayEquals(
                    ListUtils.toLongArray(comments),
                    ListUtils.toLongArray(comments2)
                );
            }
        } else {
            assertEquals(1, tree.size());
        }
    }

    protected void assertAuthor(String entity, String id, AuthorIdDto source) {
        assertEquals(entity, source.getEntity());
        assertEquals(id, source.getId());
    }
}
