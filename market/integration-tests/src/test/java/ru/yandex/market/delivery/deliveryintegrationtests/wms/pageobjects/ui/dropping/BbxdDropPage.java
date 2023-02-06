package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dropping;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class BbxdDropPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public BbxdDropPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "dropPage";
    }

    @Step("Вводим количество и нажимаем далее")
    public void enterDropId(String dropId) {
        super.performInput(dropId);
        forward.click();
    }
}
