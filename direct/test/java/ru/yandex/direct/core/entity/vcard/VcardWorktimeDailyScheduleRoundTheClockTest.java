package ru.yandex.direct.core.entity.vcard;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.VcardWorktime.DailySchedule;

import static org.junit.Assert.assertEquals;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VcardWorktimeDailyScheduleRoundTheClockTest {
    @Parameterized.Parameter(0)
    public String encodedSchedule;

    @Parameterized.Parameter(1)
    public boolean isRoundTheClock;

    @Parameterized.Parameter(2)
    public String desc;

    @Parameterized.Parameters(name = "{2} = {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"1#1#00#00#00#00", true, "00:00 - 00:00 в один день"},
                {"1#1#07#00#07#00", false, "07:00 - 07:00 в один день"},
                {"0#5#07#00#07#00", false, "07:00 - 07:00 на несколько дней"},
                {"0#6#00#00#00#00", true, "00:00 - 00:00 на неделю"},
                {"0#6#07#00#07#00", true, "07:00 - 07:00 на неделю"},
                {"0#0#00#00#23#45", false, "00:00 - 23:45"},
        });
    }

    @Test
    public void roundTheClockTest() {
        DailySchedule ds = DailySchedule.fromEncodedString(encodedSchedule);

        assertEquals(String.format("%s: isRoundTheClock() == %s", desc, isRoundTheClock), isRoundTheClock,
                ds.isRoundTheClock());
    }
}
