package ru.yandex.market.tsum.pipelines.common.jobs.juggler;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
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
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyAuthAttrsOwners;
import ru.yandex.market.tsum.pipelines.sre.resources.RtcServiceSpec;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 14/08/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateJugglerChecksJobTest {
    @Spy
    private RtcServiceSpec rtcServiceSpec = new RtcServiceSpec(
        "service_1",
        "Test service 1",
        "marketservice1",
        "markettest",
        new NannyAuthAttrsOwners(
            Arrays.asList(
                new StaffPerson("login1", -1, null, null, null, null),
                new StaffPerson("login2", -1, null, null, null, null)
            ),
            Collections.singletonList(new StaffGroup(1, "group1", "dpt_group1"))
        ));


    @Spy
    private StartrekTicket startrekTicket = new StartrekTicket("TEST-1");

    @InjectMocks
    private CreateJugglerChecksJob createJugglerChecksJob;

    @Before
    public void setup() {
        rtcServiceSpec.setNannyServices(Arrays.asList(
            new NannyService(
                "testing_market_service_1_sas",
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
                "prestable_market_service_1_vla",
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
                "production_market_service_1_iva",
                new GenCfgGroup(
                    "IVA_MARKET_PROD_SERVICE_1",
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
        rtcServiceSpec.fillInJugglerHostForCType(GenCfgCType.TESTING);
        rtcServiceSpec.fillInJugglerHostForCType(GenCfgCType.PRESTABLE);
        rtcServiceSpec.fillInJugglerHostForCType(GenCfgCType.PRODUCTION);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getIssueDescription() throws Exception {
        String expected = IOUtils.toString(this.getClass().getResourceAsStream(
            "/createJugglerChecksJobTest/startrekTicketDescription.txt"), StandardCharsets.UTF_8.name());
        String result = createJugglerChecksJob.getIssueDescription("pipeline");
        Assert.assertEquals(expected, result);
    }
}
