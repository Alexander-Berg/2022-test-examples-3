package ru.yandex.market.pers.qa.controller.api.comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.client.model.SortField;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.utils.ConvertUtils;
import ru.yandex.market.pers.qa.mock.mvc.AnswerMvcMocks;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.CommentFilter;
import ru.yandex.market.pers.qa.model.CommentParam;
import ru.yandex.market.pers.qa.model.CommentStatus;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.utils.CommentUtils;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkAuthor;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkCanDelete;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkChildCount;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkComment;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkParams;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkParent;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkRoot;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkState;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkStatus;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkSubComments;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkText;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkUser;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 15.08.2019
 */
public class AnswerCommentControllerTest extends QAControllerTest {

    @Autowired
    public CommentService commentService;
    @Autowired
    public AnswerMvcMocks answerMvc;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testCreateDeleteComment() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);

        CommentFilter commentFilter = new CommentFilter()
            .commentProject(CommentProject.QA)
            .rootId(answerId)
            .sort(Sort.asc(SortField.ID))
            .allowsNonPublic(false);

        List<Comment> comments = commentService.getComments(commentFilter);
        assertEquals(0, comments.size());

        long comment1 = answerMvc.createComment(UID, answerId, "test");
        long comment2 = answerMvc.createComment(UID, answerId, "test2");

        comments = commentService.getComments(commentFilter);
        assertEquals(2, comments.size());
        assertTrue(comments.stream().map(Comment::getId).collect(Collectors.toSet())
            .containsAll(Arrays.asList(comment1, comment2)));

        // try delete
        answerMvc.deleteComment(UID, comment1, status().is2xxSuccessful());
        // and again
        answerMvc.deleteComment(UID, comment1, status().is4xxClientError());

        comments = commentService.getComments(commentFilter);
        assertEquals(1, comments.size());
        assertEquals(comment2, comments.get(0).getId().longValue());
    }

    @Test
    void testTree() throws Exception {
        long questionId = createQuestion();
        long[] answerIds = {
            createAnswer(questionId),
            createAnswer(questionId)
        };

        long[] comments1 = {
            answerMvc.createComment(UID, answerIds[0], "Some comment"),
            answerMvc.createComment(UID + 1, answerIds[0], "Other comment"),
            answerMvc.createComment(UID + 2, answerIds[0], "Other comment 2"),
        };

        long[] comments2 = {
            answerMvc.createComment(UID + 1, answerIds[1], "User comment"),
            answerMvc.createComment(UID, answerIds[1], "User comment 2"),
        };

        long commentDeleted = answerMvc.createComment(UID, answerIds[1], "User comment 3");
        answerMvc.deleteComment(UID, commentDeleted, status().is2xxSuccessful());

        // by uid
        List<CommentTreeDto> comments = answerMvc.getCommentsBulk(UID, answerIds).getData();
        assertEquals(2, comments.size());
        checkComments(answerIds[0], comments1, comments, 0);
        checkComments(answerIds[1], comments2, comments, 1);

        List<CommentDto> commentsList = comments.get(0).getComments();
        assertTrue(commentsList.get(0).isCanDelete());
        assertFalse(commentsList.get(1).isCanDelete());

        // by yandexuid
        comments = answerMvc.getCommentsBulkYandexuid(YANDEXUID, answerIds).getData();
        assertEquals(2, comments.size());
        checkComments(answerIds[0], comments1, comments, 0);
        checkComments(answerIds[1], comments2, comments, 1);

        commentsList = comments.get(0).getComments();
        assertFalse(commentsList.get(0).isCanDelete());
        assertFalse(commentsList.get(1).isCanDelete());
    }

    @Test
    public void testBulkCommentsCountMax() throws Exception {
        answerMvc
            .getCommentsBulkCountWithoutMapping(LongStream.range(0, ControllerConstants.MAX_BATCH_SIZE + 1)
                .toArray(), status().isBadRequest());
    }

    @Test
    public void testAnswerCommentCount() throws Exception {
        final long questionId = createModelQuestion(1, "Question", 1);
        final long answer1Id = createAnswer(questionId, UID, "Answer");
        final long answer2Id = createAnswer(questionId, UID + 1, "Answer");
        commentService.createAnswerComment(1, "Comment", answer1Id);
        commentService.createAnswerComment(2, "Comment", answer1Id);
        commentService.createAnswerComment(1, "Comment", answer2Id);
        Map<Long, CountDto> countDtoMap = answerMvc.getCommentsBulkCount(
            new long[]{answer1Id, answer2Id}, status().is2xxSuccessful());
        Assertions.assertEquals(2, countDtoMap.size());
        Assertions.assertEquals(2, countDtoMap.get(answer1Id).getCount());
        Assertions.assertEquals(1, countDtoMap.get(answer2Id).getCount());
    }

    private void checkComments(long answerId, long[] comments1, List<CommentTreeDto> comments, int index) {
        assertEquals(answerId, comments.get(index).getEntityId().longValue());
        assertEquals(ControllerConstants.ENTITY_ANSWER, comments.get(index).getEntity());
        assertArrayEquals(comments1, getIds(comments.get(index).getComments()));
    }

    private long[] getIds(List<CommentDto> data) {
        return data.stream()
            .mapToLong(CommentDto::getId)
            .toArray();
    }

    @Test
    public void testNewSchemaConsistency() throws Exception {
        // create some comments. Check:
        // - child_count
        // - first_level_child_count
        // - parents table
        // - properties table
        // - child_count table

        // Comments tree:
        // c0 - QA 1 - by user
        //  - c1 (child of c0) - by user
        //  - c2 (child of c0) - by shop (banned)
        //    - c3 (child of c2) - by vendor, reply to c2
        // c4 - QA 1 - by user
        // c5 - QA 2 - by user

        long question = createModelQuestion(MODEL_ID, UID);
        long answer = createAnswer(question, UID, "Answer!");
        long answer2 = createAnswer(question, UID, "Answer 2");

        long shopId = 13123;
        long brandId = 524524;

        Long parent = null;
        long[] c = {
            parent = commentService.createComment(CommentProject.QA, UID, "c0", answer),
            commentService.createComment(CommentProject.QA, UID, "c1", answer, parent),
            parent = commentService.createShopComment(CommentProject.QA, UID, "c2", answer, shopId, parent),
            commentService.createVendorComment(CommentProject.QA, UID, "c3", answer, brandId, parent),

            commentService.createComment(CommentProject.QA, UID, "c4", answer),
            commentService.createComment(CommentProject.QA, UID, "c5", answer2),
        };

        commentService.banCommentByManager(c[2]);
        commentService.markAsReplyTo(c[3], c[2]);
        commentService.deleteComment(CommentProject.QA, c[4], UserInfo.uid(UID));

        Multimap<Long, CommentParam> parameters =
            getParametersNew(Arrays.stream(c).boxed().collect(Collectors.toList()));

        List<Comment> comments = jdbcTemplate.query(
            "select *\n" +
                "from com.comment\n" +
                "order by id",
            (rs, rowNum) -> Comment.valueOf(rs)
        );

        List<CommentDto> result = comments.stream()
            .map(comment1 -> {
                CommentDto dto = ConvertUtils.toDto(comment1);
                dto.setAuthor(new AuthorIdDto(comment1.getAuthor().getType(), comment1.getAuthor().getId()));
                return dto;
            })
            .map(comment -> {
                Collection<CommentParam> params = parameters.get(comment.getId());
                if (params != null && !params.isEmpty()) {
                    comment.getParameters().putAll(CommentUtils.toMap(params));
                }
                return comment;
            })
            .collect(Collectors.toList());

        CommentAssert.assertComments(result,
            checkComment(c[0],
                checkParent(null),
                checkRoot(CommentProject.QA, answer),
                checkState(CommentState.NEW),
                checkText("c0"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(UserInfo.uid(UID)),
                checkCanDelete(false),
                checkParams(Map.of()),
                checkChildCount(1, 2),
                checkSubComments()
            ),
            checkComment(c[1],
                checkParent(c[0]),
                checkRoot(CommentProject.QA, answer),
                checkState(CommentState.NEW),
                checkText("c1"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(UserInfo.uid(UID)),
                checkCanDelete(false),
                checkParams(Map.of()),
                checkChildCount(0, 0),
                checkSubComments()
            ),
            checkComment(c[2],
                checkParent(c[0]),
                checkRoot(CommentProject.QA, answer),
                checkState(CommentState.BANNED),
                checkText("c2"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                checkCanDelete(false),
                checkParams(Map.of()),
                checkChildCount(1, 1),
                checkStatus(CommentStatus.REJECTED_BY_MANAGER),
                checkSubComments()
            ),
            checkComment(c[3],
                checkParent(c[2]),
                checkRoot(CommentProject.QA, answer),
                checkState(CommentState.NEW),
                checkText("c3"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(new UserInfo(UserType.VENDOR, brandId)),
                checkCanDelete(false),
                checkParams(Map.of(CommentUtils.PARAM_REPLY_ENTITY, "3-" + shopId)),
                checkChildCount(0, 0),
                checkSubComments()
            ),
            checkComment(c[4],
                checkParent(null),
                checkRoot(CommentProject.QA, answer),
                checkState(CommentState.DELETED),
                checkText("c4"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(UserInfo.uid(UID)),
                checkCanDelete(false),
                checkParams(Map.of()),
                checkChildCount(0, 0),
                checkSubComments()
            ),
            checkComment(c[5],
                checkParent(null),
                checkRoot(CommentProject.QA, answer2),
                checkState(CommentState.NEW),
                checkText("c5"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(UserInfo.uid(UID)),
                checkCanDelete(false),
                checkParams(Map.of()),
                checkChildCount(0, 0),
                checkSubComments()
            )
        );

        // check comments

        // check parents

        // check properties
        // check
    }

    /**
     * All parameters except for STATUS - it is already fetched with basic comment query.
     */
    public Multimap<Long, CommentParam> getParametersNew(Collection<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
        }

        ListMultimap<Long, CommentParam> result = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

        // Important to use connect to main DB. Do not use replica here!
        // Could lead to missing fields in created comments
        jdbcTemplate.query(
            "select comment_id as id, name, value\n" +
                "from com.property \n" +
                "where comment_id = ANY(?)\n" +
                "order by id, name",
            (rs, rowNum) -> {
                result.put(
                    rs.getLong("id"),
                    new CommentParam(
                        rs.getString("name"),
                        rs.getString("value")
                    ));
                return null;
            }, List.of(
                ListUtils.toLongArray(commentIds)
            ).toArray());

        return result;
    }
}
