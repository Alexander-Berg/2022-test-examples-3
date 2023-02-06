package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.LaunchMultitestingPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.MultitestingLaunchDetailsPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.MultitestingPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectPage;
import ru.yandex.market.tsum.pipe.ui.steps.MultitestingSteps;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.timeoutHasExpired;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 19.06.2018
 */
public class LaunchAndCleanupOldMultitestingTest {
    @Rule
    public final WebDriverRule webDriver =
        new WebDriverRule(TsumUrls.projectMultitestingsPage(TestData.TEST_PROJECT_ID));

    private final ProjectPage projectPage = webDriver.createPageObject(ProjectPage::new);
    private final MultitestingPage multitestingPage = webDriver.createPageObject(MultitestingPage::new);
    private final MultitestingLaunchDetailsPage multitestingLaunchDetailsPage =
        webDriver.createPageObject(MultitestingLaunchDetailsPage::new);
    private final LaunchMultitestingPage launchMultitestingPage =
        webDriver.createPageObject(LaunchMultitestingPage::new);

    private final MultitestingSteps multitestingSteps = new MultitestingSteps(webDriver);

    @Test
    @DisplayName("Запуск и очистка МТ, который был создан давно (старой версией ЦУМа)")
    @Issue("https://st.yandex-team.ru/MARKETINFRA-3346")
    public void test() {
        if (!oldMultitestingExists()) {
            createOldMultitesting();
            fail("Долгоживущий мультитестинг не существовал." +
                " Он был создан, следующий запуск этого теста должен пройти успешно.");
        }

        navigateFromTestProjectMultitestingsPageToOldMultitestingPage();

        cleanupMultitestingFromMultitestingPageIfNotIdle();

        launchMultitestingFromMultitestingPage();

        multitestingSteps.navigateFromMultitestingLaunchDetailsPageToMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("READY");

        multitestingSteps.cleanupMultitestingFromMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("IDLE");
    }

    @Step("Смотрим существует ли долгоживущий мультитестинг")
    private boolean oldMultitestingExists() {
        return should(isDisplayed()).whileWaitingUntil(timeoutHasExpired(TimeUnit.SECONDS.toMillis(120)))
            .matches(projectPage.oldMultitestingLink);
    }

    @Step("Создаём старый мультитестинг (этот шаг должен запуститься только при первом запуске тестов)")
    private void createOldMultitesting() {
        multitestingSteps.navigateFromMainPageToTestProjectMultitestingsPage();
        multitestingSteps.createAndLaunchMultitestingFromProjectMultitestingsPage(
            TestData.OLD_MT_NAME,
            "mt-ui-tests-simple"
        );

        multitestingSteps.navigateFromMultitestingLaunchDetailsPageToMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("READY");

        multitestingSteps.cleanupMultitestingFromMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("IDLE");
    }

    @Step("Переходим на страницу старого мультитестинга")
    private void navigateFromTestProjectMultitestingsPageToOldMultitestingPage() {
        webDriver.click(projectPage.oldMultitestingLink);
        webDriver.assertStep(multitestingPage.status, isDisplayed());
    }

    @Step("Если статус не IDLE, то пытаемся очистить")
    private void cleanupMultitestingFromMultitestingPageIfNotIdle() {
        webDriver.assertWaitStep(multitestingPage.status, isDisplayed());
        if (!"IDLE".equals(multitestingPage.status.getText())) {
            multitestingSteps.cleanupMultitestingFromMultitestingPage();
            multitestingSteps.refreshUntilMultitestingStatusIs("IDLE");
        }
    }

    @Step("Запускаем мультитестинг")
    private void launchMultitestingFromMultitestingPage() {
        webDriver.click(multitestingPage.launchButton);
        webDriver.assertWaitStep(launchMultitestingPage.launchButton, isDisplayed());

        webDriver.sendKeys(launchMultitestingPage.manualResourcesForm.simplePipelineFixVersionField, "Какое-то значение");

        webDriver.click(launchMultitestingPage.launchButton);
        webDriver.assertWaitStep(multitestingLaunchDetailsPage.pipeGraph, isDisplayed());
    }
}
