package ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverage;
import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverageDiff;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author s-ermakov
 */
public class JaCoCoCodeCoverageJobTest {
    private static final int RELEASE_BUILD_ID = 14247210;
    private static final int TOTAL_TESTS_COUNT = 228;
    private static final int TESTS_COUNT_DIFF = 10;
    private JaCoCoCodeCoverageJob job;

    @Before
    public void setUp() throws Exception {
        job = new JaCoCoCodeCoverageJob();
    }

    @Test
    public void testNotification() {
        CodeCoverage masterCoverage = getCodeCoverage("/artifacts/jacoco_master_branch.xml");
        CodeCoverage releaseCoverage = getCodeCoverage("/artifacts/jacoco_release_branch.xml");

        BuildItem item = Mockito.mock(BuildItem.class);
        Mockito.when(item.getId()).thenReturn(RELEASE_BUILD_ID);

        CodeCoverageDiff diff = new CodeCoverageDiff(masterCoverage, releaseCoverage);

        String telegramMessage = createTelegramNotification(diff,
            "https://teamcity.yandex-team.ru/viewLog.html?buildId=14247210",
            TOTAL_TESTS_COUNT, TESTS_COUNT_DIFF);
        String commonMessage = createCommonNotification(diff,
            "https://teamcity.yandex-team.ru/viewLog.html?buildId=14247210",
            TOTAL_TESTS_COUNT, TESTS_COUNT_DIFF);

        assertMessage(telegramMessage);
        assertMessage(commonMessage);
    }

    @Test
    public void testNotificationWithoutPreviousBuild() {
        CodeCoverage releaseCoverage = getCodeCoverage("/artifacts/jacoco_release_branch.xml");

        BuildItem item = Mockito.mock(BuildItem.class);
        Mockito.when(item.getId()).thenReturn(RELEASE_BUILD_ID);

        CodeCoverageDiff diff = new CodeCoverageDiff(null, releaseCoverage);

        String telegramMessage = createTelegramNotification(diff,
            "https://teamcity.yandex-team.ru/viewLog.html?buildId=14247210",
            TOTAL_TESTS_COUNT, null);
        String commonMessage = createCommonNotification(diff,
            "https://teamcity.yandex-team.ru/viewLog.html?buildId=14247210",
            TOTAL_TESTS_COUNT, null);

        assertMessageWithoutDiff(telegramMessage);
        assertMessageWithoutDiff(commonMessage);
    }

    private void assertMessage(String message) {
        assertThat(message, allOf(
            containsString("Общее количество тестов: 228 (+10)"),
            containsString("Classes: 24% (+8%)"),
            containsString("Methods: 16.959% (+9.942%)"),
            containsString("Blocks: 11.044% (+5.187%)"),
            containsString("Lines: 13.614% (+7.618%)")
        ));
    }

    private void assertMessageWithoutDiff(String message) {
        assertThat(message, allOf(
            containsString("Общее количество тестов: 228\n"),
            containsString("Classes: 24%\n"),
            containsString("Methods: 16.959%\n"),
            containsString("Blocks: 11.044%\n"),
            containsString("Lines: 13.614%\n")
        ));
    }

    private String createTelegramNotification(CodeCoverageDiff codeCoverageDiff,
                                              String reportUrl, int totalTestsCount,
                                              @Nullable Integer testsCountDiff) {
        return NotificationUtils.render(
            "templates/codeCoverageTelegramNotification.md",
            JaCoCoCodeCoverageJob.class,
            JaCoCoCodeCoverageJob.notificationData(codeCoverageDiff, reportUrl, totalTestsCount, testsCountDiff));
    }

    private String createCommonNotification(CodeCoverageDiff codeCoverageDiff,
                                            String reportUrl, int totalTestsCount,
                                            @Nullable Integer testsCountDiff) {
        return NotificationUtils.render(
            "templates/codeCoverageCommonNotification.md",
            JaCoCoCodeCoverageJob.class,
            JaCoCoCodeCoverageJob.notificationData(codeCoverageDiff, reportUrl, totalTestsCount, testsCountDiff));
    }

    private CodeCoverage getCodeCoverage(String path) {
        JaCoCoParser jaCoCoParser = new JaCoCoParser();
        try (InputStream inputStream = this.getClass().getResourceAsStream(path)) {
            return jaCoCoParser.parseXml(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
