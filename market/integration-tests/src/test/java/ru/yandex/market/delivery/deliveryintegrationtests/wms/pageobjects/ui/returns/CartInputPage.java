package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Slf4j
public class CartInputPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    public CartInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("cartInput"));
    }

    @Step("Вводим тару: {cart}")
    public CartPage enterCart(String cart) {
        input.sendKeys(cart);
        input.pressEnter();
        return new CartPage(driver);
    }
}
