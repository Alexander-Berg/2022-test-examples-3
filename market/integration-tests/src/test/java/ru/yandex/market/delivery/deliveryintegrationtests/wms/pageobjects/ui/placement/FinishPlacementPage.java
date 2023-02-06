package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

public class FinishPlacementPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;

    public FinishPlacementPage(WebDriver driver) {
        super(driver);
    }

    @Step("Завершаем размещение")
    public OrderCreationPage finishPlacement(){
        forwardButton.click();
        return new OrderCreationPage(driver);
    }

    protected String getUrl() {
        return "finishPlacementPage$";
    }
}
