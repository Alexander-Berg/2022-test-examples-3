package ru.yandex.market.api.server;

import org.junit.Test;

import ru.yandex.market.api.server.sec.IpList;
import ru.yandex.market.api.server.sec.IpListParser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class IpListParserTest {

    @Test
    public void shouldParseSingleString() throws Exception {
        IpList parsed = IpListParser.parse("1.2.3.4");
        assertTrue(parsed.contains("1.2.3.4"));
    }

    @Test
    public void shouldParseMaskString() throws Exception {
        IpList parsed = IpListParser.parse("1.2.3.0/24");

        assertTrue(parsed.contains("1.2.3.0"));
        assertTrue(parsed.contains("1.2.3.255"));
        assertFalse(parsed.contains("1.2.4.0"));
    }
}
