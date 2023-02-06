package ru.yandex.market.tsum.pipelines.market_infra.jobs;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.tsum.clients.teamcity.AgentPool;
import ru.yandex.market.tsum.clients.teamcity.BuildAgent;
import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.clients.teamcity.BuildItemsPage;
import ru.yandex.market.tsum.clients.teamcity.TeamcityClient;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

/**
 * @author Dmitry Poluyanov <a href="https://t.me/neiwick">Dmitry Poluyanov</a>
 * @since 04.09.17
 */
@ContextConfiguration(classes = TestPrestableAgentsTestRegression.TestPrestableAgentsTestConfigurationRegression.class)
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
public class TestPrestableAgentsTestRegression {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    @InjectMocks
    private TestAgentsRegression testAgentsRegression;

    @Autowired
    private TeamcityClient teamcityClient;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        AgentPool agentPool = Mockito.mock(AgentPool.class);

        Mockito.when(agentPool.getBuildAgents())
            .thenReturn(Collections.singletonList(new BuildAgent()));

        Mockito.when(teamcityClient.getAgentPool(Mockito.anyInt()))
            .thenReturn(Futures.immediateFuture(agentPool));
    }

    @Test
    public void successfulWhenFailRateGreaterThenInStableByPermittedValue() throws Exception {
        BuildItemsPage currentSlice = new BuildItemsPage();
        List<BuildItem> prestableBuilds = createSuccessBuilds(45);
        prestableBuilds.addAll(createBuilds(5, BuildItem.Status.FAILURE));

        ReflectionTestUtils.setField(currentSlice, "build", prestableBuilds);

        BuildItemsPage prevSlice = new BuildItemsPage();
        List<BuildItem> stableBuilds = createSuccessBuilds(50);

        ReflectionTestUtils.setField(prevSlice, "build", stableBuilds);

        Mockito.when(teamcityClient.getBuilds(Mockito.any()))
            .thenReturn(Futures.immediateFuture(currentSlice))
            .thenReturn(Futures.immediateFuture(prevSlice));

        Mockito.when(teamcityClient.getNextBuildItemsPage(Mockito.any()))
            .thenCallRealMethod();

        testAgentsRegression.execute(new TestJobContext());
    }

    @Test
    public void failsIfMoreFailsThenInStable() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
            "Prestable agents have unacceptable rate of failed builds. Expected less then 30%. Actual: 50%."
        );

        BuildItemsPage currentSlice = new BuildItemsPage();
        List<BuildItem> prestableBuilds = createSuccessBuilds(25);
        prestableBuilds.addAll(createBuilds(25, BuildItem.Status.FAILURE));

        ReflectionTestUtils.setField(currentSlice, "build", prestableBuilds);

        BuildItemsPage prevSlice = new BuildItemsPage();
        List<BuildItem> stableBuilds = createSuccessBuilds(40);
        stableBuilds.addAll(createBuilds(10, BuildItem.Status.FAILURE));

        ReflectionTestUtils.setField(prevSlice, "build", stableBuilds);

        Mockito.when(teamcityClient.getBuilds(Mockito.any()))
            .thenReturn(Futures.immediateFuture(currentSlice))
            .thenReturn(Futures.immediateFuture(prevSlice));

        Mockito.when(teamcityClient.getNextBuildItemsPage(Mockito.any()))
            .thenCallRealMethod();

        testAgentsRegression.execute(new TestJobContext());
    }

    private List<BuildItem> createSuccessBuilds(int amount) {
        return createBuilds(amount, BuildItem.Status.SUCCESS);
    }

    private List<BuildItem> createBuilds(int amount, BuildItem.Status status) {
        List<BuildItem> builds = IntStream.range(0, amount).mapToObj(i -> new BuildItem()).collect(Collectors.toList());
        builds.forEach(build -> build.setStatus(status));
        return builds;
    }

    @Configuration
    static class TestPrestableAgentsTestConfigurationRegression {
        @Bean
        TeamcityClient teamcityClient() {
            return Mockito.mock(TeamcityClient.class);
        }

        @Bean
        TestAgentsRegression testPrestableAgents() {
            return new TestAgentsRegression();
        }
    }
}
