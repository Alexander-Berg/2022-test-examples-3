package ru.yandex.travel.testing.mockito;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ThreadSafeMockAnswer<T> implements Answer<Object>, ThreadSafeMock<T> {
    private final Class<T> implType;
    private final MockHolder<T> defaultImpl;
    private final AtomicReference<MockHolder<T>> overriddenImpl;

    public ThreadSafeMockAnswer(Class<T> implType, Consumer<T> defaultInitializer) {
        this.implType = implType;
        this.defaultImpl = createNewImpl(null, defaultInitializer);
        this.overriddenImpl = new AtomicReference<>(null);
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        try {
            Object impl;
            if (invocation.getMethod().getDeclaringClass() == ThreadSafeMock.class) {
                impl = this;
            } else {
                impl = getCurrentMocksHolder();
            }
            return invocation.getMethod().invoke(impl, invocation.getArguments());
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @Override
    public void resetToDefaultMocks() {
        overriddenImpl.set(null);
    }

    @Override
    public void initNewMocks(Consumer<T> initializer) {
        overriddenImpl.set(createNewImpl(defaultImpl, initializer));
    }

    @Override
    public void extendCurrentMocks(Consumer<T> initializer) {
        overriddenImpl.set(createNewImpl(getCurrentMocksHolderImpl(), initializer));
    }

    @Override
    public T getCurrentMocksHolder() {
        return getCurrentMocksHolderImpl().impl;
    }

    public MockHolder<T> getCurrentMocksHolderImpl() {
        MockHolder<T> overriddenImpl = this.overriddenImpl.get();
        return overriddenImpl != null ? overriddenImpl : defaultImpl;
    }

    private MockHolder<T> createNewImpl(MockHolder<T> base, Consumer<T> initializer) {
        T newImpl = Mockito.mock(implType);
        if (base != null && base.initializer != null) {
            initializer = initializer != null ?
                    base.initializer.andThen(initializer) :
                    base.initializer;
        }
        if (initializer != null) {
            initializer.accept(newImpl);
        }
        return new MockHolder<>(initializer, newImpl);
    }

    @RequiredArgsConstructor
    private static class MockHolder<T> {
        private final Consumer<T> initializer;
        private final T impl;
    }
}
