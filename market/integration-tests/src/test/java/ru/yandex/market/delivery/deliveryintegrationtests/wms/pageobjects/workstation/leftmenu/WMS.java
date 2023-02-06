package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.Configuration;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.Execution;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.Ingoing;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.Outgoing;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class WMS extends AbstractWsPage {

    @Name("Меню Входящие")
    @FindBy(xpath = "//div[@id = '$7fizw4']")
    private HtmlElement ingoing;

    @Name("Меню Входящие")
    @FindBy(xpath = "//div[@id = '$oah7q6']")
    private HtmlElement outgoing;

    @Name("Меню Выполнение")
    @FindBy(xpath = "//div[@id = '$7xqgr6']")
    private HtmlElement execution;

    @Name("Меню Конфигурация")
    @FindBy(xpath = "//div[@id = '$moj83l']")
    private HtmlElement configuration;

    public WMS(WebDriver driver) {
        super(driver);
    }

    @Step("Открываем меню Входящие")
    public Ingoing ingoing() {
        safeClick(ingoing);
        return new Ingoing(driver);
    }

    @Step("Открываем меню Исходящие")
    public Outgoing outgoing() {
        safeClick(outgoing);
        return new Outgoing(driver);
    }

    @Step("Открываем меню Выполнение")
    public Execution execution() {
        safeClick(execution);
        return new Execution(driver);
    }

    @Step("Открываем меню Конфигурация")
    public Configuration configuration() {
        safeClick(configuration);
        return new Configuration(driver);
    }
}
