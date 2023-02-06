package ru.yandex.market.jmf.attributes.test.duration;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.query.ComparisonFilter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.utils.Maps;

@ContextConfiguration(classes = DurationColumnAttributeTest.Configuration.class)
public class DurationColumnAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.duration();
    }

    @Test
    public void betweenFilterTest() {
        Map<Duration, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                Duration.ofSeconds(10), false,
                Duration.ofSeconds(60), true,
                Duration.ofSeconds(100), true,
                Duration.ofSeconds(200), true,
                Duration.ofSeconds(300), false
        );

        ComparisonFilter<Duration> filter = Filters.between(attributeCode, Duration.ofSeconds(60),
                Duration.ofSeconds(200));
        testComparisonFiltering(filter, valuesAndShouldTheyBeMatched);
    }

    @Test
    public void betweenFilterTestOnLinkedObject() {
        Map<Duration, Boolean> valuesAndShouldTheyBeMatched = Maps.of(
                Duration.ofSeconds(10), false,
                Duration.ofSeconds(60), true,
                Duration.ofSeconds(100), true,
                Duration.ofSeconds(200), true,
                Duration.ofSeconds(300), false
        );

        ComparisonFilter<Duration> filter = Filters.between(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                Duration.ofSeconds(60), Duration.ofSeconds(200)
        );
        testComparisonFilteringOnLinkedObject(filter, valuesAndShouldTheyBeMatched);
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:duration_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
