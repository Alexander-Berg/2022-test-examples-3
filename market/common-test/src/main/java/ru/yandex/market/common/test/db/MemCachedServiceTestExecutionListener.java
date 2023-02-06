package ru.yandex.market.common.test.db;

import java.util.function.Consumer;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.common.cache.memcached.impl.DefaultMemCachedAgent;
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;
import ru.yandex.market.common.test.mockito.MemCachedServiceMock;

/**
 * Listener для сброса моков мемкеша перед каждым тестом.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class MemCachedServiceTestExecutionListener extends AbstractTestExecutionListener {
    @Override
    public void beforeTestMethod(TestContext testContext) {
        doForEachBean(testContext, MemCachedServiceMock.class, MemCachedServiceMock::cleanAll);
        doForEachBean(testContext, DefaultMemCachedAgent.class, DefaultMemCachedAgent::close); // clean local caches
        doForEachBean(testContext, MemCachedClientFactoryMock.class, MemCachedClientFactoryMock::close);
    }

    private <T> void doForEachBean(TestContext testContext, Class<T> clazz, Consumer<? super T> action) {
        BeanFactoryUtils.beansOfTypeIncludingAncestors(
                testContext.getApplicationContext(),
                clazz,
                true,
                false
        )
                .values()
                .forEach(action);
    }
}
