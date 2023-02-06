package ru.yandex.direct.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.UrlUtils.trimPort;

@RunWith(Parameterized.class)
public class UrlUtilsTrimPortTest {
    @Parameterized.Parameters
    public static String[][] parameters() {
        return new String[][]{
                {"www.yandex.ru:8080", "www.yandex.ru"},
                {"dsaf:80", "dsaf"},
                {"www.yandex.ru", "www.yandex.ru"},
                {"127.0.0.1:8080", "127.0.0.1"},
                {"[aaa::1]:123", "[aaa::1]"},
                {"[aaa::1]", "[aaa::1]"},
        };
    }

    @Parameterized.Parameter(0)
    public String input;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void trimPortTest() {
        assertThat(trimPort(input)).isEqualTo(expected);
    }
}
