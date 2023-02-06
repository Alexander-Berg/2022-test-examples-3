package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class FinalLocationInputPage extends AbstractInputPage {

    public FinalLocationInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ШК финальной локации")
    public void enterFinalLocation(String finalLoc) {
        super.performInput(finalLoc);
    }

    @Override
    protected String getUrl() {
        return "finalLocationInputPage$";
    }
}
