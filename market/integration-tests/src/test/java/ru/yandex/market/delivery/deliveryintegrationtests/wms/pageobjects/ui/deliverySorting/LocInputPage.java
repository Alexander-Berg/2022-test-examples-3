package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.deliverySorting;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class LocInputPage extends AbstractInputPage {

    public LocInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getUrl() {
        return "locInputPage";
    }

    @Step("Вводим ячейку дропинга {droppingCell}")
        public SortingPage enterDroppingCell(String droppingCell) {
        super.performInput(droppingCell);
        return new SortingPage(driver);
    }
}
