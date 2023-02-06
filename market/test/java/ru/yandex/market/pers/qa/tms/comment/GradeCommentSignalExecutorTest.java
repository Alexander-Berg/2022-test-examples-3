package ru.yandex.market.pers.qa.tms.comment;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.GradeCommentService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.utils.CommonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.09.2019
 */
public class GradeCommentSignalExecutorTest extends PersQaTmsTest {
    public static final int BATCH_SIZE = 1000;
    private static final long USER_ID = 1231;

    @Autowired
    private AnswerService answerService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private GradeCommentSignalExecutor executor;
    @Autowired
    private CommentService commentService;
    @Autowired
    private GradeClient gradeClient;
    @Autowired
    private GradeCommentService gradeCommentService;

    @Override
    protected void resetMocks() {
        super.resetMocks();
        // mock client
        doNothing().when(gradeClient).sendGradeCommentSignal(any());
    }

    @Test
    void testIgnoreOtherProjects() {
        for (CommentProject project : CommentProject.values()) {
            long rootId = 1234123;

            switch (project) {
                case GRADE:
                    continue;
                case QA:
                    Question question = questionService.createModelQuestion(USER_ID, "test", project.getId());
                    rootId = answerService.createAnswer(USER_ID, "test", question.getId()).getId();
                    break;
                case POST:
                    rootId =  questionService.createQuestion(
                            Question.buildInterestPost(USER_ID, "test", "test", 1L),null).getId();
            }

            long commentId = commentService.createComment(project, USER_ID, "test", rootId);
            commentService.banCommentByManager(commentId);
            commentService.restoreCommentByManager(commentId);
            commentService.deleteComment(project, commentId, UserInfo.uid(USER_ID));
        }

        // check queue is empty
        assertEquals(0, gradeCommentService.getCommentsToSynch(BATCH_SIZE).size());
    }

    @Test
    void testSignals() {
        // create comments
        long rootId = 1234123;
        long shopId = 4234;

        long[] comments = {
            commentService.createComment(CommentProject.GRADE, USER_ID, "test", rootId),
            commentService.createShopComment(CommentProject.GRADE, USER_ID, "test", rootId, shopId),
            commentService.createComment(CommentProject.GRADE, USER_ID, "test response", rootId),
        };

        // check queue
        assertTrue(gradeCommentService.getCommentsToSynch(BATCH_SIZE).containsAll(CommonUtils.list(comments)));

        // send creates
        checkSendOnCreate(comments);

        // check queue
        assertEquals(0, gradeCommentService.getCommentsToSynch(BATCH_SIZE).size());

        // send removes
        checkSendOnRemove(comments);

        // send restore
        checkSendOnRestore(comments);
    }

    private void checkSendOnCreate(long[] comments) {
        resetMocks();

        // send signals
        executor.process();

        // verify sent signals
        assertSentSignals(comments);
    }

    private void checkSendOnRemove(long[] comments) {
        resetMocks();

        commentService.deleteComment(CommentProject.GRADE, comments[0], UserInfo.uid(USER_ID));
        commentService.banCommentByManager(comments[1]);
        commentService.banCommentsByRegExp(Collections.singletonList(comments[2]));

        // send signals
        executor.process();

        // verify sent signals
        assertSentSignals(comments);
    }

    private void checkSendOnRestore(long[] comments) {
        resetMocks();

        commentService.restoreCommentByManager(comments[0]);
        commentService.restoreCommentByManager(comments[1]);
        commentService.restoreCommentByManager(comments[2]);

        // send signals
        executor.process();

        // verify sent signals
        assertSentSignals(new long[]{comments[1], comments[2]});
    }

    private void assertSentSignals(long[] comments) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<?>> commentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(gradeClient, times(1)).sendGradeCommentSignal((List<Long>) commentsCaptor.capture());
        commentsCaptor.getValue().containsAll(CommonUtils.list(comments));
    }
}
