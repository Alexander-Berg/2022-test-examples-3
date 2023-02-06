package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdElement;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

public class Header extends AbstractTsdElement {

    @Name("Назад")
    @FindBy(id = "prevScreenButton")
    private HtmlElement backButton;

    @Name("Вперед")
    @FindBy(id = "nextScreenButton")
    private HtmlElement forvardButton;

    @Name("Меню")
    @FindBy(xpath = "//button[@onclick = 'menuDIV.inforOpen();']")
    private HtmlElement menuButton;

    private static final Logger log = LoggerFactory.getLogger(Header.class);

    public Header(WebDriver driver) {
        super(driver);
    }

    @Step("В верхней плашке: Жмем назад")
    public void backButtonClick() {
        wait.until(elementToBeClickable(backButton));
        backButton.click();
    }

    @Step("В верхней плашке: Жмем вперед")
    public void forvardButtonClick() {
        wait.until(elementToBeClickable(forvardButton));
        forvardButton.click();
    }

    @Step("В верхней плашке: Открываем меню")
    public SystemMenu menuButtonClick() {

        waitSpinnerIfPresent();
        wait.until(elementToBeClickable(menuButton));
        menuButton.click();

        return new SystemMenu(driver);
    }

    @Step("Возврат в Главное Меню")
    public void ReturnToMainMenu() {
        menuButtonClick()
                .backToMainMenu();
    }

}
