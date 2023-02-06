package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topcontextmenu;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Function extends AbstractWsPage {

    @Name("Пакетные заказы")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Пакетные заказы')]]]")
    private HtmlElement batchOrder;

    @Name("Разгруппировать заказы")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Разгруппировать заказы')]]]")
    private HtmlElement ungroupBatch;

    public Function(WebDriver driver) {
        super(driver);
    }

    public void createBatchOrder() {
        batchOrder.click();
        overlayBusy.waitUntilHidden();
    }

    public void ungroupBatchOrder() {
        ungroupBatch.click();
        overlayBusy.waitUntilHidden();
    }
}
