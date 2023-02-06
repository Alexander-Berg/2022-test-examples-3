package ru.yandex.autotests.innerpochta.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

/**
 * @author a-zoshchuk
 */
@Aspect
public class MakeScreenAfterStepAspect {

    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Step)")
    public void withStepAnnotation() {
    }

    @Pointcut("@annotation(ru.yandex.autotests.innerpochta.annotations.WithoutScreenshot)")
    public void withNoScreenshotAnnotation() {
    }

    @Pointcut("execution(* *(..))")
    public void anyMethod() {
    }

//    @After("withStepAnnotation() && anyMethod() && !withNoScreenshotAnnotation()")
//    public void takeScreenshot(JoinPoint thisJoinPoint) throws Throwable {
//        if (thisJoinPoint.getThis() != null) {
//            ((AllSteps) thisJoinPoint.getThis()).getScreenshot();
//        }
//    }
}
