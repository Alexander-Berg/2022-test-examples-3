package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

public class FinishOptimizationPlacement extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;

    public FinishOptimizationPlacement(WebDriver driver) {
        super(driver);
    }

    @Step("Завершаем размещение")
    public EmptyIdScanPage finishOptimizationPlacement(){
        forwardButton.click();
        return new EmptyIdScanPage(driver);
    }

    protected String getUrl() {
        return "finishOptimizationPlacement$";
    }
}
