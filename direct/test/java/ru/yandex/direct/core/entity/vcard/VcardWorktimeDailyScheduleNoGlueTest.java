package ru.yandex.direct.core.entity.vcard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.VcardWorktime.DailySchedule;

import static org.junit.Assert.assertFalse;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VcardWorktimeDailyScheduleNoGlueTest {
    @Parameterized.Parameter(0)
    public String leftEncodedSchedule;

    @Parameterized.Parameter(1)
    public String rightEncodedSchedule;

    @Parameterized.Parameters(name = "{0} и {1} нельзя склеить")
    public static Collection<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<>(Arrays.asList(new Object[][]{
                {"5#5#11#0#18#0", "6#6#11#0#18#1"},
                {"5#5#11#0#18#0", "3#3#11#0#18#0"},
        }));
        Object[][] swappedTestCases =
                testCases.stream().map(a -> new Object[]{a[1], a[0]}).toArray(Object[][]::new);
        testCases.addAll(Arrays.asList(swappedTestCases));

        return testCases;
    }

    @Test
    public void cantBeGluedTest() {
        DailySchedule leftSchedule = DailySchedule.fromEncodedString(leftEncodedSchedule);
        DailySchedule rightSchedule = DailySchedule.fromEncodedString(rightEncodedSchedule);

        assertFalse(String.format("расписания %s и %s нельзя склеить", leftSchedule, rightSchedule),
                leftSchedule.canBeGluedWith(rightSchedule));
    }

    @Test(expected = IllegalArgumentException.class)
    public void glueTest() {
        DailySchedule leftSchedule = DailySchedule.fromEncodedString(leftEncodedSchedule);
        DailySchedule rightSchedule = DailySchedule.fromEncodedString(rightEncodedSchedule);

        DailySchedule gluedSchedule = leftSchedule.glueWith(rightSchedule);
    }
}
