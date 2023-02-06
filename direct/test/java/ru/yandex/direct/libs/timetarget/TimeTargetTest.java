package ru.yandex.direct.libs.timetarget;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public class TimeTargetTest {

    @Test
    public void parseRawString_success_parseSimpleString() {
        TimeTarget timeTarget = TimeTarget.parseRawString("1Ab2Ab3A4Au8;p:w");

        TimeTarget expected = new TimeTarget()
                .withPreset(TimeTarget.Preset.WORKTIME)
                .withWeekdayCoefMap(ImmutableMap.of(
                        WeekdayType.MONDAY, new HoursCoef().withHourCoef(0, 10),
                        WeekdayType.TUESDAY, new HoursCoef().withHourCoef(0, 10),
                        WeekdayType.WEDNESDAY, new HoursCoef().withHourCoef(0, 100),
                        WeekdayType.THURSDAY, new HoursCoef().withHourCoef(0, 200),
                        WeekdayType.HOLIDAY, new HoursCoef()
                ))
                .withOriginalTimeTarget("1Ab2Ab3A4Au8;p:w");

        assertThat(timeTarget, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void parseRawString_success_parseWorkingHolidays() throws Exception {
        TimeTarget timeTarget = TimeTarget.parseRawString("3A4Au5AbBbCbDb6AcBcCcDc9");

        TimeTarget expected = new TimeTarget()
                .withWeekdayCoefMap(ImmutableMap.of(
                        WeekdayType.WEDNESDAY, new HoursCoef().withHourCoef(0, 100),
                        WeekdayType.THURSDAY, new HoursCoef().withHourCoef(0, 200),
                        WeekdayType.FRIDAY, new HoursCoef()
                                .withHourCoef(0, 10)
                                .withHourCoef(1, 10)
                                .withHourCoef(2, 10)
                                .withHourCoef(3, 10),
                        WeekdayType.SATURDAY, new HoursCoef()
                                .withHourCoef(0, 20)
                                .withHourCoef(1, 20)
                                .withHourCoef(2, 20)
                                .withHourCoef(3, 20),
                        WeekdayType.WORKING_WEEKEND, new HoursCoef()
                ))
                .withOriginalTimeTarget("3A4Au5AbBbCbDb6AcBcCcDc9");

        assertThat(timeTarget, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void parseRawString_success_onlyPresetSpecified() {
        TimeTarget timeTarget = TimeTarget.parseRawString(";p:w");

        TimeTarget expected = new TimeTarget()
                .withPreset(TimeTarget.Preset.WORKTIME)
                .withWeekdayCoefMap(getDefaultWeekdayCoefs())
                .withOriginalTimeTarget(";p:w");

        assertThat(timeTarget, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void parseRawString_success_emptyProperties() {
        TimeTarget timeTarget = TimeTarget.parseRawString(";p:");

        TimeTarget expected = new TimeTarget()
                .withWeekdayCoefMap(emptyMap())
                .withOriginalTimeTarget(";p:");

        assertThat(timeTarget, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRawString_fail_weekdayIdNotDefined() throws Exception {
        TimeTarget timeTarget = TimeTarget.parseRawString("Ab2Ab3A8;p:w");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRawString_fail_twoCoefLetters() throws Exception {
        TimeTarget timeTarget = TimeTarget.parseRawString("1Abb2Ab3A8;p:w");
    }

    @Test
    public void toRawFormat_success_convertSimpleCase() throws Exception {
        TimeTarget originTimeTarget = new TimeTarget()
                .withPreset(TimeTarget.Preset.WORKTIME)
                .withWeekdayCoefMap(ImmutableMap.of(
                        WeekdayType.MONDAY, new HoursCoef().withHourCoef(0, 10),
                        WeekdayType.TUESDAY, new HoursCoef().withHourCoef(0, 10),
                        WeekdayType.WEDNESDAY, new HoursCoef().withHourCoef(0, 100),
                        WeekdayType.THURSDAY, new HoursCoef().withHourCoef(0, 200),
                        WeekdayType.HOLIDAY, new HoursCoef()
                ));

        String actual = originTimeTarget.toRawFormat();

        String expected = "1Ab2Ab3A4Au8;p:w";
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void stripProps_success() {
        var softly = new SoftAssertions();
        softly.assertThat(TimeTarget.stripProps("")).isEqualTo("");
        softly.assertThat(TimeTarget.stripProps(";p:a")).isEqualTo("");
        softly.assertThat(TimeTarget.stripProps("1Abb2Ab3A8;p:w")).isEqualTo("1Abb2Ab3A8");
        softly.assertAll();
    }

    private Map<WeekdayType, HoursCoef> getDefaultWeekdayCoefs() {
        Map<WeekdayType, HoursCoef> weekdayCoefs = new HashMap<>();
        for (int dayNum = 1; dayNum < 8; dayNum++) {
            weekdayCoefs.put(WeekdayType.getById(dayNum), TimeTargetUtils.defaultTargetingHoursCoefs());
        }
        return weekdayCoefs;
    }
}

