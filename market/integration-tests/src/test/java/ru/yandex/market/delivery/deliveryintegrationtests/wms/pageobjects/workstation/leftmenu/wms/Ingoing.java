package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Ingoing extends AbstractWsPage {
    @Name("ПУО-приемка")
    @FindBy(xpath = "//div[@id = '$96gaq8']")
    private HtmlElement puoInbound;

    @Name("Проверка входящих с закрытием")
    @FindBy(xpath = "//div[@epnylabel = 'Проверка']")
    private HtmlElement approveClosePuo;

    @Name("Детали ПУО-приемки")
    @FindBy(xpath = "//div[@id = '$dgcdqc']")
    private HtmlElement puoInboundDetails;

    public Ingoing(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем меню: ПУО-приемка")
    public HtmlElement puoInbound() {
        safeClick(puoInbound);
        return puoInbound;
    }

    @Step("Выбираем меню: Проверка входящих с закрытием")
    public HtmlElement approveClosePuo() {
        safeClick(approveClosePuo);
        return approveClosePuo;
    }

    @Step("Выбираем меню: Детали ПУО-приемки")
    public HtmlElement puoInboundDetails() {
        safeClick(puoInboundDetails);
        return puoInboundDetails;
    }
}
