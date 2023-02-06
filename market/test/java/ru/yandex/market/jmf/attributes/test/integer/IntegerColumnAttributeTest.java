package ru.yandex.market.jmf.attributes.test.integer;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.query.ComparisonFilter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.utils.Maps;

@ContextConfiguration(classes = IntegerColumnAttributeTest.Configuration.class)
public class IntegerColumnAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.longValue();
    }

    @Test
    public void betweenFilterTest() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, false,
                0L, true,
                1L, true,
                2L, true,
                3L, false
        );

        ComparisonFilter<Long> filter = Filters.between(attributeCode, 0L, 2L);
        testComparisonFiltering(filter, testSpec);
    }

    @Test
    public void greaterThanFilterTest() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, false,
                1L, false,
                2L, true
        );

        ComparisonFilter<Long> filter = Filters.greaterThan(attributeCode, 1L);
        testComparisonFiltering(filter, testSpec);
    }

    @Test
    public void greaterThanOrEqualToFilterTest() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, false,
                1L, true,
                2L, true
        );

        ComparisonFilter<Long> filter = Filters.greaterThanOrEqual(attributeCode, 1L);
        testComparisonFiltering(filter, testSpec);
    }

    @Test
    public void lessThanOrEqualToFilterTest() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, true,
                1L, true,
                2L, false
        );

        ComparisonFilter<Long> filter = Filters.lessThanOrEqual(attributeCode, 1L);
        testComparisonFiltering(filter, testSpec);
    }

    @Test
    public void lessThanFilterTest() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, true,
                1L, false,
                2L, false
        );

        ComparisonFilter<Long> filter = Filters.lessThan(attributeCode, 1L);
        testComparisonFiltering(filter, testSpec);
    }

    @Test
    public void betweenFilterTestOnLinkedObject() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, false,
                0L, true,
                1L, true,
                2L, true,
                3L, false
        );

        ComparisonFilter<Long> filter = Filters.between(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                0L, 2L
        );
        testComparisonFilteringOnLinkedObject(filter, testSpec);
    }

    @Test
    public void greaterThanFilterTestOnLinkedObject() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, false,
                1L, false,
                2L, true
        );

        ComparisonFilter<Long> filter = Filters.greaterThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                1L
        );
        testComparisonFilteringOnLinkedObject(filter, testSpec);
    }

    @Test
    public void greaterThanOrEqualToFilterTestOnLinkedObject() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, false,
                1L, true,
                2L, true
        );

        ComparisonFilter<Long> filter = Filters.greaterThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                1L
        );
        testComparisonFilteringOnLinkedObject(filter, testSpec);
    }

    @Test
    public void lessThanOrEqualToFilterTestOnLinkedObject() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, true,
                1L, true,
                2L, false
        );

        ComparisonFilter<Long> filter = Filters.lessThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                1L
        );
        testComparisonFilteringOnLinkedObject(filter, testSpec);
    }

    @Test
    public void lessThanFilterTestOnLinkedObject() {
        Map<Long, Boolean> testSpec = Maps.of(
                -1L, true,
                1L, false,
                2L, false
        );

        ComparisonFilter<Long> filter = Filters.lessThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                1L
        );
        testComparisonFilteringOnLinkedObject(filter, testSpec);
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:integer_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
