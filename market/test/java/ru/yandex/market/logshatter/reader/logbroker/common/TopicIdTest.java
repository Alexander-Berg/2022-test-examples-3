package ru.yandex.market.logshatter.reader.logbroker.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 26.02.2019
 */
public class TopicIdTest {
    @Test
    public void fromString() {
        TopicId topicId = TopicId.fromString("rt3.man--market-health/testing@testing--other");
        assertEquals("rt3.man--market-health/testing@testing--other", topicId.asString());
        assertEquals("market-health/testing@testing--other", topicId.asStringWithoutDataCenter());
        assertEquals("man", topicId.getDataCenter());
        assertEquals("market-health/testing@testing", topicId.getIdent());
        assertEquals("other", topicId.getLogType());
    }

    @Test
    public void fromString_noDataCenter() {
        TopicId topicId = TopicId.fromString("market-health/testing@testing--other");
        assertEquals("market-health/testing@testing--other", topicId.asString());
        assertEquals("market-health/testing@testing--other", topicId.asStringWithoutDataCenter());
        assertNull(topicId.getDataCenter());
        assertEquals("market-health/testing@testing", topicId.getIdent());
        assertEquals("other", topicId.getLogType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromString_onlyIdent() {
        TopicId.fromString("market-health-testing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromString_tooManyParts() {
        TopicId.fromString("rt3.man--market-health-testing--other--something-else");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromString_tooFewPartsInDataCenter() {
        TopicId.fromString("man--market-health-testing--other");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromString_tooManyPartsInDataCenter() {
        TopicId.fromString("man.man.man--market-health-testing--other");
    }
}
