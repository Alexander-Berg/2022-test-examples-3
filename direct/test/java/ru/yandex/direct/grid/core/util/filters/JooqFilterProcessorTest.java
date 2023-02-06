package ru.yandex.direct.grid.core.util.filters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.ytwrapper.dynamic.dsl.YtDSL;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.util.filters.JooqFilterProvider.not;

@RunWith(Parameterized.class)
public class JooqFilterProcessorTest {
    private static class FilterClass {
        private final List<String> allowedStrings;
        private final LocalDate minDate;
        private final BigDecimal maxNumber;
        private final Set<String> containsSubstr;
        private final FilterClass subFilter;

        private FilterClass(List<String> allowedStrings, LocalDate minDate, BigDecimal maxNumber,
                            Set<String> containsSubstr, FilterClass subFilter) {
            this.allowedStrings = allowedStrings;
            this.minDate = minDate;
            this.maxNumber = maxNumber;
            this.containsSubstr = containsSubstr;
            this.subFilter = subFilter;
        }

        private FilterClass(List<String> allowedStrings, LocalDate minDate, BigDecimal maxNumber,
                            FilterClass subFilter) {
            this(allowedStrings, minDate, maxNumber, null, subFilter);
        }

        public List<String> getAllowedStrings() {
            return allowedStrings;
        }

        public LocalDate getMinDate() {
            return minDate;
        }

        public BigDecimal getMaxNumber() {
            return maxNumber;
        }

        public Set<String> getContainsSubstr() {
            return containsSubstr;
        }

        public FilterClass getSubFilter() {
            return subFilter;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("FilterClass{");
            sb.append("allowedStrings=").append(allowedStrings);
            sb.append(", minDate=").append(minDate);
            sb.append(", maxNumber=").append(maxNumber);
            sb.append(", subFilter=").append(subFilter);
            sb.append('}');
            return sb.toString();
        }
    }

    private static final LocalDate TEST_DATE = LocalDate.of(2019, 12, 6);
    private static final BigDecimal TEST_NUM = BigDecimal.valueOf(1000);
    private static final List<String> TEST_LIST = ImmutableList.of("test1", "test2", "test3");
    private static final Field<String> STRING_FIELD = DSL.field("string", String.class);
    private static final Field<LocalDate> DATE_FIELD = DSL.field("date", LocalDate.class);
    private static final Field<BigDecimal> NUM_FIELD = DSL.field("num", BigDecimal.class);
    private static final String TEST_SET_ELEMENT_1 = "substr1";
    private static final String TEST_SET_ELEMENT_2 = "substr2";
    private static final String TEST_SET_ELEMENT_3 = "substr3";
    private static final Set<String> TEST_SET = ImmutableSet.of(
            TEST_SET_ELEMENT_1, TEST_SET_ELEMENT_2, TEST_SET_ELEMENT_3
    );

    private static final JooqFilterProcessor<FilterClass> SUB_PROCESSOR =
            JooqFilterProcessor.<FilterClass>builder()
                    .withFilter(FilterClass::getAllowedStrings, not(STRING_FIELD::in))
                    .withFilter(FilterClass::getMinDate, DATE_FIELD::ge)
                    .withFilter(FilterClass::getContainsSubstr,
                            JooqFilterProvider.not(JooqFilterProvider.inSetSubstringIgnoreCase(STRING_FIELD)))
                    .build();

    private static final JooqFilterProcessor<FilterClass> PROCESSOR =
            JooqFilterProcessor.<FilterClass>builder()
                    .withFilter(FilterClass::getAllowedStrings, STRING_FIELD::in)
                    .withFilter(FilterClass::getMaxNumber, NUM_FIELD::le)
                    .withFilter(FilterClass::getContainsSubstr,
                            JooqFilterProvider.inSetSubstringIgnoreCase(STRING_FIELD))
                    .withFilter(FilterClass::getSubFilter, SUB_PROCESSOR)
                    .build();

    @Parameterized.Parameters(name = "filterClass = {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {null, null},
                {new FilterClass(null, null, null, null), null},
                {new FilterClass(null, null, TEST_NUM, null),
                        NUM_FIELD.le(TEST_NUM)},
                {new FilterClass(TEST_LIST, null, TEST_NUM, null),
                        STRING_FIELD.in(TEST_LIST).and(NUM_FIELD.le(TEST_NUM))},
                {new FilterClass(TEST_LIST, TEST_DATE, TEST_NUM, null),
                        STRING_FIELD.in(TEST_LIST).and(NUM_FIELD.le(TEST_NUM))},
                {new FilterClass(TEST_LIST, TEST_DATE, TEST_NUM, new FilterClass(null, null, TEST_NUM, null)),
                        STRING_FIELD.in(TEST_LIST).and(NUM_FIELD.le(TEST_NUM))},
                {new FilterClass(TEST_LIST, null, TEST_NUM, new FilterClass(null, TEST_DATE, TEST_NUM, null)),
                        STRING_FIELD.in(TEST_LIST).and(NUM_FIELD.le(TEST_NUM)).and(DATE_FIELD.ge(TEST_DATE))},
                {new FilterClass(TEST_LIST, null, TEST_NUM, new FilterClass(TEST_LIST, TEST_DATE, TEST_NUM, null)),
                        STRING_FIELD.in(TEST_LIST).and(NUM_FIELD.le(TEST_NUM)).and(
                                DSL.not(STRING_FIELD.in(TEST_LIST)).and(DATE_FIELD.ge(TEST_DATE)))},
                {new FilterClass(null, null, null, TEST_SET, null),
                        YtDSL.isSubstring(TEST_SET_ELEMENT_1, YtDSL.toLower(STRING_FIELD)).or(
                                YtDSL.isSubstring(TEST_SET_ELEMENT_2, YtDSL.toLower(STRING_FIELD)).or(
                                        YtDSL.isSubstring(TEST_SET_ELEMENT_3, YtDSL.toLower(STRING_FIELD))))},
                {new FilterClass(null, null, null,
                        new FilterClass(null, null, null, TEST_SET, null)),
                        YtDSL.isSubstring(TEST_SET_ELEMENT_1, YtDSL.toLower(STRING_FIELD)).or(
                                YtDSL.isSubstring(TEST_SET_ELEMENT_2, YtDSL.toLower(STRING_FIELD)).or(
                                        YtDSL.isSubstring(TEST_SET_ELEMENT_3, YtDSL.toLower(STRING_FIELD)))).not()}
        });
    }

    @Parameterized.Parameter(0)
    public FilterClass filterClass;

    @Parameterized.Parameter(1)
    public Condition expectedResult;

    @Test
    public void testSortCorrect() {
        Condition result = PROCESSOR.apply(filterClass);

        assertThat(result)
                .isEqualTo(expectedResult);
    }
}
