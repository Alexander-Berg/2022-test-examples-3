package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import java.util.Set;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import com.codeborne.selenide.SelenideElement;

public class ContainerInputPage extends AbstractInputPage {

    @Name("Кнопка начала отбора")
    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement startPickingButton;

    public ContainerInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим тару отбора")
    public ContainerInputPage enterCart(String containerLabel) {
        super.performInput(containerLabel);
        return new ContainerInputPage(driver);
    }

    public ContainerInputPage enterCarts(Set<String> containerLabels) {
        for (String containerLabel : containerLabels) {
            enterCart(containerLabel);
        }
        return new ContainerInputPage(driver);
    }

    @Step("Начинаем отбор")
    public LocationInputPage clickStartPickingButton() {
        startPickingButton.click();
        return new LocationInputPage(driver);
    }

    @Override
    protected String getUrl() {
        return "containerInputPage$";
    }
}
