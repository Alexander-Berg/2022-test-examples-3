package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import java.util.concurrent.TimeUnit;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class DestinationMultiSourcePage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    private static final By DEMAND_FOR_ANOMALY_CART_SCAN = byXpath("//label//span[text()='Тара для размещения']");

    public DestinationMultiSourcePage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("destinationMultiSource"));
    }

    @Step("Вводим тару и выбираем локацию")
    public TasksWithLocationPage placeContainer(String anomalyCartId, String location) {
        Retrier.retry(() -> {
            SeleniumUtil.clearInput(input, getWebDriver());
            input.sendKeys(anomalyCartId);
            forward.click();
            waitElementHidden(DEMAND_FOR_ANOMALY_CART_SCAN, true);
        }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        input.sendKeys(location);
        forward.click();
        return new TasksWithLocationPage(driver, anomalyCartId);
    }
}
