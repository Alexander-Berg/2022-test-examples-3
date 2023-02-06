package ru.yandex.market.checker;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.TestHelper;
import ru.yandex.market.checker.core.CoreCheckerTask;
import ru.yandex.market.checker.core.Session;
import ru.yandex.market.checker.dao.CoreCheckerDao;
import ru.yandex.market.checker.zora.ZoraCheckerTaskRunner;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author imelnikov
 */
public class CheckerTaskProcessorTest extends EmptyTest {

    @Autowired
    CheckerTaskProcessor checkerTaskProcessor;

    @Test
    public void addTasks() {
        CheckerTaskRunner runner = mock(ZoraCheckerTaskRunner.class);
        CoreCheckerDao checkerDao = mock(CoreCheckerDao.class);

        Session session = mock(Session.class);
        List<CoreCheckerTask> tasks = Collections.singletonList(TestHelper.createTask("yandex.ru"));
        when(session.getTasks()).thenReturn(tasks);

        checkerTaskProcessor.processTasks(runner, checkerDao, session);
        verify(runner, times(tasks.size())).executeTask(any(CoreCheckerTask.class), eq(checkerDao) );
    }
}
