package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Outgoing extends AbstractWsPage {
    @Name("Заказ на отгрузку")
    @FindBy(xpath = "//div[@id = '$ri4541']")
    private HtmlElement outgoingOrder;

    @Name("Упаковка")
    @FindBy(xpath = "//div[@id = '$k5tegp']")
    private HtmlElement packing;

    public Outgoing(WebDriver driver) {
        super(driver);
    }

    @Step("Меню Исходящие: Заказ на отгрузку")
    public HtmlElement outgoingOrder() {
        safeClick(outgoingOrder);
        return outgoingOrder;
    }

    @Step("Меню Исходящие: Упаковка")
    public HtmlElement packing() {
        safeClick(packing);
        return packing;
    }
}
