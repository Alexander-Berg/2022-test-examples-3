package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class MovingPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(MovingPage.class);

    @Name("Поле ввода ROV-ки")
    @FindBy(xpath = "//input[@attribute='SKU']")
    private HtmlElement productField;

    @Name("Поле ввода НЗН")
    @FindBy(xpath = "//input[@attribute='ID']")
    private HtmlElement nznField;

    @Name("Количество")
    @FindBy(xpath = "//input[@id = 'I9214ii']")
    private HtmlElement countToMove;

    @Name("Таблица пемещений")
    @FindBy(id = "SectionContent0_0")
    private HtmlElement transfersTable;

    @Name("Чекбокс выбора певого серийного номера в списке")
    @FindBy(xpath = "//input[@id='$8ww8ii_rowChkBox_Count']")
    private HtmlElement checkboxItemToMove;

    @Name("Сохранить")
    @FindBy(xpath = "//div[@id='A1190i9']")
    private HtmlElement saveButton;

    private static final String checkboxItemToMoveXpath
            = "//input[@id='$8ww8ii_rowChkBox_Count']";

    private static final String destinationCellXpath
            = "//td[@id='$u2til6_cell_RowNumber_12' and @class = 'editable']";

    private static final String destinationCellInputXpath
            = "//td[@id='$u2til6_cell_RowNumber_12']//input[@attribute='TOLOCATION']";

    private static final String countToMoveXpath
            = "//td[@id='$u2til6_cell_0_14' and @class = 'editable']";

    private static final String countToMoveInputXpath
            = "//td[@id='$u2til6_cell_0_14']//input[@attribute='QUANTITYTOMOVE']";

    @Name("Стрелка вниз")
    @FindBy(id = "$q6bjee_image")
    private HtmlElement arrowKey;

    @Name("Выполнить перемещение")
    @FindBy(xpath = "//span[@id='menuDiv']//div[@id='$40e7ec']")
    private HtmlElement runGoTo;

    public MovingPage(WebDriver driver) {
        super(driver);
    }

    public MovingPage inputProductSku(String sku) {
        return this;
    }

    @Step("Вводим поле Товар (ROV)")
    public MovingPage inputRov(String rov) {
        //Поля постоянно теряют фокус, пришлось обложить ретраями
        Retrier.retrySeleniumStep(() -> {
            productField.click();
            overlayBusy.waitUntilHidden();
            productField.sendKeys(rov);
            overlayBusy.waitUntilHidden();
        });

        return this;
    }

    @Step("Вводим поле НЗН")
    public MovingPage inputNzn(String nzn) {
        //Поля постоянно теряют фокус, пришлось обложить ретраями
        Retrier.retrySeleniumStep(() -> {
            nznField.click();
            overlayBusy.waitUntilHidden();
            nznField.sendKeys(nzn);
            overlayBusy.waitUntilHidden();
        });

        return this;
    }

    @Step("Выполняем фильтрацию")
    public MovingPage filterButtonClick() {
        //Поля постоянно теряют фокус, пришлось обложить ретраями
        Retrier.retrySeleniumStep(() -> {
            overlayBusy.waitUntilHidden();
            waitClickableAndClick(By.xpath("//input[@id = '$u2til6_filterbutton']"));
            overlayBusy.waitUntilHidden();
        });

        return this;
    }

    @Step("Выполнить перемещение отфильтрованных товаров")
    public MovingPage moveFilteredItemsToCell(String cell)
    {
        Assertions.assertTrue(resultsOnPage() > 0);
        for (int i = 0; i < resultsOnPage(); i++) {
            String destinationCellXpathI = destinationCellXpath
                    .replaceFirst("RowNumber", String.valueOf(i)
                    );
            String destinationCellInputXpathI = destinationCellInputXpath
                    .replaceFirst("RowNumber", String.valueOf(i)
                    );

            Retrier.retrySeleniumStep(() -> {
                waitClickableAndClick(By.xpath(destinationCellXpathI));
                overlayBusy.waitUntilHidden();
                //Элемент появляется только после клика на ячейку, приходится искать динамически
                waitClickableAndSendKeys(By.xpath(destinationCellInputXpathI), cell);
            });
        }
        arrowKey.click();
        runGoTo.click();
        overlayBusy.waitUntilHidden();
        return this;
    }

    @Step("Выполнить перемещение {count} отфильтрованных товаров в {cell}")
    public MovingPage moveFilteredItemsCountToCell (String cell, int count){
        Assertions.assertTrue(resultsOnPage() > 0);
            String destinationCellXpathI = destinationCellXpath
                    .replaceFirst("RowNumber", String.valueOf(0)
                    );
            String destinationCellInputXpathI = destinationCellInputXpath
                    .replaceFirst("RowNumber", String.valueOf(0)
                    );
            Retrier.retrySeleniumStep(() -> {
                waitClickableAndClick(By.xpath(destinationCellXpathI));
                overlayBusy.waitUntilHidden();
                //Элемент появляется только после клика на ячейку, приходится искать динамически
                waitClickableAndSendKeys(By.xpath(destinationCellInputXpathI), cell);
            });
            Retrier.retrySeleniumStep(() -> {
                waitClickableAndClick(By.xpath(countToMoveXpath));
                overlayBusy.waitUntilHidden();
                SeleniumUtil.clearInput(driver.findElement(By.xpath(countToMoveInputXpath)), driver);
                waitClickableAndSendKeys(By.xpath(countToMoveInputXpath), "" + count);
            });

        arrowKey.click();
        runGoTo.click();
        overlayBusy.waitUntilHidden();
        switchToSubWindow();
        for (int i = 0; i < count; i++) {
            String checkboxItemToMoveXpathI = checkboxItemToMoveXpath
                    .replaceFirst("Count", String.valueOf(i)
                    );
            Retrier.retrySeleniumStep(() -> {
                        waitClickableAndClick(By.xpath(checkboxItemToMoveXpathI));
            });
        }
        saveButton.click();
        switchToMainWindow();
        return this;
    }

    private void waitClickableAndClick(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator));
        driver.findElement(locator).click();
    }

    private void waitClickableAndSendKeys(By locator, String keysData) {
        wait.until(ExpectedConditions.elementToBeClickable(locator));
        driver.findElement(locator).sendKeys(keysData);
    }

}
