package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.steps.MultitestingSteps;

import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.03.2018
 */
public class CreateAndLaunchAndArchiveMultitestingTest {
    @Rule
    public final WebDriverRule webDriver = new WebDriverRule(TsumUrls.mainPage());
    private final MultitestingSteps multitestingSteps = new MultitestingSteps(webDriver);

    @Test
    @DisplayName("Создание, запуск и архивация МТ")
    public void multitestingTest() {
        multitestingSteps.navigateFromMainPageToTestProjectMultitestingsPage();

        multitestingSteps.createAndLaunchMultitestingFromProjectMultitestingsPage(
            generateUniqueMultitestingName(),
            "mt-ui-tests-simple"
        );

        multitestingSteps.navigateFromMultitestingLaunchDetailsPageToMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("READY");

        multitestingSteps.cleanupAndArchiveMultitestingFromMultitestingPage();
        multitestingSteps.refreshUntilMultitestingStatusIs("ARCHIVED");
    }

    private static String generateUniqueMultitestingName() {
        return String.format("ui-tests-%s", UUID.randomUUID());
    }
}
