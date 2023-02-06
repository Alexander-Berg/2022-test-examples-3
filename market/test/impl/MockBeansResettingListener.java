package ru.yandex.market.jmf.utils.test.impl;

import java.util.HashSet;
import java.util.Set;

import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.test.context.event.annotation.AfterTestMethod;

@Component
public class MockBeansResettingListener implements BeanPostProcessor {
    private final Set<Object> mockBeans;

    public MockBeansResettingListener() {
        this.mockBeans = new HashSet<>();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        MockingDetails mockingDetails = Mockito.mockingDetails(bean);
        if (mockingDetails.isMock()) {
            mockBeans.add(bean);
        }

        return bean;
    }

    @AfterTestMethod
    public void resetMocks() {
        Mockito.reset(mockBeans.toArray());
    }
}
