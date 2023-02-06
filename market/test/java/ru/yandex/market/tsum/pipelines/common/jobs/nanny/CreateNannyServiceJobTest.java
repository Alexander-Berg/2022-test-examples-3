package ru.yandex.market.tsum.pipelines.common.jobs.nanny;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.tsum.MarketMapClient;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.multitesting.YpAllocationParams;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.AbcServiceResource;
import ru.yandex.market.tsum.pipelines.common.resources.EnvironmentResource;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroup;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgRelease;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxResource;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentChangeRequest;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentSpecResource;
import ru.yandex.market.tsum.pipelines.sre.jobs.GenerateAndCommitDeplateConfigJob;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyAuthAttrsOwners;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyDiskQuota;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyDiskQuotaType;
import ru.yandex.market.tsum.pipelines.sre.resources.RtcServiceSpec;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 08/08/2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CreateNannyServiceJobTest {
    @Autowired
    private JobTester jobTester;

    private RtcServiceSpec newRtcServiceSpec() {
        return new RtcServiceSpec(
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
    }

    public NannyCreateServiceJob prepareGencfgCreate() {
        RtcServiceSpec rtcServiceSpec = newRtcServiceSpec();

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
                ),
                Arrays.asList(
                    new NannyDiskQuota(NannyDiskQuotaType.WORKDIR, 2),
                    new NannyDiskQuota(NannyDiskQuotaType.ROOT, 1)
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
                ),
                Arrays.asList(
                    new NannyDiskQuota(NannyDiskQuotaType.ROOT, 1),
                    new NannyDiskQuota(NannyDiskQuotaType.WORKDIR, 2)
                )
            ))
        );
        rtcServiceSpec.setAppSandboxResource(new SandboxResource("TEST_TASK_TYPE", 1L, "TEST_RESOURCE_TYPE", 2L));
        rtcServiceSpec.setTemplateServiceId("templateServiceId");
        rtcServiceSpec.setGencfgRelease("tags/stable-104-r432");
        rtcServiceSpec.setApplicationName("service-1");
        EnvironmentResource environment = new EnvironmentResource(Environment.TESTING, "service_1");
        GenCfgRelease genCfgRelease = new GenCfgRelease("tags/stable-104-r432");

        return createJob(rtcServiceSpec, new NannyCreateServiceJobConfig(), environment, genCfgRelease);
    }

    public NannyCreateServiceJob prepareYPLiteCreate() {
        RtcServiceSpec rtcServiceSpec = newRtcServiceSpec();

        rtcServiceSpec.setNannyServices(Arrays.asList(
            new NannyService(
                "testing_market_service_1_sas",
                new YpAllocationParams.Builder()
                    .withReplicaCount(1)
                    .withCpuGuaranteeMillis(1000)
                    .withMemoryMb(1024)
                    .withRootFsQuotaGb(1)
                    .withWorkDirGb(3)
                    .withNetworkMacro("_TEST_")
                    .withVolumes(List.of(
                        new YpAllocationParams.Volume("/logs", 512)
                    ))
                    .withLocation(YpAllocationParams.Location.SAS).build(),
                Environment.TESTING,
                "service_1"
            ),
            new NannyService(
                "testing_market_service_1_vla",
                new YpAllocationParams.Builder()
                    .withReplicaCount(1)
                    .withCpuGuaranteeMillis(1000)
                    .withMemoryMb(1024)
                    .withRootFsQuotaGb(1)
                    .withWorkDirGb(3)
                    .withNetworkMacro("_TEST_")
                    .withVolumes(List.of())
                    .withLocation(YpAllocationParams.Location.VLA).build(),
                Environment.TESTING,
                "service_1"
            ),
            new NannyService(
                "testing_market_service_2_vla",
                new YpAllocationParams.Builder()
                    .withReplicaCount(1)
                    .withCpuGuaranteeMillis(1000)
                    .withMemoryMb(1024)
                    .withRootFsQuotaGb(1)
                    .withWorkDirGb(3)
                    .withNetworkMacro("_TEST_")
                    .withVolumes(List.of())
                    .withLocation(YpAllocationParams.Location.VLA).build(),
                Environment.TESTING,
                "service_2"
            ),
            new NannyService(
                "production_market_service_1_vla",
                new YpAllocationParams.Builder()
                    .withReplicaCount(1)
                    .withCpuGuaranteeMillis(1000)
                    .withMemoryMb(1024)
                    .withRootFsQuotaGb(1)
                    .withWorkDirGb(3)
                    .withNetworkMacro("_TEST_")
                    .withVolumes(List.of())
                    .withLocation(YpAllocationParams.Location.VLA).build(),
                Environment.PRODUCTION,
                "service_1"
            )
        ));
        rtcServiceSpec.setAppSandboxResource(new SandboxResource("TEST_TASK_TYPE", 1L, "TEST_RESOURCE_TYPE", 2L));
        rtcServiceSpec.setTemplateServiceId("templateServiceId");
        rtcServiceSpec.setGencfgRelease("tags/stable-104-r432");
        rtcServiceSpec.setApplicationName("service-1");
        EnvironmentResource environment = new EnvironmentResource(Environment.TESTING, "service_1");

        return createJob(rtcServiceSpec, new NannyCreateServiceJobConfig(true, false, true), environment, null);
    }

    @Test
    public void prepareGencfgProgramArgs() throws Exception {
        String expected = " testing_market_service_1_sas:SAS_MARKET_TEST_SERVICE_1:-1:-1" +
            " prestable_market_service_1_vla:VLA_MARKET_PREP_SERVICE_1:2048:1024" +
            " production_market_service_1_iva:IVA_MARKET_PROD_SERVICE_1:2048:1024" +
            " --app-sandbox-resource-id 2" +
            " --abc-service-id 1" +
            " --gencfg-release tags/stable-104-r432 --owners-login login1 --owners-login login2" +
            " --owners-group 1" +
            " --managers-login login1 --managers-login login2" +
            " --managers-group 1" +
            " --template-service-id templateServiceId" +
            " --application-name service-1" +
            " --activate " +
            " --debug " +
            " --abc-slug consumer";
        NannyCreateServiceJob nannyCreateServiceJob = prepareGencfgCreate();
        String actual = nannyCreateServiceJob.prepareProgramArgs();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void prepareYPLiteProgramArgs() throws Exception {
        String expected = " testing_market_service_1_sas:YP:0:marketservice1:1:1000:1024:_TEST_:hdd:1:3:/logs:hdd:512" +
            " testing_market_service_1_vla:YP:0:marketservice1:1:1000:1024:_TEST_:hdd:1:3" +
            " --app-sandbox-resource-id 2" +
            " --abc-service-id 1" +
            " --owners-login login1 --owners-login login2" +
            " --owners-group 1" +
            " --managers-login login1 --managers-login login2" +
            " --managers-group 1" +
            " --template-service-id templateServiceId" +
            " --application-name service-1" +
            " --activate " +
            " --debug " +
            " --abc-slug consumer";
        NannyCreateServiceJob nannyCreateServiceJob = prepareYPLiteCreate();
        String actual = nannyCreateServiceJob.prepareProgramArgs();
        Assert.assertEquals(expected, actual);
    }

    private NannyCreateServiceJob createJob(RtcServiceSpec rtcServiceSpec,
                                            NannyCreateServiceJobConfig configNannyCreateServiceJob,
                                            EnvironmentResource environment,
                                            GenCfgRelease genCfgRelease) {
        JobInstanceBuilder<NannyCreateServiceJob> jobBuilder =
            jobTester.jobInstanceBuilder(NannyCreateServiceJob.class)
                .withBean(Mockito.mock(SandboxClient.class))
                .withBean(Mockito.mock(MarketMapClient.class))
                .withBean(Mockito.mock(NannyClient.class))
                .withResource(rtcServiceSpec)
                .withResource(configNannyCreateServiceJob)
                .withResource(new AbcServiceResource(
                    "testabc",
                    1))
                .withResource(
                    SandboxTaskJobConfig.newBuilder(GenerateAndCommitDeplateConfigJob.SANDBOX_TASK_TYPE)
                        .build()
                )
                .withResource(createComponentChangeRequest());
        if (environment != null) {
            jobBuilder.withResource(environment);
        }
        if (genCfgRelease != null) {
            jobBuilder.withResource(genCfgRelease);
        }
        return jobBuilder.create();
    }

    private ComponentChangeRequest createComponentChangeRequest() {
        ComponentChangeRequest componentChangeRequest = new ComponentChangeRequest();
        ComponentSpecResource componentSpecResource = new ComponentSpecResource();
        componentSpecResource.setAbcSlug("consumer");
        componentChangeRequest.setTargetComponentSpecResource(componentSpecResource);
        return componentChangeRequest;
    }

}
