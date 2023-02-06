package ru.yandex.market.jmf.attributes.test.decimal;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.query.ComparisonFilter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.utils.Maps;

@ContextConfiguration(classes = DecimalColumnAttributeTest.Configuration.class)
public class DecimalColumnAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.bigDecimal();
    }

    @Test
    public void betweenFilterTest() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(-1), false,
                BigDecimal.valueOf(0.99), true,
                BigDecimal.valueOf(0), true,
                BigDecimal.valueOf(2), true,
                BigDecimal.valueOf(2.02), false
        );

        ComparisonFilter<BigDecimal> filter = Filters.between(attributeCode, BigDecimal.valueOf(-0.99),
                BigDecimal.valueOf(2));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilterTest() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), false,
                BigDecimal.valueOf(1), false,
                BigDecimal.valueOf(2), true
        );

        ComparisonFilter<BigDecimal> filter = Filters.greaterThan(attributeCode, BigDecimal.valueOf(1));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilterTest() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), false,
                BigDecimal.valueOf(1), true,
                BigDecimal.valueOf(2), true
        );

        ComparisonFilter<BigDecimal> filter = Filters.greaterThanOrEqual(attributeCode, BigDecimal.valueOf(1));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilterTest() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), true,
                BigDecimal.valueOf(1), false,
                BigDecimal.valueOf(2), false
        );

        ComparisonFilter<BigDecimal> filter = Filters.lessThan(attributeCode, BigDecimal.valueOf(1));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilterTest() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), true,
                BigDecimal.valueOf(1), true,
                BigDecimal.valueOf(2), false
        );

        ComparisonFilter<BigDecimal> filter = Filters.lessThanOrEqual(attributeCode, BigDecimal.valueOf(1));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void betweenFilterTestOnLinkedObject() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(-1), false,
                BigDecimal.valueOf(0.99), true,
                BigDecimal.valueOf(0), true,
                BigDecimal.valueOf(2), true,
                BigDecimal.valueOf(2.02), false
        );

        ComparisonFilter<BigDecimal> filter = Filters.between(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                BigDecimal.valueOf(-0.99), BigDecimal.valueOf(2)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilterTestOnLinkedObject() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), false,
                BigDecimal.valueOf(1), false,
                BigDecimal.valueOf(2), true
        );

        ComparisonFilter<BigDecimal> filter = Filters.greaterThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                BigDecimal.valueOf(1)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilterTestOnLinkedObject() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), false,
                BigDecimal.valueOf(1), true,
                BigDecimal.valueOf(2), true
        );

        ComparisonFilter<BigDecimal> filter = Filters.greaterThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                BigDecimal.valueOf(1)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilterTestOnLinkedObject() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), true,
                BigDecimal.valueOf(1), false,
                BigDecimal.valueOf(2), false
        );

        ComparisonFilter<BigDecimal> filter = Filters.lessThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                BigDecimal.valueOf(1)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilterTestOnLinkedObject() {
        Map<BigDecimal, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                BigDecimal.valueOf(0.99), true,
                BigDecimal.valueOf(1), true,
                BigDecimal.valueOf(2), false
        );

        ComparisonFilter<BigDecimal> filter = Filters.lessThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                BigDecimal.valueOf(1)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:decimal_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
