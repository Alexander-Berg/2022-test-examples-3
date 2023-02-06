package ru.yandex.direct.config;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class DirectConfigPropertySourceTest {
    private DirectConfigPropertySource src;

    @Before
    public void init() {
        Config config = ConfigFactory.parseMap(ImmutableMap.of(
                "app.name.1", 1,
                "app.name.str", "string"
        ));
        src = new DirectConfigPropertySource("xxx", new DirectConfig(config));
    }

    @Test
    public void getPropertyReturnsNumber() throws Exception {
        assertThat(src.getProperty("app.name.1"), is("1"));
    }

    @Test
    public void getPropertyReturnsString() throws Exception {
        assertThat(src.getProperty("app.name.str"), is("string"));
    }

    @Test
    public void getPropertyReturnsExitentStringWithDefault() throws Exception {
        assertThat(src.getProperty("app.name.str3:DEF"), nullValue());
    }

    @Test
    public void getPropertyReturnsExitentStringWithComplexDefault() throws Exception {
        assertThat(src.getProperty("app.name.str3:http://ya.ru"), nullValue());
    }

    @Test
    public void getPropertyReturnsExitentStringWithoutDefault() throws Exception {
        assertThat(src.getProperty("app.name.str:DEF"), nullValue());
    }

}
