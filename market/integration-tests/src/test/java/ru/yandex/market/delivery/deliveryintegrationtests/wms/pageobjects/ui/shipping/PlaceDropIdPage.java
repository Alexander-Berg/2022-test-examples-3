package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class PlaceDropIdPage extends AbstractInputPage {

    public PlaceDropIdPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "itemInputPage";
    }

    @Step("Сканируем дропку")
    public CellInputPage enterDropId(String dropId) {
        super.performInput(dropId);
        return new CellInputPage(driver);
    }
}
