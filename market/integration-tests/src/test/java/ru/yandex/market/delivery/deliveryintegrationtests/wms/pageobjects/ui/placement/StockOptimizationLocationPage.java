package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class StockOptimizationLocationPage extends AbstractInputPage {

    public StockOptimizationLocationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим Ячейку для размещения")
    public PlaceIdPage enterCell(String cell) {
        super.performInput(cell);
        return new PlaceIdPage(driver);
    }

    @Override
    protected String getUrl() {
        return "stockOptimizationLocationPage";
    }
}
