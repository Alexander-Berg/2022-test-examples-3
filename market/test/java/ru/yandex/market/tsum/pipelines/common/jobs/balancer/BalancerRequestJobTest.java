package ru.yandex.market.tsum.pipelines.common.jobs.balancer;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroup;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyAuthAttrsOwners;
import ru.yandex.market.tsum.pipelines.sre.resources.RtcServiceSpec;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 29/08/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class BalancerRequestJobTest {
    @Spy
    private RtcServiceSpec rtcServiceSpec = new RtcServiceSpec(
        "service_1",
        "Test service 1",
        "marketservice1",
        "markettest",
        new NannyAuthAttrsOwners(
            Collections.singletonList(new StaffPerson("login1", -1, null, null, null, null)),
            Collections.singletonList(new StaffGroup(1, "group1", "dpt_group1"))
        ));


    @InjectMocks
    private BalancerRequestJob balancerRequestJob;

    @Before
    public void setup() {
        rtcServiceSpec.setNannyServices(Arrays.asList(
            new NannyService(
                "testing_market_service_1",
                new GenCfgGroup(
                    "SAS_MARKET_TEST_SERVICE_1",
                    GenCfgGroupSpec.newBuilder()
                        .withLocation(GenCfgLocation.SAS)
                        .withCType(GenCfgCType.TESTING)
                        .withMemoryGb(1)
                        .withCpuCount(1)
                        .withInstances(1)
                        .withDiskGb(100)
                        .build(),
                    null
                )
            ),
            new NannyService(
                "prestable_market_service_1",
                new GenCfgGroup(
                    "VLA_MARKET_PREP_SERVICE_1",
                    GenCfgGroupSpec.newBuilder()
                        .withLocation(GenCfgLocation.VLA)
                        .withCType(GenCfgCType.PRESTABLE)
                        .withMemoryGb(1)
                        .withCpuCount(1)
                        .withInstances(1)
                        .withDiskGb(100)
                        .build(),
                    null
                )
            ),
            new NannyService(
                "production_market_service_1",
                new GenCfgGroup(
                    "VLA_MARKET_PROD_SERVICE_1",
                    GenCfgGroupSpec.newBuilder()
                        .withLocation(GenCfgLocation.IVA)
                        .withCType(GenCfgCType.PRODUCTION)
                        .withMemoryGb(1)
                        .withCpuCount(1)
                        .withInstances(1)
                        .withDiskGb(100)
                        .build(),
                    null
                )
            ))
        );
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getIssueComment() throws Exception {
        String description = balancerRequestJob.getIssueComment();
        Assert.assertNotNull(description);
    }
}
