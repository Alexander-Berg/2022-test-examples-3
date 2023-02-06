package ru.yandex.direct.core.entity.vcard;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VcardWorktimeBadFormatsTest {
    @Parameterized.Parameter(0)
    public String encodedWorktime;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"0#1#9#0#18#0,2#4#9#0#18#0"},
                {"0#1#9#0#18#0;2#4#9#0#18#0;"},
                {";0#1#9#0#18#0;2#4#9#0#18#0"},
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeExceptionTest() {
        VcardWorktime.fromEncodedString(encodedWorktime);
    }
}
