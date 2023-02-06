package ru.yandex.market.tsum.pipe.ui.pipelines;

import io.qameta.allure.Step;
import org.junit.Rule;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.common.matchers.ClassNameMatcher;
import ru.yandex.market.tsum.pipe.ui.common.matchers.ListSizeMoreThanMatcher;
import ru.yandex.market.tsum.pipe.ui.page_objects.MainPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectsPage;
import ru.yandex.market.tsum.pipe.ui.pipelines.page_objects.PipelineEditorPage;
import ru.yandex.market.tsum.pipe.ui.pipelines.page_objects.PipelinePage;
import ru.yandex.market.tsum.pipe.ui.pipelines.page_objects.PipelinesPage;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public abstract class PipelineEditorBaseTest {
    @Rule
    public final WebDriverRule webDriver;
    protected final MainPage mainPage;
    protected final ProjectsPage projectsPage;
    protected final ProjectPage projectPage;
    protected final PipelinesPage pipelinesPage;
    protected final PipelinePage pipelinePage;
    protected final PipelineEditorPage pipelineEditorPage;

    protected PipelineEditorBaseTest(WebDriverRule webDriver) {
        this.webDriver = webDriver;
        this.mainPage = webDriver.createPageObject(MainPage::new);
        this.projectsPage = webDriver.createPageObject(ProjectsPage::new);
        this.projectPage = webDriver.createPageObject(ProjectPage::new);
        this.pipelinesPage = webDriver.createPageObject(PipelinesPage::new);
        this.pipelinePage = webDriver.createPageObject(PipelinePage::new);
        this.pipelineEditorPage = webDriver.createPageObject(PipelineEditorPage::new);
    }

    protected void walkToPipelinePage() {
        goToProjectsPage();
        goToTestProjectPage();
        goToPipelinesPage();
        goToTestPipelinePage();
    }

    @Step("Переходим на страницу со списком проектов")
    protected void goToProjectsPage() {
        webDriver.click(mainPage.projectsLink);
        webDriver.assertWaitStep(projectsPage.testProjectsLink, isDisplayed());
    }

    @Step("Переходим на страницу тестового проекта")
    protected void goToTestProjectPage() {
        webDriver.click(projectsPage.testProjectsLink);
        webDriver.assertWaitStep(projectPage.pipelineManagementLink, isDisplayed());
    }

    @Step("Переходим на страницу пайплайнов")
    protected void goToPipelinesPage() {
        webDriver.click(projectPage.pipelineManagementLink);
        webDriver.assertWaitStep(pipelinesPage.testPipeline, isDisplayed());
    }

    @Step("Переходим на страницу тестового пайплайна")
    protected void goToTestPipelinePage() {
        webDriver.click(pipelinesPage.testPipeline);
        webDriver.assertWaitStep(pipelinePage.publicRows, new ListSizeMoreThanMatcher(1));
    }

    @Step("Нажимаем создать версию")
    protected void clickCreateDraft() {
        webDriver.click(pipelinePage.createDraftButton);
        webDriver.assertWaitStep(pipelineEditorPage.createButton, isDisplayed());
    }

    @Step("Нажимаем добавить джобу")
    protected void clickCreateJob() {
        webDriver.click(pipelineEditorPage.createButton);
        webDriver.assertWaitStep(pipelineEditorPage.createOrEditJobDialog, isDisplayed());
        webDriver.assertWaitStep(pipelineEditorPage.createOrEditJobDialog.title, isDisplayed());
    }

    @Step("Сохраняем пайплайн")
    protected void clickSavePipeline() {
        webDriver.click(pipelineEditorPage.saveButton);
        webDriver.assertWaitStep(pipelineEditorPage.saveButton, new ClassNameMatcher("disabled-menu"));
    }

    @Step("Выходим из редактора")
    protected void clickExitEditor() {
        webDriver.click(pipelineEditorPage.exitButton);
        webDriver.assertWaitStep(pipelinePage.createDraftButton, isDisplayed());
    }

    @Step("Заполняем форму джобы")
    protected void fillJobForm(String title, String executorName) {
        webDriver.sendKeys(pipelineEditorPage.createOrEditJobDialog.title, title);
        webDriver.selectByValueInReactSelect(
            pipelineEditorPage.createOrEditJobDialog.executorSelect, executorName
        );
    }

    @Step("Сохраняем джобу")
    protected void saveJob() {
        webDriver.click(pipelineEditorPage.createOrEditJobDialog.saveButton);
        new WebDriverWait(webDriver, 3)
            .until(ExpectedConditions.invisibilityOfElementLocated(By.id(PipelineEditorPage.JOB_FORM_ID)));
    }
}
