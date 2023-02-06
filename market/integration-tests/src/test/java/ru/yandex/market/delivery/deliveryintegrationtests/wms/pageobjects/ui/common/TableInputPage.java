package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class TableInputPage extends AbstractInputPage {

    public TableInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "tableInput";
    }

    @Step("Вводим стол")
    public PrinterInputPage enterTable(String table) {
        performInput(table);
        return new PrinterInputPage(driver);
    }
}
