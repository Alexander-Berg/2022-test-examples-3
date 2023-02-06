package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.ArrayList;
import java.util.List;

public class SequentialInventoryPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(SequentialInventoryPage.class);

    String sortUpXpath = "//img[@src = 'images/sortarrow_up.gif']";
    String sortDownXpath = "//img[@src = 'images/sortarrow_down.gif']";

    @Name("Сортировка: Партия")
    @FindBy(xpath = "//span[contains(@onclick, 'LOT') and contains(@onclick, 'setSortColName')]")
    private HtmlElement lotSort;

    @Name("Кнопка следующая страница")
    @FindBy(xpath = "//div[starts-with(@id, 'slot_4E')]//img[@alt = 'Далее' or @title = 'Далее']")
    private HtmlElement nextButton;

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$in5dkl_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации: Владелец")
    @FindBy(xpath = "//input[@attribute='STORERKEY']")
    private HtmlElement storerField;

    @Name("Поле фильтрации: Партия")
    @FindBy(xpath = "//input[@attribute='LOT']")
    private HtmlElement batchField;

    @Name("Поле фильтрации: Ячейка")
    @FindBy(xpath = "//input[@attribute='LOC']")
    private HtmlElement cellField;

    @Name("Поле фильтрации: НЗН")
    @FindBy(xpath = "//input[@attribute='ID']")
    private HtmlElement nznField;

    @Name("Поле фильтрации: Серийный номер")
    @FindBy(xpath = "//input[@attribute='SERIALNUMBER']")
    private HtmlElement serialField;

    @Name("Поле фильтрации: Артикул поставщика")
    @FindBy(xpath = "//input[@attribute='MANUFACTURERSKU']")
    private HtmlElement supplierSkuField;

    public SequentialInventoryPage(WebDriver driver) {
        super(driver);
    }

    @Step("Проверка, что присутствует сортировка по возрастанию")
    public SequentialInventoryPage checkSortUp() {
        isElementPresent(By.xpath(sortUpXpath));

        return this;
    }

    @Step("Проверка, что присутствует сортировка по убыванию")
    public SequentialInventoryPage checkSortDown() {
        isElementPresent(By.xpath(sortDownXpath));

        return this;
    }

    @Step("Сортировка: по партии")
    public SequentialInventoryPage sortByLot() {
        lotSort.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим владельца")
    public SequentialInventoryPage inputStorer(String storer) {
        storerField.sendKeys(storer);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим Номер партии")
    public SequentialInventoryPage inputBatchNumber(String batchNumber) {
        batchField.sendKeys(batchNumber);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим Ячейку")
    public SequentialInventoryPage inputCellNumber(String cellId) {
        cellField.sendKeys(cellId);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим НЗН")
    public SequentialInventoryPage inputNzn(String nzn) {
        nznField.sendKeys(nzn);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим Серийный номер")
    public SequentialInventoryPage inputSerialNumber(String serialNumber) {
        serialField.sendKeys(serialNumber);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим Артикул поставщика")
    public SequentialInventoryPage inputSupplierSku(String supplierSku) {
        supplierSkuField.sendKeys(supplierSku);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Запускаем фильтрацию")
    public SequentialInventoryPage filterButtonClick() {
        overlayBusy.waitUntilHidden();
        filterButton.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    private boolean tryToClickNext() {

        if (nextButton.getAttribute("class").equals("actionIcon")) {
            nextButton.click();
            overlayBusy.waitUntilHidden();
            return true;
        }

        return false;
    }

    /**
     * xpathMask should contain 'RowNumber' placeholder for iterating over rows
     * <p>
     * sample:
     * xpathMask = "//span[@id = '$in5dkl_cell_RowNumber_6_span']";
     **/
    private List<String> getValuesFromResults(String xpathMask, int maxResults) {

        List<String> values = new ArrayList<>();

        do {

            for (int i = 0; i < resultsOnPage(); i++) {

                String value = driver.findElement(By.xpath(xpathMask
                        .replaceFirst("RowNumber", String.valueOf(i))))
                        .getText();

                if (!values.contains(value)) values.add(value);

                if (values.size() >= maxResults) return values;
            }

        } while (tryToClickNext());

        return values;
    }

    public List<String> getSerialsFromFilterResults() {
        return getSerialsFromFilterResults(Integer.MAX_VALUE);
    }

    @Step("Достаем серийники из результатов фильтра (максимально {maxResults})")
    public List<String> getSerialsFromFilterResults(int maxResults) {

        String serialsColumnXpathMask = "//span[@id = '$in5dkl_cell_RowNumber_6_span']";

        return getValuesFromResults(serialsColumnXpathMask, maxResults);
    }

    public List<String> getNznsFromFilterResults() {
        return getNznsFromFilterResults(Integer.MAX_VALUE);
    }

    @Step("Достаем НЗН из результатов фильтра (максимально {maxResults})")
    public List<String> getNznsFromFilterResults(int maxResults) {

        String serialsColumnXpathMask = "//span[@id = '$in5dkl_cell_RowNumber_5_span']";

        return getValuesFromResults(serialsColumnXpathMask, maxResults);
    }
}
