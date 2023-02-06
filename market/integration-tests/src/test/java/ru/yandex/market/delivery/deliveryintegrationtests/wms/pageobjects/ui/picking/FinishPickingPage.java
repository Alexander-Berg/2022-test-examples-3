package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;

public class FinishPickingPage extends AbstractInputPage {

    public FinishPickingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ШК финальной локации")
    public FinishPickingPage enterPicktoLoc(String picktoLock) {
        super.performInput(picktoLock);
        return this;
    }

    @Override
    protected String getUrl() {
        return "finishPickingPage$";
    }

    @Step("Отказываемся от следующего задания на этом участке")
    public void refuseNextAssignmentInCurrentArea() {
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        modalWindow.clickBack();
    }

    @Step("Берем следующее задание на этом участке")
    public void acceptNextAssignmentInCurrentArea() {
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        modalWindow.clickForward();
    }
}
