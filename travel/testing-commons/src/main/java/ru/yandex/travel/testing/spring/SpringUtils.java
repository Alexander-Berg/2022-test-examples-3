package ru.yandex.travel.testing.spring;

import org.springframework.aop.framework.AopProxyUtils;

public class SpringUtils {
    @SuppressWarnings("unchecked")
    public static  <T> T unwrapAopProxy(T object) {
        return (T) AopProxyUtils.getSingletonTarget(object);
    }
}
