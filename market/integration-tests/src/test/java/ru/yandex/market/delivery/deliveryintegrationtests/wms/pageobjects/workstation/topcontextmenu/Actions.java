package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topcontextmenu;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Actions extends AbstractWsPage {

    @Name("Закрыть ПУО-приемку")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Закрыть ПУО-приемку')]]]")
    private HtmlElement closePuo;

    @Name("Закрыть с проверкой")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Закрыть с проверкой')]]]")
    private HtmlElement aproveClosePuo;

    @Name("Зарезервировать")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Зарезервировать')]]]")
    private HtmlElement reserve;

    @Name("Отменить резервирование")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Отменить резервирование')]]]")
    private HtmlElement cancelReserve;

    @Name("Запуск")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Запуск')] or text()[contains(.,'Запустить')]]]")
    private HtmlElement start;

    public Actions(WebDriver driver) {
        super(driver);
    }

    public void closePuo() {
        closePuo.click();
    }

    public void aproveClosePuo() { aproveClosePuo.click(); }

    public void reserve() {
        reserve.click();
        overlayBusy.waitUntilHidden();
    }

    public void cancelReserve() {
        cancelReserve.click();
        overlayBusy.waitUntilHidden();
    }

    public void start() {
        start.click();
        overlayBusy.waitUntilHidden();
    }
}
