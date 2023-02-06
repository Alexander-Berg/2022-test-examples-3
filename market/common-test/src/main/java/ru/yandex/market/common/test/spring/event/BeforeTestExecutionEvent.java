package ru.yandex.market.common.test.spring.event;

import org.springframework.test.context.TestContext;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class BeforeTestExecutionEvent extends TestLifecyleEvent {
    public BeforeTestExecutionEvent(TestContext testContext) {
        super(testContext);
    }
}
