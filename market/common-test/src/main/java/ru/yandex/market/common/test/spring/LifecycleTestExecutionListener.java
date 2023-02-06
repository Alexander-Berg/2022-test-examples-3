package ru.yandex.market.common.test.spring;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import ru.yandex.market.common.test.spring.event.AfterTestClassEvent;
import ru.yandex.market.common.test.spring.event.AfterTestExecutionEvent;
import ru.yandex.market.common.test.spring.event.AfterTestMethodEvent;
import ru.yandex.market.common.test.spring.event.BeforeTestClassEvent;
import ru.yandex.market.common.test.spring.event.BeforeTestExecutionEvent;
import ru.yandex.market.common.test.spring.event.BeforeTestMethodEvent;
import ru.yandex.market.common.test.spring.event.PrepareTestInstanceEvent;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class LifecycleTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        testContext.getApplicationContext().publishEvent(new BeforeTestClassEvent(testContext));
    }

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        testContext.getApplicationContext().publishEvent(new PrepareTestInstanceEvent(testContext));
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        testContext.getApplicationContext().publishEvent(new BeforeTestMethodEvent(testContext));
    }

    @Override
    public void beforeTestExecution(TestContext testContext) throws Exception {
        testContext.getApplicationContext().publishEvent(new BeforeTestExecutionEvent(testContext));
    }

    @Override
    public void afterTestExecution(TestContext testContext) throws Exception {
        testContext.getApplicationContext().publishEvent(new AfterTestExecutionEvent(testContext));
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        testContext.getApplicationContext().publishEvent(new AfterTestMethodEvent(testContext));
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        testContext.getApplicationContext().publishEvent(new AfterTestClassEvent(testContext));
    }

}
