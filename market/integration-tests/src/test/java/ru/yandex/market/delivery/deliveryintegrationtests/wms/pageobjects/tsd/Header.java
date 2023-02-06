package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementDecorator;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementLocatorFactory;

import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

public class Header {
    private WebDriver driver;
    private WebDriverWait wait;

    @Name("Назад")
    @FindBy(id = "prevScreenButton")
    public HtmlElement backButton;

    @Name("Вперед")
    @FindBy(id = "nextScreenButton")
    public HtmlElement forvardButton;

    @Name("Меню")
    @FindBy(xpath = "//button[@onclick = 'menuDIV.inforOpen();']")
    public HtmlElement menuButton;

    private static final Logger log = LoggerFactory.getLogger(Header.class);

    public Header(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(this.driver, WebDriverTimeout.LONG_WAIT_TIMEOUT);

        PageFactory.initElements(new HtmlElementDecorator(new HtmlElementLocatorFactory(driver)), this);
    }

    @Step("В верхней плашке: Жмем назад")
    public void backButtonClick() {
        wait.until(elementToBeClickable(backButton));
        backButton.click();
    }

    @Step("В верхней плашке: Жмем вперед")
    public void forvardButtonClick() {
        wait.until(elementToBeClickable(forvardButton));
        forvardButton.click();
    }

    @Step("В верхней плашке: Открываем меню")
    public void menuButtonClick() {
        wait.until(elementToBeClickable(menuButton));
        menuButton.click();
    }

    @Step("Ждем, когда отработает спиннер")
    public void waitSpinner() {
        String overlayXpath = "//*[@id = 'inforLoadingOverlay' or @id = 'inforOverlay' or @class = 'loadingText']";

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

        if (driver.findElements(By.xpath(overlayXpath)).size() != 0) {
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
            wait.until(invisibilityOfElementLocated(By.xpath(overlayXpath)));
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }
}
