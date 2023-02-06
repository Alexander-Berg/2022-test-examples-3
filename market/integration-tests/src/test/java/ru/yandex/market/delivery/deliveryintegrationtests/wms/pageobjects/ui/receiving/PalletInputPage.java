package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.inventorization.QtyInputPage;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class PalletInputPage extends AbstractInputPage {

    @FindBy(xpath = "//span[@data-e2e='loc']")
    private SelenideElement locElement;
    @FindBy(xpath = "//button[@data-e2e='button_back']")
    private SelenideElement no;

    public PalletInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "palletInput";
    }

    @Step("Сканируем паллету")
    public CellInputPage enterPallet(String pallet) {
        super.performInput(pallet);
        return new CellInputPage(driver);
    }
}
