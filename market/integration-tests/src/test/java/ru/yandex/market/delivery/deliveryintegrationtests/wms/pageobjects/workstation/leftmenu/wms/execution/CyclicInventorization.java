package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.execution;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class CyclicInventorization extends AbstractWsPage {

    @Name("Инициация инв-ии по ячейкам")
    @FindBy(xpath = "//div[@id = '$b6m9wn']")
    private HtmlElement startCellInventorization;

    public CyclicInventorization(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем меню: Инициация инв-ии по ячейкам")
    public void startCellInventorization() {
        safeClick(startCellInventorization);
        overlayBusy.waitUntilHidden();
    }
}
