package ru.yandex.direct.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.UrlUtils.isUrlInternal;

@RunWith(Parameterized.class)
public class UrlUtilsIsInternalTest {

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][]{
                {"www.yandex.ru:8080", false},
                {"https://www.ya.ru", false},
                {"https://www.yandex-team.ru:8080", true},
                {"https://yandex.net:8080", true},
                {"http://www.yandex.net", true},
                {"http://www.yandex.net/134/123/123", true},
                {"http://12312323.storage-int.mds.yandex.net", true},
                {"http://123233.proxy.sandbox.yandex-team.ru", true},
                {"http://123233.proxy.sandboxyandex-team.ru", false},
                {"http://wwwyandex.net", false},
                {"http://194.87.237.37/static/yandex_new_all_smb.yml", false},
                {"http://ТепличныеСветильники.РФ", false}
        };
    }


    @Parameterized.Parameter()
    public String input;

    @Parameterized.Parameter(1)
    public boolean expected;

    @Test
    public void isUrlInternalTest() {
        assertThat(isUrlInternal(input)).isEqualTo(expected);
    }
}
