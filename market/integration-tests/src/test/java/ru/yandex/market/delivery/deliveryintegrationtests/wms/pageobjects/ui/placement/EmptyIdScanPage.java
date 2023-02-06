package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import java.util.List;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class EmptyIdScanPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='start-placement-button']")
    private SelenideElement forwardButton;

    @FindBy(xpath = "//button[@data-e2e='change-picking-mode']")
    private SelenideElement changePickingMode;

    public EmptyIdScanPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ШК пустой нзн")
    public PickingByUITPage enterEmptyContainer(String containerLabel) {
        super.performInput(containerLabel);
        return new PickingByUITPage(driver);
    }

    @Step("Меняем режим на набор НЗН")
    public PickingByIdPage changePickingMode() {
        changePickingMode.click();
        return new PickingByIdPage(driver);
    }

    @Step("Переходим к размещению")
    public PlacementLocationPage startPlacement(){
        forwardButton.click();
        return new PlacementLocationPage(driver);
    }

    @Step("Набираем УИТы в пустую НЗН и перемещаем в ячейку {movingCell}")
    public EmptyIdScanPage moveUIT(String emptyContainer, String movingCell, List<String> UITs) {
        enterEmptyContainer(emptyContainer);
        for (int i = 0; i < UITs.size(); i++){
            new PickingByUITPage(driver)
                    .enterUITToMove(UITs.get(i));
        }
        return new PickingByUITPage(driver)
                .startPlacement()
                .enterCell(movingCell)
                .enterCart(emptyContainer)
                .finishOptimizationPlacement();
    };

    @Override
    protected String getUrl() {
        return "emptyIdScanPage";
    }
}
