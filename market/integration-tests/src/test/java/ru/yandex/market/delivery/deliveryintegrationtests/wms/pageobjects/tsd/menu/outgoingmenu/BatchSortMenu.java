package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.outgoingmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.sorting.CartSelectPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class BatchSortMenu extends AbstractTsdPage {

    @Name("0 Переназначить пакетный заказ")
    @FindBy(xpath = "//button[text() = 'Переназначить пакетный заказ']")
    private HtmlElement reassignBatchOrder;

    @Name("1 Новая сортировка")
    @FindBy(xpath = "//button[text() = 'Новая сортировка']")
    private HtmlElement newSorting;

    public BatchSortMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Переназначить пакетный заказ")
    public void reassignBatchOrder() {
        reassignBatchOrder.click();
    }

    @Step("Новая сортировка")
    public CartSelectPage newSorting() {
        newSorting.click();

        return new CartSelectPage(driver);
    }
}
