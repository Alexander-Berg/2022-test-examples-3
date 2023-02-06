package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

import static com.codeborne.selenide.Condition.visible;

public class DropContainerInputPage extends AbstractInputPage {

    @FindBy(xpath = "//*[name()='svg'][@data-e2e='icon-empty-container']")
    private SelenideElement noCartBox;

    public DropContainerInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ШК нзн")
    public DropContainerInputPage enterCart(String containerLabel, Boolean skipIconValidation) {
        super.performInput(containerLabel);
        if (!skipIconValidation) {
            noCartBox.shouldBe(visible);
        }
        return this;
    }

    @Override
    protected String getUrl() {
        return "dropContainerInputPage$";
    }
}
