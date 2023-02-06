package ru.yandex.util.ip;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class IpCompressorTest extends TestBase {
    @Test
    public void test() throws Exception {
        Assert.assertEquals(
            "2001:0:0:1::1",
            IpCompressor.INSTANCE.apply(
                InetAddress.getByName(
                    "2001:0:0:1:0:0:0:1")));
        Assert.assertEquals(
            "2001:db8::2:0",
            IpCompressor.INSTANCE.apply(
                InetAddress.getByName(
                    "2001:0db8:0:0000:00:000:0002:0000")));
        Assert.assertEquals(
            "2001:db8:f::2:0",
            IpCompressor.INSTANCE.apply(
                InetAddress.getByName(
                    "2001:0db8:0f:0000:00:000:0002:0000")));
        Assert.assertEquals(
            "2001:db8:f:f:f:0:2:1",
            IpCompressor.INSTANCE.apply(
                InetAddress.getByName(
                    "2001:0db8:0f:000f:0f:000:0002:0001")));
        Assert.assertEquals(
            "::1",
            IpCompressor.INSTANCE.apply(
                InetAddress.getByName(
                    "0000:0000:0000:0000:0000:0000:0000:0001")));
        Assert.assertEquals(
            "::",
            IpCompressor.INSTANCE.apply(
                InetAddress.getByName(
                    "0000:0000:0000:0000:0000:0000:0000:0000")));
    }
}

