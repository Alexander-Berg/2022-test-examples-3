package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.MainPage;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.02.2018
 */
@DisplayName("Главная страница")
public class MainPageTest {
    @Rule
    public final WebDriverRule webDriver = new WebDriverRule(TsumUrls.mainPage());

    private final MainPage mainPage = webDriver.createPageObject(MainPage::new);

    @Test
    @DisplayName("Страница загружается")
    public void pageLoads() {
        webDriver.assertWaitStep(mainPage.diagnosticToolsLink, isDisplayed());
    }
}
