package ru.yandex.market.tsum.pipelines.common.jobs.platform;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.tsum.clients.platform.PlatformClient;
import ru.yandex.market.tsum.clients.platform.model.ComponentRuntimeModel;
import ru.yandex.market.tsum.clients.platform.model.EnvironmentRuntimeModel;
import ru.yandex.market.tsum.clients.platform.model.EnvironmentStatus;
import ru.yandex.market.tsum.clients.platform.model.EnvironmentVersionLocator;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

/**
 * @author Dmitry Poluyanov <a href="https://t.me/neiwick">Dmitry Poluyanov</a>
 * @since 27.09.17
 */
@ContextConfiguration(classes = PlatformDeployJobTest.PlatformDeployJobTestConfiguration.class)
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "tsum.platform.url=http://localhost")
public class PlatformDeployJobTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TestJobContext jobContext;

    @Autowired
    @InjectMocks
    private PlatformDeployJob platformDeployJob;

    @Mock
    private DeployJobResource deployJobResource;

    @Mock
    private PlatformDeployJob.PlatformDeployJobConfig platformDeployJobConfig;

    @Autowired
    private PlatformClient platformClient;

    @Mock
    private ComponentRuntimeModel component;

    @Before
    public void before() {
        jobContext = new TestJobContext();
        MockitoAnnotations.initMocks(this);
        platformDeployJob.setResources(Collections.singletonList(deployJobResource));
        EnvironmentRuntimeModel env = Mockito.mock(EnvironmentRuntimeModel.class);
        Mockito.when(platformClient.getEnvironmentStable(Mockito.any())).thenReturn(env);
        Mockito.when(platformClient.getHash(Mockito.anyString(), Mockito.anyString())).thenReturn("hash:123");
        Mockito.when(platformDeployJobConfig.shouldDeploy("test")).thenReturn(true);
        Mockito.when(env.getComponents()).thenReturn(Collections.singletonMap("test", component));
        Mockito.when(env.getObjectId()).thenReturn("project.app.test");
        Mockito.when(deployJobResource.getRepositoriesMap())
            .thenReturn(Collections.singletonMap("test", DeployJobResource.tagOf("x", "y")));
        Mockito.when(deployJobResource.getApplicationId()).thenReturn("app");
        Mockito.when(platformDeployJobConfig.isFailOnMissingDeployJobResource()).thenReturn(false);
    }

    @Test
    public void execute() throws Exception {
        EnvironmentVersionLocator version = new EnvironmentVersionLocator();
        ReflectionTestUtils.setField(version, "status", EnvironmentStatus.DEPLOYED);

        Mockito.when(platformClient.getEnvironmentStatus(Mockito.any()))
            .thenReturn(version);

        platformDeployJob.execute(jobContext);
        Mockito.verify(platformClient).fastDeployComponent(Mockito.anyString(), Mockito.anyLong(), Mockito.anyList());
    }

    @Test
    public void recover() throws Exception {
        EnvironmentVersionLocator version = new EnvironmentVersionLocator();
        ReflectionTestUtils.setField(version, "status", EnvironmentStatus.DEPLOYED);

        Mockito.when(platformClient.getEnvironmentStatus(Mockito.any()))
            .thenReturn(version);

        Mockito.when(component.getRepository()).thenReturn("x:y");
        Mockito.when(component.getHash()).thenReturn("hash:123");

        platformDeployJob.recover(jobContext);
        Mockito.verify(platformClient, Mockito.never()).fastDeployComponent(Mockito.anyString(), Mockito.anyLong(),
            Mockito.anyList());
    }

    @Test
    public void finishedOnDeployedStatus() throws Exception {
        EnvironmentVersionLocator deployingVersion = new EnvironmentVersionLocator();
        ReflectionTestUtils.setField(deployingVersion, "status", EnvironmentStatus.DEPLOYING);

        EnvironmentVersionLocator deployedVersion = new EnvironmentVersionLocator();
        ReflectionTestUtils.setField(deployedVersion, "status", EnvironmentStatus.DEPLOYED);

        Mockito.when(platformClient.getEnvironmentStatus(Mockito.any()))
            .thenReturn(deployingVersion)
            .thenReturn(deployedVersion);

        platformDeployJob.execute(jobContext);
    }

    @Test
    public void failsOnCancelled() throws Exception {
        expectedException.expect(JobManualFailException.class);
        expectedException.expectMessage(("Environment status is [CANCELED] but expected is [DEPLOYED]"));

        EnvironmentVersionLocator deployingVersion = new EnvironmentVersionLocator();
        ReflectionTestUtils.setField(deployingVersion, "status", EnvironmentStatus.DEPLOYING);

        EnvironmentVersionLocator canceledVersion = new EnvironmentVersionLocator();
        ReflectionTestUtils.setField(canceledVersion, "status", EnvironmentStatus.CANCELED);

        Mockito.when(platformClient.getEnvironmentStatus(Mockito.any()))
            .thenReturn(deployingVersion)
            .thenReturn(canceledVersion);

        platformDeployJob.execute(jobContext);
    }

    static class PlatformDeployTestJob extends PlatformDeployJob {
        PlatformDeployTestJob(PlatformClient platformClient, String platformUrl) {
            super(platformClient, platformUrl);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("c05fbdc7-ac21-4782-a641-147499b72b59");
        }

        @Override
        protected Poller.PollerBuilder<EnvironmentVersionLocator> createPoller() {
            return super.createPoller().allowIntervalLessThenOneSecond(true).interval(0, TimeUnit.MILLISECONDS);
        }
    }

    @Configuration
    static class PlatformDeployJobTestConfiguration {
        @Bean
        PlatformDeployJob platformDeployJob(PlatformClient platformClient,
                                            @Value("${tsum.platform.url}") String platformUrl) {
            return new PlatformDeployTestJob(platformClient, platformUrl);
        }

        @Bean
        PlatformClient platformClient() {
            return Mockito.mock(PlatformClient.class);
        }

        @Bean
        Poller.PollerBuilder pollerBuilder() {
            return Poller.builder()
                .allowIntervalLessThenOneSecond(true)
                .interval(1, TimeUnit.MILLISECONDS);
        }
    }
}
