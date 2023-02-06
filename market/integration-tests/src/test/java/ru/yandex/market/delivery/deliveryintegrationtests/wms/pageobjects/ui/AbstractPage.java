package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.qatools.properties.PropertyLoader;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.HtmlElementsCommon;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.qatools.htmlelements.annotations.Name;

import java.util.ArrayList;
import java.util.Set;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.switchTo;
import static org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe;

public abstract class AbstractPage extends HtmlElementsCommon {
    private String parentWindowHandler;
    private String subWindowHandler;
    protected NotificationDialog notificationDialog;

    @Name("Счетчик результатов")
    @FindBy(xpath = "//span[contains(text(), ' из ')]")
    private SelenideElement resulstCounter;

    @Name("Прелодер таблицы показывается")
    @FindBy(xpath = "//*[@data-e2e='enabled_table_preloader']")
    private SelenideElement enabledTablePreloader;

    @Name("Прелодер таблицы скрыт")
    @FindBy(xpath = "//*[@data-e2e='disabled_table_preloader']")
    private SelenideElement disabledTablePreloader;

    private By resultCounterXPath = byXpath("//span[contains(text(), ' из ')]");

    public AbstractPage(WebDriver driver) {
        super(driver);
        notificationDialog = new NotificationDialog(driver);
        PropertyLoader.newInstance().populate(this);
        page(this);
    }

    public void highlightElement(SelenideElement element) {
        Selenide.executeJavaScript("arguments[0].style.border='2px solid red'", element);
    }

    @Step("Ждем, что когда пропадёт спиннер")
    protected void waitTablePreloader() {
        disabledTablePreloader.should(Condition.appear);
    }

    protected void waitElementHasFocus(SelenideElement element) {

        int timeout_milis = WebDriverTimeout.MEDIUM_WAIT_TIMEOUT * 1000;
        int step_milis = 250;

        int i = 0;
        while (i < timeout_milis) {
            if (element.equals(getActivElement())) break;

            i = i + step_milis;
        }

        Assertions.assertTrue(element.equals(getActivElement()),
                "Element doesn't have focus after waiting for "
                        + timeout_milis + " milliseconds.");
    }

    public void performInputInActiveElement(SelenideElement input, String text) {
        waitElementHasFocus(input);
        getActivElement().sendKeys(text);
        getActivElement().pressEnter();
    }

    public SelenideElement getActivElement() {
        return $(switchTo().activeElement());
    }

    protected void switchToSubWindow(int tabs) {
        wait.until(numberOfWindowsToBe(tabs + 1));

        Set<String> handles = driver.getWindowHandles();

        for (String handle : handles) {
            if (!handle.equals(parentWindowHandler)) subWindowHandler = handle;
        }

        driver.switchTo().window(subWindowHandler);
    }

    protected void switchToMainWindow() {
        wait.until(numberOfWindowsToBe(1));
        driver.switchTo().window(driver.getWindowHandles().iterator().next());
    }

    public int resultsOnPage() {

        if (!isElementPresent(resultCounterXPath)) {
            return 0;
        }

        String sep = "-";
        String counterText = resulstCounter.getText()
                // NOTE: There are two different separators: "-" and "–".
                .replace("–", sep);
        int firstIndex = Integer.valueOf(StringUtils.substringBefore(counterText, sep));
        int lastIndex = Integer.valueOf(StringUtils.substringBetween(counterText, sep, " из"));

        return lastIndex - firstIndex + 1;
    }


    protected void openNextTab() {
        String currentHandle = driver.getWindowHandle();
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        for (String actual: tabs) {
            if (!actual.equalsIgnoreCase(currentHandle)) {
                driver.switchTo().window(actual);
                break;
            }
        }
    }

}
