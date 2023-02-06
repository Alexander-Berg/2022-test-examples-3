package ru.yandex.chemodan.ratelimiter;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.util.AppNameHolder;
import ru.yandex.commune.alive2.AliveAppInfo;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author tolmalev
 */
public class ClusteredRpsLimiterTest {

    private final Wrapped wrapped;

    public ClusteredRpsLimiterTest() {
        ClusteredRpsLimiter rpsLimiter = new ClusteredRpsLimiter("name", 1000, null);
        AppNameHolder.setIfNotPresent(new SimpleAppName("test", "test"));
        AppName appName = AppNameHolder.get();

        rpsLimiter.listChanged(Cf.list(new AliveAppInfo(appName.serviceName(), appName.appName(), Instant.now(), "1.0.0", "localhost", 0,
                "good", Option.empty())));

        wrapped = rpsLimiter.wrapLimited(new Wrapped() {
            @Override
            public void execV() {
            }

            @Override
            public int execI() {
                return 1;
            }

            @Override
            public int execThrowRuntime() {
                throw new RuntimeException1();
            }

            @Override
            public int execThrowNonRuntime() throws Exception {
                throw new Exception1();
            }
        }, Wrapped.class);
    }

    @Test
    public void wrapLimited() {
        wrapped.execV();
        Assert.equals(1, wrapped.execI());
    }

    @Test(expected = RuntimeException1.class)
    public void wrapRuntimeException() {
        wrapped.execThrowRuntime();
    }

    @Test(expected = Exception1.class)
    public void wrapNonRuntimeException() throws Exception {
        wrapped.execThrowNonRuntime();
    }

    private interface Wrapped {
        void execV();
        int execI();

        int execThrowRuntime();

        int execThrowNonRuntime() throws Exception;
    }

    private static class RuntimeException1 extends RuntimeException {
    }

    private static class Exception1 extends Exception {
    }
}
