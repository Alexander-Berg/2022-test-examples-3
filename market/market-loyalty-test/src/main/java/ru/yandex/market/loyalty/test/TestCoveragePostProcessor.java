package ru.yandex.market.loyalty.test;

import org.aopalliance.intercept.MethodInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TestCoveragePostProcessor extends AbstractAdvisingBeanPostProcessor {
    private static final long serialVersionUID = 1L;

    private final Map<String, Class<?>> originalClasses = new HashMap<>();
    private final transient TestCoverageRule testCoverageRule;

    public TestCoveragePostProcessor(TestCoverageRule testCoverageRule) {
        this.testCoverageRule = testCoverageRule;
        this.advisor = new DefaultPointcutAdvisor((MethodInterceptor) invocation -> {
            testCoverageRule.calledMethods
                    .computeIfAbsent(invocation.getMethod().getDeclaringClass(), (c) -> new HashSet<>())
                    .add(invocation.getMethod());
            return invocation.proceed();
        });
    }

    @Override
    public Object postProcessBeforeInitialization(@NotNull Object bean, String beanName) throws BeansException {
        Class<?> originalClass = bean.getClass();
        if (testCoverageRule.testsToRun.containsKey(bean.getClass())) {
            //запоминаем все имена бинов, вызовы которых нужно запоминать
            originalClasses.put(beanName, originalClass);
        }
        return bean;
    }


    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) {
        if (originalClasses.containsKey(beanName)) {
            //если наш бин, то
            return super.postProcessAfterInitialization(bean, beanName);
        } else {
            return bean;
        }
    }
}
