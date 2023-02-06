package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class GateInputPage extends AbstractInputPage {

    public GateInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "tableInput";
    }

    @Step("Вводим стол")
    public ReceiptInputPage enterTable(String table) {
        performInput(table);
        return new ReceiptInputPage(driver);
    }
}
