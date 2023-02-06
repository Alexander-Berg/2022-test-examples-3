package ru.yandex.market.common.test.spring.event;

import org.springframework.test.context.TestContext;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public abstract class TestLifecyleEvent {
    private final TestContext testContext;

    public TestLifecyleEvent(TestContext testContext) {
        this.testContext = testContext;
    }

    public TestContext getTestContext() {
        return testContext;
    }

}
