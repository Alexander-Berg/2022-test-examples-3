package ru.yandex.market.logshatter.reader.logbroker.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 26.02.2019
 */
public class PartitionIdUtilsTest {
    @Test
    public void topicAndPartitionNumberToString() {
        assertEquals(
            "rt3.man--market-health-stable--other:5",
            PartitionIdUtils.toString("rt3.man--market-health-stable--other", 5)
        );
    }
}
