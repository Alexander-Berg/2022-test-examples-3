package ru.yandex.market.logistics.lom.utils;

import java.util.function.Function;

import lombok.Getter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Captor результата возврата mock/spy.
 * Опционально применяет spie к результату.
 *
 * @param <T>
 */
@Getter
public class ResultCaptor<T> implements Answer<T> {
    private T result;
    private final Function<T, T> spie;

    public ResultCaptor(Function<T, T> spie) {
        this.spie = spie;
    }

    public ResultCaptor() {
        this(Function.identity());
    }

    @SuppressWarnings("unchecked")
    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        result = spie.apply((T) invocation.callRealMethod());
        return result;
    }
}
