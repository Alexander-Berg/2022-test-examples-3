package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.timeline.TimelinePage;

import java.util.List;
import java.util.function.Supplier;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 12.10.18
 */
@DisplayName("Таймлайн")
public class TimelinePageTest {
    @Rule
    public final WebDriverRule webDriver = new WebDriverRule(TsumUrls.timelinePage());

    private final TimelinePage timelinePage = webDriver.createPageObject(TimelinePage::new);

    @Test
    @DisplayName("Страница таймлайна загружается")
    public void timelinePageLoads() {
        webDriver.assertWaitStep(timelinePage.filterPanel, isDisplayed());
        webDriver.assertWaitStep(timelinePage.timelineChart, isDisplayed());
    }

    @Test
    @DisplayName("Тест фильтров")
    public void testFilters() {
        fillFilters();

        webDriver.assertWaitStep(
            webDriver.getCurrentUrl(),
            new IsEqual<>(String.format(
                "%s/?authors=author1&authors=author2&" +
                    "from=now%%2Fd&projects=project1&projects=project2&" +
                    "tags=tag1&tags=tag2&to=now&types=type1&types=type2",
                TsumUrls.timelinePage()
            ))
        );

        resetFilters();

        webDriver.assertWaitStep(
            webDriver.getCurrentUrl(),
            new IsEqual<>(String.format(
                "%s/?from=now%%2Fd&to=now",
                TsumUrls.timelinePage()
            ))
        );
    }

    @Step("Заполняем фильтры")
    private void fillFilters() {
        webDriver.sendKeys(timelinePage.tagFilterInput, "tag1" + Keys.RETURN + "tag2" + Keys.RETURN);
        webDriver.sendKeys(timelinePage.typeFilterInput, "type1" + Keys.RETURN + "type2" + Keys.RETURN);
        webDriver.sendKeys(timelinePage.authorFilterInput, "author1" + Keys.RETURN + "author2" + Keys.RETURN);
        webDriver.sendKeys(timelinePage.projectFilterInput, "project1" + Keys.RETURN + "project2" + Keys.RETURN);

        checkTagInputDisplayed(
            "tag1", "tag2",
            "type1", "type2",
            "author1", "author2",
            "project1", "project2"
        );
    }

    @Step("Очищаем фильтры")
    private void resetFilters() {
        webDriver.click(timelinePage.resetFiltersButton);

        checkTagInputNotDisplayed(
            "tag1", "tag2",
            "type1", "type2",
            "author1", "author2",
            "project1", "project2"
        );
    }

    private void checkTagInputDisplayed(String... values) {
        for (String value : values) {
            webDriver.assertWaitStep(
                webDriver.findElement(By.xpath("//span[@class='react-tagsinput-tag' and text()='" + value + "']")),
                isDisplayed()
            );
        }
    }

    private void checkTagInputNotDisplayed(String... values) {
        for (String value : values) {
            webDriver.assertWaitStep(
                () -> webDriver.findElements(
                    By.xpath("//span[@class='react-tagsinput-tag' and text()='" + value + "']")
                ),
                new BaseMatcher<Supplier<List<WebElement>>>() {
                    @Override
                    public boolean matches(Object item) {
                        return ((Supplier<List<WebElement>>) item).get().isEmpty();
                    }

                    @Override
                    public void describeTo(Description description) {

                    }
                }
            );
        }
    }
}
