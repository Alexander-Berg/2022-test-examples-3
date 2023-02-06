package ru.yandex.market.tsum.pipelines.sre.rtc.jobs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.gencfg.GenCfgVolume;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.context.InternalJobContext;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroup;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.helpers.ApproverHelper;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyAuthAttrsOwners;
import ru.yandex.market.tsum.pipelines.sre.resources.RtcServiceSpec;
import ru.yandex.market.tsum.release.utils.IssueUtils;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.internal.stubbing.answers.AnswerFunctionalInterfaces.toAnswer;

@Ignore("integration test")
@RunWith(Parameterized.class)
public class CreateRtcServiceSpecJobCreateStartrekIssueAndRequireApprovalTest {

    private static final String STARTREK_OAUTH_TOKEN = "*******";

    private static final GenCfgGroupSpec GROUP_SPEC_REQUIRING_APPROVAL = GenCfgGroupSpec.newBuilder()
        .withLocation(GenCfgLocation.SAS)
        .withCType(GenCfgCType.PRODUCTION)
        .withMemoryGb(1)
        .withCpuCount(1)
        .withInstances(1)
        .withDiskGb(100)
        .withVolumes(Collections.singletonList(new GenCfgVolume(RandomUtils.nextInt(0, 100), "/storage")))
        .build();

    private static final GenCfgGroupSpec GROUP_SPEC_NOT_REQUIRING_APPROVAL = GenCfgGroupSpec.newBuilder()
        .withLocation(GenCfgLocation.SAS)
        .withCType(GenCfgCType.TESTING)
        .withMemoryGb(1)
        .withCpuCount(1)
        .withInstances(1)
        .withDiskGb(100)
        .withVolumes(Collections.singletonList(new GenCfgVolume(RandomUtils.nextInt(10, 100), "/storage")))
        .build();

    private final GenCfgGroupSpec groupSpec;

    private CreateRtcServiceSpecJob job;
    private TsumJobContext jobContext;
    private RtcServiceSpec rtcServiceSpec;
    private Session startrekSession;

    public CreateRtcServiceSpecJobCreateStartrekIssueAndRequireApprovalTest(
        @SuppressWarnings("unused") String testName,
        GenCfgGroupSpec groupSpec) {
        this.groupSpec = groupSpec;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return List.of(
            new Object[]{"GROUP_SPEC_REQUIRING_APPROVAL", GROUP_SPEC_REQUIRING_APPROVAL},
            new Object[]{"GROUP_SPEC_NOT_REQUIRING_APPROVAL", GROUP_SPEC_NOT_REQUIRING_APPROVAL}
        );
    }

    @Before
    public void setUp() throws Exception {
        startrekSession = StartrekClientBuilder.newBuilder()
            .uri("https://st-api.yandex-team.ru")
            .connectionTimeout(5000, TimeUnit.MILLISECONDS)
            .socketTimeout(120000, TimeUnit.MILLISECONDS)
            .userAgent("market-tsum")
            .build(STARTREK_OAUTH_TOKEN);

        jobContext = Mockito.mock(TsumJobContext.class);
        InternalJobContext internalJobContext = Mockito.mock(InternalJobContext.class);
        ApproverHelper approver = Mockito.mock(ApproverHelper.class);
        Mockito.when(approver.getApproval(any(), any(), any())).thenAnswer(toAnswer(
            (JobContext context, String issueKey, Object o) -> {
                IssueUtils.closeIssueWithComment(startrekSession.issues(), issueKey,
                    "closed after a pretend approval, see " + this.getClass().getName());

                return o;
            }));
        Mockito.when(approver.getApplicationUrl(any())).thenReturn("https://tsum.yandex-team.ru/application-url");
        Mockito.when(internalJobContext.approver()).thenReturn(approver);
        Mockito.when(jobContext.internal()).thenReturn(internalJobContext);
        Mockito.when(jobContext.getPipeLaunchUrl()).thenReturn("https://tsum.yandex-team.ru/pipe-launch-url");

        job = new CreateRtcServiceSpecJob();
        job.startrekTicket = new StartrekTicket("MARKETINFRA-6070");
        job.startrekIssues = startrekSession.issues();

        // можно поставить 1 и проверить, обновляются ли тикеты
        String nameSuffix = RandomStringUtils.randomNumeric(10);
        rtcServiceSpec = new RtcServiceSpec(
            "service_" + nameSuffix,
            "Test service " + nameSuffix,
            "marketservice" + nameSuffix,
            "markettest",
            new NannyAuthAttrsOwners(
                Collections.singletonList(new StaffPerson("login1", -1, null, null, null, null)),
                Collections.singletonList(new StaffGroup(1, "group1", "dpt_group1"))
            ));


        rtcServiceSpec.setNannyServices(Collections.singletonList(
            new NannyService(
                "testing_market_service_" + nameSuffix,
                new GenCfgGroup(
                    "SAS_MARKET_TEST_SERVICE_" + nameSuffix,
                    groupSpec,
                    null
                )
            ))
        );
    }

    @Test
    public void testCreateStartrekIssueAndRequireApproval() throws TimeoutException, InterruptedException {
        job.createStartrekIssueAndRequireApproval(jobContext, rtcServiceSpec);
    }
}
