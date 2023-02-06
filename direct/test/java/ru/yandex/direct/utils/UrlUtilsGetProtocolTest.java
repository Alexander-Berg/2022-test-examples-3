package ru.yandex.direct.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.UrlUtils.getProtocol;

@RunWith(Parameterized.class)
public class UrlUtilsGetProtocolTest {
    @Parameterized.Parameters
    public static String[][] parameters() {
        return new String[][]{
                {"www.yandex.ru:8080", "http"},
                {"https://www.yandex.ru:8080", "https"},
                {"https://yandex.ru:8080", "https"},
                {"http://www.yandex.ru", "http"},
                {"grpc://www.yandex.ru", "http"},
                {"", "http"},
        };
    }

    @Parameterized.Parameter(0)
    public String input;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void getProtocolTest() {
        assertThat(getProtocol(input, "http")).isEqualTo(expected);
    }
}
