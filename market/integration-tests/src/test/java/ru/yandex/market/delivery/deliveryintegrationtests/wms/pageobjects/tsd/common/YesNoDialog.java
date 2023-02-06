package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdElement;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

public class YesNoDialog extends AbstractTsdElement {

    @FindBy(className = "inforDialog")
    private HtmlElement choiceDialog;

    @FindBy(xpath = "//button[text() = 'Да']")
    private HtmlElement yesButton;

    @FindBy(xpath = "//button[text() = 'Нет']")
    private HtmlElement noButton;

    public YesNoDialog(WebDriver driver) {
        super(driver);
    }

    @Step("Подтверждаем выбор")
    public void yes() {
        wait.until(visibilityOf(choiceDialog));
        wait.until(elementToBeClickable(yesButton));
        yesButton.click();
        waitSpinner();
    }

    @Step("Отклоняем выбор")
    public void no() {
        wait.until(visibilityOf(choiceDialog));
        wait.until(elementToBeClickable(noButton));
        noButton.click();
        waitSpinner();
    }

    public Boolean IsPresent () {
        return isElementPresent(By.xpath("//button[text() = 'Да']"));
    }
}
