package ru.yandex.direct.grid.model.entity.campaign.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.grid.model.campaign.timetarget.GdHolidaysSettings;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.test.utils.differ.AlwaysEqualsDiffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.joda.time.DateTimeConstants.DAYS_PER_WEEK;
import static org.joda.time.DateTimeConstants.HOURS_PER_DAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

class CampaignDataConverterTest {

    private static final DefaultCompareStrategy TIME_TARGET_COMPARE_STRATEGY =
            onlyExpectedFields().forFields(newPath("originalTimeTarget")).useDiffer(new AlwaysEqualsDiffer());

    @ParameterizedTest
    @MethodSource("stringProvider")
    void testWithValueSource(String dbDisabledDomains, String dbDisabledSsp, Set<String> expected) {
        assertEquals(expected, CampaignDataConverter.toDisabledPlacesSet(dbDisabledDomains, dbDisabledSsp));
    }

    static Stream<Arguments> stringProvider() {
        return Stream.of(
                arguments(null, null, null),
                arguments("", "[]", null),
                arguments("", "[\"\"]", null),
                arguments("string", "[\"\"]", new TreeSet<>(Set.of("string"))),
                arguments("", "[\"string\"]", new TreeSet<>(Set.of("string"))),
                arguments("string", "[\"string\"]", new TreeSet<>(Set.of("string"))),
                arguments("domain", "[\"ssp\"]", new TreeSet<>(Set.of("domain", "ssp"))),
                arguments("domain1, domain2", "[\"ssp1\", \"ssp2\"]",
                        new TreeSet<>(Set.of("domain1", "domain2", "ssp1", "ssp2")))
        );
    }

    @ParameterizedTest
    @MethodSource("timeBoardsProvider")
    void testToGdTimeTarget_regularDays(String pattern, List<List<Integer>> expectedTimeBoard) {
        TimeTarget timeTarget = TimeTarget.parseRawString(pattern);

        GdTimeTarget expected = new GdTimeTarget().withTimeBoard(expectedTimeBoard);
        GdTimeTarget actual = CampaignDataConverter.toGdTimeTarget(timeTarget);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    void testToGdTimeTarget_workingWeekends() {
        TimeTarget timeTarget = TimeTarget.parseRawString("9");

        GdTimeTarget expected = new GdTimeTarget().withUseWorkingWeekends(true);
        GdTimeTarget actual = CampaignDataConverter.toGdTimeTarget(timeTarget);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @ParameterizedTest
    @MethodSource("holidaysSettingsProvider")
    void testToGdTimeTarget_holidaysSettings(String pattern, GdHolidaysSettings expected) {
        TimeTarget timeTarget = TimeTarget.parseRawString(pattern);
        GdHolidaysSettings actual = CampaignDataConverter.toGdTimeTarget(timeTarget).getHolidaysSettings();
        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    void testToGdTimeTarget_fulfilled() {
        TimeTarget timeTarget = TimeTarget.parseRawString("1A2A3A4A5A6A7A8ABd9");

        GdHolidaysSettings expectedSettings = new GdHolidaysSettings()
                .withIsShow(true)
                .withStartHour(0).withEndHour(2)
                .withRateCorrections(TimeTarget.PredefinedCoefs.USUAL.getValue());
        GdTimeTarget expected = new GdTimeTarget()
                .withTimeBoard(createTimeBoardOneHourEachDay())
                .withHolidaysSettings(expectedSettings)
                .withUseWorkingWeekends(true)
                .withEnabledHolidaysMode(true);

        GdTimeTarget actual = CampaignDataConverter.toGdTimeTarget(timeTarget);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(allFields()));
    }

    @ParameterizedTest
    @MethodSource("timeBoardsProvider")
    void testToTimeTarget_regularDays(String expectedPattern, List<List<Integer>> timeBoard) {
        GdTimeTarget gdTimeTarget = new GdTimeTarget()
                .withTimeBoard(timeBoard)
                .withEnabledHolidaysMode(false)
                .withUseWorkingWeekends(false);

        TimeTarget expected = TimeTarget.parseRawString(expectedPattern);
        TimeTarget actual = CampaignDataConverter.toTimeTarget(gdTimeTarget);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(TIME_TARGET_COMPARE_STRATEGY));
    }

    @Test
    void testToTimeTarget_workingWeekends() {
        GdTimeTarget gdTimeTarget = new GdTimeTarget()
                .withTimeBoard(createEmptyTimeBoard())
                .withUseWorkingWeekends(true)
                .withEnabledHolidaysMode(false);

        TimeTarget expected = TimeTarget.parseRawString("12345679");
        TimeTarget actual = CampaignDataConverter.toTimeTarget(gdTimeTarget);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(TIME_TARGET_COMPARE_STRATEGY));
    }

    @ParameterizedTest
    @MethodSource("holidaysSettingsProvider")
    void testToTimeTarget_holidaysSettings(String expectedPattern, GdHolidaysSettings settings) {
        GdTimeTarget gdTimeTarget = new GdTimeTarget()
                .withTimeBoard(createEmptyTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(true)
                .withHolidaysSettings(settings);

        TimeTarget expected = TimeTarget.parseRawString(expectedPattern);
        TimeTarget actual = CampaignDataConverter.toTimeTarget(gdTimeTarget);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(TIME_TARGET_COMPARE_STRATEGY));
    }

    @Test
    void testToTimeTarget_fulfilled() {
        GdHolidaysSettings settings = new GdHolidaysSettings()
                .withIsShow(true)
                .withStartHour(0).withEndHour(1)
                .withRateCorrections(TimeTarget.PredefinedCoefs.USUAL.getValue());
        GdTimeTarget gdTimeTarget = new GdTimeTarget()
                .withTimeBoard(createTimeBoardOneHourEachDay())
                .withUseWorkingWeekends(true)
                .withEnabledHolidaysMode(true)
                .withHolidaysSettings(settings);

        TimeTarget expected = TimeTarget.parseRawString("1A2A3A4A5A6A7A8ABd9");
        TimeTarget actual = CampaignDataConverter.toTimeTarget(gdTimeTarget);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(TIME_TARGET_COMPARE_STRATEGY));
    }

    static Stream<Arguments> timeBoardsProvider() {
        return Stream.of(
                arguments("1A2A3A4A5A6A7A", createTimeBoardOneHourEachDay()),
                arguments("1ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "2ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "3ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "4ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "5ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "6ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "7ABCDEFGHIJKLMNOPQRSTUVWX", createFullTimeBoard()),
                arguments("1ABCDEFGHIJKLMNOPQRSTUVWX234567", createFullMondayTimeBoard()),
                arguments("1234567ABCDEFGHIJKLMNOPQRSTUVWX", createFullSundayTimeBoard())
        );
    }

    static Stream<Arguments> holidaysSettingsProvider() {
        return Stream.of(
                /* Don't show on holidays */
                arguments("12345678", new GdHolidaysSettings().withIsShow(false)),
                /* Show several hours with default coef */
                arguments("12345678IJKLMNOP", new GdHolidaysSettings().withIsShow(true)
                        .withStartHour(8).withEndHour(16)),
                /* Show several hours with predefined coef */
                arguments("12345678IbJbKb", new GdHolidaysSettings().withIsShow(true)
                        .withStartHour(8).withEndHour(11)
                        .withRateCorrections(10)),
                /* Different coefs for different hours (first one should be taken for rate corrections) */
                arguments("12345678IbJcKd", new GdHolidaysSettings().withIsShow(true)
                        .withStartHour(8).withEndHour(11)
                        .withRateCorrections(10)),
                /* Just one single hour at the start of the day */
                arguments("12345678A", new GdHolidaysSettings().withIsShow(true)
                        .withStartHour(0).withEndHour(1)),
                /* Just one single hour at the end of the day */
                arguments("12345678X", new GdHolidaysSettings().withIsShow(true)
                        .withStartHour(23).withEndHour(24)),
                /* The whole day */
                arguments("12345678ABCDEFGHIJKLMNOPQRSTUVWX", new GdHolidaysSettings().withIsShow(true)
                        .withStartHour(0).withEndHour(24))
        );
    }

    private static List<List<Integer>> createTimeBoardOneHourEachDay() {
        List<List<Integer>> result = new ArrayList<>(DAYS_PER_WEEK);
        for (int i = 0; i < DAYS_PER_WEEK; i++) {
            List<Integer> day = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 0));
            day.set(0, 100);
            result.add(i, day);
        }
        return result;
    }

    private static List<List<Integer>> createFullTimeBoard() {
        List<List<Integer>> result = new ArrayList<>(DAYS_PER_WEEK);
        for (int i = 0; i < DAYS_PER_WEEK; i++) {
            List<Integer> day = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 100));
            result.add(i, day);
        }
        return result;
    }

    private static List<List<Integer>> createFullMondayTimeBoard() {
        List<List<Integer>> result = new ArrayList<>(DAYS_PER_WEEK);
        List<Integer> monday = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 100));
        result.add(monday);
        for (int i = 1; i < DAYS_PER_WEEK; i++) {
            List<Integer> day = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 0));
            result.add(i, day);
        }
        return result;
    }

    private static List<List<Integer>> createFullSundayTimeBoard() {
        List<List<Integer>> result = new ArrayList<>(DAYS_PER_WEEK);
        for (int i = 0; i < 6; i++) {
            List<Integer> day = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 0));
            result.add(i, day);
        }
        List<Integer> sunday = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 100));
        result.add(sunday);
        return result;
    }

    private static List<List<Integer>> createEmptyTimeBoard() {
        return new ArrayList<>(Collections.nCopies(DAYS_PER_WEEK, Collections.nCopies(HOURS_PER_DAY, 0)));
    }
}
