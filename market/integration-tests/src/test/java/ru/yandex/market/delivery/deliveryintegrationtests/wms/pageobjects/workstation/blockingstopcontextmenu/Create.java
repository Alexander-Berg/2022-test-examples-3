package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.blockingstopcontextmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Create extends AbstractWsPage {

    @Name("Пункт меню \"Создать\": Блокировать по ячейке")
    @FindBy(xpath = "//span[@id = 'menuDiv']//div[@onclick][.//div[text()[contains(.,'Резервировать по номеру партии')]]]")
    private HtmlElement blockByLotButton;

    public Create(WebDriver driver) {
        super(driver);
    }

    @Step("Блокировать по партии")
    public BlockLotPopUp blockByLot() {
        blockByLotButton.click();
        return new BlockLotPopUp(driver);
    }
}
