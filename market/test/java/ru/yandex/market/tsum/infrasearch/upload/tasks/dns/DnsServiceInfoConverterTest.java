package ru.yandex.market.tsum.infrasearch.upload.tasks.dns;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DnsServiceInfoConverterTest {

    @Test
    public void ipv6ToPtrTest() throws Exception {
        String name = "2a02:6b8:0:1a16:225:90ff:fe93:7b0a";
        String actual = DnsServiceInfoConverter.ipv6ToPtr(name);
        assertEquals("a.0.b.7.3.9.e.f.f.f.0.9.5.2.2.0.6.1.a.1.0.0.0.0.8.b.6.0.2.0.a.2.ip6.arpa.", actual);
    }

}
