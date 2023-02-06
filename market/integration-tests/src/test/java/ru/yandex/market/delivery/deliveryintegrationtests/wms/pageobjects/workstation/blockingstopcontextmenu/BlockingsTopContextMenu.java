package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.blockingstopcontextmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class BlockingsTopContextMenu extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(BlockingsTopContextMenu.class);

    @Name("Меню: Создать")
    @FindBy(xpath = "//*[@id='$iscbie_label']")
    private HtmlElement createMenu;

    public BlockingsTopContextMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Меню Создать")
    public Create create() {
        createMenu.click();
        return new Create(driver);
    }
}
