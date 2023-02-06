package ru.yandex.market.common.test.spring.event;

import org.springframework.test.context.TestContext;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class AfterTestClassEvent extends TestLifecyleEvent {
    public AfterTestClassEvent(TestContext testContext) {
        super(testContext);
    }
}
