package ru.yandex.market.admin.ui.model.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author sergey-fed
 */
public final class IpListValidationUtilsTest {

    private static void assertTrueAll(List<String> ips) {
        for (String ipString : ips) {
            assertTrue("Valid IP address " + ipString + " is false negative", IpAddressValidationUtils.isValidIPorSubNet(ipString));
        }
    }

    private static void assertFalseAll(List<String> ips) {
        for (String ipString : ips) {
            assertFalse("Invalid IP address " + ipString + " is false positive", IpAddressValidationUtils.isValidIPorSubNet(ipString));
        }
    }

    @Test
    public void testIPv6LoopbackAddress() {
        List<String> ipv6LoopbackAddresses = Arrays.asList("0:0:0:0:0:0:0:1", "::1", "::");

        assertTrueAll(ipv6LoopbackAddresses);
    }

    @Test
    public void testIPv4LoopbackAddress() {
        List<String> ipv4LoopbackAddress = Arrays.asList("127.0.0.1");

        assertTrueAll(ipv4LoopbackAddress);
    }

    @Test
    public void testValidIPv6Address() {
        List<String> validIPv6Addresses = Arrays.asList("ABCD:EF01:2345:6789:ABCD:EF01:2345:6789",
                "2001:DB8:0:0:8:800:200C:417A", "FF01:0:0:0:0:0:0:101", "0:0:0:0:0:0:0:0", "2001:DB8::8:800:200C:417A",
                "FF01::101");

        assertTrueAll(validIPv6Addresses);
    }

    @Test
    public void testValidIPv4Address() {
        List<String> validIPv4Addresses = Arrays.asList("192.168.0.1");

        assertTrueAll(validIPv4Addresses);
    }

    @Test
    public void testValidIPv6WithCIDR() {
        List<String> ipv6WithCIDR = Arrays.asList("2001:0DB8:0000:CD30:0000:0000:0000:0000/60",
                "2001:0DB8::CD30:0:0:0:0/60", "2001:0DB8:0:CD30::/32");

        assertTrueAll(ipv6WithCIDR);
    }

    @Test
    public void testValidIPv4WithCIDR() {
        List<String> ipv6WithCIDR = Arrays.asList("192.168.0.1/32", "192.168.0.1/0", "255.255.255.255");

        assertTrueAll(ipv6WithCIDR);
    }

    @Test
    public void testIPv6mappedIPv4Address() {
        List<String> ipv6WithCIDR = Arrays.asList("0:0:0:0:0:0:13.1.68.3", "0:0:0:0:0:FFFF:129.144.52.38",
                "::13.1.68.3", "::ffff:130.193.60.87");

        assertTrueAll(ipv6WithCIDR);
    }

    @Test
    public void testIPv6mappedIPv4WithCIDR() {
        List<String> ipv6WithCIDR = Arrays.asList("0:0:0:0:0:0:13.1.68.3/32");

        assertTrueAll(ipv6WithCIDR);
    }

    @Test
    public void testInvalidIPv6Address() {
        List<String> invalidIPv6 = Arrays.asList("2001:0DB8:0:CD3", "2001::0DB8::CD3", "abc", "::124G", "::12345");

        assertFalseAll(invalidIPv6);
    }

    @Test
    public void testInvalidIPv4Addresses() {
        List<String> invalidIPv6 = Arrays.asList("192.168.0.256", "092.168.0.256", "192.168.0.AAA");

        assertFalseAll(invalidIPv6);
    }

    @Test
    public void testInvalidIPv6WithCIDR() {
        List<String> invalidIPv6 = Arrays.asList("2001:0DB8:0:CD3/60", "2001:0DB8::CD30/ABC", "0:0:0:0:0:0:13.1.68.3/256");

        assertFalseAll(invalidIPv6);
    }

    @Test
    public void testInvalidIPv4WithCIDR() {
        List<String> invalidIPv6 = Arrays.asList("192.168.0.1/60");

        assertFalseAll(invalidIPv6);
    }

}
