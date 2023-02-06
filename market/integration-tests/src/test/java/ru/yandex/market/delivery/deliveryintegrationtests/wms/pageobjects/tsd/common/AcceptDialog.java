package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdElement;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

public class AcceptDialog extends AbstractTsdElement {

    @FindBy(id="acceptDialog")
    private HtmlElement acceptDialog;

    @FindBy(xpath = "//button[text() = 'Принять']")
    private HtmlElement acceptButton;

    public AcceptDialog(WebDriver driver) {
        super(driver);
    }

    @Step("Подтверждаем выбор")
    public void accept() {
        wait.until(visibilityOf(acceptDialog));
        wait.until(elementToBeClickable(acceptButton));
        acceptButton.click();
        waitSpinnerIfPresent();
    }
}
