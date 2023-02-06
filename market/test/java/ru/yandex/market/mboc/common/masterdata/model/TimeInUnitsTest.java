package ru.yandex.market.mboc.common.masterdata.model;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TimeInUnitsTest {
    private static final TimeInUnits MONTH_12 = new TimeInUnits(12, TimeInUnits.TimeUnit.MONTH);
    private static final TimeInUnits DAYS_360 = new TimeInUnits(360, TimeInUnits.TimeUnit.DAY);
    private static final TimeInUnits DAYS_365 = new TimeInUnits(365, TimeInUnits.TimeUnit.DAY);
    private static final TimeInUnits MONTH_1 = new TimeInUnits(1, TimeInUnits.TimeUnit.MONTH);
    private static final TimeInUnits YEAR_1 = new TimeInUnits(1, TimeInUnits.TimeUnit.YEAR);
    private static final TimeInUnits WEEK_1 = new TimeInUnits(1, TimeInUnits.TimeUnit.WEEK);
    private static final TimeInUnits DAY_1 = new TimeInUnits(1, TimeInUnits.TimeUnit.DAY);
    private static final TimeInUnits MONTH_72 = new TimeInUnits(72, TimeInUnits.TimeUnit.MONTH);
    private static final TimeInUnits MONTH_73 = new TimeInUnits(73, TimeInUnits.TimeUnit.MONTH);
    private static final TimeInUnits YEAR_6 = new TimeInUnits(6, TimeInUnits.TimeUnit.YEAR);
    private static final TimeInUnits MAX_LIMITED = new TimeInUnits(Integer.MAX_VALUE, TimeInUnits.TimeUnit.YEAR);

    @Test
    public void testComparison() {
        // Possible convertation ranges of 1 year to some other time units
        checkBorders(YEAR_1, 360, 365, TimeInUnits.TimeUnit.DAY);
        checkBorders(YEAR_1, 12, 12, TimeInUnits.TimeUnit.MONTH);
        checkBorders(YEAR_1, 24 * 360, 24 * 365, TimeInUnits.TimeUnit.HOUR);

        // Possible convertation ranges of 1 month to some other time units
        checkBorders(MONTH_1, 30, 30, TimeInUnits.TimeUnit.DAY);
        checkBorders(MONTH_1, 720, 720, TimeInUnits.TimeUnit.HOUR);

        // Possible convertation ranges of 12 months to some other time units
        checkBorders(MONTH_12, 360, 365, TimeInUnits.TimeUnit.DAY);
        checkBorders(MONTH_12, 24 * 360, 24 * 365, TimeInUnits.TimeUnit.HOUR);
        checkBorders(MONTH_12, 1, 1, TimeInUnits.TimeUnit.YEAR);

        // Week is always 7 days or 168 hours
        checkBorders(WEEK_1, 7, 7, TimeInUnits.TimeUnit.DAY);
        checkBorders(WEEK_1, 168, 168, TimeInUnits.TimeUnit.HOUR);

        //Day is always 24 hours
        checkBorders(DAY_1, 24, 24, TimeInUnits.TimeUnit.HOUR);
        checkBorders(DAYS_365, 24 * 365, 24 * 365, TimeInUnits.TimeUnit.HOUR);

        // Converatation range is  growing, so
        // 1 year = [360 days, 365 days], 2 years = [720 days, 730 days], 3 years = [1080 days, 1095 days] ...
        // 1 year = [12 months, 12 months], 7 years = [83 months, 85 months], 13 years = [154 months, 158 months] ...
        for (int i = 0; i <= 70; i++) {
            int deltaMonthMax = i / 6;
            int deltaMonthMin = (i - 1) / 6;
            int deltaDays = i * 5;
            TimeInUnits iYears = new TimeInUnits(i, TimeInUnits.TimeUnit.YEAR);
            checkBorders(iYears, 12 * i - deltaMonthMin, 12 * i + deltaMonthMax, TimeInUnits.TimeUnit.MONTH);
            checkBorders(iYears, 365 * i - deltaDays, 365 * i, TimeInUnits.TimeUnit.DAY);
        }
    }

    @Test
    public void testSimilarityCheck() {
        Assertions.assertThat(MONTH_12.isSameAs(DAYS_360)).isTrue();
        Assertions.assertThat(MONTH_12.isSameAs(DAYS_365)).isTrue();
        Assertions.assertThat(DAYS_360.isSameAs(YEAR_1)).isTrue();
        Assertions.assertThat(DAYS_365.isSameAs(YEAR_1)).isTrue();
        Assertions.assertThat(MONTH_12.isSameAs(YEAR_1)).isTrue();
        // Comparing of times in same units don't use convertation ranges.
        Assertions.assertThat(DAYS_360.isSameAs(DAYS_365)).isFalse();
        Assertions.assertThat(YEAR_1.isSameAs(new TimeInUnits(2, TimeInUnits.TimeUnit.YEAR))).isFalse();
        Assertions.assertThat(MONTH_12.isSameAs(new TimeInUnits(13, TimeInUnits.TimeUnit.MONTH))).isFalse();

        Assertions.assertThat(MONTH_12.isSameOrHaveCommonTimeInGreaterUnit(DAYS_360)).isTrue();
        Assertions.assertThat(MONTH_12.isSameOrHaveCommonTimeInGreaterUnit(DAYS_365)).isTrue();
        Assertions.assertThat(DAYS_360.isSameOrHaveCommonTimeInGreaterUnit(YEAR_1)).isTrue();
        Assertions.assertThat(DAYS_365.isSameOrHaveCommonTimeInGreaterUnit(YEAR_1)).isTrue();
        Assertions.assertThat(MONTH_12.isSameOrHaveCommonTimeInGreaterUnit(YEAR_1)).isTrue();
        // 360 days and 365 days have common time in greater unit - 12 months (or 1 year)
        // common time - time, which convertation range intersects both convertation ranges of given times
        Assertions.assertThat(DAYS_360.isSameOrHaveCommonTimeInGreaterUnit(DAYS_365)).isTrue(); // <-different
        Assertions.assertThat(
            YEAR_1.isSameOrHaveCommonTimeInGreaterUnit(new TimeInUnits(2, TimeInUnits.TimeUnit.YEAR))
        ).isFalse();
        Assertions.assertThat(
            MONTH_12.isSameOrHaveCommonTimeInGreaterUnit(new TimeInUnits(13, TimeInUnits.TimeUnit.MONTH))
        ).isFalse();

        checkBorders(YEAR_6, 2160, 2190, TimeInUnits.TimeUnit.DAY);
        checkBorders(MONTH_72, 2160, 2190, TimeInUnits.TimeUnit.DAY);
        checkBorders(MONTH_73, 2190, 2220, TimeInUnits.TimeUnit.DAY);

        // Convertations ranges of 72 months, 73 months and 6 years are large enough, so
        Assertions.assertThat(YEAR_6.isSameAs(MONTH_72)).isTrue();
        Assertions.assertThat(YEAR_6.isSameAs(MONTH_73)).isTrue();
        // Comparing of times in same units don't use convertation ranges.
        Assertions.assertThat(MONTH_72.isSameAs(MONTH_73)).isFalse();

        Assertions.assertThat(YEAR_6.isSameOrHaveCommonTimeInGreaterUnit(MONTH_72)).isTrue();
        Assertions.assertThat(YEAR_6.isSameOrHaveCommonTimeInGreaterUnit(MONTH_73)).isTrue();
        // 72 and 73 month have common time in greater unit - 6 years
        Assertions.assertThat(MONTH_72.isSameOrHaveCommonTimeInGreaterUnit(MONTH_73)).isTrue(); // <-different
    }

    @Test
    public void testUnlimited() {
        // All unlimited timeInUnits have time 1
        Assertions.assertThat(new TimeInUnits(455, TimeInUnits.TimeUnit.UNLIMITED).getTime()).isEqualTo(1);

        // Unlimited is greater than all limited
        assertLonger(TimeInUnits.UNLIMITED, MAX_LIMITED);
        assertShorter(MAX_LIMITED, TimeInUnits.UNLIMITED);
        Assertions.assertThat(TimeInUnits.UNLIMITED.isSameAs(MAX_LIMITED)).isFalse();
        Assertions.assertThat(TimeInUnits.UNLIMITED.isSameOrHaveCommonTimeInGreaterUnit(MAX_LIMITED)).isFalse();

        // Unlimited is equal to unlimited
        TimeInUnits unlimited1 = new TimeInUnits(12345, TimeInUnits.TimeUnit.UNLIMITED);
        TimeInUnits unlimited2 = new TimeInUnits(67890, TimeInUnits.TimeUnit.UNLIMITED);
        Assertions.assertThat(unlimited1).isEqualTo(unlimited2);
        Assertions.assertThat(unlimited1.isSameAs(unlimited2)).isTrue();
        Assertions.assertThat(unlimited1.isSameOrHaveCommonTimeInGreaterUnit(unlimited2)).isTrue();

        // Unlimited can only be converted to unlimited and only unlimited can be converted to unlimited.
        Assertions.assertThat(TimeInUnits.UNLIMITED.getTimeInUnit(TimeInUnits.TimeUnit.UNLIMITED)).isEqualTo(1);
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> TimeInUnits.UNLIMITED.getTimeInUnit(TimeInUnits.TimeUnit.YEAR))
            .withMessage("UNLIMITED can't be converted to YEAR");
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> MAX_LIMITED.getTimeInUnit(TimeInUnits.TimeUnit.UNLIMITED))
            .withMessage("YEAR can't be converted to UNLIMITED");
    }

    private void checkBorders(TimeInUnits value, int shortest, int longest, TimeInUnits.TimeUnit timeUnit) {
        assertLonger(value, new TimeInUnits(shortest - 1, timeUnit));
        assertShorter(value, new TimeInUnits(longest + 1, timeUnit));
        for (int i = shortest - 1; i <= longest; i++) {
            assertNotShorter(value, new TimeInUnits(i, timeUnit));
        }
        for (int i = longest + 1; i >= shortest; i--) {
            assertNotLonger(value, new TimeInUnits(i, timeUnit));
        }
    }

    private void assertNotLonger(TimeInUnits expectedToBeShorterOrEqual, TimeInUnits expectedToBeLongerOrEqual) {
        Assertions.assertThat(expectedToBeShorterOrEqual.isNotLongerThan(expectedToBeLongerOrEqual)).isTrue();
    }

    private void assertNotShorter(TimeInUnits expectedToBeLongerOrEqual, TimeInUnits expectedToBeShorterOrEqual) {
        Assertions.assertThat(expectedToBeLongerOrEqual.isNotShorterThan(expectedToBeShorterOrEqual)).isTrue();
    }

    private void assertLonger(TimeInUnits expectedToBeLonger, TimeInUnits expectedToBeShorter) {
        Assertions.assertThat(expectedToBeLonger.isNotLongerThan(expectedToBeShorter)).isFalse();
    }

    private void assertShorter(TimeInUnits expectedToBeShorter, TimeInUnits expectedToBeLonger) {
        Assertions.assertThat(expectedToBeShorter.isNotShorterThan(expectedToBeLonger)).isFalse();
    }
}
