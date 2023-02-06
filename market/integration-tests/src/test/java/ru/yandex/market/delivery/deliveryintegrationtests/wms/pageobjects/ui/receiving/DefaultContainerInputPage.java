package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Slf4j
public class DefaultContainerInputPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    public DefaultContainerInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("defaultContainerInputPage"));
    }

    @Step("Вводим дефолтную тару: {cart}")
    public BarcodeInputPage enterCart(String cart) {
        input.sendKeys(cart);
        input.pressEnter();
        return new BarcodeInputPage(driver);
    }
}
