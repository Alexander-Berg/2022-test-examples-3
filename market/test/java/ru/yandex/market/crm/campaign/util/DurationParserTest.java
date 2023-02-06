package ru.yandex.market.crm.campaign.util;

import java.io.IOException;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.util.json.DurationParser;

import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
public class DurationParserTest {

    private DurationParser parser;

    private static void assertDuration(int expHours, int expMinutes, int expSeconds, Duration duration) {
        assertEquals(
                Duration.ofSeconds(expSeconds).plusMinutes(expMinutes).plusHours(expHours),
                duration
        );
    }

    @Before
    public void setUp() throws Exception {
        parser = new DurationParser();
    }

    @Test
    public void testDeserialize() throws IOException {
        Duration duration = deserialize("01:12:45");
        assertDuration(1, 12, 45, duration);
    }

    @Test
    public void testDeserializeSecondsOnly() throws IOException {
        Duration duration = deserialize("00:00:14");
        assertDuration(0, 0, 14, duration);
    }

    @Test
    public void testLongDuration() throws IOException {
        Duration duration = deserialize("127:00:01");
        assertDuration(127, 0, 1, duration);
    }

    private Duration deserialize(String text) throws IOException {
        return parser.parse(text);
    }

}
