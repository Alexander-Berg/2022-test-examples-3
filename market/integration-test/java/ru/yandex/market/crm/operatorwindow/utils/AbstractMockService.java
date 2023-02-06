package ru.yandex.market.crm.operatorwindow.utils;

import org.mockito.Mockito;

public abstract class AbstractMockService<T> implements MockService {
    private final T mock;

    public AbstractMockService(T mock) {
        this.mock = mock;
    }

    @Override
    public void clear() {
        Mockito.reset(mock);
    }
}
