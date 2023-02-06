package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class PrinterPackingPage extends AbstractInputPage {

    public PrinterPackingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем принтер {printer}")
    public PackingPage enterPrinter(String printer) {
        super.performInput(printer);
        return new PackingPage(driver);
    }

    @Step("Выбираем принтер {printer}")
    public ContainerPackingPage enterPrinterForPackId(String printer) {
        super.performInput(printer);
        return new ContainerPackingPage(driver);
    }

    @Override
    protected String getUrl() {
        return "printerPage";
    }
}
