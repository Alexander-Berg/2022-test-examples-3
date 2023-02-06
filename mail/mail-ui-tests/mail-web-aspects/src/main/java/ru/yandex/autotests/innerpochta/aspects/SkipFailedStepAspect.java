package ru.yandex.autotests.innerpochta.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author mabelpines
 */

@Aspect
public class SkipFailedStepAspect {

    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Step)")
    void stepMethod() {
    }

    @Pointcut("@annotation(ru.yandex.autotests.innerpochta.annotations.SkipIfFailed)")
    void skipIfFailed() {
    }

    @Pointcut("execution(* *(..))")
    void anyMethod() {
    }

    @Around("stepMethod() && anyMethod() && skipIfFailed()")
    public Object skipFailedStep(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Exception var3) {
//            AllureLifecycle lifecycle = Allure.getLifecycle();
//            lifecycle.updateStep(
//                (testResult) -> {
//                    StepResult step = testResult.getSteps().get(testResult.getSteps().size() - 1);
//                    step.withStatus(Status.SKIPPED).withName("[SKIP] " + step.getName());
//                }
//            );
            return joinPoint.getThis();
        }
    }

}
