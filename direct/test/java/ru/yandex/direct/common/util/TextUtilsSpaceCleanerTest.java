package ru.yandex.direct.common.util;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class TextUtilsSpaceCleanerTest {

    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameter(value = 1)
    public String expectedText;

    @Parameterized.Parameter(value = 2)
    @SuppressWarnings("unused")
    public String description;

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {null, null, "строка null"},
                {"", "", "Пустая строка"},
                {"    ", "", "Пробелы -> Пустая строка"},
                {"\u0009a b\u0009", "a b", "удаление пробельных символов в начале и конце"},
                {"a\u0009\u0009\u0009b   c", "a b c",
                        "замена несколько последовательных пробельных символов на пробел"},
                {"a ,\u0009b", "a,b", "удаление пробельных символов перед и после знаков пунктуации"},
                {"a . b", "a.b", "Разделитель точка"},
                {"a , b", "a,b", "Разделитель запятая"},
                {"a ; b", "a;b", "Разделитель точка с запятой"},
                {"a ! b", "a!b", "Разделитель восклицательный знак"},
                {"a : b", "a:b", "Разделитель двоеточие"},
                {"a ( b", "a(b", "Разделитель левая скобка"},
                {"a ) b", "a)b", "Разделитель правая скобка"},
                {"a ? b", "a?b", "Разделитель вопросительный знак"},
                {"слона ( купить ) ", "слона(купить)", "Русские символы с множественными пробелами"},
                {"hi ! ? ", "hi! ?", "Несколько разделителей"}
        });
    }

    @Test
    public void testSuccess() {
        String actualText = TextUtils.spaceCleaner(text);
        assertThat(actualText).isEqualTo(expectedText);
    }
}
