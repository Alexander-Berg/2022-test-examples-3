package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class CartPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;


    public CartPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("cartPage$"));
    }

    @Step("Подтверждаем")
    public ItemInputPage confirm() {
        forward.click();
        return new ItemInputPage(this.driver);
    }

}
