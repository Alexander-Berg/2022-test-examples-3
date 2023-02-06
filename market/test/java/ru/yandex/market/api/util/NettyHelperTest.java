package ru.yandex.market.api.util;

import io.netty.util.concurrent.BlockingOperationException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.ApplicationContextHolder;
import ru.yandex.market.api.server.Environment;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;

/**
 * @author dimkarp93
 */
public class NettyHelperTest extends UnitTestBase {

    private Environment env;

    @Override
    public void setUp() throws Exception {
        env = ApplicationContextHolder.getEnvironment();
        ApplicationContextHolder.setEnvironment(Environment.INTEGRATION_TEST);
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        ApplicationContextHolder.setEnvironment(env);
        super.tearDown();
    }

    @Test(expected = BlockingOperationException.class)
    public void timeout() {
        Futures.waitAndGet(
            Pipelines.startWith(asyncOperation(10L))
                .thenSync(x -> Futures.waitAndGet(asyncOperation(10000L)))
        );
    }



    private Future<Integer> asyncOperation(long timeout) {
        Promise<Integer> p = Futures.newPromise();
        new Thread(
            () -> {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {}
                p.trySuccess(12);
            }
        )
        .start();
        return p;
    }
}
