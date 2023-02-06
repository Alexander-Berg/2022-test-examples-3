package ru.yandex.market.checkout.pushapi.client.http;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Objects;

public class SpyBeanPostProcessor implements BeanPostProcessor {
    private final String beanName;

    public SpyBeanPostProcessor(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(Objects.equals(this.beanName, beanName)) {
            return Mockito.spy(bean);
        }
        return bean;
    }
}