package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.common;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe;

public class PopupAlert extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(PopupAlert.class);

    @Name("Кнопка Да")
    @FindBy(xpath = "//input[@value='ДА' or @value='Ок']")
    private HtmlElement yesButton;

    @Name("Кнопка Отменить")
    @FindBy(xpath = "//input[@value='Отменить' or @value='ОТМЕНА']")
    private HtmlElement cancelButton;

    public PopupAlert(WebDriver driver) {
        super(driver);
    }

    @Step("Нажимаем Да в попап окне")
    public void yesButtonClick () {
        switchToSubWindow();
        yesButton.click();
        switchToMainWindow();
        wait.until(numberOfWindowsToBe(1));
        overlayBusy.waitUntilHidden();
    }

    @Step("Нажимаем Отмена в попап окне")
    public void cancelButtonClick () {
        switchToSubWindow();
        cancelButton.click();
        switchToMainWindow();
        wait.until(numberOfWindowsToBe(1));
    }
}
