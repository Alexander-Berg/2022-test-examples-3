package ru.yandex.market.checkout.checkouter.lock;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.yt.ytclient.rpc.RpcError;

/**
 * Пока не получается проверить работу с YT на уровне CI, Sanbbox, поэтому используется стаб
 * @see ru.yandex.market.checkout.stub.lock.LockStorageStub
 * */
public class LockServiceTest extends AbstractServicesTestBase {
    @Value("${market.checkout.zookeeper.waitTimeout:2000}") int zkWaitTimeoutMs;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Disabled // Проект заморожен https://st.yandex-team.ru/MARKETCHECKOUT-27367
    public void lockTest(boolean useYtForLock) throws ExecutionException, InterruptedException {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_YT_FOR_LOCK, useYtForLock);
        if (useYtForLock) {
            checkouterFeatureWriter.writeValue(CollectionFeatureType.YT_LOCK_ENTITIES, List.of("order"));
        }

        var executor = Executors.newFixedThreadPool(2);
        var count = new AtomicInteger(0);

        Callable<?> call = () -> {
            try {
                orderService.transaction(1, id -> {
                    try {
                        Thread.sleep(zkWaitTimeoutMs + 500);
                    } catch (InterruptedException e) {
                        Assertions.fail(e);
                    }

                    count.getAndIncrement();

                    return null;
                });
            } catch (RpcError e) {
                Assertions.fail(e);
            } catch (RuntimeException e) {
                if (e.getCause().getClass() != TimeoutException.class) {
                    Assertions.fail(e);
                }
            } catch (Exception e) {
                Assertions.fail(e);
            }
            return null;
        };
        var result1 = executor.submit(call);
        var result2 = executor.submit(call);

        result1.get();
        result2.get();

        // Один поток дошёл до обработки, второй упёрся в блокировку
        Assertions.assertEquals(1, count.get());
    }
}
