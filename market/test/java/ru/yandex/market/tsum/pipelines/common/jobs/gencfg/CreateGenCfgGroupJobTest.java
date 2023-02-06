package ru.yandex.market.tsum.pipelines.common.jobs.gencfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgClient;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.gencfg.GenCfgVolume;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroup;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyAuthAttrsOwners;
import ru.yandex.market.tsum.pipelines.sre.resources.RtcServiceSpec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 04/08/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateGenCfgGroupJobTest {
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

    @Spy
    private StartrekTicket startrekTicket = new StartrekTicket("TEST-1");

    @InjectMocks
    private CreateGenCfgGroupJob createGenCfgGroupJob;

    @Mock
    private GenCfgClient genCfgClient;

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
                        .withDiskGb(1)
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
                        .withDiskGb(1)
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
                        .withDiskGb(1)
                        .withVolumes(Arrays.asList(
                            new GenCfgVolume(100, "/logs"),
                            new GenCfgVolume(2, "/cores"),
                            new GenCfgVolume(6, ""),
                            new GenCfgVolume(3, "/")
                        ))
                        .build(),
                    null
                )
            ))
        );
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void prepareProgramArgs() {
        when(genCfgClient.getGroupInfo(any())).thenReturn(Optional.empty());
        List<String> args = createGenCfgGroupJob.prepareProgramArgs();
        List<String> expected = Arrays.asList(
            "--group SAS_MARKET_TEST_SERVICE_1 --description \"Market Service Group TEST-1\" --memory 1.0 " +
                "--power 40 --instances 1 --ctype testing --itype marketservice1 --prj markettest",
            "--group VLA_MARKET_PREP_SERVICE_1 --description \"Market Service Group TEST-1\" --memory 1.0 " +
                "--power 40 --instances 1 --ctype prestable --itype marketservice1 --prj markettest",
            "--group VLA_MARKET_PROD_SERVICE_1 --description \"Market Service Group TEST-1\" --memory 1.0 " +
                "--power 40 --instances 1 --ctype production --itype marketservice1 --prj markettest " +
                "--volume-workdir 6 --volume-root 3 --volume-logs 100 --volume-cores 2"
        );
        Assert.assertEquals(expected, args);
    }
}
