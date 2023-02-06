package ru.yandex.market.common.test.spring.event;

import org.springframework.test.context.TestContext;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class BeforeTestMethodEvent extends TestLifecyleEvent {
    public BeforeTestMethodEvent(TestContext testContext) {
        super(testContext);
    }
}
