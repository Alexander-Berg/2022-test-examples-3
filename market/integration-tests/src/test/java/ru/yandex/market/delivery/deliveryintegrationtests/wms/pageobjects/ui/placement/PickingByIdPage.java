package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class PickingByIdPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='start-placement-button']")
    private SelenideElement forwardButton;

    @FindBy(xpath = "//button[@data-e2e='change-picking-mode']")
    private SelenideElement changePickingMode;

    public PickingByIdPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим НЗН  для перемещения")
    public PickingByIdPage enterIdToMove(String containerLabel) {
        super.performInput(containerLabel);
        return new PickingByIdPage(driver);
    }

    @Step("Переходим к размещению")
    public StockOptimizationLocationPage startPlacement(){
        forwardButton.click();
        return new StockOptimizationLocationPage(driver);
    }

    @Override
    protected String getUrl() {
        return "pickingByIdPage";
    }
}
