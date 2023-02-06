package ru.yandex.market.core.test.context;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

/**
 * Более умный аналог {@code <mockito:spy/>}.
 * <ol>
 * <li>Работает в том числе с EarlyBeanReference.
 * <li>Использует не spy, а mock с настройками по умолчанию на простое делегирование вызовов в оригинальный объект.
 * Это нужно для того, чтобы early reference, уже использованные в других объектах, увидели инициализированные
 * property, чего не происходит при использовании обычного spy, который копирует свойства объекта, а не использует
 * делегирование.
 * </ol>
 * Для работы требуется разрешение Spring на "переопределение" уже использованных объектов.
 * Реального переопределения здесь не происходит, т.к. возвращается уже закэшированный мок.
 * <p>
 * // TODO удалить этот PostProcessor после исправления тикета https://st.yandex-team.ru/MBI-30432
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 * @see ru.yandex.market.core.test.context.AllowSpiesInitializer
 * @see AbstractAutowireCapableBeanFactory#setAllowRawInjectionDespiteWrapping(boolean)
 */
public class EarlyDelegatingSpy extends InstantiationAwareBeanPostProcessorAdapter {

    private final Set<String> beanNames;

    private final Map<String, Object> earlyReferences = new HashMap<>();

    public EarlyDelegatingSpy(Set<String> beanNames) {
        this.beanNames = beanNames;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        return doProcess(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return doProcess(bean, beanName);
    }

    private Object doProcess(Object bean, String beanName) {
        if (beanNames.contains(beanName)) {
            return earlyReferences.computeIfAbsent(beanName, s ->
                    Mockito.mock(bean.getClass(),
                            Mockito.withSettings().defaultAnswer(invocation -> {
                                        try {
                                            return invocation.getMethod().invoke(bean, invocation.getArguments());
                                        } catch (InvocationTargetException ite) {
                                            throw ite.getTargetException();
                                        }
                                    }
                            )));
        }
        return bean;
    }
}
