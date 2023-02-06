package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.sorting;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ItemSortPage extends AbstractTsdPage {

    private static final String SCREEN_ID = "BP07LT";

    @Name("Всего")
    @FindBy(id="remqty_0")
    private HtmlElement totalItemsField;

    @Name("НЗН")
    @FindBy(id="ID_0")
    private HtmlElement nznField;

    @Name("Товар")
    @FindBy(id="ALTSKU_0")
    private HtmlElement itemRovField;

    @Name("Ячейка")
    @FindBy(id="SortLoc_0")
    private HtmlElement sortingCellField;

    @Name("Подтверждение ячейки")
    @FindBy(id="Confirm_0")
    private HtmlElement sortingCellConfirmationField;

    public ItemSortPage(WebDriver driver) {
        super(driver, SCREEN_ID);
    }

    public int getTotalItems() {
        assertScreenIsOpen();
        return Integer.valueOf(totalItemsField.getAttribute("value"));
    }

    public String getNzn() {
        assertScreenIsOpen();
        return nznField.getAttribute("value");
    }

    public String getItemRov() {
        assertScreenIsOpen();
        return itemRovField.getAttribute("value");
    }

    public String getSortingCell() {
        assertScreenIsOpen();
        return sortingCellField.getAttribute("value");
    }

    @Step("Вводим серийник товара")
    public ItemSortPage enterSortingCellConfirmation(String cell) {
        assertScreenIsOpen();
        sortingCellConfirmationField.sendKeys(cell);
        sortingCellConfirmationField.sendKeys(Keys.ENTER);

        return this;
    }

    public SortingFinishedDialog SortingFinished() {
        return new SortingFinishedDialog(driver);
    }
}
