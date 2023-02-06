package ru.yandex.util.ip;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class CidrTest extends TestBase {
    public CidrTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        String[] addrs =
            {"127.0.0.1", "::1", "2a02:6b8:0:3400:0:a48::2", "10.10.10.2"};
        for (int i = 0; i < addrs.length; ++i) {
            Assert.assertTrue(
                Cidr.fromString(addrs[i])
                    .matches(InetAddress.getByName(addrs[i])));
            for (int j = 0; j < addrs.length; ++j) {
                if (i != j) {
                    Assert.assertFalse(
                        Cidr.fromString(addrs[j])
                            .matches(InetAddress.getByName(addrs[i])));
                }
            }
        }

        Assert.assertTrue(
            Cidr.fromString("127.0.0.1/8")
                .matches(InetAddress.getByName("127.0.12.3")));
        Assert.assertFalse(
            Cidr.fromString("127.0.0.1/8")
                .matches(InetAddress.getByName("128.0.0.1")));

        Assert.assertTrue(
            Cidr.fromString("5.45.224.0/23")
                .matches(InetAddress.getByName("5.45.224.0")));
        Assert.assertTrue(
            Cidr.fromString("5.45.224.0/23")
                .matches(InetAddress.getByName("5.45.225.0")));
        Assert.assertFalse(
            Cidr.fromString("5.45.224.0/23")
                .matches(InetAddress.getByName("5.45.226.0")));

        Assert.assertTrue(
            Cidr.fromString("2a02:6b8:b080:912a::/63")
                .matches(InetAddress.getByName("2a02:6b8:b080:912a:aca::1")));
        Assert.assertTrue(
            Cidr.fromString("2a02:6b8:b080:912a::/63")
                .matches(InetAddress.getByName("2a02:6b8:b080:912b:aca::2")));
        Assert.assertFalse(
            Cidr.fromString("2a02:6b8:b080:912a::/63")
                .matches(InetAddress.getByName("2a02:6b8:b080:912c:aca::1")));
    }
}

