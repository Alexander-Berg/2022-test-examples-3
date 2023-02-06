package ru.yandex.market.common.test.spring.event;

import org.springframework.test.context.TestContext;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class AfterTestMethodEvent extends TestLifecyleEvent {
    public AfterTestMethodEvent(TestContext testContext) {
        super(testContext);
    }
}
