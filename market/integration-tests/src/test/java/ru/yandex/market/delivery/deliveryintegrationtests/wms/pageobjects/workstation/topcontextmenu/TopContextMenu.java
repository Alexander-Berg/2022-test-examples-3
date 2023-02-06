package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topcontextmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class TopContextMenu extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(TopContextMenu.class);

    @Name("Меню Действия")
    @FindBy(xpath = "//div[@onclick][.//td[text()[contains(., 'Действия')] or text()[contains(., 'Действие')]]]")
    private HtmlElement actions;

    @Name("Меню Функция")
    @FindBy(xpath = "//div[@onclick][.//td[text()[contains(., 'Функция')]]]")
    private HtmlElement function;

    public TopContextMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Меню Действия")
    public Actions Actions() {
        actions.click();
        return new Actions(driver);
    }

    @Step("Меню Функция")
    public Function Function() {
        function.click();
        return new Function(driver);
    }
}
