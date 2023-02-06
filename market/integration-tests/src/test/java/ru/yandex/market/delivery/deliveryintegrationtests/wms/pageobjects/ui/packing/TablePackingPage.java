package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class TablePackingPage extends AbstractInputPage {

    public TablePackingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем стол {workingArea}")
    public PrinterPackingPage enterWorkingArea(String workingArea) {
        super.performInput(workingArea);
        return new PrinterPackingPage(driver);
    }

    @Override
    protected String getUrl() {
        return "tablePackingPage";
    }
}
