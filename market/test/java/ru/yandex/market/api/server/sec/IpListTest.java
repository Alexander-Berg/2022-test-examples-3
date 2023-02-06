package ru.yandex.market.api.server.sec;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class IpListTest {

    @Test
    public void shouldCheckIpAddress() throws Exception {
        IpList ips = new IpList();

        ips.add("127.0.0.1");

        assertTrue(ips.contains("127.0.0.1"));
        assertFalse(ips.contains("127.0.0.2"));
    }

    @Test
    public void shouldCheckIpAddress24Mask() throws Exception {
        IpList ips = new IpList();

        ips.add("127.0.0.1/24");

        assertTrue(ips.contains("127.0.0.1"));
        assertTrue(ips.contains("127.0.0.255"));
        assertFalse(ips.contains("127.0.1.0"));
    }

    @Test
    public void testConvertToStringList() {
        IpList ips = new IpList();

        ips.add("127.0.0.1/24");
        ips.add("192.168.224.65/24");

        List<String> strings = ips.toStringList();
        assertEquals(Arrays.asList("127.0.0.1/24", "192.168.224.65/24"), strings);
    }
}