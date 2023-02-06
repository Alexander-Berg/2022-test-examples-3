package ru.yandex.market.markup2.tasks.card_duplicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author galaev@yandex-team.ru
 * @since 21/07/2017.
 */
public class CardDuplicatesMetricsTest {

    @Test
    public void testMetricsDataSerialization() throws IOException {
        CardDuplicatesMetricsData metrics = new CardDuplicatesMetricsData(1, 0, 1,
            "0.0000", "40.0000", "60.0000", "90.0000", "types");

        Class<CardDuplicatesMetricsData> clazz = CardDuplicatesMetricsData.class;
        ObjectMapper objectMapper = new ObjectMapper();

        String value = objectMapper.writeValueAsString(metrics);
        System.out.println(value);
        CardDuplicatesMetricsData metricsCopy = objectMapper.readValue(value, clazz);

        Assert.assertEquals(metrics.getCategoryId(), metricsCopy.getCategoryId());
        Assert.assertEquals(metrics.getDuplicatesCount(), metricsCopy.getDuplicatesCount());
        Assert.assertEquals(metrics.getTotalCount(), metricsCopy.getTotalCount());
        Assert.assertEquals(metrics.getCentile1(), metricsCopy.getCentile1());
        Assert.assertEquals(metrics.getCentile5(), metricsCopy.getCentile5());
        Assert.assertEquals(metrics.getCentile50(), metricsCopy.getCentile50());
        Assert.assertEquals(metrics.getCardTypes(), metricsCopy.getCardTypes());
        Assert.assertEquals(metrics.getRate(), metricsCopy.getRate());
    }
}
