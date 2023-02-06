package ru.yandex.market.delivery.transport_manager.util;

import java.util.concurrent.atomic.AtomicReference;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.objenesis.ObjenesisHelper;

import ru.yandex.misc.io.IoFunction;

@SuppressWarnings({"HideUtilityClassConstructor", "ParameterNumber"})
@UtilityClass
public class ProxyUtil {
    /**
     * Способ резолва циклических зависимостей:
     * 1. Делаем atomicReference, который пока не заполеняем
     * 2. Создаём прокси, который использует atomicReference
     * 3. Создаём оба зависящих друг от друга бина. Одному из них передаём прокси вместо ещё не созданного объекта
     * 4. После создания помещаем в atomicReference
     *
     * @param creator ф-ция создания объекта. На входе прокси для объекта, который будет создан. На выходе сам объект.
     * @param clazz класс создаваемого объекта (для прокси)
     */
    @SneakyThrows
    public static <T> T createWithSelfReference(IoFunction<T, T> creator, Class<T> clazz) {
        AtomicReference<T> selfRef = new AtomicReference<>();
        MethodInterceptor methodInterceptor = (obj, method, args, proxy) -> {
            T realValue = selfRef.get();
            if (realValue == null) {
                throw new IllegalStateException("Proxy is not initialized yet");
            }
            return method.invoke(realValue, args);
        };

        Enhancer e = new Enhancer();
        e.setSuperclass(clazz);
        e.setCallbackType(methodInterceptor.getClass());

        final Class<?> proxyClass = e.createClass();
        Enhancer.registerCallbacks(proxyClass, new Callback[]{methodInterceptor});
        T p = (T) ObjenesisHelper.newInstance(proxyClass);

        T realValue = creator.apply(p);
        selfRef.set(realValue);
        return realValue;
    }
}
