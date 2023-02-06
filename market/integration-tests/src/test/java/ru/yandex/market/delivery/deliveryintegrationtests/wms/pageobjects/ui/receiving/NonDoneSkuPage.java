package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class NonDoneSkuPage extends AbstractPage {
    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement yesButton;

    public NonDoneSkuPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("nonDoneSkuPage$"));
    }

    @Step("Подтверждаем закрытие грузоместа")
    public void approveClosingPallet() {
        yesButton.click();
    }
}
