package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class AnomalyQtyConfirmationPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;

    public AnomalyQtyConfirmationPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("qtyInput"));
    }

    @Step("Вводим количество товара для приемки в аномалию")
    public AnomalyQtyConfirmationPage enterQty(String qty) {
        input.sendKeys(qty);

        return this;
    }

    @Step("Нажимаем кнопку \"Далее\"")
    public CartInputPage confirm() {
        forwardButton.click();
        return new CartInputPage(driver);
    }
}
