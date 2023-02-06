package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.inventorization;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class CellInputPage extends AbstractInputPage {

    public CellInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "cellInput";
    }

    @Step("Сканируем ячейку")
    public PalletInputPage enterCell(String cell) {
        super.performInput(cell);
        return new PalletInputPage(driver);
    }
}
