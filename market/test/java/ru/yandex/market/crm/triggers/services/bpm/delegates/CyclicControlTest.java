package ru.yandex.market.crm.triggers.services.bpm.delegates;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Iterators;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.crm.triggers.services.bpm.delegates.CyclicControl.CycleOverThresholdException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CyclicControlTest {

    private CyclicControl control;

    private CollectVariablesAnswer answer = new CollectVariablesAnswer();

    private DelegateExecution delegateExecution;

    private JdbcTemplate jdbcTemplate;

    private Clock clock;

    @Before
    public void setUp() {
        delegateExecution = mock(DelegateExecution.class);
        doAnswer(answer).when(delegateExecution).setVariable(anyString(), any());
        when(delegateExecution.getVariable(anyString())).thenAnswer(answer);
        jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                eq("select CAST(extract(EPOCH from clock_timestamp()) * 1000 AS bigint)"),
                same(Long.class)
        )).thenAnswer(invocation -> clock.instant().toEpochMilli());
    }

    /**
     * Проверка основного кейса:
     * при при превышении порога интенсивности должно выбрасываться исключение
     */
    @Test(expected = CycleOverThresholdException.class)
    public void baseCaseTest() {
        control  = new CyclicControl(jdbcTemplate, 3, 0.2);
        clock = new TestClock(Duration.ofMillis(10));
        control.execute(delegateExecution);
        control.execute(delegateExecution);
        control.execute(delegateExecution);
    }

    /**
     * Кейс когда все отметки времени одинаковые,
     * не должно быть деления на 0 (Не должно быть {@link ArithmeticException})
     */
    @Test(expected = CycleOverThresholdException.class)
    public void sameTick() {
        control  = new CyclicControl(jdbcTemplate, 2, 1.0);
        clock = new TestClock(Duration.ofMillis(0));
        control.execute(delegateExecution);
        control.execute(delegateExecution);
        control.execute(delegateExecution);
    }

    /**
     * Не должно выбрасываться исключение когда, интенсивность вызовов не превышает порог
     */
    @Test
    public void belowThreshold() {
        control  = new CyclicControl(jdbcTemplate, 2, 2.0);
        clock = new TestClock(Duration.ofSeconds(1));
        control.execute(delegateExecution);
        control.execute(delegateExecution);
        control.execute(delegateExecution);
        control.execute(delegateExecution);
    }

    /**
     * Не должно выбрасываться исключение, если мин. кол-во циклов не пройдено
     */
    @Test
    public void belowMinCycleCount() {
        control  = new CyclicControl(jdbcTemplate, 3, 1.0);
        clock = new TestClock(Duration.ofMillis(1));
        control.execute(delegateExecution);
        control.execute(delegateExecution);
    }

    /**
     * Проверка при неравномерном появлении отметок времени, не превышающем порог
     */
    @Test
    public void unevenTicks() {
        control  = new CyclicControl(jdbcTemplate, 3, 1.0);
        clock = new TestClock(Duration.ofMillis(1), Duration.ofSeconds(2));
        control.execute(delegateExecution);
        control.execute(delegateExecution);
        control.execute(delegateExecution);
        control.execute(delegateExecution);
    }

    private static class CollectVariablesAnswer implements Answer {

        private Map<String, Object> vars = new HashMap<>();

        @Override
        public Object answer(InvocationOnMock invocation) {
            switch (invocation.getMethod().getName()) {
                case "setVariable":
                    vars.put(invocation.getArgument(0, String.class), invocation.getArgument(1));
                    return null;
                case "getVariable":
                    return vars.get(invocation.getArgument(0, String.class));
            }
            return null;
        }
    }

    /**
     * Часы возвращающие при последовательных вызовах отметки времени через интервалы
     * переданные в конструкторе.
     */
    static class TestClock extends Clock {

        private final Iterator<Duration> intervalsIt;

        private Instant lastInstant;

        TestClock(Duration... intervals) {
            this.intervalsIt = Iterators.cycle(intervals);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            if (null == lastInstant) {
                lastInstant = Instant.now();
            } else {
                lastInstant = lastInstant.plusNanos(intervalsIt.next().toNanos());
            }
            return lastInstant;
        }
    }
}
