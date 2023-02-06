package ru.yandex.market.jmf.timings.test.impl;

import org.springframework.stereotype.Service;

import ru.yandex.market.jmf.timings.impl.TimerTriggerContext;
import ru.yandex.market.jmf.timings.impl.TimerTriggerHandler;

@Service
public class TimerTestUtils {

    private final TimerTriggerHandler timerTriggerHandler;

    public TimerTestUtils(TimerTriggerHandler timerTriggerHandler) {
        this.timerTriggerHandler = timerTriggerHandler;
    }

    // принудительно вызывает срабатывание триггера на истечение таймера
    public void simulateTimerExpiration(String entityGid, String attributeCode) {
        TimerTriggerContext triggerContext = new TimerTriggerContext(entityGid, attributeCode);
        timerTriggerHandler.invoke(triggerContext);
    }
}
