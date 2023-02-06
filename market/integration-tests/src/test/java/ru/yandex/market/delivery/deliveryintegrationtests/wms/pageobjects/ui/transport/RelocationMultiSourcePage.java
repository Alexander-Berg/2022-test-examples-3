package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class RelocationMultiSourcePage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    private final NotificationDialog notificationDialog;

    public RelocationMultiSourcePage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("relocationMultiSource"));
        notificationDialog = new NotificationDialog(driver);
    }

    @Step("Вводим контейнер для размещения аномальных тар")
    public DestinationMultiSourcePage inputContainer(String anomalyCartId) {
        input.sendKeys(anomalyCartId);
        input.pressEnter();
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);
        Boolean canStartPlacement = $$(byXpath("//span[text()='Оставшиеся тары для размещения']")).size() > 0;
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        if (canStartPlacement.booleanValue() == Boolean.FALSE) {
            forward.click();
            new ModalWindow(driver).clickForward();
        }
        return new DestinationMultiSourcePage(driver);
    }
}
