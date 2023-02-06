package ru.yandex.market.tsum.clients.dynamic_dns;

import java.net.InetAddress;

import org.junit.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


// TODO: Сделать, чтобы тесты проверяли что ошибки в record.verify логгируются
public class DnsRecordTest {
    public void testNotFound() throws Exception {
        DnsRecord record = new DnsRecord("nonexistent.domain", Type.A, "1.2.3.4", 3600);
        assertFalse(record.verify());
    }

    public void testRecordFieldsDiffer() throws Exception {
        Record[] response = {new ARecord(Name.fromString("test.domain.ru."), DClass.IN, 3000, InetAddress.getByName(
            "1.2.3.4"))};
        DnsRecord record = spy(new DnsRecord("test.domain.ru", Type.A, "1.2.3.4", 3600));
        when(record.lookup()).thenReturn(response);
        assertFalse(record.verify());
    }

    @Test
    public void testRecordMatches() throws Exception {
        Record[] response = {new ARecord(Name.fromString("test.domain.ru."), DClass.IN, 3600, InetAddress.getByName(
            "1.2.3.4"))};
        DnsRecord record = spy(new DnsRecord("test.domain.ru", Type.A, "1.2.3.4", 3600));
        when(record.lookup()).thenReturn(response);
        assertTrue(record.verify());
    }
}
