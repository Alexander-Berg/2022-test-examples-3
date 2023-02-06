package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementDecorator;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementLocatorFactory;

public class MainMenuPage1 {
    private WebDriver driver;

    @Name("0 Приемка")
    @FindBy(xpath = "//button[text() = 'Приемка']")
    private HtmlElement inboundButton;

    @Name("2 Поп. и перем.")
    @FindBy(xpath = "//button[text() = 'Поп. и перем.']")
    private HtmlElement moveButton;

    @Name("3 Отбор")
    @FindBy(xpath = "//button[text() = 'Отбор']")
    private HtmlElement pickutton;

    @Name("4 Меню Исходящие")
    @FindBy(xpath = "//button[text() = 'Меню Исходящие']")
    private HtmlElement menuOutButton;

    public MainMenuPage1(WebDriver driver) {
        this.driver = driver;

        PageFactory.initElements(new HtmlElementDecorator(new HtmlElementLocatorFactory(driver)), this);
    }

    @Step("Главное меню - 0 Приемка")
    public void MenuInboundClick () {
        inboundButton.click();
    }

    @Step("Главное меню - 2 Поп. и перем.")
    public void MenuMoveClick () {
        moveButton.click();
    }

    @Step("Главное меню - 3 Отбор")
    public void MenuPickClick () {
        pickutton.click();
    }

    @Step("Главное меню - 4 Меню Исходящие")
    public void MenuOutClick () {
        menuOutButton.click();
    }
}
