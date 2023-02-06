package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common;

import lombok.extern.slf4j.Slf4j;
import static com.codeborne.selenide.Selectors.byXpath;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$$;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

@Slf4j
public class TablePreloader {
    private WebDriver driver;
    private WebDriverWait wait;

    public TablePreloader(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(this.driver, WebDriverTimeout.LONG_WAIT_TIMEOUT);
    }

    public void waitUntilHidden() {
        String visibleOverlayXpath = "//div[@class = 'enabled_table_preloader']";
        String invisibleOverlayXpath = "//div[@class = 'disabled_table_preloader']";

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

        if ($$(byXpath(visibleOverlayXpath)).size() != 0) {
            log.info("Found overlay");
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
            wait.until(presenceOfElementLocated(byXpath(invisibleOverlayXpath)));
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }
}
