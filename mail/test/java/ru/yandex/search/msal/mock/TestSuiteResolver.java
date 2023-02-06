package ru.yandex.search.msal.mock;

import java.util.Set;
import java.util.function.Function;

public class TestSuiteResolver
    implements Function<Thread, DbMock>
{
    private final DbMock mock;
    private final Set<Thread> threads;

    public TestSuiteResolver(final DbMock mock, final Set<Thread> threads) {
        this.mock = mock;
        this.threads = threads;
    }

    @Override
    public DbMock apply(final Thread thread) {
        if (threads.contains(thread)) {
            return mock;
        }

        return null;
    }
}
