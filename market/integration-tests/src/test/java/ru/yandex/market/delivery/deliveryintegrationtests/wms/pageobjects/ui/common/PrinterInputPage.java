package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class PrinterInputPage extends AbstractInputPage {

    public PrinterInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим принтер")
    public void enterPrinter(String printer) {
        performInput(printer);
    }

    @Override
    protected String getUrl() {
        return "printerInput.*";
    }
}
