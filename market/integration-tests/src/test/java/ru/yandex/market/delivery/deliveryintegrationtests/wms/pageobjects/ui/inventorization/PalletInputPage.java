package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.inventorization;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import com.codeborne.selenide.SelenideElement;

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

    @Step("Сканируем нетроганую паллету")
    public PalletInputPage enterPallet(String pallet) {
        super.performInput(pallet);
        return new PalletInputPage(driver);
    }

    @Step("Сканируем троганую паллету")
    public QtyInputPage enterTouchedPallet(String pallet) {
        super.performInput(pallet);
        return new QtyInputPage(driver);
    }

    @Step("На вопрос есть ли еще НЗН в ячейке нажимаем нет")
    public PalletInputPage clickNo() {
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.clickBack();
        return this;
    }

    @Step("Проверяем локацию")
    public boolean checkLoc(String loc) {
        return locElement.getText().equals(loc);
    }
}
