package ru.yandex.market.common.test.mockito;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

/**
 * Стратегия ответа, для работы с builder'ами.
 * Позволяет использовать сам объект mock'a если тип возвращаемый методом совпадает с типом объекта mock'a.
 *
 * @author Vladislav Bauer
 */
public class SelfReturningAnswer implements Answer<Object> {

    @Override
    public Object answer(final InvocationOnMock invocation) throws Throwable {
        final Object mock = invocation.getMock();
        final Method invocationMethod = invocation.getMethod();

        final Class<?> returnType = invocationMethod.getReturnType();
        final boolean sameTime = returnType.isInstance(mock);

        return sameTime ? mock : Mockito.RETURNS_DEFAULTS.answer(invocation);
    }

}
