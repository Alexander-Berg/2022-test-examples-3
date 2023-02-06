package ru.yandex.direct.core.entity.vcard;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.VcardWorktime.DailySchedule;

import static org.junit.Assert.assertNotEquals;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VcardWorktimeDailyScheduleEqualsTest {
    private static final DailySchedule REFERENCE_SCHEDULE = DailySchedule.fromEncodedString("0#0#0#0#0#0");

    @Parameterized.Parameter(0)
    public String differentEncodedSchedule;

    @Parameterized.Parameter(1)
    public String desc;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"1#1#0#0#0#0", "отличный день начала"},
                {"0#1#0#0#0#0", "отличный день окончания"},
                {"0#0#1#0#1#0", "отличный час начала"},
                {"0#0#0#1#0#1", "отличная минута начала"},
                {"0#0#0#0#1#0", "отличный час окончания"},
                {"0#0#0#0#0#1", "отличная минута окончания"},
        });
    }

    @Test
    public void equalsTest() {
        DailySchedule diffSchedule = DailySchedule.fromEncodedString(differentEncodedSchedule);
        assertNotEquals("расписания не эквивалентны", REFERENCE_SCHEDULE, diffSchedule);
    }
}
