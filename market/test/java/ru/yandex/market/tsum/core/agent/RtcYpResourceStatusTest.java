package ru.yandex.market.tsum.core.agent;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.tsum.registry.proto.model.DeployType;

import static org.junit.Assert.assertEquals;

public class RtcYpResourceStatusTest {

    @Test
    public void compareTest() {
        RtcYpResourceStatus status1 = new RtcYpResourceStatus("MARKET_RESOURCE_1", PackageType.SOX);
        status1.addInstanceStatus(false, DeployType.NANNY, "service_id", 0);
        RtcYpResourceStatus status2 = new RtcYpResourceStatus("MARKET_RESOURCE_1", PackageType.SOX);
        status2.addInstanceStatus(false, DeployType.NANNY, "service_id", 1);
        Assert.assertNotEquals(status1, status2);

        RtcYpResourceStatus status3 = new RtcYpResourceStatus("MARKET_RESOURCE_1", PackageType.SOX);
        status3.addInstanceStatus(false, DeployType.NANNY, "service_id", 0);
        Assert.assertEquals(status1, status3);
    }

    @Test
    public void formatCurrentProblemsMessage() throws IOException {
        RtcYpResourceStatus status = new RtcYpResourceStatus("MARKET_RESOURCE", PackageType.SOX);
        status.addInstanceStatus(false, DeployType.NANNY, "service_id_1", 0);
        status.addInstanceStatus(false, DeployType.YANDEX_DEPLOY, "service_id_2.du_1", 1);
        status.addInvalidServiceName(DeployType.NANNY, "invalid_nanny_service");
        status.addInvalidServiceName(DeployType.YANDEX_DEPLOY, "invalid_yd_service");

        assertEquals(
            IOUtils.readInputStream(new ClassPathResource("agent/rtcYpResourceStatusFormatted.txt").getInputStream()),
            status.formatCurrentProblemsMessage()
        );
    }
}
