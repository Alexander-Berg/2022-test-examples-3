package ru.yandex.market.jmf.logic.def.test.impl;

import org.springframework.stereotype.Component;

import ru.yandex.market.jmf.queue.retry.internal.RetryTaskGroupingStrategy;
import ru.yandex.market.jmf.trigger.impl.AsyncTriggerData;

@Component
public class TestRetryTaskGroupingStrategy implements RetryTaskGroupingStrategy<AsyncTriggerData> {

    private int invocations = 0;

    @Override
    public String getGroupId(AsyncTriggerData context) {
        invocations++;
        return "IM_GROUP_ID";

    }

    public int getInvocations() {
        return invocations;
    }

    public void resetInvocations() {
        invocations = 0;
    }
}
