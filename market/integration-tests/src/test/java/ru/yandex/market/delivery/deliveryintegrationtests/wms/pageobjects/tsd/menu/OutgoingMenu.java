package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.outgoingmenu.BatchSortMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.LoadingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.PackingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.ShipingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.SortingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.WaveConsolidationPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class OutgoingMenu extends AbstractTsdPage {

    @Name("0 Пакетная сорт.")
    @FindBy(xpath = "//button[text() = 'Пакетная сорт.']")
    private HtmlElement batchSortingButton;

    @Name("2 Упаковка")
    @FindBy(xpath = "//button[text() = 'Упаковка']")
    private HtmlElement packingButton;

    @Name("4 Загрузка в ТС")
    @FindBy(xpath = "//button[text() = 'Загрузка в ТС']")
    private HtmlElement loadButton;

    @Name("5 Отгрузка")
    @FindBy(xpath = "//button[text() = 'Отгрузка']")
    private HtmlElement shipButton;

    @Name("7 Консолид. волны")
    @FindBy(xpath = "//button[text() = 'Консолид. волны']")
    private HtmlElement consolidationButton;

    @Name("8 Сортировка")
    @FindBy(xpath = "//button[text() = 'Сортировка']")
    private HtmlElement sortingButton;

    public OutgoingMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Пакетная сортировка")
    public BatchSortMenu batchSortGoods() {
        batchSortingButton.click();
        return new BatchSortMenu(driver);
    }

    @Step("Упаковка")
    public PackingPage packGoods() {
        packingButton.click();
        return new PackingPage(driver);
    }

    @Step("Загрузка в ТС")
    public LoadingPage loadGoods() {
        loadButton.click();
        return new LoadingPage(driver);
    }

    @Step("Отгрузка")
    public ShipingPage shipGoods() {
        shipButton.click();
        return new ShipingPage(driver);
    }

    @Step("Консолидация волны")
    public WaveConsolidationPage consolidateWave() {
        consolidationButton.click();
        return new WaveConsolidationPage(driver);
    }

    @Step("Сортировка")
    public SortingPage sortGoods() {
        sortingButton.click();
        return new SortingPage(driver);
    }
}
