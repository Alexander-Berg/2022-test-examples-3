package ru.yandex.market.tsum.clients.teamcity;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.TestTeamcity;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.01.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestTeamcity.class})
public class TeamcityClientTest {
    @Autowired
    private TeamcityClient teamcityClient;

    @Test
    public void buildPayload() throws IOException, JSONException {
        String actualJson = TeamcityClient.buildPayload(new TeamcityBuild(
                "featureBranch_1",
                "MarketInfra_Sandbox_TestPipelineBuild_v2",
                ImmutableMap.of("CHANGELOG_PUSH_MOMENT", "BEFORE_BUILD"),
                new TeamcityTriggeringOptions(),
                Collections.singletonList("some-tag"),
                9968738,
                14601622
            )
        );

        String expectedJson = Resources.toString(
            Resources.getResource("teamcity/build.json"),
            Charsets.UTF_8
        );

        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Ignore
    @Test
    public void getBuilds() throws ExecutionException, InterruptedException {
        BuildItemsPage buildItemsPage = teamcityClient.getBuilds(
            TeamcityClient.BuildLocator.newBuilder()
                .withStartDate(Instant.ofEpochMilli(System.currentTimeMillis() - 1800000),
                    TeamcityClient.BuildLocator.DateCondition.AFTER)
                .withAffectedProjectId("MARKET")
                .withAgentName("market-infra-stable-qloud-agent-4")
                .withSinceDate(Instant.ofEpochMilli(System.currentTimeMillis() - 1900000))
                .withState(BuildItem.State.FINISHED)
                .build()
        ).get();
    }

    @Ignore
    @Test
    public void getBuild() throws ExecutionException, InterruptedException {
        BuildItem buildItem = teamcityClient.getBuild(12835858).get();
    }

    @Ignore
    @Test
    public void getBuildStatistics() throws ExecutionException, InterruptedException {
        BuildStatistics statistics = teamcityClient.getBuildStatistics(12835858).get();
    }

    @Ignore
    @Test
    public void getAgentPoolByAgentId() throws ExecutionException, InterruptedException {
        AgentPool agentPool = teamcityClient.getAgentPoolByAgentId(88966).get();
    }

    @Ignore
    @Test
    public void getArtifcatContent() throws ExecutionException, InterruptedException {
        BuildItem buildItem = new BuildItem();
        buildItem.setId(13937388);
        String artifact = teamcityClient.getArtifactContent(buildItem, "/packages.json").get();

        System.out.println(artifact);
    }

    @Test
    public void doubleLookupLimit() {
        Assert.assertEquals(
            "https://teamcity.yandex-team.ru/app/rest/builds?locator=affectedProject:(id:Market),finishDate:" +
                "(date:20180308T100000%2B0300,condition:after),finishDate:(date:20180308T190000%2B0300," +
                "condition:before),state:finished,branch:default:any,lookupLimit:20000",
            teamcityClient.doubleLookupLimit("https://teamcity.yandex-team" +
                ".ru/app/rest/builds?locator=affectedProject:(id:Market),finishDate:(date:20180308T100000%2B0300," +
                "condition:after),finishDate:(date:20180308T190000%2B0300,condition:before),state:finished," +
                "branch:default:any,lookupLimit:10000")
        );

        Assert.assertEquals(
            "https://teamcity.yandex-team.ru/app/rest/builds?locator=affectedProject:(id:Market),finishDate:" +
                "(date:20180308T100000%2B0300,condition:after),finishDate:(date:20180308T190000%2B0300," +
                "condition:before),branch:default:any,lookupLimit:20000,state:finished",
            teamcityClient.doubleLookupLimit("https://teamcity.yandex-team" +
                ".ru/app/rest/builds?locator=affectedProject:(id:Market),finishDate:(date:20180308T100000%2B0300," +
                "condition:after),finishDate:(date:20180308T190000%2B0300,condition:before),branch:default:any," +
                "lookupLimit:10000,state:finished")
        );

        Assert.assertEquals(
            "https://teamcity.yandex-team.ru/app/rest/builds?locator=affectedProject:(id:Market),finishDate:" +
                "(date:20180308T100000%2B0300,condition:after),finishDate:(date:20180308T190000%2B0300," +
                "condition:before),branch:default:any,state:finished",
            teamcityClient.doubleLookupLimit("https://teamcity.yandex-team" +
                ".ru/app/rest/builds?locator=affectedProject:(id:Market),finishDate:(date:20180308T100000%2B0300," +
                "condition:after),finishDate:(date:20180308T190000%2B0300,condition:before),branch:default:any," +
                "state:finished")
        );
    }

    @Test
    public void parseBuildItemParameters() throws IOException {
        String body = Resources.toString(Resources.getResource("teamcity/buildItemParameters.json"), Charsets.UTF_8);

        Map<String, String> parameters = TeamcityClient.parseBuildItemParameters(body).get();

        Assert.assertEquals(parameters.size(), 392);
        Assert.assertEquals(parameters.get("env.LANGUAGE"), "en_US.UTF8");

        String parameterWithoutValue = "secure:teamcity.password.env.CONDUCTOR_TOKEN";
        Assert.assertEquals(parameters.get(parameterWithoutValue), "");
    }
}
