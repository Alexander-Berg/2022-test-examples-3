package ru.yandex.market.tsum.spok.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.core.auth.TsumUser;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.core.registry.v2.model.spok.BalancerParams;
import ru.yandex.market.tsum.core.registry.v2.model.spok.MemcachedParams;
import ru.yandex.market.tsum.core.registry.v2.model.spok.PostgreSqlParams;
import ru.yandex.market.tsum.core.registry.v2.model.spok.RtcEnvironmentSpec;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.registry.proto.model.AppType;
import ru.yandex.market.tsum.registry.proto.model.DeployType;
import ru.yandex.market.tsum.registry.proto.model.JavaTemplate;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.SpokDraft;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.ComponentSpec;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.Features;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.InstallationSpec;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.JavaInfo;
import ru.yandex.market.tsum.registry.v2.dao.model.componentspec.JugglerInfo;
import ru.yandex.market.tsum.spok.pipelines.SpokPipelineFactory;
import ru.yandex.market.tsum.spok.pipelines.jobs.builder.util.SpokPipelineContext;

import static org.mockito.Mockito.when;

@Configuration
public class SpokPipelineTestConfiguration {

    private final TsumUser tsumUser = new TsumUser("admin", Collections.singleton(TsumUser.ADMIN_ROLE));

    @Bean
    List<Pipeline> spokPipelinesToValidate() {
        return commonPipelines();
    }

    private List<Pipeline> commonPipelines() {
        return ImmutableList.of(
            javaPipeline(),
            cppPipeline(),
            pythonPipeline()
        );
    }

    @Bean
    public Pipeline existingJavaPipeline() {
        return pipelineWithFullRtcEnvironmentSettings("JAVA_EXISTING");
    }

    @Bean
    public Pipeline javaPipeline() {
        return pipelineWithFullRtcEnvironmentSettings("JAVA");
    }

    @Bean
    public Pipeline javaPipelineWithPgTemplate() {
        return pipelineWithFullRtcEnvironmentSettings("JAVA", JavaTemplate.GENERIC_WITH_POSTGRES);
    }

    @Bean
    public Pipeline javaPipelineWithBazingaTemplate() {
        return pipelineWithFullRtcEnvironmentSettings("JAVA", JavaTemplate.BAZINGA_TMS_WITH_MONGO);
    }

    @Bean
    public Pipeline javaPipelineWithQuartzTemplate() {
        return pipelineWithFullRtcEnvironmentSettings("JAVA", JavaTemplate.QUARTZ_TMS_WITH_POSTGRES);
    }

    @Bean
    public Pipeline cppPipeline() {
        return pipelineWithFullRtcEnvironmentSettings("CPP");
    }

    @Bean
    public Pipeline pythonPipeline() {
        return pipelineWithFullRtcEnvironmentSettings("PYTHON");
    }

    @Bean
    public Pipeline golangPipeline() {
        return pipelineWithFullRtcEnvironmentSettings("GOLANG");
    }

    private Pipeline pipelineWithFullRtcEnvironmentSettings(String pipelineType) {
        return pipelineWithFullRtcEnvironmentSettings(pipelineType, JavaTemplate.GENERIC);
    }

    private Pipeline pipelineWithFullRtcEnvironmentSettings(String pipelineType, JavaTemplate template) {
        ServiceParams serviceParams = setupServiceParams(pipelineType, template);

        ComponentSpec componentSpec = setupComponentSpec(pipelineType);

        Component componentMock = setupComponent(serviceParams);
        String nannyTvmClientId = "testClientId";
        String ypTvmClientId = "testClientId";
        String author = "user";

        SpokPipelineContext context = SpokPipelineContext.Builder.builder()
            .withComponent(componentMock)
            .withNannyTvmClientId(nannyTvmClientId)
            .withYpTvmClientId(ypTvmClientId)
            .withComponentSpec(componentSpec)
            .withAuthor(author)
            .withTicket(serviceParams.getStartrekTicket())
            .withAbcSlug(serviceParams.getAbcSlug())
            .withParentAbcSlug(serviceParams.getParentAbcSlug())
            .build();

        return SpokPipelineFactory.newInstance(context).createPipeline();
    }

    private ComponentSpec setupComponentSpec(String applicationType) {
        Features testFeatures = new Features();
        testFeatures.setSolomon(false);
        testFeatures.setReleasePipeline(false);
        testFeatures.setPgaas(false);
        testFeatures.setLiquibase(false);
        testFeatures.setPerInstallationBalancer(false);
        testFeatures.setCodeGeneration(false);
        testFeatures.setNanny(true);
        testFeatures.setMemcached(false);
        testFeatures.setNannyDashboard(false);
        testFeatures.setYaDeploy(false);

        JavaInfo testJavaInfo = new JavaInfo(JavaTemplate.GENERIC,
            "ru.yandex.market.test",
            "TEST");

        InstallationSpec testInstallationSpec = new InstallationSpec();

        JugglerInfo jugglerInfo = new JugglerInfo("namespace");
        jugglerInfo.setHost("test-testing");
        testInstallationSpec.setJugglerInfo(jugglerInfo);

        ComponentSpec componentSpec = new ComponentSpec("author", "componentId");

        componentSpec.setApplicationType(AppType.valueOf(applicationType));
        componentSpec.setJavaInfo(testJavaInfo);
//        componentSpec.setFeatures(testFeatures);
        componentSpec.setDeployType(DeployType.YANDEX_DEPLOY);
        componentSpec.setInstallations(Map.of(Environment.TESTING, Map.of("test", testInstallationSpec)));

        return componentSpec;
    }

    private ServiceParams setupServiceParams(String applicationType, JavaTemplate template) {
        ServiceParams serviceParams = new ServiceParams();
        serviceParams.setName("test");
        serviceParams.setApplicationType(applicationType);
        serviceParams.setUser(tsumUser);
        serviceParams.setStartrekTicket("testStartrekTicket");
        serviceParams.setJavaAppTemplate(template);
        serviceParams.setJavaTraceModuleName("SERVICE_NAME_TRACE_MODULE");
        serviceParams.setAbcSlug("marketinfra");
        serviceParams.setInstallations(Map.of(Environment.TESTING, Map.of("test",
            RtcEnvironmentSpec.builder().withLocations(List.of()).build())));
        return serviceParams;
    }

    private Component setupComponent(ServiceParams serviceParams) {
        Component componentMock = Mockito.mock(Component.class);

        SpokDraft spokDraft = new SpokDraft(serviceParams);

        when(componentMock.getSpokDraft())
            .thenReturn(spokDraft);
        when(componentMock.getName())
            .thenReturn("spok-test-app");
        Map<Environment, RtcEnvironmentSpec> rtcEnvironmentsSpecs = rtcEnvironmentSpecs();

        spokDraft.setEnvironments(rtcEnvironmentsSpecs);
        spokDraft.setActiveEnvironments(activeEnvironments());

        return componentMock;
    }

    private RtcEnvironmentSpec createRtcEnvironmentSpecWithFullSettings() {

        MemcachedParams memcachedParams = Mockito.mock(MemcachedParams.class);
        when(memcachedParams.getMemcachedName())
            .thenReturn("testMemcachedName");
        when(memcachedParams.getMemcachedComment())
            .thenReturn("testMemecachedComment");
        when(memcachedParams.getMemcachedSizeMb())
            .thenReturn(42L);

        String testAccessValue = "testAccessValue";
        BalancerParams balancerParams = Mockito.mock(BalancerParams.class);
        when(balancerParams.getExpectedRps())
            .thenReturn("testExpectedRpsValue");
        when(balancerParams.getNeedSsl())
            .thenReturn(false);
        when(balancerParams.isExternal())
            .thenReturn(false);

        PostgreSqlParams postgreSqlParams = Mockito.mock(PostgreSqlParams.class);
        when(postgreSqlParams.getConnectionLimit())
            .thenReturn(10);
        when(postgreSqlParams.getDatabaseName())
            .thenReturn("testDataBaseName");
        when(postgreSqlParams.getInstanceType())
            .thenReturn("testInstanceType");
        when(postgreSqlParams.getVolumeSizeGb())
            .thenReturn(10L);

        RtcEnvironmentSpec rtcEnvironmentSpec = Mockito.mock(RtcEnvironmentSpec.class);

        when(rtcEnvironmentSpec.getPostgreSql())
            .thenReturn(postgreSqlParams);
        when(rtcEnvironmentSpec.getMemcached())
            .thenReturn(memcachedParams);
        when(rtcEnvironmentSpec.getBalancer())
            .thenReturn(balancerParams);
        when(rtcEnvironmentSpec.getNannyLocations())
            .thenReturn(Arrays.asList(GenCfgLocation.VLA, GenCfgLocation.SAS, GenCfgLocation.MAN));


        return rtcEnvironmentSpec;
    }

    private Map<Environment, RtcEnvironmentSpec> rtcEnvironmentSpecs() {
        Map<Environment, RtcEnvironmentSpec> rtcSpecsByEnvironments = Maps.newEnumMap(Environment.class);
        List<Environment> environments = Arrays.asList(
            Environment.TESTING,
            Environment.PRESTABLE,
            Environment.PRODUCTION
        );
        for (Environment environment : environments) {
            RtcEnvironmentSpec rtcSpecs = createRtcEnvironmentSpecWithFullSettings();
            rtcSpecsByEnvironments.put(environment, rtcSpecs);
        }
        return rtcSpecsByEnvironments;
    }

    private Set<Environment> activeEnvironments() {
        return new HashSet<>(Arrays.asList(Environment.TESTING,
            Environment.PRESTABLE,
            Environment.PRODUCTION)
        );
    }
}
