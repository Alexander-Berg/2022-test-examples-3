package ru.yandex.market.jmf.attributes.test.date;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDate;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.query.ComparisonFilter;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.utils.Maps;

@ContextConfiguration(classes = DateColumnAttributeTest.Configuration.class)
public class DateColumnAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.date();
    }

    @Test
    public void betweenFilterTest() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusWeeks(1), false,
                today.minusDays(5), true,
                today.plusDays(1), true,
                today.plusDays(5), true,
                today.plusDays(6), false
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.between(attributeCode, today.minusDays(5),
                today.plusDays(5));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilterTest() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), false,
                today, false,
                today.plusDays(1), true
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.greaterThan(attributeCode, today);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilterTest() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), false,
                today, true,
                today.plusDays(1), true
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.greaterThanOrEqual(attributeCode, today);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilterTest() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), true,
                today, false,
                today.plusDays(1), false
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.lessThan(attributeCode, today);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilterTest() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), true,
                today, true,
                today.plusDays(1), false
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.lessThanOrEqual(attributeCode, today);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void todayFilterTest() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(5), false,
                today.plusDays(5), false,
                today.atStartOfDay().toLocalDate(), true,
                today.atStartOfDay().toLocalDate().plusDays(1), false,
                today.atTime(LocalTime.MAX).toLocalDate(), true,
                today, true
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.today(attributeCode, today);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void betweenFilterTestOnLinkedObject() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusWeeks(1), false,
                today.minusDays(5), true,
                today.plusDays(1), true,
                today.plusDays(5), true,
                today.plusDays(6), false
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.between(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                today.minusDays(5), today.plusDays(5)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilterTestOnLinkedObject() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), false,
                today, false,
                today.plusDays(1), true
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.greaterThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                today
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilterTestOnLinkedObject() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), false,
                today, true,
                today.plusDays(1), true
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.greaterThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                today
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilterTestOnLinkedObject() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), true,
                today, false,
                today.plusDays(1), false
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.lessThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                today
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilterTestOnLinkedObject() {
        LocalDate today = Now.localDate();

        Map<ChronoLocalDate, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                today.minusDays(1), true,
                today, true,
                today.plusDays(1), false
        );

        ComparisonFilter<ChronoLocalDate> filter = Filters.lessThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                today
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lt_runtime() {
        LocalDate value = Randoms.date();

        Filter filter = Filters.lt(attributeCode, value);

        boolean result = doRuntimeFilter(value.minusDays(1), filter);
        Assertions.assertTrue(result);

        boolean result1 = doRuntimeFilter(value, filter);
        Assertions.assertFalse(result1);

        boolean result2 = doRuntimeFilter(value.plusDays(1), filter);
        Assertions.assertFalse(result2);
    }

    @Test
    public void lе_runtime() {
        LocalDate value = Randoms.date();

        Filter filter = Filters.le(attributeCode, value);

        boolean result = doRuntimeFilter(value.minusDays(1), filter);
        Assertions.assertTrue(result);

        boolean result1 = doRuntimeFilter(value, filter);
        Assertions.assertTrue(result1);

        boolean result2 = doRuntimeFilter(value.plusDays(1), filter);
        Assertions.assertFalse(result2);
    }

    @Test
    public void gt_runtime() {
        LocalDate value = Randoms.date();

        Filter filter = Filters.gt(attributeCode, value);

        boolean result = doRuntimeFilter(value.minusDays(1), filter);
        Assertions.assertFalse(result);

        boolean result1 = doRuntimeFilter(value, filter);
        Assertions.assertFalse(result1);

        boolean result2 = doRuntimeFilter(value.plusDays(1), filter);
        Assertions.assertTrue(result2);
    }

    @Test
    public void gе_runtime() {
        LocalDate value = Randoms.date();

        Filter filter = Filters.ge(attributeCode, value);

        boolean result = doRuntimeFilter(value.minusDays(1), filter);
        Assertions.assertFalse(result);

        boolean result1 = doRuntimeFilter(value, filter);
        Assertions.assertTrue(result1);

        boolean result2 = doRuntimeFilter(value.plusDays(1), filter);
        Assertions.assertTrue(result2);
    }

    @Test
    public void eq_runtime() {
        LocalDate value = Randoms.date();

        Filter filter = Filters.eq(attributeCode, value);

        boolean result = doRuntimeFilter(value.minusDays(1), filter);
        Assertions.assertFalse(result);

        boolean result1 = doRuntimeFilter(value, filter);
        Assertions.assertTrue(result1);

        boolean result2 = doRuntimeFilter(value.plusDays(1), filter);
        Assertions.assertFalse(result2);
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:date_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
