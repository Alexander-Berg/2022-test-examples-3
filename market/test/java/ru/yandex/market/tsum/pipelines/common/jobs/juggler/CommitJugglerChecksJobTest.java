package ru.yandex.market.tsum.pipelines.common.jobs.juggler;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.pipelines.common.resources.AbcServiceResource;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroup;
import ru.yandex.market.tsum.pipelines.common.resources.MdbAlertResource;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;
import ru.yandex.market.tsum.pipelines.common.resources.SolomonAlertsConfigResource;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.common.resources.YandexDeployStage;
import ru.yandex.market.tsum.pipelines.sre.resources.NannyAuthAttrsOwners;
import ru.yandex.market.tsum.pipelines.sre.resources.RtcServiceSpec;
import ru.yandex.market.tsum.pipelines.sre.resources.YandexDeployServiceSpec;

public class CommitJugglerChecksJobTest {
    private final CommitJugglerChecksJob job;
    private final CommitYandexDeployJugglerChecksJob yandexDeployJugglerChecksJob;

    public CommitJugglerChecksJobTest() {
        job = new CommitJugglerChecksJob(
            createRtcServiceSpec(),
            createStartrekTicket(),
            createCommitJugglerChecksJobConfig(),
            createSolomonAlertsConfigResource(),
            createMdbAlertResources(),
            createAbcServiceResource()
        );

        yandexDeployJugglerChecksJob = new CommitYandexDeployJugglerChecksJob(
            createDeployServiceSpec(),
            createAbcServiceResource(),
            createCommitDeployJugglerChecksJobConfig()
        );
    }

    private YandexDeployServiceSpec createDeployServiceSpec() {
        YandexDeployServiceSpec deployServiceSpec = new YandexDeployServiceSpec(
            "service",
            "service",
            "market-test",
            null,
            null,
            "marketservice1",
            "markettest",
            "layerId",
            Arrays.asList(
                new YandexDeployStage(
                    "stage-testing",
                    Environment.TESTING
                ),
                new YandexDeployStage(
                    "stage-prestable",
                    Environment.PRESTABLE
                ),
                new YandexDeployStage(
                    "stage-production",
                    Environment.PRODUCTION
                )
            ),
            null
        );
        Arrays.asList(Environment.TESTING, Environment.PRESTABLE, Environment.PRODUCTION)
            .forEach(deployServiceSpec::fillInJugglerHostForEnvironment);
        return deployServiceSpec;
    }

    private RtcServiceSpec createRtcServiceSpec() {
        RtcServiceSpec rtcServiceSpec = new RtcServiceSpec(
            "service",
            "Test service 1",
            "marketservice1",
            "markettest",
            new NannyAuthAttrsOwners(
                Arrays.asList(
                    new StaffPerson("login1", -1, null, null, null, null),
                    new StaffPerson("login2", -1, null, null, null, null)
                ),
                Collections.singletonList(new StaffGroup(1, "group1", "dpt_group1"))
            )
        );
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
        Stream.of(GenCfgCType.values()).forEach(rtcServiceSpec::fillInJugglerHostForCType);
        return rtcServiceSpec;
    }

    private SolomonAlertsConfigResource createSolomonAlertsConfigResource() {
        SolomonAlertsConfigResource solomonAlertsConfig = new SolomonAlertsConfigResource();
        solomonAlertsConfig.setApplicationName("service");
        solomonAlertsConfig.setSolomonProjectId("market-service");
        solomonAlertsConfig.setSolomonServiceId("service_ag");
        solomonAlertsConfig.setEnvironmentNames(List.of("testing", "prestable", "stable"));
        return solomonAlertsConfig;
    }

    private List<MdbAlertResource> createMdbAlertResources() {
        return List.of(
            new MdbAlertResource("testing", "postgres", "service"),
            new MdbAlertResource("prestable", "postgres", "service"),
            new MdbAlertResource("stable", "postgres", "service")
        );
    }

    private static StartrekTicket createStartrekTicket() {
        return new StartrekTicket("MARKETINFRA-0");
    }

    private static CommitJugglerChecksJobConfig createCommitJugglerChecksJobConfig() {
        return new CommitJugglerChecksJobConfig.Builder()
            .withCustomChecks(Arrays.asList("custom_check1", "custom_check2"))
            .withCustomTags(Arrays.asList("custom_tag1", "custom_tag2"))
            .withCtypeToTagsMap(ImmutableMap.of(
                GenCfgCType.TESTING, Arrays.asList("custom_tag_testing1", "custom_tag_testing2")
            ))
            .build();
    }

    private static CommitYandexDeployJugglerChecksJobConfig createCommitDeployJugglerChecksJobConfig() {
        return new CommitYandexDeployJugglerChecksJobConfig.Builder()
            .withCustomChecks(Arrays.asList("custom_check1", "custom_check2"))
            .withCustomTags(Arrays.asList("custom_tag1", "custom_tag2"))
            .withEnvironmentToTagsMap(ImmutableMap.of(
                Environment.TESTING, Arrays.asList("custom_tag_testing1", "custom_tag_testing2")
            ))
            .build();
    }

    private static AbcServiceResource createAbcServiceResource() {
        return new AbcServiceResource("test-abc-service", 0);
    }

    @Test
    public void testGeneratePullRequestTitle() {
        String expected = "MARKETINFRA-0 Add Juggler configs for SPoK service service";
        String result = job.generatePullRequestTitle(createRtcServiceSpec());
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGenerateSandboxTaskParams() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put(
            "test-abc-service/market.rtc.service-testing.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.rtc.nanny.service-testing.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "test-abc-service/market.rtc.service-prestable.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.rtc.nanny.service-prestable.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "test-abc-service/market.rtc.service.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.rtc.nanny.service.yaml")),
                StandardCharsets.UTF_8
            )
        );

        expected.put(
            "market.alert.service-testing.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.alert.service-testing.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "market.alert.service-prestable.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.alert.service-prestable.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "market.alert.service.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.alert.service.yaml")),
                StandardCharsets.UTF_8
            )
        );

        expected.put(
            "market.mdb.postgres.service-testing.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.mdb.postgres.service-testing.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "market.mdb.postgres.service-prestable.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.mdb.postgres.service-prestable.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "market.mdb.postgres.service.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.mdb.postgres.service.yaml")),
                StandardCharsets.UTF_8
            )
        );

        Map<String, String> result = job.generateSandboxTaskParams();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGenerateSandboxTaskParamsForDeploy() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put(
            "test-abc-service/market.rtc.service-testing.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.rtc.deploy.service-testing.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "test-abc-service/market.rtc.service-prestable.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.rtc.deploy.service-prestable.yaml")),
                StandardCharsets.UTF_8
            )
        );
        expected.put(
            "test-abc-service/market.rtc.service.yaml",
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(
                    "/commitJugglerChecksJobTest/market.rtc.deploy.service.yaml")),
                StandardCharsets.UTF_8
            )
        );
        Map<String, String> result = yandexDeployJugglerChecksJob.generateSandboxTaskParams();
        Assert.assertEquals(expected, result);
    }
}
