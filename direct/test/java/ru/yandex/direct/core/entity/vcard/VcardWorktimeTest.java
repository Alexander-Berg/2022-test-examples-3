package ru.yandex.direct.core.entity.vcard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.vcard.VcardWorktime.DailySchedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@ParametersAreNonnullByDefault
public class VcardWorktimeTest {

    @Test
    public void fromEncodedStringEmptyTest() {
        VcardWorktime emptyWorktime = new VcardWorktime(new DailySchedule[0]);

        assertEquals(emptyWorktime, VcardWorktime.fromEncodedString(""));
    }

    @Test
    public void fromEncodedStringNullTest() {
        VcardWorktime emptyWorktime = new VcardWorktime(new DailySchedule[0]);

        assertEquals(emptyWorktime, VcardWorktime.fromEncodedString(null));
    }

    @Test
    public void fromEncodedStringGoodWorktimeTest() {
        VcardWorktime wt = VcardWorktime.fromEncodedString("1#2#3#4#5#6;4#5#6#7#8#9");

        VcardWorktime expectedWorktime = new VcardWorktime(new DailySchedule[]{
                DailySchedule.fromEncodedString("1#2#3#4#5#6"),
                DailySchedule.fromEncodedString("4#5#6#7#8#9")
        });

        assertEquals(expectedWorktime, wt);
    }

    @Test
    public void fromDailySchedulesEmptyTest() {
        VcardWorktime emptyWorktime = new VcardWorktime(new DailySchedule[0]);

        assertEquals(emptyWorktime, VcardWorktime.fromDailySchedules(Collections.emptyList()));
    }

    @Test
    public void fromDailySchedulesTest() {
        List<DailySchedule> dailySchedules = Arrays.asList(
                DailySchedule.fromEncodedString("4#5#6#7#8#9"),
                DailySchedule.fromEncodedString("1#2#3#4#5#6")
        );

        VcardWorktime worktime = VcardWorktime.fromDailySchedules(dailySchedules);

        VcardWorktime expectedWorktime = new VcardWorktime(new DailySchedule[]{
                dailySchedules.get(1),
                dailySchedules.get(0)
        });
        assertEquals(expectedWorktime, worktime);
    }

    @Test
    public void toEncodedStringEmptyTest() {
        VcardWorktime emptyWorktime = new VcardWorktime(new DailySchedule[0]);

        assertThat(emptyWorktime.toEncodedString())
                .isEmpty();
    }

    @Test
    public void toEncodedStringWorktimeTest() {
        VcardWorktime worktime = new VcardWorktime(new DailySchedule[]{
                DailySchedule.fromEncodedString("1#2#3#4#5#6"),
        });

        String expectedEncodedString = "1#2#03#04#05#06";
        assertThat(worktime.toEncodedString())
                .isEqualTo(expectedEncodedString);
    }

    @Test
    public void toEncodedStringWorktimeListTest() {
        VcardWorktime worktime = new VcardWorktime(new DailySchedule[]{
                DailySchedule.fromEncodedString("1#2#3#4#5#6"),
                DailySchedule.fromEncodedString("4#5#6#7#8#9")
        });

        String expectedEncodedString = "1#2#03#04#05#06;4#5#06#07#08#09";
        assertThat(worktime.toEncodedString())
                .isEqualTo(expectedEncodedString);
    }

}
