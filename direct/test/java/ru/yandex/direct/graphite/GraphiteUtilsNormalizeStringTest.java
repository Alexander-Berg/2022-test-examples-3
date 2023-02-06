package ru.yandex.direct.graphite;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.graphite.GraphiteUtils.normalizeMetricPart;

@RunWith(Parameterized.class)
public class GraphiteUtilsNormalizeStringTest {

    @Parameterized.Parameter(0)
    public String sourceString;

    @Parameterized.Parameter(1)
    public String normalizedString;

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"", "_"},
                {" ", "_"},
                {"  ", "__"},
                {".", "_"},
                {"..", "__"},
                {". ", "__"},
                {" .", "__"},
                {"ppcdev1.yandex.ru", "ppcdev1_yandex_ru"},
                {"pycckиe нe cдaюTcR", "pycck_e__e_c_a_TcR"},
                {"some       synthetic.....test. .  ..string", "some_______synthetic_____test_______string"},
        });
    }

    @Test
    public void test() {
        assertThat(normalizeMetricPart(sourceString)).isEqualTo(normalizedString);
    }
}
