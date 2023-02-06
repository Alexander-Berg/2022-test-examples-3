package ru.yandex.direct.redislock;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Very simple test: computes sha1 digests and compares them with precomputed ones;
 */
public class RedisScriptingRoutineTest {
    @Test
    public void computeSha1Digest() throws Exception {
        final String filename = "sha1list.txt";
        List<String> lines = ResourceLoader.readResourceAsLines("/" + filename);
        for (String line : lines) {
            String[] split = line.split(" ", 2);
            String expectedSha1Sum = split[0].toLowerCase();
            String routineBody = split[1];
            String computedSha1Sum = RedisScriptingRoutine.of(filename, routineBody).getDigest().toLowerCase();
            assertThat(computedSha1Sum, is(expectedSha1Sum));
        }
    }
}
