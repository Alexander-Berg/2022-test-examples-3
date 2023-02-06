package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Lottable08;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class BalancesPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(BalancesPage.class);

    @Name("Счетчик результатов")
    @FindBy(xpath = "//div[starts-with(@id, 'slot_')]//td[contains(text(), 'из')]")
    private HtmlElement resulstCounter;


    @Name("Кнопка следующая страница")
    @FindBy(xpath = "//div[starts-with(@id, 'slot')]//img[@alt = 'Далее' or @title = 'Далее']")
    private HtmlElement nextButton;

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$333ti3_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации: Партия")
    @FindBy(xpath = "//input[@attribute='LOT']")
    private HtmlElement batchField;

    @Name("Поле фильтрации: Ячейка")
    @FindBy(xpath = "//input[@attribute='LOC']")
    private HtmlElement cellField;

    @Name("Поле фильтрации: НЗН")
    @FindBy(xpath = "//input[@attribute='ID']")
    private HtmlElement nznField;

    @Name("Поле фильтрации: Атрибут партии 08")
    @FindBy(xpath = "//input[@attribute='LOTTABLE08']")
    private HtmlElement lottable08Field;

    @Name("Поле фильтрации: Артикул поставщика")
    @FindBy(xpath = "//input[@attribute='MANUFACTURERSKU']")
    private HtmlElement supplierSkuField;

    @Name("Результат фильтрации: Номер партии")
    @FindBy(xpath = "//span[@id='$333ti3_cell_0_3_span']")
    private HtmlElement resultBatch;

    public BalancesPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим Номер партии")
    public BalancesPage inputBatchNumber(String batchNumber) {
        batchField.sendKeys(batchNumber);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим Ячейку")
    public BalancesPage inputCellNumber(String cellId) {
        cellField.sendKeys(cellId);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим НЗН")
    public BalancesPage inputNzn(String nzn) {
        nznField.sendKeys(nzn);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим Артикул поставщика")
    public BalancesPage inputSupplierSku(String supplierSku) {
        supplierSkuField.sendKeys(supplierSku);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Вводим Атрибут партии 08")
    public BalancesPage inputLottable08(Lottable08 lottable08) {
        lottable08Field.sendKeys(lottable08.getId().toString());
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Запускаем фильтрацию")
    public void filterButtonClick() {
        overlayBusy.waitUntilHidden();
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    private boolean tryToClickNext() {

        if (nextButton.getAttribute("class").equals("actionIcon")) {
            nextButton.click();
            overlayBusy.waitUntilHidden();
            return true;
        }

        return false;
    }

    @Step("Получаем доступное количество из первой строки результатов фильтрации")
    public int getAvailableAmount(int expectedAmount) {

        String xpathMask = "//span[@id = '$333ti3_cell_RowNumber_7_span']";

        int availableItems = 0;

        do {

            for (int i = 0; i < resultsOnPage(); i++) {

                String itemsString = driver.findElement(By.xpath(xpathMask
                        .replaceFirst("RowNumber", String.valueOf(i))))
                        .getText()
                        .replaceAll(" |,|\\..*", "");

                availableItems = availableItems + Integer.parseInt(itemsString);

                if (availableItems >= expectedAmount) {
                    return availableItems;
                }
            }

        } while (tryToClickNext());

        return availableItems;
    }

    @Step("Получаем номер партии из результатов фильтра")
    public String getBatchFromFilterResults() {
        return resultBatch.getText();
    }
}
