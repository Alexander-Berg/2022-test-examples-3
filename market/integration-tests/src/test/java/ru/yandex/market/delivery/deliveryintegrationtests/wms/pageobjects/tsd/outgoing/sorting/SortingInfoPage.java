package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.sorting;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.AcceptDialog;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class SortingInfoPage extends AbstractTsdPage {

    private static final String SCREEN_ID = "BAPSUM";

    private AcceptDialog acceptDialog = new AcceptDialog(driver);

    @Name("Пакетн. заказ")
    @FindBy(id="orderkey_0")
    private HtmlElement batchOrderId;

    @Name("ВОЛНА")
    @FindBy(id="wavekey_0")
    private HtmlElement waveId;

    @Name("Кол-во заказов")
    @FindBy(id="orderCount_0")
    private HtmlElement orderCount;

    @Name("Сорт. станция")
    @FindBy(id="sortloc_1")
    private HtmlElement sortingStation;

    public SortingInfoPage(WebDriver driver) {
        super(driver, SCREEN_ID);
    }

    @Step("Получаем номер пакетного заказа")
    public String getBatchOrderId() {
        assertScreenIsOpen();
        return batchOrderId.getAttribute("value");
    }

    @Step("Получаем номер волны")
    public String getWaveId() {
        assertScreenIsOpen();
        return waveId.getAttribute("value");
    }

    @Step("Получаем количество заказов")
    public int getOrderCount() {
        assertScreenIsOpen();
        return Integer.valueOf(orderCount.getAttribute("value"));
    }

    @Step("Получаем номер сортировочной станции")
    public String getSortingStation() {
        assertScreenIsOpen();
        return sortingStation.getAttribute("value");
    }

    @Step("Подтверждаем информациюю и переходим дальше")
    public ItemSerialPage advance() {
        assertScreenIsOpen();
        driver.switchTo().activeElement().sendKeys(Keys.ENTER);

        return new ItemSerialPage(driver);
    }
}
