package ru.yandex.direct.core.entity.vcard;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VcardWorktimeGlueTest {
    @Parameterized.Parameter(0)
    public String ungluedString;

    @Parameterized.Parameter(1)
    public String gluedString;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"0#1#9#0#18#0;2#4#9#0#18#0", "0#4#09#00#18#00"},
                {"0#1#9#0#18#0;2#2#9#0#18#0;3#4#09#0#18#0", "0#4#09#00#18#00"},
                {"0#1#9#0#18#0;2#2#9#0#18#0;3#4#09#0#18#0;5#5#11#00#18#00;6#6#11#00#18#00",
                        "0#4#09#00#18#00;5#6#11#00#18#00"},
                {"0#1#9#0#18#0;6#6#11#00#18#00;3#4#09#0#18#0;2#2#9#0#18#0;5#5#11#00#18#00",
                        "0#4#09#00#18#00;5#6#11#00#18#00"},
        });
    }

    @Test
    public void glueTest() {
        VcardWorktime expectedGlued = VcardWorktime.fromEncodedString(gluedString);

        VcardWorktime decodedUngluedWorktime = VcardWorktime.fromEncodedString(ungluedString);

        assertEquals(expectedGlued, decodedUngluedWorktime);
    }
}
