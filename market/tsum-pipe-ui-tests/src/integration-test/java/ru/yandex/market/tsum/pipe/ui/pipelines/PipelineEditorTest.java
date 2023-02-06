package ru.yandex.market.tsum.pipe.ui.pipelines;

import com.google.common.base.Preconditions;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.yandex.market.tsum.pipe.ui.common.matchers.ListExactSizeMatcher;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

@DisplayName("Редактор пайплайнов")
public class PipelineEditorTest extends PipelineEditorBaseTest {
    public PipelineEditorTest() {
        super(new WebDriverRule(TsumUrls.mainPage()));
    }

    @Test
    @DisplayName("Редактирование пайплайна")
    public void editPipelineTest() {
        walkToPipelinePage();

        clickCreateDraft();
        createJob();
        clickSavePipeline();
        clickExitEditor();

        Preconditions.checkState(pipelinePage.draftRows.size() > 0, "Can't find drafts");

        pipelinePage.draftRows.forEach(draft -> {
            draft.menuButton.click();
            webDriver.assertWaitStep(draft.deleteButton, WebElementMatchers.isDisplayed());
            draft.deleteButton.click();
            new WebDriverWait(webDriver, 3000)
                .until(ExpectedConditions.alertIsPresent());
            webDriver.switchTo().alert().accept();
        });

        webDriver.assertWaitStep(pipelinePage.draftRows, new ListExactSizeMatcher(0));
    }

    private void createJob() {
        clickCreateJob();
        fillJobForm("ui-tests-new", "MarketTeamcityBuildJob");
        saveJob();
    }

}
