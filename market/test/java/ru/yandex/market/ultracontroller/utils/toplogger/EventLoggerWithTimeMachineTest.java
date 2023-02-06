package ru.yandex.market.ultracontroller.utils.toplogger;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class EventLoggerWithTimeMachineTest {

    @Test
    public void push() throws InterruptedException {
        List<Integer> result = new ArrayList<>();
        EventLoggerWithTimeMachine<Integer> eventLogger = new EventLoggerWithTimeMachine<>(
            0, 3, 100, x -> result.add(x.getInnerEvent())
        );
        eventLogger.push(0,1, 1);
        eventLogger.push(1,2, 2);
        eventLogger.push(2,3, 3);
        eventLogger.push(3,4, 4);
        eventLogger.heartbeat(101);
        eventLogger.push(102, 5, 1);
        eventLogger.heartbeat(204);
        assertEquals(Arrays.asList(4, 3, 2, 5), result);
    }
}