package ru.yandex.direct.redislock.lettuce;

import java.util.List;
import java.util.function.Supplier;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.redislock.ConfigData;
import ru.yandex.direct.redislock.DistributedLock;
import ru.yandex.direct.redislock.DistributedLockException;
import ru.yandex.direct.redislock.RedisLockRoutineLoader;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Ignore
public class LettuceLockTest {

    long lockAttemptTimeout;
    long ttl;
    Supplier<StatefulRedisClusterConnection<String, String>> connectionSupplier;

    @Before
    public void prepare() {
        lockAttemptTimeout = 200;
        ttl = 10000;

        //Хосты и порты взяты из libs-internal/config/src/main/resources/db-config.devtest:redis:host
        List<RedisURI> nodes = ConfigData.REDIS_URIS;
        RedisClusterClient client = RedisClusterClient.create(nodes);
        connectionSupplier = client::connect;
    }

    @Test
    public void testUnitLock() throws DistributedLockException, InterruptedException {
        DistributedLock lettuce = new LettuceLock(connectionSupplier, lockAttemptTimeout, ttl, "test",
                "lock", 1, new RedisLockRoutineLoader());

        assertThat(lettuce.isLocked(), is(false));

        assertThat(lettuce.lock(), is(true));
        assertThat(lettuce.isLocked(), is(true));

        assertThat(lettuce.unlock(), is(true));
        assertThat(lettuce.isLocked(), is(false));

        // Lock after unlock
        assertThat(lettuce.lock(), is(true));
        assertThat(lettuce.isLocked(), is(true));

        assertThat(lettuce.unlock(), is(true));
        assertThat(lettuce.isLocked(), is(false));

    }

    @Test
    public void testLockFailWhenLocked() throws DistributedLockException, InterruptedException {
        DistributedLock lettuce = new LettuceLock(connectionSupplier, lockAttemptTimeout, ttl, "test",
                "lock", 1, new RedisLockRoutineLoader());

        DistributedLock otherLettuce = new LettuceLock(connectionSupplier, lockAttemptTimeout, ttl, "test",
                "lock", 1, new RedisLockRoutineLoader());

        assertThat(lettuce.lock(), is(true));
        assertThat(otherLettuce.lock(), is(false));

        assertThat(lettuce.isLocked(), is(true));
        assertThat(otherLettuce.isLocked(), is(false));

        assertThat(lettuce.unlock(), is(true));
    }

    @Test
    public void testMultiplyLocks() throws InterruptedException {
        DistributedLock[] lettuces = new DistributedLock[10];
        for (int ind = 0; ind < 10; ++ind) {
            lettuces[ind] = new LettuceLock(connectionSupplier, lockAttemptTimeout, ttl, "test",
                    "lock", 5, new RedisLockRoutineLoader());
        }

        for (int ind = 0; ind < 10; ++ind) {
            if (ind < 5) {
                assertThat(lettuces[ind].lock(), is(true));
                assertThat(lettuces[ind].isLocked(), is(true));
            } else {
                assertThat(lettuces[ind].lock(), is(false));
                assertThat(lettuces[ind].isLocked(), is(false));
            }
        }

        for (int ind = 0; ind < 10; ++ind) {
            assertThat(lettuces[ind].unlock(), is(true));
            assertThat(lettuces[ind].isLocked(), is(false));
        }

        // Lock after unlock
        for (int ind = 0; ind < 5; ++ind) {
            assertThat(lettuces[ind].lock(), is(true));
            assertThat(lettuces[ind].isLocked(), is(true));
        }

        for (int ind = 0; ind < 5; ++ind) {
            assertThat(lettuces[ind].unlock(), is(true));
            assertThat(lettuces[ind].isLocked(), is(false));
        }
    }

    @Test
    public void testTryLock() {
        DistributedLock lettuce = new LettuceLock(connectionSupplier, lockAttemptTimeout, ttl, "test",
                "lock", 1, new RedisLockRoutineLoader());

        DistributedLock otherLettuce = new LettuceLock(connectionSupplier, lockAttemptTimeout, ttl, "test",
                "lock", 1, new RedisLockRoutineLoader());

        assertThat(lettuce.tryLock(), is(true));
        assertThat(otherLettuce.tryLock(), is(false));

        assertThat(lettuce.isLocked(), is(true));
        assertThat(otherLettuce.isLocked(), is(false));

        assertThat(lettuce.unlock(), is(true));
        assertThat(lettuce.isLocked(), is(false));
    }
}
