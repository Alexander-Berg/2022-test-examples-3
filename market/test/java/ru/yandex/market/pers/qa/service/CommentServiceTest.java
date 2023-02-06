package ru.yandex.market.pers.qa.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.CommentFilter;
import ru.yandex.market.pers.qa.model.CommentWithAnswer;
import ru.yandex.market.pers.qa.model.DateFilter;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author vvolokh
 * 12.12.2018
 */
public class CommentServiceTest extends PersQATest {

    private static final long USER_ID = 1L;
    private static final long MODEL_ID = -1L;
    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void testFetchCommentsForLogging() {
        final Question question = questionService.createModelQuestion(USER_ID, "Test question?", MODEL_ID);
        final Answer answer = answerService.createAnswer(USER_ID, "Answer!", question.getId());

        long commentToBeSkipped = commentService.createComment(CommentProject.QA, USER_ID, "Comment", answer.getId());
        long commentToBeFetched = commentService.createComment(CommentProject.QA, USER_ID, "Comment", answer.getId());
        long deletedComment = commentService.createComment(CommentProject.QA, USER_ID, "Comment", answer.getId());
        commentService.banCommentByManager(deletedComment);
        long articleComment = commentService.createComment(CommentProject.ARTICLE, USER_ID, "Comment", answer.getId());
        long commentNotCheckedBySuggest = commentService.createComment(CommentProject.QA, USER_ID, "Comment", answer.getId());

        //pretend that one comment was already processed and possibly logged
        configurationService.mergeValue(CommentService.CFG_LAST_PROCESSED_FOR_TRIGGER, commentToBeSkipped);
        //pretend that comments after 'articleComment' are not checked by suggest service yet
        configurationService.mergeValue(CommentService.getLastAutoCheckedIdCfgKey(CommentProject.QA), articleComment);

        List<CommentWithAnswer> list = commentService.getCommentToProcessByTms();
        assertEquals(1, list.size());
        assertEquals(commentToBeFetched, (long)list.get(0).getCommentId());
    }

    @Test
    public void testDeleteHardCommentsByFilter() {
        final Question question = questionService.createModelQuestion(USER_ID, "Test question?", MODEL_ID);
        final Answer answer = answerService.createAnswer(USER_ID, "Answer!", question.getId());

        commentService.createComment(CommentProject.QA, USER_ID, "Comment", answer.getId());
        CommentFilter filter = new CommentFilter()
                .userId(USER_ID)
                .allowsNonPublic();
        CommentFilter filterWithDate = filter.dateFilter(new DateFilter(null, getDateTo()));

        commentService.deleteHardCommentsByFilter(filterWithDate);
        List<Comment> comments = commentService.getComments(filter);
        assertEquals(1, comments.size());

        commentService.deleteCommentsByFilter(filter);
        List<Comment> deletedComments = commentService.getComments(filter);
        assertEquals(1, deletedComments.size());
        assertEquals(CommentState.DELETED, deletedComments.get(0).getState());

        commentService.deleteHardCommentsByFilter(filterWithDate);
        List<Comment> deletedHardComments = commentService.getComments(filterWithDate);
        Assertions.assertEquals(0, deletedHardComments.size());
    }

    @Test
    public void testGetCommentsIds() {
        final Question question = questionService.createModelQuestion(USER_ID, "Test question?", MODEL_ID);
        final Answer answer = answerService.createAnswer(USER_ID, "Answer!", question.getId());

        long commentId1 = commentService.createComment(CommentProject.QA, USER_ID, "Comment", answer.getId());
        long commentId2 = commentService.createComment(CommentProject.QA, USER_ID + 1, "Comment", answer.getId());

        CommentFilter filter = new CommentFilter()
            .userId(USER_ID)
            .allowsNonPublic();
        List<Long> commentsIds = commentService.getCommentsIds(filter);

        assertEquals(1, commentsIds.size());
        assertEquals(commentId1, commentsIds.get(0).longValue());
    }

    private Date getDateTo() {
        return Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    }
}
