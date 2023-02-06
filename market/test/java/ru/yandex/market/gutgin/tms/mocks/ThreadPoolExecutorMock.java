package ru.yandex.market.gutgin.tms.mocks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ThreadPoolExecutorMock extends ThreadPoolExecutor {
    private ThreadPoolExecutorMock(
        int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public List<Runnable> getRunnableList() {
        return null;
    }

    public static ThreadPoolExecutorMock newInstance() {
        List<Runnable> runnableList = new ArrayList<>();

        ThreadPoolExecutorMock mock = mock(ThreadPoolExecutorMock.class, invocation -> {
            throw new UnsupportedOperationException(invocation.toString());
        });

        doReturn(0).when(mock).getActiveCount();

        doReturn(new LinkedBlockingQueue<Runnable>()).when(mock).getQueue();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnableList.add(runnable);
            return null;
        }).when(mock).submit(any(Runnable.class));

        //noinspection ResultOfMethodCallIgnored
        doReturn(runnableList).when(mock).getRunnableList();

        return mock;
    }
}
