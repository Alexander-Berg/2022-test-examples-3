package ru.yandex.market.mbo.tt.action;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.framework.core.AbstractServRequest;
import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.market.mbo.core.ui.util.XmlAdapterImpl;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.comments.Comment;
import ru.yandex.market.mbo.tt.comments.CommentType;
import ru.yandex.market.mbo.tt.comments.CommentsManager;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.utils.web.MarketServRequest;
import ru.yandex.market.mbo.utils.web.MarketServResponse;

import java.sql.Date;
import java.util.Calendar;

public class SetTaskStatusServantletTest {

    private static final long TASK_ID_WITH_COMMENT = 1;
    private static final long TASK_ID_WITHOUT_COMMENT = 2;

    private static final long COMMENT_ID = 10;
    private static final Status SOME_STATUS = Status.TASK_IN_PROCESS;
    private static final long SOME_CONTENT_ID = 12;

    private static final long SOME_TASK_LIST_ID = 100;
    private static final long USER_ID = 10000;

    private SetTaskStatusServantlet setTaskStatusServantlet;

    @Before
    public void setUp() throws Exception {
        StatusManager statusManager = Mockito.mock(StatusManager.class);
        Mockito.when(statusManager.changeTaskStatus(USER_ID, TASK_ID_WITH_COMMENT, SOME_STATUS)).thenReturn(true);

        TaskTracker taskTracker = Mockito.mock(TaskTracker.class);

        Mockito.when(taskTracker.getTask(TASK_ID_WITH_COMMENT))
                .thenAnswer(invocation -> new Task(TASK_ID_WITH_COMMENT, SOME_CONTENT_ID,
                                                  SOME_TASK_LIST_ID, SOME_STATUS));
        Mockito.when(taskTracker.getTaskList(TASK_ID_WITH_COMMENT))
                .thenAnswer(invocation -> new TaskList(SOME_TASK_LIST_ID, 0, 0, 0,
                                                       SOME_STATUS, TaskType.CHECK_FILL_MODEL_FROM_VENDOR,
                                                       0, new Date(0)));

        CommentsManager commentsManager = Mockito.mock(CommentsManager.class);
        Mockito.when(commentsManager.getLastComment(CommentType.TASK_COMMENT, TASK_ID_WITH_COMMENT))
                    .thenAnswer(invocation -> new Comment(COMMENT_ID, "Comment",
                                                          USER_ID, Calendar.getInstance()));

        Mockito.when(commentsManager.getLastComment(CommentType.TASK_COMMENT, TASK_ID_WITHOUT_COMMENT))
                    .thenAnswer(invocation -> Comment.FAKE_COMMENT);

        setTaskStatusServantlet = new SetTaskStatusServantlet(
            statusManager,
            taskTracker,
            null,
            commentsManager
        );
    }

    @Test
    public void checkCommentExist() {
        ServResponse resp = new MockServResponse();

        setTaskStatusServantlet.doProcess(new MarketServRequest<>(createRequest(TASK_ID_WITH_COMMENT)),
                                          new MarketServResponse(new XmlAdapterImpl(), resp));

        Assert.assertEquals(resp.hasErrors(), false);
    }

    @Test
    public void checkNoComment() {
        ServResponse resp = new MockServResponse();

        setTaskStatusServantlet.doProcess(new MarketServRequest<>(createRequest(TASK_ID_WITHOUT_COMMENT)),
                                          new MarketServResponse(new XmlAdapterImpl(), resp));

        Assert.assertEquals(resp.hasErrors(), true);
    }

    private ServRequest createRequest(long taskId) {
        ServRequest rq = new AbstractServRequest(USER_ID, null, null) { };
        rq.setParam("task-id", String.valueOf(taskId));
        rq.setParam("task-status", String.valueOf(SOME_STATUS.getId()));
        rq.setParam("check-comment-existence", "true");
        return rq;
    }
}
