package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.MainPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectsPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.releases.ReleaseLaunchPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.releases.ReleasePage;

import static org.hamcrest.CoreMatchers.*;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.03.2018
 */
@DisplayName("Релизы")
public class ReleaseTest {
    @Rule
    public final WebDriverRule webDriver = new WebDriverRule(TsumUrls.mainPage());

    private final MainPage mainPage = webDriver.createPageObject(MainPage::new);
    private final ProjectsPage projectsPage = webDriver.createPageObject(ProjectsPage::new);
    private final ProjectPage projectPage = webDriver.createPageObject(ProjectPage::new);
    private final ReleaseLaunchPage releaseLaunchPage = webDriver.createPageObject(ReleaseLaunchPage::new);
    private final ReleasePage releasePage = webDriver.createPageObject(ReleasePage::new);

    @Test
    @DisplayName("Запуск и завершение релиза")
    public void releaseTest() {
        goToProjectsPage();
        goToTestProjectPage();
        goToTestSimplePipelineLaunchPage();
        launchTestSimpleRelease();
        triggerJobWithManualTrigger();
        waitForReleaseToFinish();
    }

    @Step("Переходим на страницу со списком проектов")
    private void goToProjectsPage() {
        webDriver.click(mainPage.projectsLink);
        webDriver.assertWaitStep(projectsPage.testProjectsLink, isDisplayed());
    }

    @Step("Переходим на страницу тестового проекта")
    private void goToTestProjectPage() {
        webDriver.click(projectsPage.testProjectsLink);
        webDriver.assertWaitStep(projectPage.testSimpleLink, isDisplayed());
    }

    @Step("Переходим на страницу запуска пайплайна test-simple")
    private void goToTestSimplePipelineLaunchPage() {
        webDriver.click(projectPage.testSimpleLink);
        webDriver.assertWaitStep(releaseLaunchPage.launchButton, isDisplayed());
    }

    @Step("Запускаем пайплайн test-simple")
    private void launchTestSimpleRelease() {
        webDriver.sendKeys(
            releaseLaunchPage.simplePipelineFixVersionField,
            "Релиз " + getClass().getName() + ". Старые можно завершать."
        );
        webDriver.click(releaseLaunchPage.launchButton);
    }

    @Step("Запускаем джобу с ручным подтверждением")
    private void triggerJobWithManualTrigger() {
        webDriver.assertWaitStep(
            releasePage.pipeGraph.jobLaunchButton,
            both(isDisplayed()).and(not(hasClass(containsString("disabled"))))
        );
        webDriver.click(releasePage.pipeGraph.jobLaunchButton);
    }

    @Step("Дожидаемся завершения релиза")
    private void waitForReleaseToFinish() {
        webDriver.assertWaitStep(releasePage.finishedLabel, isDisplayed());
    }
}
