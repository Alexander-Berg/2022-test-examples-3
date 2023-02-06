package ru.yandex.market.jmf.attributes.test.time;

import java.time.LocalTime;
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

@ContextConfiguration(classes = TimeColumnAttributeTest.Configuration.class)
public class TimeColumnAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.time();
    }

    @Test
    public void betweenFilterTest() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), false,
                now.minusMinutes(5), true,
                now.plusMinutes(1), true,
                now.plusHours(2), true,
                now.plusHours(2).plusSeconds(1), false
        );

        ComparisonFilter<LocalTime> filter = Filters.between(attributeCode, now.minusMinutes(5), now.plusHours(2));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilterTest() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), false,
                now, false,
                now.plusHours(2), true
        );

        ComparisonFilter<LocalTime> filter = Filters.greaterThan(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilterTest() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), false,
                now, true,
                now.plusHours(2), true
        );

        ComparisonFilter<LocalTime> filter = Filters.greaterThanOrEqual(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilterTest() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), true,
                now, true,
                now.plusHours(2), false
        );

        ComparisonFilter<LocalTime> filter = Filters.lessThanOrEqual(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilterTest() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), true,
                now, false,
                now.plusHours(2), false
        );

        ComparisonFilter<LocalTime> filter = Filters.lessThan(attributeCode, now);
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void betweenFilterTestOnLinkedObject() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusHours(1), false,
                now.minusMinutes(5), true,
                now.plusMinutes(1), true,
                now.plusHours(2), true,
                now.plusHours(2).plusSeconds(1), false
        );

        ComparisonFilter<LocalTime> filter = Filters.between(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now.minusMinutes(5), now.plusHours(2)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanFilterTestOnLinkedObject() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), false,
                now, false,
                now.plusHours(2), true
        );

        ComparisonFilter<LocalTime> filter = Filters.greaterThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void greaterThanOrEqualToFilterTestOnLinkedObject() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), false,
                now, true,
                now.plusHours(2), true
        );

        ComparisonFilter<LocalTime> filter = Filters.greaterThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanOrEqualToFilterTestOnLinkedObject() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), true,
                now, true,
                now.plusHours(2), false
        );

        ComparisonFilter<LocalTime> filter = Filters.lessThanOrEqual(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void lessThanFilterTestOnLinkedObject() {
        LocalTime now = getNow();

        Map<LocalTime, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                now.minusMinutes(5), true,
                now, false,
                now.plusHours(2), false
        );

        ComparisonFilter<LocalTime> filter = Filters.lessThan(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                now
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    private LocalTime getNow() {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
        // В тестах максимальное отклонение вперед это 2 часа и 1 секунда. Если это время переходит на следующие
        // сутки, то тест развалится. Делаем так, чтобы оставались в рамках одних суток. Атрибут типа Время не
        // предназначен для сравнения времен в разных сутках
        if (now.plusHours(2).plusSeconds(1).isBefore(now)) {
            now = now.minusHours(2).minusSeconds(1);
        }

        // В тестах максимальное отклонение назад это 1 час. Если это время переходит в предыдущие
        // сутки, то тест развалится. Делаем так, чтобы оставались в рамках одних суток. Атрибут типа Время не
        // предназначен для сравнения времен в разных сутках
        if (now.minusHours(1).isAfter(now)) {
            now = now.plusHours(1);
        }
        return now;
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:time_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
