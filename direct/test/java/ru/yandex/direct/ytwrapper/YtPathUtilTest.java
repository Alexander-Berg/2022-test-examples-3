package ru.yandex.direct.ytwrapper;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class YtPathUtilTest {
    private static final int ITERATIONS_NUM = 10000;

    @Test
    public void testTmpPath() {
        Set<String> tmpPaths = new HashSet<>();
        for (int i = 0; i < ITERATIONS_NUM; i++) {
            String path = YtPathUtil.generateTemporaryPath();
            assertTrue("Path is valid", path.startsWith(YtPathUtil.TEMPORARY_TABLES_STORAGE + "/"));
            tmpPaths.add(path);
        }
        assertThat("Paths are unique", tmpPaths.size(), equalTo(ITERATIONS_NUM));
    }

    @Test
    public void testPathGen() {
        assertThat(
                "Generated path equals expected",
                YtPathUtil.generatePath("//test/test/./"),
                equalTo("//test/test"));
        assertThat(
                "Generated path equals expected",
                YtPathUtil.generatePath("//test/test/../test2"),
                equalTo("//test/test2"));
        assertThat(
                "Generated path equals expected",
                YtPathUtil.generatePath("//test/test/", "./", "../", "test2/test3"),
                equalTo("//test/test2/test3"));
    }
}
