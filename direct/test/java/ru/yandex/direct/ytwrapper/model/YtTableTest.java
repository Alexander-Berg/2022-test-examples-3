package ru.yandex.direct.ytwrapper.model;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class YtTableTest {
    private YtTable table;

    @Before
    public void before() {
        table = new YtTable("//some/very/long/path");
    }

    @Test
    public void testTableName() {
        assertThat("Имя получается корректно", table.getName(), equalTo("path"));
    }

    @Test
    public void testTableYPath() {
        assertThat("Путь получается корректно", table.ypath(), equalTo(YPath.simple("//some/very/long/path")));
    }

    @Test
    public void testTableYPathWithParams() {
        assertThat(
                "Имя получается корректно",
                table.ypath(Arrays.asList(
                        new YtField<>("f1", Long.class),
                        new YtField<>("f2", String.class))),
                equalTo(YPath.simple("//some/very/long/path").withColumns("f1", "f2"))
        );
    }
}
