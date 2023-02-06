package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdElement;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

public class WarningDialog extends AbstractTsdElement {

    @FindBy(xpath = "//button[text() = 'OK' or text() = 'Да']")
    private HtmlElement okBtn;

    @FindBy(xpath = "//button[text() = 'Нет']")
    private HtmlElement noBtn;

    public WarningDialog(WebDriver driver) {
        super(driver);
    }

    public Boolean IsPresent (String message) {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);

        String searchString = "//*[text()[contains(.,'message_text_placeholder')]]"
                        .replaceFirst("message_text_placeholder", message);

        Boolean result = driver
                .findElements(By.xpath(searchString))
                .size() != 0;

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        return result;
    }

    public Boolean IsPresentWithMessage (String message) {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);

        String searchString = "//div[@class = 'detailedMessage' " +
                "and text()[contains(.,'message_text_placeholder')]]"
                        .replaceFirst("message_text_placeholder", message);

        Boolean result = driver
                .findElements(By.xpath(searchString))
                .size() != 0;

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        return result;
    }

    @Step("Закрываем окно предупреждения")
    public void clickOk() {
        waitOverlayHiddenIfPresent();
        wait.until(elementToBeClickable(okBtn));
        okBtn.click();
    }

    @Step("Нажимаем кнопку Нет")
    public void clickNo() {
        waitOverlayHiddenIfPresent();
        wait.until(elementToBeClickable(noBtn));
        noBtn.click();
    }
}
