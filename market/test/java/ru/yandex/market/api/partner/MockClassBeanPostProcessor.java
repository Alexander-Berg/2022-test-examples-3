package ru.yandex.market.api.partner;

import java.util.List;

import javax.annotation.Nonnull;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * BeanPostProcessor для замокивания всех объектов заданных классов.
 */
public class MockClassBeanPostProcessor implements BeanPostProcessor {

    private final List<Class> classes;

    public MockClassBeanPostProcessor(List<Class> classes) {
        this.classes = classes;
    }

    @Override
    public Object postProcessBeforeInitialization(@Nonnull Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        for (Class clazz : classes) {
            if (beanClass.equals(clazz)) {
                return Mockito.mock(beanClass);
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, String beanName) throws BeansException {
        return bean;
    }
}
