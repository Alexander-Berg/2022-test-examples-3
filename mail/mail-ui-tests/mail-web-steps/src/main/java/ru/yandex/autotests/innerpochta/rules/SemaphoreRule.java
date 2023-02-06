package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;

/**
 * @author marchart
 */
public class SemaphoreRule extends ExternalResource {

    private String semaphorePermits = UrlProps.urlProps().getSemaphorePermits();

    private String semaphoreHost = UrlProps.urlProps().getBaseUri();

    public static SemaphoreRule semaphoreRule() {
        return new SemaphoreRule();
    }

    public TestRule enableSemaphore() {
        if (semaphorePermits != null) {
            return new ru.yandex.qatools.hazelcast.SemaphoreRule(
                "Enable YMail semaphore for host " + semaphoreHost,
                Integer.parseInt(semaphorePermits)
            );
        }
        return new TestRule() {
            @Override
            public Statement apply(Statement statement, Description description) {
                return statement;
            }
        };
    }
}
