package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.common;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

public class OverlayBusy {
    private WebDriver driver;
    private WebDriverWait wait;

    public OverlayBusy(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(this.driver, WebDriverTimeout.LONG_WAIT_TIMEOUT);
    }

    public void waitUntilHidden() {
        String overlayXpath = "//div[@class = 'overlay busy is-hidden' or @class = 'overlay busy']";

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

        if (driver.findElements(By.xpath(overlayXpath)).size() != 0) {
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
            wait.until(invisibilityOfElementLocated(By.xpath(overlayXpath)));
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }
}
