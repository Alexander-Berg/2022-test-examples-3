package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.MultitestingLaunchDetailsPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.multitestings.MultitestingPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectPage;
import ru.yandex.market.tsum.pipe.ui.steps.MultitestingSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.timeoutHasExpired;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 20.06.2018
 */
public class TriggerJobInOldMultitestingLaunchTest {
    @Rule
    public final WebDriverRule webDriver =
        new WebDriverRule(TsumUrls.projectMultitestingsPage(TestData.TEST_PROJECT_ID));

    private final ProjectPage projectPage = webDriver.createPageObject(ProjectPage::new);
    private final MultitestingPage multitestingPage = webDriver.createPageObject(MultitestingPage::new);
    private final MultitestingLaunchDetailsPage multitestingLaunchDetailsPage =
        webDriver.createPageObject(MultitestingLaunchDetailsPage::new);

    private final MultitestingSteps multitestingSteps = new MultitestingSteps(webDriver);

    @Test
    @DisplayName("Перезапуск джобы в запуске МТ, который был создан давно (старой версией ЦУМа)")
    @Issue("https://st.yandex-team.ru/MARKETINFRA-3347")
    public void test() {
        if (!multitestingWithOldLaunchExists()) {
            createMultitestingWithOldLaunch();
            fail("Мультитестинг с долгоживущим запуском не существовал." +
                " Он был создан, следующий запуск этого теста должен пройти успешно.");
        }

        navigateFromTestProjectMultitestingsPageToMultitestingWithOldLaunchPage();

        navigateFromMultitestingPageToLastLaunchDetailsPage();
        restartJob1();

        multitestingSteps.navigateFromMultitestingLaunchDetailsPageToMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("READY");
    }

    @Step("Смотрим существует ли мультитестинг с долгоживущим запуском")
    private boolean multitestingWithOldLaunchExists() {
        return should(isDisplayed()).whileWaitingUntil(timeoutHasExpired(TimeUnit.SECONDS.toMillis(120)))
            .matches(projectPage.multitestingWithOldLaunchLink);
    }

    @Step("Создаём мультитестинг с долгоживущим запуском (этот шаг должен запуститься только при первом запуске тестов)")
    private void createMultitestingWithOldLaunch() {
        multitestingSteps.navigateFromMainPageToTestProjectMultitestingsPage();
        multitestingSteps.createAndLaunchMultitestingFromProjectMultitestingsPage(
            TestData.MT_WITH_OLD_LAUNCH_NAME,
            "mt-ui-tests-simple"
        );

        multitestingSteps.navigateFromMultitestingLaunchDetailsPageToMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("READY");
    }

    @Step("Переходим на страницу старого мультитестинга")
    private void navigateFromTestProjectMultitestingsPageToMultitestingWithOldLaunchPage() {
        webDriver.click(projectPage.multitestingWithOldLaunchLink);
        webDriver.assertStep(multitestingPage.status, allOf(isDisplayed(), hasText("READY")));
    }

    @Step("Переходим на страницу последнего запуска")
    private void navigateFromMultitestingPageToLastLaunchDetailsPage() {
        webDriver.click(multitestingPage.lastLaunchLink);
    }

    @Step("Рестартим джобу")
    private void restartJob1() {
        webDriver.click(multitestingLaunchDetailsPage.pipeGraph.restartJob1Button);
        webDriver.acceptAlert();
    }
}
