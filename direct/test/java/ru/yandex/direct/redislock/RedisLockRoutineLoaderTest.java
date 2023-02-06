package ru.yandex.direct.redislock;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * This test is paranoid: sha1 digests are hardcoded.
 */
public class RedisLockRoutineLoaderTest {
    @Test
    public void initializationSanityTest() throws Exception {
        RedisLockRoutineLoader loader = new RedisLockRoutineLoader();
        String expected = "5e144a32d086512d542a502d60896cfe424afc5d".toLowerCase();
        assertThat(loader.getIterativeLockRoutine().getDigest().toLowerCase(), is(expected));

        expected = "c4a37c082458bc03f4da77f00c8abf7793104458".toLowerCase();
        assertThat(loader.getUnlockRoutine().getDigest().toLowerCase(), is(expected));
    }
}
