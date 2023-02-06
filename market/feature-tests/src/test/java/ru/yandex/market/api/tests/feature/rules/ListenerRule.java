package ru.yandex.market.api.tests.feature.rules;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.yandex.market.api.listener.Listener;
import ru.yandex.market.api.listener.expectations.HttpExpectations;

public class ListenerRule extends ExternalResource {
    private final Listener listener;

    public ListenerRule(HttpExpectations httpExpectations,
                        int port) {
        listener = Listener.listener(httpExpectations, port);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }

    @Override
    protected void before() throws Throwable {
        listener.start();
    }

    @Override
    protected void after() {
        listener.stop();
    }
}
