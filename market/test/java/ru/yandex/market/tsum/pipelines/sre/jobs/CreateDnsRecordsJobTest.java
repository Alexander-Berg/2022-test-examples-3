package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.dynamic_dns.DnsRecord;
import ru.yandex.market.tsum.clients.dynamic_dns.DynamicDnsApiClient;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipelines.sre.resources.VirtualIPs;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.Silent.class)
@Ignore("Этот тест на самом деле интеграционный: он на самом деле идёт в DNS, потому что мок record не работает")
public class CreateDnsRecordsJobTest {

    @Mock
    private JobContext jobContext;

    @Mock
    private DnsRecord record;

    @Mock
    private DynamicDnsApiClient dnsApiClient;

    static final class MockCreateDnsRecordsJob extends CreateDnsRecordsJob {
        MockCreateDnsRecordsJob() {
            super(0);
        }

        @Override
        void waitUntilDnsAppears(DnsRecord record) {
        }

        @Override
        void commentIssue(VirtualIPs virtualIps) {
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("3ef4c9a0-8acd-453e-95f0-0d77b63f0ca8");
        }
    }

    @InjectMocks
    private CreateDnsRecordsJob job = new MockCreateDnsRecordsJob();

    public CreateDnsRecordsJobTest() {
    }

    @Test
    public void executeDnsRecordExists() throws Exception {
        doNothing().when(dnsApiClient).addRecord(any());
        when(record.verify()).thenReturn(true);
        job.execute(jobContext);
        verify(dnsApiClient, times(0)).addRecord(any());
    }

    @Test
    public void executeDnsRecordDoesntExists() throws Exception {
        doNothing().when(dnsApiClient).addRecord(any());
        when(record.verify()).thenReturn(false);
        job.setVirtualIpsList(List.of(new VirtualIPs(
            BalancerPipelineTestFactory.getVirtualIPs().getFqdn(),
            BalancerPipelineTestFactory.getVirtualIPs().getStartrekTicket(),
            Set.of("1.1.1.1", "::1")
        )));
        job.execute(jobContext);
        verify(dnsApiClient, times(2)).addRecord(any());
    }
}
