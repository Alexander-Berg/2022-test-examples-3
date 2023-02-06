package ru.yandex.market.hrms.e2etests.selenium;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeleniumUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SeleniumUtil.class);

    private SeleniumUtil() { }

    public static void jsClick(WebElement element, WebDriver driver) {
        JavascriptExecutor executor = (JavascriptExecutor)driver;
        executor.executeScript("arguments[0].click()", element);
    }

    public static void clearInput(WebElement element, WebDriver driver) {
        Actions actions = new Actions(driver);
        actions.sendKeys(Keys.HOME).build().perform();
        actions.keyDown(Keys.LEFT_SHIFT);

        String textInForm = element.getAttribute("value");

        for (int i =0; i < textInForm.length(); i++) {
            actions.sendKeys(Keys.ARROW_RIGHT);
        }

        actions.keyUp(Keys.LEFT_SHIFT);
        actions.sendKeys(Keys.BACK_SPACE);
        actions.build().perform();
    }

    public static void clearSession(WebDriver driver) {
        LOG.info("Clearing session");
        driver.manage().deleteAllCookies();

        //Падает с ошибкой, если выполнить когда wms не открыта
        if (driver.getCurrentUrl().matches("htt.*") ) {
            ((JavascriptExecutor) driver).executeScript("sessionStorage.clear()");
            ((JavascriptExecutor) driver).executeScript("localStorage.clear()");
        }

        LOG.info("Session cleared");
    }

    public static void closeBrowser(WebDriver driver) {
        if (driver != null) {
            LOG.info("Quitting browser {}", driver);
            try {
                driver.quit();
            } catch (Throwable throwable) {
                if (throwable.getMessage().contains("Session timed out or not found")
                        || throwable instanceof WebDriverException) {
                    LOG.info("Browser already closed");
                } else {
                    throw throwable;
                }
            }
        } else {
            LOG.info("No browser currently open");
        }
    }
}
