package ru.yandex.market.tsum.core.agent;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ru.yandex.common.util.IOUtils;
import ru.yandex.market.tsum.agent.ConfId;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RtcResourceStatusTest {

    @Test
    public void formatCurrentProblemsMessage() throws IOException {
        RtcResourceStatus resourceStatus = new RtcResourceStatus("MARKET_RESOURCE", PackageType.SOX);

        List<ChecksumMismatch> changedFiles = Arrays.asList(
            new ChecksumMismatch("first-file", "123", "dsafdrfdsf2342", "dew0e392ekdlsda"),
            new ChecksumMismatch("second-file", "123", "erw0fds90asfds", "klpiowrew89qw0")
        );

        List<String> missedFiles = Arrays.asList("third-file", "next-file");
        AgentMongoDao.ConfIdEntity confId = new AgentMongoDao.ConfIdEntity("some_serice", "service_configuration", null);
        resourceStatus.addInstanceStatus(confId, "host1.market.yandex.net", changedFiles, missedFiles, System.currentTimeMillis());

        String expected = IOUtils.readInputStream(new ClassPathResource("agent/rtcFormattedReport.md").getInputStream());
        String actualMessage = resourceStatus.formatCurrentProblemsMessage();
        assertEquals(expected, actualMessage);
    }
}