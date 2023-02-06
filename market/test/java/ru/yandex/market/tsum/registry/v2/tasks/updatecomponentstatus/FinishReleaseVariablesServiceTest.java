package ru.yandex.market.tsum.registry.v2.tasks.updatecomponentstatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.core.auth.TsumUser;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.core.registry.v2.model.spok.PostgreSqlParams;
import ru.yandex.market.tsum.core.registry.v2.model.spok.RtcEnvironmentSpec;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroup;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroupInfo;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;
import ru.yandex.market.tsum.registry.v2.dao.ComponentSpecsDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentUpdateRequest;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.BalancerInfo;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.ComponentSpec;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.DeliveryMachine;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.YasmInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class FinishReleaseVariablesServiceTest {
    private static final String SERVICE_SLUG = "slug";
    private static final String ARCADIA_PATH = "market/arcadia/path";
    private static final String COMPONENT_ID = "component_id";
    private static final String TICKET_KEY = "MARKETINFRA-1";
    private static final String TESTING_DB_NAME = "db-testing";
    private static final String SERVICE_NAME = "rtc-app-name";
    private static final String TICKET = "MARKET-123";

    static final ServiceParams SERVICE_PARAMS;
    static final Component COMPONENT;
    static final ComponentSpec COMPONENT_SPEC;
    static final ComponentUpdateRequest COMPONENT_UPDATE_REQUEST;
    static final ComponentSpecsDao COMPONENT_SPECS_DAO;
    static final FinishReleaseVariablesService service;

    static {
        String author = "user42";
        ServiceParams serviceParams = new ServiceParams();
        serviceParams.setApplicationType("JAVA");
        serviceParams.setUser(new TsumUser(author));
        serviceParams.setArcadiaPath(ARCADIA_PATH);
        serviceParams.setStartrekTicket(TICKET_KEY);
        serviceParams.setName(SERVICE_NAME);
        serviceParams.setPgaasEnabled(true);
        serviceParams.setAbcSlug(SERVICE_SLUG);
        Map<String, RtcEnvironmentSpec> testingInstallations = new HashMap<>();
        testingInstallations.put(SERVICE_NAME, rtcEnvironmentSpec(TESTING_DB_NAME));

        serviceParams.setInstallations(Map.ofEntries(
            Map.entry(Environment.TESTING, testingInstallations)));

        SERVICE_PARAMS = serviceParams;
        COMPONENT = new Component(serviceParams, "123");
        COMPONENT_SPEC = new ComponentSpec(COMPONENT, serviceParams, author);

        COMPONENT_UPDATE_REQUEST = new ComponentUpdateRequest(
            author,
            COMPONENT_ID,
            "",
            COMPONENT_SPEC.getId(),
            TICKET
        );

        COMPONENT_SPEC.getInstallations().forEach((env, map) -> {
            map.forEach((name, installation) -> {
                installation.setBalancerInfo(new BalancerInfo("fqdn"));
                installation.getPostgresInfo().setMdbClusterId("mdbClusterId");
                installation.getPostgresInfo().setMdbClusterName("mdbClusterName");
                installation.getPostgresInfo().setMdbClusterName("mdbClusterName");
                installation.getPostgresInfo().setMdbFolderId("mdbFolderId");
                installation.getPostgresInfo().setYavSecret("sec-1231");
                installation.setYasmInfo(new YasmInfo("prj", "itype", "ctype"));
            });
        });

        COMPONENT_SPEC.setDeliveryMachine(new DeliveryMachine("projectId", "pipelineId", false, false, false));

        COMPONENT_SPECS_DAO = mock(ComponentSpecsDao.class);

        when(COMPONENT_SPECS_DAO.get(COMPONENT_SPEC.getId())).thenReturn(COMPONENT_SPEC);

        service = new FinishReleaseVariablesService(COMPONENT_SPECS_DAO);
    }

    static final Map<String, Object> EXPECTED_RESULT = Map.of(
        "serviceName", SERVICE_NAME,
        "serviceSlug", SERVICE_SLUG,
        "arcadiaPath", ARCADIA_PATH,
        "pipelines", List.of(
            new FinishReleaseVariablesService.PipelineDescription()
                .withUrl("https://tsum.yandex-team.ru/pipe/projects/projectId/pipelines/rtc-app-name")
                .withName("rtc-app-name"),
            new FinishReleaseVariablesService.PipelineDescription()
                .withUrl("https://tsum.yandex-team.ru/pipe/projects/projectId/pipelines/rtc-app-name-hotfix")
                .withName("rtc-app-name-hotfix")),
        "tsumReleaseMachine", new FinishReleaseVariablesService.ReleaseMachine()
                .withUrl("https://tsum.yandex-team.rupipe/projects/projectId/delivery-dashboard/rtc-app-name")
                .withName("rtc-app-name pipeline"),
        "nannyReleaseDashboardUrl", "https://nanny.yandex-team.ru/ui/#/services/dashboards/catalog/market_rtc-app-name/",
        "environments", List.of(
            new FinishReleaseVariablesService.EnvironmentDescription()
                .withName("TESTING")
                .withBalancer(Collections.singletonList("fqdn"))
                .withNannyServiceUrls(List.of("https://nanny.yandex-team.ru/ui/#/services/catalog/testing_market_rtc_app_name_vla/"))
                .withDeployStagesUrls(Collections.emptyList())
                .withYasmUrls(List.of(
                    "https://yasm.yandex-team.ru/template/panel/market-nginx/itype=itype;" +
                        "ctype=TESTING;" +
                        "prj=prj/",
                    "https://yasm.yandex-team.ru/template/panel/market-porto/itype=itype;" +
                        "ctype=TESTING;" +
                        "prj=prj/"))
                .withPostgres(new FinishReleaseVariablesService.EnvironmentPostgresDescription()
                    .withDbName("db-testing")
                    .withYcUrl("https://yc.yandex-team.ru/folders/mdbFolderId/managed-postgresql/cluster/mdbClusterId")
                    .withYavSecretName("rtc-app-name-testing")
                    .withYavUrl("https://yav.yandex-team" +
                        ".ru/secret/sec-1231")
                )
        ),
        "isTemporaryQuotaUsed", false
    );

    @Test
    public void test() {
        Map<String, Object> result = service.prepareVariables(COMPONENT, COMPONENT_UPDATE_REQUEST);

        List.of(
            "pipelines",
            "tsumReleaseMachine",
            "nannyReleaseDashboardUrl",
            "environments",
            "arcadiaPath",
            "serviceSlug",
            "serviceName",
            "isTemporaryQuotaUsed"
        ).forEach(field -> {
            assertThat(result.get(field).toString()).isEqualTo(EXPECTED_RESULT.get(field).toString());
        });
    }

    private static RtcEnvironmentSpec rtcEnvironmentSpec(String databaseName) {
        RtcEnvironmentSpec spec = new RtcEnvironmentSpec();
        spec.setNannyLocations(List.of(GenCfgLocation.VLA));
        spec.setPostgreSql(new PostgreSqlParams(databaseName, "s2.nano", 0, 0));
        return spec;
    }

    private static NannyService nannyService(String name, GenCfgLocation location, GenCfgCType ctype) {
        return new NannyService(name,
            new GenCfgGroup("group",
                GenCfgGroupSpec.newBuilder()
                    .withMinPower(1)
                    .withMemoryGb(1)
                    .withDiskGb(1)
                    .withInstances(1)
                    .withCType(ctype)
                    .withLocation(location)
                    .withVolumes(Collections.emptyList())
                    .build(),
                new GenCfgGroupInfo("name", "release", "master",
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
    }
}
