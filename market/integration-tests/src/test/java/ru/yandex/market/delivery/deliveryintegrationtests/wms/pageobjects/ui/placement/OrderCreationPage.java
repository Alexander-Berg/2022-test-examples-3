package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class OrderCreationPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='start-placement-button']")
    private SelenideElement forwardButton;

    public OrderCreationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ШК нзн")
    public OrderCreationPage enterContainer(String containerLabel) {
        super.performInput(containerLabel);
        return new OrderCreationPage(driver);
    }

    @Step("Переходим к размещению")
    public PlacementLocationPage startPlacement(){
        forwardButton.click();
        return new PlacementLocationPage(driver);
    }

    @Override
    protected String getUrl() {
        return "orderCreationPage";
    }
}
