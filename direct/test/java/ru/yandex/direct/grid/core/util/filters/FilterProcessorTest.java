package ru.yandex.direct.grid.core.util.filters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.grid.core.util.filters.FilterProvider.beforeOrEqual;
import static ru.yandex.direct.grid.core.util.filters.FilterProvider.contains;
import static ru.yandex.direct.grid.core.util.filters.FilterProvider.greaterOrEqual;
import static ru.yandex.direct.grid.core.util.filters.FilterProvider.isSubString;

@RunWith(Parameterized.class)
public class FilterProcessorTest {
    private static final LocalDate TEST_DATE = LocalDate.now();

    private static class FilterClass {
        private final Set<String> allowedStrings;
        private final LocalDate minDate;
        private final BigDecimal maxNumber;
        private final String stringPart;

        private FilterClass(Set<String> allowedStrings, LocalDate minDate, BigDecimal maxNumber,
                            String stringPart) {
            this.allowedStrings = allowedStrings;
            this.minDate = minDate;
            this.maxNumber = maxNumber;
            this.stringPart = stringPart;
        }

        public Set<String> getAllowedStrings() {
            return allowedStrings;
        }

        public LocalDate getMinDate() {
            return minDate;
        }

        public BigDecimal getMaxNumber() {
            return maxNumber;
        }

        public String getStringPart() {
            return stringPart;
        }

        @Override
        public String toString() {
            String minDateStr = minDate != null
                    ? minDate.until(TEST_DATE, ChronoUnit.DAYS) + " days until today"
                    : null;
            final StringBuilder sb = new StringBuilder("FilterClass{");
            sb.append("allowedStrings=").append(allowedStrings);
            sb.append(", minDate=").append(minDateStr);
            sb.append(", maxNumber=").append(maxNumber);
            sb.append(", stringPart='").append(stringPart).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    private static final FilterProviderTest.TestFP FP_ONE =
            new FilterProviderTest.TestFP("test1", BigDecimal.valueOf(12), TEST_DATE.minusDays(10));
    private static final FilterProviderTest.TestFP FP_TWO =
            new FilterProviderTest.TestFP("test2", BigDecimal.valueOf(1.5), TEST_DATE);
    private static final FilterProviderTest.TestFP FP_THREE =
            new FilterProviderTest.TestFP("test3", BigDecimal.valueOf(140), TEST_DATE.plusDays(10));
    private static final FilterProviderTest.TestFP FP_FOUR =
            new FilterProviderTest.TestFP("test4", BigDecimal.valueOf(0), TEST_DATE);
    private static final FilterProviderTest.TestFP FP_FIVE =
            new FilterProviderTest.TestFP("test5", BigDecimal.valueOf(-1), null);
    private static final FilterProviderTest.TestFP FP_SIX = null;
    private static final List<FilterProviderTest.TestFP> ITEMS_LIST = Arrays.asList(
            FP_ONE, FP_TWO, FP_THREE, FP_FOUR, FP_FIVE, FP_SIX
    );

    private static final FilterProcessor<FilterClass, FilterProviderTest.TestFP> PROCESSOR =
            new FilterProcessor.Builder<FilterClass, FilterProviderTest.TestFP>()
                    .withFilter(FilterClass::getAllowedStrings, contains(FilterProviderTest.TestFP::getStr))
                    .withFilter(FilterClass::getMinDate, beforeOrEqual(FilterProviderTest.TestFP::getDate))
                    .withFilter(FilterClass::getMaxNumber, greaterOrEqual(FilterProviderTest.TestFP::getBigDecimal))
                    .withFilter(FilterClass::getStringPart, isSubString(FilterProviderTest.TestFP::getStr))
                    .build();

    @Parameterized.Parameters(name = "filterClass = {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {null, ITEMS_LIST, null},
                {new FilterClass(null, null, null, null), ITEMS_LIST,
                        null},
                {new FilterClass(null, null, BigDecimal.valueOf(1000), null),
                        Arrays.asList(FP_ONE, FP_TWO, FP_THREE, FP_FOUR, FP_FIVE),
                        null},
                {new FilterClass(ImmutableSet.of("test1", "test2", "test3"), null, BigDecimal.valueOf(139.9), null),
                        Arrays.asList(FP_ONE, FP_TWO),
                        null},
                {new FilterClass(ImmutableSet.of("test1", "test2", "test3"), null, BigDecimal.valueOf(139.9), "t10"),
                        Collections.emptyList(),
                        null},
                {new FilterClass(ImmutableSet.of("test1", "test2", "test3"), null, BigDecimal.valueOf(139.9), "test"),
                        Arrays.asList(FP_ONE, FP_TWO),
                        null},
                {new FilterClass(ImmutableSet.of("test1", "test2", "test3"), TEST_DATE.minusDays(3),
                        BigDecimal.valueOf(139.9), "test"),
                        Collections.singletonList(FP_TWO),
                        null},
        });
    }

    @Parameterized.Parameter(0)
    public FilterClass filterClass;

    @Parameterized.Parameter(1)
    public List<FilterProviderTest.TestFP> expectedResult;

    @Parameterized.Parameter(2)
    public Class<?> exceptionClass;

    @Test
    public void testSortCorrect() {
        if (exceptionClass == null) {
            List<FilterProviderTest.TestFP> result = PROCESSOR.filter(filterClass, ITEMS_LIST);

            assertThat(result)
                    .containsExactlyElementsOf(expectedResult);
        } else {
            assertThatThrownBy(() -> PROCESSOR.filter(filterClass, ITEMS_LIST))
                    .isInstanceOf(exceptionClass);
        }
    }
}
