package ru.yandex.market.mbo.gwt.client.utils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 01.12.2017
 */
@SuppressWarnings("checkstyle:magicnumber")
public class DebounceTest {

    Consumer<String> function;
    private TimerFactory timerFactory;

    @Before
    public void init() {
        function = mock(Consumer.class);
        timerFactory = new TimerFactory();
    }

    @Test
    public void first() {
        Debounce<String> debounce = new Debounce<>(5, Debounce.Mode.FIRST, function, timerFactory);
        TimerMock timerMock = timerFactory.getTimerMock();

        debounce.callDebounced("first call");
        timerMock.tick();
        debounce.callDebounced("second call");
        timerMock.tick(3);
        debounce.callDebounced("third call");
        timerMock.tick(5);
        debounce.callDebounced("next session first call");
        timerMock.tick(3);
        debounce.callDebounced("last call");

        InOrder inOrder = inOrder(function);
        inOrder.verify(function).accept("first call");
        inOrder.verify(function).accept("next session first call");
        Mockito.verifyNoMoreInteractions(function);
    }

    @Test
    public void last() {
        Debounce<String> debounce = new Debounce<>(5, Debounce.Mode.LAST, function, timerFactory);
        TimerMock timerMock = timerFactory.getTimerMock();

        debounce.callDebounced("first call");
        timerMock.tick();
        debounce.callDebounced("second call");
        timerMock.tick(3);
        debounce.callDebounced("last call");

        // waiting timeout
        verify(function, never()).accept(anyString());
        timerMock.tick(2);
        // not yet
        verify(function, never()).accept(anyString());
        timerMock.tick(3);

        // now
        InOrder inOrder = inOrder(function);
        inOrder.verify(function).accept("last call");
        Mockito.verifyNoMoreInteractions(function);    }

    @Test
    public void firstAndLast() {
        Debounce<String> debounce = new Debounce<>(5, Debounce.Mode.FIRST_AND_LAST, function, timerFactory);
        TimerMock timerMock = timerFactory.getTimerMock();

        debounce.callDebounced("first call");
        timerMock.tick();
        debounce.callDebounced("second call");
        timerMock.tick(3);
        debounce.callDebounced("third call");
        timerMock.tick(5);
        debounce.callDebounced("next session first call");
        debounce.callDebounced("next session first call 2");
        timerMock.tick();
        debounce.callDebounced("next session second call");
        timerMock.tick();
        debounce.callDebounced("next session last call");
        timerMock.tick(10);

        // now
        InOrder inOrder = inOrder(function);
        inOrder.verify(function).accept("first call");
        inOrder.verify(function).accept("third call");
        inOrder.verify(function).accept("next session first call");
        inOrder.verify(function).accept("next session last call");
        Mockito.verifyNoMoreInteractions(function);
    }

    @Test
    public void avoidLastCallIfNoMoreCalls() {
        Debounce<String> debounce = new Debounce<>(5, Debounce.Mode.FIRST_AND_LAST, function, timerFactory);
        TimerMock timerMock = timerFactory.getTimerMock();

        timerMock.tick(40);
        debounce.callDebounced("single");
        timerMock.tick(20);

        verify(function).accept("single");
    }

    @Test
    public void dontUseTimerWithZeroDaley() {
        TimerFactory localTimerFactory = new TimerFactory() {
            @Override
            public Debounce.TimerInterface create(Runnable runnable) {
                timerMock = spy(new TimerMock(runnable));
                return timerMock;
            }
        };
        Debounce<String> debounce = new Debounce<>(0, Debounce.Mode.FIRST_AND_LAST, function, localTimerFactory);
        TimerMock timerMock = localTimerFactory.getTimerMock();
        debounce.callDebounced("one");
        debounce.callDebounced("two");
        timerMock.tick();
        debounce.callDebounced("three");
        timerMock.tick();
        debounce.callDebounced("four");

        InOrder inOrder = inOrder(function);
        inOrder.verify(function).accept("one");
        inOrder.verify(function).accept("two");
        inOrder.verify(function).accept("three");
        inOrder.verify(function).accept("four");
        Mockito.verifyNoMoreInteractions(function);

        verify(timerMock, never()).schedule(anyInt());
    }

    private static class TimerFactory implements Debounce.TimerFactory {

        protected TimerMock timerMock;

        @Override
        public Debounce.TimerInterface create(Runnable runnable) {
            timerMock = new TimerMock(runnable);
            return timerMock;
        }

        public TimerMock getTimerMock() {
            if (timerMock == null) {
                throw new RuntimeException("timer was not initialized. Call getTimerMock after Debounce constructor");
            }
            return timerMock;
        }
    }

    private static class TimerMock implements Debounce.TimerInterface {
        private final Runnable runnable;
        int nextCall = -1;
        int now = 0;

        private TimerMock(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void schedule(int delayMillis) {
            nextCall = now + delayMillis;
        }

        @Override
        public boolean isRunning() {
            return nextCall > now;
        }

        @Override
        public void cancel() {
            nextCall = -1;
        }

        public void tick() {
            tick(1);
        }

        public void tick(int millis) {
            now += millis;
            if (nextCall <= now && nextCall > -1) {
                runnable.run();
                nextCall = -1;
            }
        }
    }
}
