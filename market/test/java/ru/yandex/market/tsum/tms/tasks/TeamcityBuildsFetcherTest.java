package ru.yandex.market.tsum.tms.tasks;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.clients.teamcity.AgentPool;
import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.clients.teamcity.BuildStatistics;
import ru.yandex.market.tsum.clients.teamcity.TeamcityClient;
import ru.yandex.market.tsum.clients.teamcity.TestOccurrences;
import ru.yandex.market.tsum.tms.tasks.teamcity.TeamcityBuildsFetcher;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 25.01.18
 */
public class TeamcityBuildsFetcherTest {
    @Test
    public void createTskvRecord() throws Exception {
        String buildItemString = Resources.toString(Resources.getResource("teamcity/buildItem.json"), Charsets.UTF_8);
        String buildStatisticsString = Resources.toString(Resources.getResource("teamcity/buildStatistics.json"), Charsets.UTF_8);
        String agentPoolString = Resources.toString(Resources.getResource("teamcity/agentPool.json"), Charsets.UTF_8);

        BuildItem buildItem = TeamcityClient.parseBuildItem(buildItemString);
        BuildStatistics buildStatistics = TeamcityClient.parseBuildStatistics(buildStatisticsString);
        AgentPool agentPool = TeamcityClient.parseAgentPool(agentPoolString);

        Assert.assertEquals(
            "tskv\tdate=" + TeamcityBuildsFetcher.formatDate(buildItem.getFinishDate()) + "\t" +
                "id=12835858\t" +
                "status=success\t" +
                "statusText=Success\t" +
                "number=609\t" +
                "branchName=master\t" +
                "buildTypeId=MarketInfra_Tsum_TsumRelease\t" +
                "buildTypeName=Tsum Release\t" +
                "buildTypeProjectId=MarketInfra_Tsum\t" +
                "buildTypeProjectName=Market :: INFRA :: Tsum\t" +
                "tags=5a65bf937c0e908fd6945c49-MarketTeamcityBuildJob0-2\t" +
                "queuedDate=" + TeamcityBuildsFetcher.formatDate(buildItem.getQueuedDate()) + "\t" +
                "startDate=" + TeamcityBuildsFetcher.formatDate(buildItem.getStartDate()) + "\t" +
                "finishDate=" + TeamcityBuildsFetcher.formatDate(buildItem.getFinishDate()) + "\t" +
                "triggerType=user\t" +
                "triggeredDate=" + TeamcityBuildsFetcher.formatDate(buildItem.getTriggered().getDate()) + "\t" +
                "triggeredUser=robot-market-infra\t" +
                "agentName=market-infra-stable-qloud-agent-4\t" +
                "lastChangesVersions=347b95a58a837dcccfe5aed2c712eab5e4000ee8\t" +
                "lastChangesDates=" +
                buildItem.getLastChanges().getChange().stream().map(c -> TeamcityBuildsFetcher.formatDate(c.getDate())).collect(Collectors.joining(",")) + "\t" +
                "lastChangesUserNames=sid-hugo\t" +
                "revisionVersions=347b95a58a837dcccfe5aed2c712eab5e4000ee8\t" +
                "revisionBranchNames=refs/heads/master\t" +
                "revisionVcsRootNames=tsum.git\t" +
                "revisionVcsRootIds=MarketInfra_TsumGit\t" +
                "artifactsSizeBytes=636501\t" +
                "buildDurationSeconds=218\t" +
                "buildDurationNetTimeSeconds=214\t" +
                "artifactsPublishingSeconds=0\t" +
                "buildFinishingSeconds=0\t" +
                "firstStepPreparationSeconds=0\t" +
                "sourceUpdateSeconds=3\t" +
                "timeSpentInQueueSeconds=2\t" +
                "ignoredTestCount=2\t" +
                "passedTestCount=159\t" +
                "totalTestCount=161\t" +
                "agentPoolName=Market Infra",
            TeamcityBuildsFetcher.createTskvRecord(buildItem, buildStatistics, agentPool)
        );
    }

    @Test
    public void testCorrectTestOccurrenceParsing() throws IOException {
        String testOccurrencesString = Resources.toString(Resources.getResource("teamcity/testOccurrences.json"), Charsets.UTF_8);

        TestOccurrences testOccurrences = TeamcityClient.parseTestOccurrences(testOccurrencesString);
        Assert.assertEquals(testOccurrences.getCount(), 10);

        List<TestOccurrences.Test> tests = testOccurrences.getTestOccurrence();
        Assert.assertEquals(tests.size(), 10);
        Assert.assertEquals(tests.get(0).getTestAndBuildId(), "id:4497,build:(id:14319959)");
        Assert.assertEquals(tests.get(0).getName(), "ru.yandex.market.mbo.cms.core.dao.node.processor.MovePlaceholdersToTypeProcessorTest.testTransformationSecondTime");
    }
}