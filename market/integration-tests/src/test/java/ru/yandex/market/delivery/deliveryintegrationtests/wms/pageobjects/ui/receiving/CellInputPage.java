package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

public class CellInputPage extends AbstractInputPage {

    public CellInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "cellInput";
    }

    @Step("Сканируем ячейку")
    public CellInputPage enterCell(String cell) {
        super.performInput(cell);
        return this;
    }

    @Step("Нажимаем на кнопку Да, подтверждаем выбор локации")
    public CellInputPage clickOk() {
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        modalWindow.clickForward();
        return this;
    }
}
