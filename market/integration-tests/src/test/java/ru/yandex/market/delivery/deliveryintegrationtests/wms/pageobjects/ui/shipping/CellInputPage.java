package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class CellInputPage extends AbstractInputPage {

    public CellInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "cellInputPage";
    }

    @Step("Сканируем ячейку")
    public void enterCell(String cell) {
        super.performInput(cell);
        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Дропка размещена"),
                "Не отображено сообщение, что дропка размещена");
    }
}
