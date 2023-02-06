package ru.yandex.market.mbo.utils.test;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

public class IntegrationTestUtil {
    private IntegrationTestUtil() {
    }

    public static <T> T configure(ApplicationContext applicationContext, T bean) {
        return configure(applicationContext, bean, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T configure(ApplicationContext applicationContext, T bean, String beanName) {
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
        Object b = factory.initializeBean(bean, beanName);
        return (T) b;
    }
}
