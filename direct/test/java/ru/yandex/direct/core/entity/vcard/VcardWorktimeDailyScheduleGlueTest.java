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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VcardWorktimeDailyScheduleGlueTest {
    @Parameterized.Parameter(0)
    public String leftEncodedSchedule;

    @Parameterized.Parameter(1)
    public String rightEncodedSchedule;

    @Parameterized.Parameter(2)
    public String expectedEncodedSchedule;


    @Parameterized.Parameters(name = "{0} + {1} = {2}")
    public static Collection<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<Object[]>(Arrays.asList(new Object[][]{
                {"0#3#10#15#18#30", "4#5#10#15#18#30", "0#5#10#15#18#30"},
                {"5#5#11#0#18#0", "6#6#11#0#18#0", "5#6#11#0#18#0"},
        }));
        Object[][] swappedTestCases =
                testCases.stream().map(a -> new Object[]{a[1], a[0], a[2]}).toArray(Object[][]::new);
        testCases.addAll(Arrays.asList(swappedTestCases));

        return testCases;
    }

    @Test
    public void canBeGluedTest() {
        DailySchedule leftSchedule = DailySchedule.fromEncodedString(leftEncodedSchedule);
        DailySchedule rightSchedule = DailySchedule.fromEncodedString(rightEncodedSchedule);

        assertTrue(String.format("расписания %s и %s можно склеить", leftSchedule, rightSchedule),
                leftSchedule.canBeGluedWith(rightSchedule));
    }

    @Test
    public void glueTest() {
        DailySchedule leftSchedule = DailySchedule.fromEncodedString(leftEncodedSchedule);
        DailySchedule rightSchedule = DailySchedule.fromEncodedString(rightEncodedSchedule);

        DailySchedule expectedSchedule = DailySchedule.fromEncodedString(expectedEncodedSchedule);
        DailySchedule gluedSchedule = leftSchedule.glueWith(rightSchedule);
        assertEquals(String.format("расписания %s и %s правильно склеились", leftSchedule, rightSchedule),
                expectedSchedule, gluedSchedule);
    }
}
