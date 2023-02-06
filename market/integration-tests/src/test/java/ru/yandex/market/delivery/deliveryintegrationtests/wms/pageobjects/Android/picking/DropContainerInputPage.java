package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.picking;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.AbstractAndroidInputPage;

import static com.codeborne.selenide.Condition.visible;

public class DropContainerInputPage extends AbstractAndroidInputPage {

    @FindBy(xpath = "//*[@content-desc='wms_dialog']")
    private SelenideElement modal;
    @FindBy(xpath = "//*[@content-desc='negative_dialog_button']")
    private SelenideElement modalNoButton;
    @FindBy(xpath = "//*[@content-desc='positive_dialog_button']")
    private SelenideElement modalYesButton;

    @Step("Вводим номер тары {cart} для сброса")
    public DropContainerInputPage enterCart(String cart) {
        super.performInput(cart);
        modal.shouldBe(visible);

        return this;
    }

    @Step("Отказываемся от следующего задания")
    public AreaInputPage refuseNextTask() {
        modalNoButton.click();

        return new AreaInputPage();
    }

    @Step("Соглашаемся взять следующее задание")
    public CartAddingPage acceptNextTask() {
        modalYesButton.click();

        return new CartAddingPage();
    }

}
