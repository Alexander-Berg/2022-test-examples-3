package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class TsdMainMenu extends AbstractTsdPage {

    @Name("0 Приемка")
    @FindBy(xpath = "//button[text() = 'Приемка']")
    private HtmlElement inboundButton;

    @Name("2 Поп. и перем.")
    @FindBy(xpath = "//button[text() = 'Поп. и перем.']")
    private HtmlElement moveButton;

    @Name("3 Отбор")
    @FindBy(xpath = "//button[text() = 'Отбор']")
    private HtmlElement complectationButton;

    @Name("4 Меню Исходящие")
    @FindBy(xpath = "//button[text() = 'Меню Исходящие']")
    private HtmlElement menuOutButton;

    public TsdMainMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Главное меню - 0 Приемка")
    public InboundMenu Inbound() {

        header
                .menuButtonClick()
                .backToMainMenu();

        inboundButton.click();

        return new InboundMenu(driver);
    }

    @Step("Главное меню - 2 Поп. и перем.")
    public MoveMenu Move() {

        header
                .menuButtonClick()
                .backToMainMenu();

        moveButton.click();

        return new MoveMenu(driver);
    }

    @Step("Главное меню - 3 Отбор")
    public ComplectationMenu Complectation() {

        header
                .menuButtonClick()
                .backToMainMenu();

        complectationButton.click();

        return new ComplectationMenu(driver);
    }

    @Step("Главное меню - 4 Меню Исходящие")
    public OutgoingMenu Outgoing() {

        header
                .menuButtonClick()
                .backToMainMenu();

        menuOutButton.click();

        return new OutgoingMenu(driver);
    }
}
