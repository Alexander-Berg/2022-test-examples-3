package ru.yandex.direct.core.entity.vcard;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.VcardWorktime.DailySchedule;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VcardWorktimeDailyScheduleBadFormatsTest {
    @Parameterized.Parameter(0)
    public String encodedSchedule;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {null},
                {""},
                {"0#0#0#0#0"},
                {"0#0#11#00#19#00#0"},
                {"0#0#0#hello#world#0"},
                {"0#0.0#0#0#0#0"},
                {"0e0#00#0#0#0#0"},
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeExceptionTest() {
        DailySchedule.fromEncodedString(encodedSchedule);
    }
}
