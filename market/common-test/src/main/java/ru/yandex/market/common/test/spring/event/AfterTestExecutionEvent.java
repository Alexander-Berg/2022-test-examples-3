package ru.yandex.market.common.test.spring.event;

import org.springframework.test.context.TestContext;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class AfterTestExecutionEvent extends TestLifecyleEvent {
    public AfterTestExecutionEvent(TestContext testContext) {
        super(testContext);
    }
}
