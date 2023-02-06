package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances;

import java.util.ArrayList;
import java.util.List;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TablePreloader;
import ru.yandex.qatools.htmlelements.annotations.Name;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;

public class BalancesOfUitPage extends AbstractPage {

    private final TablePreloader tablePreloader;

    @FindBy(xpath = "//div[@data-e2e='id_filter']//input")
    private SelenideElement searchById;

    @FindBy(xpath = "//div[@data-e2e='manufacturerSku_filter']//input")
    private SelenideElement searchByManufacturerSku;

    @FindBy(xpath = "//div[@data-e2e='serialNumber_filter']//input")
    private SelenideElement searchBySerialNumber;

    @Name("Кнопка следующая страница")
    @FindBy(xpath = NEXT_BUTTON_XPATH)
    private SelenideElement nextButton;

    @Name("Форма ввода sku")
    @FindBy(xpath = "//div[@data-e2e='sku_filter']//input")
    private SelenideElement skuFilter;

    @Name("Форма ввода номера ячейки")
    @FindBy(xpath = "//div[@data-e2e='loc_filter']//input")
    private SelenideElement locFilter;

    private static final String NEXT_BUTTON_XPATH = "//div[@data-e2e='table_paginator']//button[last()]";

    public BalancesOfUitPage(WebDriver driver) {
        super(driver);
        tablePreloader = new TablePreloader(driver);
    }

    @Step("Вводим НЗН {nzn}")
    public BalancesOfUitPage inputNzn(String nzn) {
        searchById.click();
        searchById.sendKeys(nzn);
        searchById.pressEnter();
        tablePreloader.waitUntilHidden();
        return this;
    }

    @Step("Вводим Артикул поставщика")
    public BalancesOfUitPage inputSupplierSku(String sup) {
        searchByManufacturerSku.click();
        searchByManufacturerSku.sendKeys(sup);
        searchByManufacturerSku.pressEnter();
        tablePreloader.waitUntilHidden();
        return this;
    }

    @Step("Вводим Серийный номер")
    public BalancesOfUitPage inputSerialNumber(String serialNumber) {
        searchBySerialNumber.click();
        searchBySerialNumber.sendKeys(serialNumber);
        searchBySerialNumber.pressEnter();
        tablePreloader.waitUntilHidden();
        return this;
    }

    @Step("Вводим SKU {sku}")
    public BalancesOfUitPage inputSku(String sku) {
        skuFilter.click();
        skuFilter.sendKeys(sku);
        skuFilter.pressEnter();
        tablePreloader.waitUntilHidden();
        return this;
    }

    @Step("Вводим номер ячейки {loc}")
    public BalancesOfUitPage inputLoc(String loc) {
        locFilter.click();
        locFilter.sendKeys(loc);
        locFilter.pressEnter();
        tablePreloader.waitUntilHidden();
        return this;
    }

    public List<String> getSerialsFromFilterResults() {
        return getSerialsFromFilterResults(Integer.MAX_VALUE);
    }

    @Step("Достаем серийники из результатов фильтра")
    public List<String> getSerialsFromFilterResults(int maxResults) {

        String serialsColumnXpathMask = "//td[@data-e2e='serialNumberLong_cell_row_RowNumber']";

        return getValuesFromResults(serialsColumnXpathMask, maxResults);
    }

    public List<String> getNznsFromFilterResults() {
        return getNznsFromFilterResults(Integer.MAX_VALUE);
    }

    @Step("Достаем НЗН из результатов фильтра")
    public List<String> getNznsFromFilterResults(int maxResults) {

        String serialsColumnXpathMask = "//td[@data-e2e='id_cell_row_RowNumber']";

        return getValuesFromResults(serialsColumnXpathMask, maxResults);
    }

    private List<String> getValuesFromResults(String xpathMask, int maxResults) {

        List<String> values = new ArrayList<>();

        do {

            for (int i = 0; i < resultsOnPage(); i++) {

                String value = $(byXpath(xpathMask
                        .replaceFirst("RowNumber", String.valueOf(i))))
                        .getText();

                if (!values.contains(value)) values.add(value);

                if (values.size() >= maxResults) return values;
            }

        } while (tryToClickNext());
        return values;
    }

    private boolean tryToClickNext() {

        if (!isElementPresent(byXpath(NEXT_BUTTON_XPATH))) {
            return false;
        } else if (nextButton.getAttribute("aria-disabled") == null) {
            nextButton.click();
            tablePreloader.waitUntilHidden();
            return true;
        } else {
            return false;
        }
    }

}
