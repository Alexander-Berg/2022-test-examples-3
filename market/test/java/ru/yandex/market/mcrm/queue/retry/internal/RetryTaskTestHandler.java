package ru.yandex.market.mcrm.queue.retry.internal;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import ru.yandex.market.mcrm.queue.retry.AbstractRetryTaskHandler;
import ru.yandex.market.mcrm.utils.serialize.ObjectSerializeService;

@Component(RetryTaskTestHandler.BEAN_NAME)
public class RetryTaskTestHandler extends AbstractRetryTaskHandler<TestContext> {

    public static final String BEAN_NAME = "retryTaskTestHandler";

    private Function<TestContext, Boolean> handler = (r) -> true;

    private TestContext task;
    private boolean successful;
    private boolean failure;

    public RetryTaskTestHandler(ObjectSerializeService serializeService) {
        super(TestContext.class, serializeService);
    }

    public TestContext getTask() {
        return task;
    }

    @Override
    public void handleSuccessful(TestContext task) {
        this.successful = true;
    }

    @Override
    public void handleFailure(TestContext task) {
        this.failure = true;
    }

    @Override
    public boolean invoke(TestContext task) {
        this.task = task;
        return handler.apply(task);
    }

    public boolean isFailure() {
        return failure;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setHandler(Function<TestContext, Boolean> handler) {
        this.handler = handler;
    }

    public void reset() {
        handler = (r) -> true;
        task = null;
        successful = false;
        failure = false;
    }
}
