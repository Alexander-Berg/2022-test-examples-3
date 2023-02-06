package ru.yandex.market.mbo.tt.providers.clusterizer;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@gmail.com)
 * @date 18.12.2015
 */
@SuppressWarnings("checkstyle:magicNumber")
public class EquitablePoolTest {

    @Test
    public void testGetEqual() throws Exception {
        EquitablePool<String> pool = new EquitablePool<>(Arrays.asList("foo", "bar", "baz"), 3);
        assertThat(pool.get(1), contains("foo"));
        assertThat(pool.get(2), contains("bar", "baz"));
    }

    @Test
    public void testNeededMore() throws Exception {
        EquitablePool<String> pool = new EquitablePool<>(Arrays.asList("foo", "bar", "baz"), 24);
        assertThat(pool.get(8), contains("foo"));
        assertThat(pool.get(16), contains("bar", "baz"));
    }

    @Test
    public void testEmptyPool() throws Exception {
        EquitablePool<String> pool = new EquitablePool<>(Arrays.asList("foo", "bar", "baz"), 24);
        assertThat(pool.get(8), contains("foo"));
        assertThat(pool.get(30), contains("bar", "baz"));
        assertThat(pool.get(666), is(empty()));
    }
}
