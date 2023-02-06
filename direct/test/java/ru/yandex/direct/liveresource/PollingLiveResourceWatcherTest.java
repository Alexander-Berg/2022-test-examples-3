package ru.yandex.direct.liveresource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class PollingLiveResourceWatcherTest {

    private static final String INITIAL_CONTENT = "some init content";
    private static final String NEW_CONTENT = "awesome content";
    private static final long FIXED_RATE = 12345;

    private PollingLiveResourceWatcher testingWatcher;
    private LiveResource liveResource;
    private TaskScheduler taskScheduler;
    private LiveResourceListener listener1;
    private LiveResourceListener listener2;

    @Before
    public void setUp() {
        taskScheduler = mock(TaskScheduler.class);
        liveResource = mock(LiveResource.class);
        when(liveResource.getContent()).thenReturn(INITIAL_CONTENT);
        listener1 = mock(LiveResourceListener.class);
        listener2 = mock(LiveResourceListener.class);
        testingWatcher = new PollingLiveResourceWatcher(liveResource, INITIAL_CONTENT, taskScheduler, FIXED_RATE);
    }

    // using argument captors because mockito matchers fails on primitive types

    @Test
    public void dontWatchAfterCreation() {
        verify(taskScheduler, never()).scheduleAtFixedRate(any(), rateCaptor().capture());
    }

    @Test
    public void startsWatchingAfterCallWatch() {
        testingWatcher.watch();
        verify(taskScheduler).scheduleAtFixedRate(any(), rateCaptor().capture());
    }

    @Test
    public void callsTaskSchedulerWithValidRate() {
        testingWatcher.watch();
        ArgumentCaptor<Long> rateCaptor = rateCaptor();
        verify(taskScheduler).scheduleAtFixedRate(any(), rateCaptor.capture());
        assertThat(rateCaptor.getValue(), is(FIXED_RATE));
    }

    @Test
    public void dontNotifyListenersWhenLiveResourceNotChanged() {
        testingWatcher.addListener(listener1);

        doAnswer(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(taskScheduler).scheduleAtFixedRate(any(), rateCaptor().capture());

        testingWatcher.watch();

        verify(listener1, never()).update(any());
    }

    @Test
    public void notifyListenerWhenLiveResourceChangedFirstTime() {
        testingWatcher.addListener(listener1);

        doAnswer(invocation -> {
            when(liveResource.getContent()).thenReturn(NEW_CONTENT);
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(taskScheduler).scheduleAtFixedRate(any(), rateCaptor().capture());

        testingWatcher.watch();

        ArgumentCaptor<LiveResourceEvent> eventCaptor = eventCaptor();
        verify(listener1).update(eventCaptor.capture());
        assertThat(eventCaptor.getValue(), beanDiffer(new LiveResourceEvent(NEW_CONTENT)));
    }

    @Test
    public void notifyListenerWhenLiveResourceChangedSecondTime() {
        testingWatcher.addListener(listener1);

        // using argument captors because mockito matchers fails on primitive types
        doAnswer(invocation -> {
            when(liveResource.getContent()).thenReturn(NEW_CONTENT);
            ((Runnable) invocation.getArguments()[0]).run();
            when(liveResource.getContent()).thenReturn(INITIAL_CONTENT);
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(taskScheduler).scheduleAtFixedRate(any(), rateCaptor().capture());

        testingWatcher.watch();

        ArgumentCaptor<LiveResourceEvent> eventCaptor = eventCaptor();
        verify(listener1, times(2)).update(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues().get(1), beanDiffer(new LiveResourceEvent(INITIAL_CONTENT)));
    }

    @Test
    public void notifyFirstListenerWhenLiveResourceChanged() {
        liveResourceChangedWithTwoListeners();

        ArgumentCaptor<LiveResourceEvent> eventCaptor1 = eventCaptor();
        verify(listener1).update(eventCaptor1.capture());
        assertThat(eventCaptor1.getValue(), beanDiffer(new LiveResourceEvent(NEW_CONTENT)));
    }

    @Test
    public void notifySecondListenerWhenLiveResourceChanged() {
        liveResourceChangedWithTwoListeners();

        ArgumentCaptor<LiveResourceEvent> eventCaptor2 = eventCaptor();
        verify(listener2).update(eventCaptor2.capture());
        assertThat(eventCaptor2.getValue(), beanDiffer(new LiveResourceEvent(NEW_CONTENT)));
    }

    private void liveResourceChangedWithTwoListeners() {
        testingWatcher.addListener(listener1);
        testingWatcher.addListener(listener2);

        doAnswer(invocation -> {
            when(liveResource.getContent()).thenReturn(NEW_CONTENT);
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(taskScheduler).scheduleAtFixedRate(any(), rateCaptor().capture());

        testingWatcher.watch();
    }

    @Test
    public void dontNotifyListenerTwiceWhenLiveResourceChangedOnce() {
        testingWatcher.addListener(listener1);

        doAnswer(invocation -> {
            when(liveResource.getContent()).thenReturn(NEW_CONTENT);
            ((Runnable) invocation.getArguments()[0]).run();
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(taskScheduler).scheduleAtFixedRate(any(), rateCaptor().capture());

        testingWatcher.watch();

        ArgumentCaptor<LiveResourceEvent> eventCaptor = eventCaptor();
        verify(listener1).update(eventCaptor.capture());
    }

    @Test
    public void dontFailWithoutListeners() {
        doAnswer(invocation -> {
            when(liveResource.getContent()).thenReturn(NEW_CONTENT);
            try {
                ((Runnable) invocation.getArguments()[0]).run();
            } catch (Exception e) {
                fail("failed when content changed and no listeners provided: " + e);
            }
            return null;
        }).when(taskScheduler).scheduleAtFixedRate(any(), rateCaptor().capture());

        testingWatcher.watch();
    }

    private ArgumentCaptor<LiveResourceEvent> eventCaptor() {
        return ArgumentCaptor.forClass(LiveResourceEvent.class);
    }

    private ArgumentCaptor<Long> rateCaptor() {
        return ArgumentCaptor.forClass(Long.class);
    }
}
