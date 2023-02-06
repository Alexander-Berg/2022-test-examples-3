package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class CartInputPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;

    public CartInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ШК нзн")
    public void enterCart(String containerLabel) {
        super.performInput(containerLabel);
    }

    @Step("Нажимаем кнопку сброса тар")
    public DropContainerInputPage clickDropContainerButton() {
        ModalWindow window = new ModalWindow(driver);
        window.clickForward();
        return new DropContainerInputPage(driver);
    }

    @Step("Нажимаем кнопку сброс контейнеров")
    public CartInputPage clickForwardButton() {
        ModalWindow window = new ModalWindow(driver);
        window.clickForward();
        return this;
    }

    @Override
    protected String getUrl() {
        return "cartInputPage$";
    }
}
