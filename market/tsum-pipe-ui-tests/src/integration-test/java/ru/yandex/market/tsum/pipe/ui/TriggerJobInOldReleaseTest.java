package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.releases.JobLaunchPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.releases.JobLogPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.releases.ReleaseLaunchPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.releases.ReleasePage;
import ru.yandex.qatools.htmlelements.matchers.common.IsElementDisplayedMatcher;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.fail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.*;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.timeoutHasExpired;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.03.2018
 */
public class TriggerJobInOldReleaseTest {
    @Rule
    public final WebDriverRule webDriver = new WebDriverRule(
        TsumUrls.projectPage(TestData.TEST_PROJECT_ID)
    );

    private final ProjectPage projectPage = webDriver.createPageObject(ProjectPage::new);
    private final ReleaseLaunchPage releaseLaunchPage = webDriver.createPageObject(ReleaseLaunchPage::new);
    private final ReleasePage releasePage = webDriver.createPageObject(ReleasePage::new);
    private final JobLaunchPage jobLaunchPage = webDriver.createPageObject(JobLaunchPage::new);
    private final JobLogPage jobLogPage = webDriver.createPageObject(JobLogPage::new);

    @Test
    @DisplayName("Перезапуск джобы в релизе, который был создан давно (старой версией ЦУМа)")
    public void releaseTest() {
        if (!oldReleaseExists()) {
            createOldRelease();
            fail("Старый релиз не существовал." +
                " Он был создан, следующий запуск этого теста должен пройти успешно.");
        }

        navigateFromTestProjectReleasesPageToOldReleasePage();

        String oldJob1LaunchNumber = getJob1LaunchNumber();
        restartJob1();
        waitForJob1ToHaveDifferentLastLaunchNumber(oldJob1LaunchNumber);
        waitForJob1ToBecomeSuccessful();
        checkJob1Logs();
    }

    @Step("Смотрим существует ли долгоживущий релиз")
    private boolean oldReleaseExists() {
        return should(isDisplayed()).whileWaitingUntil(timeoutHasExpired(TimeUnit.SECONDS.toMillis(120)))
            .matches(projectPage.oldReleaseLink);
    }

    @Step("Создаём долгоживущий релиз (этот шаг должен запуститься только при первом запуске тестов)")
    private void createOldRelease() {
        goToTestSimplePipelineLaunchPage();
        launchTestSimpleRelease();
        waitForJob1ToBecomeSuccessful();
    }

    @Step("Переходим на страницу запуска пайплайна test-simple")
    private void goToTestSimplePipelineLaunchPage() {
        webDriver.click(projectPage.testSimpleLink);
        webDriver.assertWaitStep(releaseLaunchPage.launchButton, isDisplayed());
    }

    @Step("Запускаем пайплайн test-simple")
    private void launchTestSimpleRelease() {
        webDriver.sendKeys(releaseLaunchPage.simplePipelineFixVersionField, TestData.OLD_RELEASE_NAME);
        webDriver.click(releaseLaunchPage.launchButton);
    }

    @Step("Переходим на страницу долгоживущего релиза")
    private void navigateFromTestProjectReleasesPageToOldReleasePage() {
        webDriver.click(projectPage.oldReleaseLink);
        webDriver.assertWaitStep(releasePage.pipeGraph.jobLaunchButton, isDisplayed());
    }

    @Step("Получаем номер последнего запуска job1")
    private String getJob1LaunchNumber() {
        webDriver.assertWaitStep(releasePage.pipeGraph.job1LastLaunchLink, isDisplayed());
        return releasePage.pipeGraph.job1LastLaunchLink.getText();
    }

    @Step("Рестартим job1")
    private void restartJob1() {
        webDriver.click(releasePage.pipeGraph.restartJob1Button);
        webDriver.acceptAlert();
    }

    @Step("Проверяем что у job1 поменялся номер последнего запуска")
    private void waitForJob1ToHaveDifferentLastLaunchNumber(String oldJob1LaunchNumber) {
        webDriver.assertWaitStep(releasePage.pipeGraph.job1LastLaunchLink, not(hasText(oldJob1LaunchNumber)));
    }

    @Step("Проверяем что job1 отработала успешно")
    private void waitForJob1ToBecomeSuccessful() {
        webDriver.assertWaitStep(releasePage.pipeGraph.job1, hasAttribute("data-ui-tests-job-status", "SUCCESSFUL"));
    }

    @Step("Проверяем что у job1 открываются логи")
    private void checkJob1Logs() {
        navigateFromTestSimplePipelineLaunchPageToJobLaunchPage();
        navigateFromJobLaunchPageToJobLogPage();
    }

    @Step("Переходим на страницу запуска джобы")
    private void navigateFromTestSimplePipelineLaunchPageToJobLaunchPage() {
        webDriver.click(releasePage.pipeGraph.job1LastLaunchLink);
        webDriver.assertWaitStep(jobLaunchPage.jobLogsLink, isDisplayed());
    }

    @Step("Переходим на страницу лога джобы")
    private void navigateFromJobLaunchPageToJobLogPage() {
        webDriver.click(jobLaunchPage.jobLogsLink);

        webDriver.assertWaitStep(
            webDriver::getWindowHandles,
            new BaseMatcher<Supplier<Set<String>>>() {
                @Override
                public void describeTo(Description description) {
                    description.appendText("вкладка с логом открылась и отображает лог");
                }

                @Override
                public boolean matches(Object item) {
                    for (String tabName: (((Supplier<Set<String>>) item).get())) {
                        webDriver.switchTo().window(tabName);
                        if (IsElementDisplayedMatcher.isDisplayed().matches(jobLogPage.pre) &&
                            jobLogPage.pre.getText().contains("Starting job")) {

                            return true;
                        }
                    }
                    return false;
                }
            }
        );
    }
}
