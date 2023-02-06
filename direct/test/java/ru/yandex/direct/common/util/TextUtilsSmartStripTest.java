package ru.yandex.direct.common.util;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class TextUtilsSmartStripTest {
    private static final String TEST_TEXT = "  a  b "
            + " \u2010\u2011\u2012\u2015\u0096"
            + " \u203a\u2039\u2033\u201e\u201d\u201c\u201a\u201b\u2018"
            + " \u2032`"
            + " \u00ab\u00bb   ";

    @Parameterized.Parameter(value = 0)
    public String text;
    @Parameterized.Parameter(value = 1)
    public boolean replaceAngleQuotes;
    @Parameterized.Parameter(value = 2)
    public String expectedText;

    @Parameterized.Parameters()
    public static Collection<Object[]> getParameters() {
        return asList(
                new Object[]{null, false, null},
                new Object[]{
                        TEST_TEXT,
                        false,
                        "a b ----- \"\"\"\"\"\"\"\"\" '' \u00ab\u00bb"},
                new Object[]{
                        TEST_TEXT,
                        true,
                        "a b ----- \"\"\"\"\"\"\"\"\" '' \"\""});
    }

    @Test
    public void testSuccess() {
        String actualText = TextUtils.smartStrip(text, replaceAngleQuotes);
        assertThat(actualText).isEqualTo(expectedText);
    }
}
