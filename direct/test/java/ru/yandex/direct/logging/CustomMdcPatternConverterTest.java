package ru.yandex.direct.logging;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(Parameterized.class)
public class CustomMdcPatternConverterTest {
    private final String[] options;
    private final CustomMdcPatternConverter.CustomMdcOptions expected;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    public CustomMdcPatternConverterTest(
            String options, CustomMdcPatternConverter.CustomMdcOptions expected, boolean fail
    ) {
        this.options = new String[]{options};
        this.expected = expected;
        if (fail) {
            this.thrown.expect(IllegalArgumentException.class);
        }
    }

    @Test
    public void testExpression() {
        CustomMdcPatternConverter.CustomMdcOptions actual = CustomMdcPatternConverter.CustomMdcOptions.parse(options);
        assertThat(actual, beanDiffer(expected));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{
                        "traceHostname",
                        new CustomMdcPatternConverter.CustomMdcOptions("traceHostname", null),
                        false},
                new Object[]{
                        "traceHostname,default('test-host')",
                        new CustomMdcPatternConverter.CustomMdcOptions("traceHostname", "test-host"),
                        false},
                new Object[]{"traceHostname,default('test-host'),else()", null, true},
                new Object[]{"default($hostname),traceHostname", null, true}
        );
    }
}
