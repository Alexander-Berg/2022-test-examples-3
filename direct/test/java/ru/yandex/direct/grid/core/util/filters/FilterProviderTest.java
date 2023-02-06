package ru.yandex.direct.grid.core.util.filters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class FilterProviderTest {
    public static class TestFP {
        private final String str;
        private final BigDecimal bigDecimal;
        private final LocalDate date;

        public TestFP(String str, BigDecimal bigDecimal, LocalDate date) {
            this.str = str;
            this.bigDecimal = bigDecimal;
            this.date = date;
        }

        public String getStr() {
            return str;
        }

        public BigDecimal getBigDecimal() {
            return bigDecimal;
        }

        public LocalDate getDate() {
            return date;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("TestFP{");
            sb.append("str='").append(str).append('\'');
            sb.append(", bigDecimal=").append(bigDecimal);
            sb.append(", date=").append(date);
            sb.append('}');
            return sb.toString();
        }
    }

    private static final String TEST_STRING = "some string";
    private static final LocalDate TEST_DATE = LocalDate.now();
    private static final TestFP TEST_FP = new TestFP(TEST_STRING, BigDecimal.TEN, TEST_DATE);
    private static final TestFP TEST_FP_NULL = new TestFP(null, null, null);

    @Test
    public void testContains() {
        BiPredicate<Set<String>, TestFP> predicate = FilterProvider.contains(TestFP::getStr);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(Collections.singleton(TEST_STRING), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(ImmutableSet.of(TEST_STRING + "1", TEST_STRING + "2"), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(Collections.emptySet(), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(Collections.singleton(TEST_STRING), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testContainsMatching() {
        BiPredicate<Set<String>, TestFP> predicate =
                FilterProvider.containsMatching(TestFP::getStr, String::equalsIgnoreCase);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(Collections.singleton(TEST_STRING), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(Collections.singleton(TEST_STRING.toUpperCase()), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(ImmutableSet.of(TEST_STRING + "1", TEST_STRING + "2"), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(Collections.emptySet(), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(Collections.singleton(TEST_STRING), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testEqualTo() {
        BiPredicate<String, TestFP> predicate = FilterProvider.equalTo(TestFP::getStr);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(TEST_STRING, TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_STRING, TEST_FP_NULL))
                .isFalse();
        soft.assertThat(predicate.test(TEST_STRING + TEST_STRING, TEST_FP))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testSubString() {
        BiPredicate<String, TestFP> predicate = FilterProvider.isSubString(TestFP::getStr);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(TEST_STRING, TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_STRING.substring(2, 5), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_STRING.substring(2, 5), TEST_FP_NULL))
                .isFalse();
        soft.assertThat(predicate.test(TEST_STRING + TEST_STRING, TEST_FP))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testBefore() {
        BiPredicate<LocalDate, TestFP> predicate = FilterProvider.before(TestFP::getDate);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(TEST_DATE.minusDays(1), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_DATE, TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(TEST_DATE.plusDays(1), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(TEST_DATE.minusDays(1), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testBeforeOrEqual() {
        BiPredicate<LocalDate, TestFP> predicate = FilterProvider.beforeOrEqual(TestFP::getDate);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(TEST_DATE.minusDays(1), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_DATE, TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_DATE.plusDays(1), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(TEST_DATE, TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testAfter() {
        BiPredicate<LocalDate, TestFP> predicate = FilterProvider.after(TestFP::getDate);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(TEST_DATE.minusDays(1), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(TEST_DATE, TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(TEST_DATE.plusDays(1), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_DATE.plusDays(1), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testAfterOrEqual() {
        BiPredicate<LocalDate, TestFP> predicate = FilterProvider.afterOrEqual(TestFP::getDate);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(TEST_DATE.minusDays(1), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(TEST_DATE, TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_DATE.plusDays(1), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(TEST_DATE.plusDays(1), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testLessThan() {
        BiPredicate<BigDecimal, TestFP> predicate = FilterProvider.lessThan(TestFP::getBigDecimal);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(BigDecimal.ZERO, TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(BigDecimal.valueOf(10.0), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(BigDecimal.valueOf(20), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(BigDecimal.ZERO, TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testLessOrEqual() {
        BiPredicate<BigDecimal, TestFP> predicate = FilterProvider.lessOrEqual(TestFP::getBigDecimal);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(BigDecimal.ZERO, TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(BigDecimal.valueOf(10.0), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(BigDecimal.valueOf(20), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(BigDecimal.valueOf(10.0), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testGreaterThan() {
        BiPredicate<BigDecimal, TestFP> predicate = FilterProvider.greaterThan(TestFP::getBigDecimal);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(BigDecimal.ZERO, TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(BigDecimal.valueOf(10.0), TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(BigDecimal.valueOf(20), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(BigDecimal.valueOf(20), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }

    @Test
    public void testGreaterOrEqual() {
        BiPredicate<BigDecimal, TestFP> predicate = FilterProvider.greaterOrEqual(TestFP::getBigDecimal);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(predicate.test(BigDecimal.ZERO, TEST_FP))
                .isFalse();
        soft.assertThat(predicate.test(BigDecimal.valueOf(10.0), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(BigDecimal.valueOf(20), TEST_FP))
                .isTrue();
        soft.assertThat(predicate.test(BigDecimal.valueOf(10.0), TEST_FP_NULL))
                .isFalse();

        soft.assertAll();
    }
}
