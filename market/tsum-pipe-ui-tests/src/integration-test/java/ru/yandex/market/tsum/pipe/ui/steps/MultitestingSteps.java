package ru.yandex.market.tsum.pipe.ui.steps;

import io.qameta.allure.Step;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.MainPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.common.PipeLaunchPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.CreateMultitestingPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.MultitestingLaunchDetailsPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.MultitestingPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectsPage;

import static org.hamcrest.CoreMatchers.allOf;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 20.06.2018
 */
public class MultitestingSteps {
    private final WebDriverRule webDriver;
    private final MainPage mainPage;
    private final ProjectsPage projectsPage;
    private final ProjectPage projectPage;
    private final CreateMultitestingPage createMultitestingPage;
    private final PipeLaunchPage pipeLaunchPage;
    private final MultitestingLaunchDetailsPage multitestingLaunchDetailsPage;
    private final MultitestingPage multitestingPage;

    public MultitestingSteps(WebDriverRule webDriver) {
        this.webDriver = webDriver;
        mainPage = webDriver.createPageObject(MainPage::new);
        projectsPage = webDriver.createPageObject(ProjectsPage::new);
        projectPage = webDriver.createPageObject(ProjectPage::new);
        createMultitestingPage = webDriver.createPageObject(CreateMultitestingPage::new);
        multitestingLaunchDetailsPage = webDriver.createPageObject(MultitestingLaunchDetailsPage::new);
        multitestingPage = webDriver.createPageObject(MultitestingPage::new);
        pipeLaunchPage = webDriver.createPageObject(PipeLaunchPage::new);
    }

    @Step("Переходим на страницу мультитестингов тестового проекта")
    public void navigateFromMainPageToTestProjectMultitestingsPage() {
        webDriver.click(mainPage.projectsLink);
        webDriver.assertWaitStep(projectsPage.testProjectsLink, isDisplayed());
        webDriver.click(projectsPage.testProjectsLink);
        webDriver.assertWaitStep(projectPage.multitestingsLink, isDisplayed());
        webDriver.click(projectPage.multitestingsLink);
        webDriver.assertWaitStep(projectPage.createMultitestingButton, isDisplayed());
    }

    @Step("Создаём и запускаем мультитестинг")
    public void createAndLaunchMultitestingFromProjectMultitestingsPage(String multitestingName, String pipelineId) {
        webDriver.click(projectPage.createMultitestingButton);
        webDriver.assertWaitStep(createMultitestingPage.createAndLaunchMultitestingButton, isDisplayed());

        webDriver.sendKeys(createMultitestingPage.nameTextInput, multitestingName);
        webDriver.sendKeys(createMultitestingPage.titleTextInput, multitestingName);
        webDriver.selectByValueInReactSelect(createMultitestingPage.pipelineSelect, pipelineId);
        webDriver.sendKeys(createMultitestingPage.manualResourcesForm.simplePipelineFixVersionField, "Какое-то значение");

        try {
            Thread.sleep(1); // wait for final form validation
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted", e);
        }

        webDriver.click(createMultitestingPage.createAndLaunchMultitestingButton);
        webDriver.assertWaitStep(pipeLaunchPage.pipeGraph, isDisplayed());
    }

    @Step("Переходим на страницу мультитестинга")
    public void navigateFromMultitestingLaunchDetailsPageToMultitestingPage() {
        webDriver.click(multitestingLaunchDetailsPage.multitestingPageBreadcrumbsLink);
        webDriver.assertWaitStep(multitestingPage.status, isDisplayed());
    }

    @Step("Ждём статус \"{expectedStatus}\"")
    public void refreshUntilMultitestingStatusIs(String expectedStatus) {
        webDriver.refreshUntil(multitestingPage.status, allOf(isDisplayed(), hasText(expectedStatus)));
    }

    @Step("Очищаем мультитестинг")
    public void cleanupMultitestingFromMultitestingPage() {
        webDriver.click(multitestingPage.cleanupButton);
    }

    @Step("Очищаем и архивируем мультитестинг")
    public void cleanupAndArchiveMultitestingFromMultitestingPage() {
        webDriver.click(multitestingPage.cleanupAndArchiveButton);
    }
}
