package ru.yandex.market.tsum.pipelines.common.jobs.aqua.testcasecomment;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.tsum.clients.aqua.behaviors.AllureTestCaseReport;
import ru.yandex.market.tsum.clients.aqua.behaviors.AllureTestCaseReportParserUtil;
import ru.yandex.market.tsum.clients.aqua.startek.StartrekAquaTestCaseService;
import ru.yandex.market.tsum.clients.aqua.startek.TestCaseForStartrek;
import ru.yandex.market.tsum.clients.aqua.testcases.AllureTestCaseResolver;
import ru.yandex.market.tsum.clients.aqua.testcases.BasicTestCaseReport;
import ru.yandex.market.tsum.clients.aqua.testcases.CheckTmsJobMessageParser;
import ru.yandex.market.tsum.clients.aqua.testcases.CheckTmsJobsTestCaseReport;
import ru.yandex.market.tsum.clients.aqua.testcases.TestCaseReport;
import ru.yandex.market.tsum.clients.aqua.testcases.TmsJobState;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AquaTestCaseCommentTest {

    @Test
    public void testFinalComment() throws IOException {
        assertGenerateMethodComment("finalTestBehaviours.json", "#|\n"
            + "|| checkTMSJobs | dynamicStatsExecutor| MBI-52251 | error ||\n"
            + "|| checkTMSJobs | fulfillmentOrderBillingExecutor | MBI-54493| error ||\n"
            + "|| completeDailyTest | | MBI-47703 | error ||\n"
            + "|#\n", "expected_comment.txt");
    }

    @Test
    public void testFinalAllCasesKnownComment() throws IOException {
        assertGenerateMethodComment("finalTestBehaviours.json", "#|\n"
            + "|| checkTMSJobs | dynamicStatsExecutor| MBI-52251 | error ||\n"
            + "|| checkTMSJobs | fulfillmentOrderBillingExecutor | MBI-54493| error ||\n"
            + "|| checkTMSJobs | pushCpaCategoriesExecutor | MBI-54493| error ||\n"
            + "|| checkTMSJobs | updateCampaignStateExecutor | MBI-54493| error ||\n"
            + "|| completeDailyTest | | MBI-47703 | error ||\n"
            + "|#\n", "expected_all_known_cases_comment.txt");
    }

    @Test
    public void testParseBehaviors() throws IOException {
        String behaviorJson = Resources.toString(
            Resources.getResource(AquaTestCaseCommentTest.class, "allure.json"), Charsets.UTF_8);

        List<AllureTestCaseReport> errorTestCases
            = AllureTestCaseReportParserUtil.parseFromJson(behaviorJson);
        Assert.assertEquals(AllureTestCaseReport.newBuilder()
            .withName("checkTMSJobs")
            .withMessage(
                "Проверка успешного запуска упавшей джобы JobState{jobName='dynamicStatsExecutor', "
                    + "status='Job Exception: class org.apache.http.ConnectionClosedException: Premature end of chunk "
                    + "coded message body: closing chunk expected', startTime=2020-12-10T02:00:05.265+03:00, "
                    + "finishTime=2020-12-10T02:54:53.566+03:00, duration=null, id=null, hostName='null'}")
            .withStatus("FAILED")
            .build(), errorTestCases.get(0));
        Assert.assertEquals(AllureTestCaseReport.newBuilder()
            .withName("completeDailyTest")
            .withMessage("Случайные кампании для тарифа FREE")
            .withStatus("FAILED")
            .build(), errorTestCases.get(1));
    }

    @Test
    public void testParseCheckTmsJobs() throws IOException {
        String behaviorJson = Resources.toString(
            Resources.getResource(AquaTestCaseCommentTest.class, "allure.json"), Charsets.UTF_8);
        List<AllureTestCaseReport> errorTestCases
            = AllureTestCaseReportParserUtil.parseFromJson(behaviorJson);
        TmsJobState tmsJobState =
            CheckTmsJobMessageParser.parseJobState(errorTestCases.get(0).getMessage());
        Assert.assertEquals(TmsJobState.newBuilder()
            .withJobName("dynamicStatsExecutor")
            .withStatus("Job Exception: class org.apache.http.ConnectionClosedException: "
                + "Premature end of chunk coded message body: closing chunk expected")
            .build(), tmsJobState);
    }

    @Test
    public void testAllureTestCaseResolver() throws IOException {
        String behaviorJson = Resources.toString(
            Resources.getResource(AquaTestCaseCommentTest.class, "allure.json"), Charsets.UTF_8);

        List<AllureTestCaseReport> errorTestCases
            = AllureTestCaseReportParserUtil.parseFromJson(behaviorJson);
        TestCaseReport checkTmsJobsTestCase = AllureTestCaseResolver.resolve(errorTestCases.get(0));
        TestCaseReport basicTestCase = AllureTestCaseResolver.resolve(errorTestCases.get(1));

        Assert.assertEquals(CheckTmsJobsTestCaseReport.newBuilder()
                .withName("checkTMSJobs")
                .withMessage("Проверка успешного запуска упавшей джобы JobState{jobName='dynamicStatsExecutor',"
                    + " status='Job Exception: class org.apache.http.ConnectionClosedException: Premature end of "
                    + "chunk coded message body: closing chunk expected', startTime=2020-12-10T02:00:05.265+03:00, "
                    + "finishTime=2020-12-10T02:54:53.566+03:00, duration=null, id=null, hostName='null'}")
                .withJobState(TmsJobState.newBuilder()
                    .withJobName("dynamicStatsExecutor")
                    .withStatus(
                        "Job Exception: class org.apache.http.ConnectionClosedException: Premature end of chunk coded" +
                            " message body: closing chunk expected")
                    .build())
                .build(),
            checkTmsJobsTestCase);

        Assert.assertEquals(BasicTestCaseReport.newBuilder()
                .withName("completeDailyTest")
                .withMessage("Случайные кампании для тарифа FREE")
                .build(),
            basicTestCase
        );
    }

    private void assertGenerateMethodComment(String behavioursJson, String knownIssues, String expectedFile)
        throws IOException {
        String behaviorJson = Resources.toString(
            Resources.getResource(AquaTestCaseCommentTest.class, behavioursJson), Charsets.UTF_8);
        List<AllureTestCaseReport> errorTestCases
            = AllureTestCaseReportParserUtil.parseFromJson(behaviorJson);
        Collection<TestCaseForStartrek> aquaTestCases = AllureTestCaseResolver.resolve(errorTestCases);

        Session startrekClient = mock(Session.class);
        StartrekAquaTestCaseService startrekAquaTestCaseService = new StartrekAquaTestCaseService(startrekClient);
        Issue issue = mock(Issue.class);
        when(issue.getDescription()).thenReturn(Option.ofNullable(knownIssues));
        when(startrekClient.issues()).thenReturn(mock(Issues.class));
        when(startrekClient.issues().get(anyString())).thenReturn(issue);

        String actual = startrekAquaTestCaseService.generateComment(aquaTestCases, "MBI-55132");
        String expected = Resources.toString(
            Resources.getResource(AquaTestCaseCommentTest.class, expectedFile), Charsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }
}
