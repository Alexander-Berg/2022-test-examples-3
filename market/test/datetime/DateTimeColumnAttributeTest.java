package ru.yandex.market.jmf.attributes.test.datetime;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.query.ComparisonFilter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.utils.Maps;

@ContextConfiguration(classes = DateTimeColumnAttributeTest.Configuration.class)
public class DateTimeColumnAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.dateTime();
    }

    @Test
    public void betweenFilterTest() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusDays(1), false,
                now.minusHours(1), true,
                now, true,
                now.plusHours(2), true,
                now.plusHours(2).plusSeconds(1), false
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.between(attributeCode, now.minusHours(2), now.plusHours(2));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilteringTest() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), false,
                now, false,
                now.plusHours(2), true
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.greaterThan(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilteringTest() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), false,
                now, true,
                now.plusHours(2), true
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.greaterThanOrEqual(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilteringTest() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), true,
                now, true,
                now.plusHours(2), false
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.lessThanOrEqual(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilteringTest() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), true,
                now, false,
                now.plusHours(2), false
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.lessThan(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void betweenFilterTestOnLinkedObject() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusDays(1), false,
                now.minusHours(1), true,
                now, true,
                now.plusHours(2), true,
                now.plusHours(2).plusSeconds(1), false
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.between(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now.minusHours(2), now.plusHours(2)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilteringTestOnLinkedObject() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), false,
                now, false,
                now.plusHours(2), true
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.greaterThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilteringTestOnLinkedObject() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), false,
                now, true,
                now.plusHours(2), true
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.greaterThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilteringTestOnLinkedObject() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), true,
                now, true,
                now.plusHours(2), false
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.lessThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilteringTestOnLinkedObject() {
        OffsetDateTime now = getNow();

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), true,
                now, false,
                now.plusHours(2), false
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.lessThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void todayFilterTest() {
        OffsetDateTime now = getNow().withHour(0).withMinute(0).withSecond(0);

        Map<OffsetDateTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusSeconds(1), false,
                now.plusDays(1), false,
                now.plusHours(1), true,
                now, true
        );

        ComparisonFilter<OffsetDateTime> filter = Filters.today(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    private OffsetDateTime getNow() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:datetime_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
