package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.should;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;

@Slf4j
public abstract class HtmlElementsCommon {

    protected WebDriver driver;
    protected WebDriverWait wait;

    private String parentWindowHandler;
    private String subWindowHandler;

    public HtmlElementsCommon(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(this.driver, WebDriverTimeout.LONG_WAIT_TIMEOUT);

        parentWindowHandler = driver.getWindowHandle();
    }


    protected void switchToSubWindow() {
        wait.until(numberOfWindowsToBe(2));

        Set<String> handles = driver.getWindowHandles();

        for (String handle : handles) {
            if (!handle.equals(parentWindowHandler)) subWindowHandler = handle;
        }

        driver.switchTo().window(subWindowHandler);
    }

    protected void switchToMainWindow() {
        wait.until(numberOfWindowsToBe(1));
        driver.switchTo().window(parentWindowHandler);
    }

    @Step("Click element {element}, when clickable")
    public static void safeClick(HtmlElement element) {
        assertThat(element, should(WrapsElementMatchers.isDisplayed()).whileWaitingUntil(timeoutHasExpired()));
        element.click();
    }

    protected Boolean isElementPresent(By by) {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);

        Boolean result = driver
                .findElements(by)
                .size() != 0;

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        return result;
    }

    public void waitElementHidden(By by) {
        waitElementHidden(by, false);
    }

    /**
     * Ждём, когда пропадет с экрана элемент:
     *
     * optional = true - когда элемент может уже не отображаться на момент вызова
     *
     * optional = false - когда нужно обязательно убедиться в наличии элемента
     * перед тем как ждать его изчезновения (default)
    */
    @Step("Проверяем, что элемент скрылся {by}")
    public void waitElementHidden(By by, boolean optional) {

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

        if (driver.findElements(by).size() != 0) {
            do {
                driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
                wait.until(invisibilityOfElementLocated(by));
                driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
            } while (driver.findElements(by).size() > 0);
        } else if (!optional) {
            throw new ElementNotVisibleException("Element not found: " + by.toString());
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }
}


