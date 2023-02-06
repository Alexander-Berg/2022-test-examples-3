package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class PlacementLocationPage extends AbstractInputPage {

    public PlacementLocationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим Ячейку для размещения")
    public PlacementIdPage enterCell(String cell) {
        super.performInput(cell);
        return new PlacementIdPage(driver);
    }

    @Override
    protected String getUrl() {
        return "placementLocationPage$";
    }
}
