package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.blockingstopcontextmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.Select;

public class BlockLotPopUp extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(BlockLotPopUp.class);

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//*[@id='$37vkiw_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации: Партия")
    @FindBy(xpath = "//input[@id='I9ycuv7']")
    private HtmlElement cellField;

    @Name("Селектор причин блокировки")
    @FindBy(xpath = "//*[@id='Idowxcr']")
    private HtmlElement blockingReasonDropdown;

    @Name("Чекбокс первой строки результатов поиска")
    @FindBy(xpath = "//*[@id='$37vkiw_rowChkBox_0']")
    private HtmlElement firstRowCheckBox;

    @Name("Кнопка \"Сохранить\"")
    @FindBy(xpath = "//*[@id='Affz2jm_label']")
    private HtmlElement saveButton;

    public BlockLotPopUp(WebDriver driver) {
        super(driver);
        switchToSubWindow();
    }

    @Step("Вводим номер партии ячейки")
    public BlockLotPopUp inputLot(String batchNumber) {
        cellField.sendKeys(batchNumber);
        return this;
    }

    @Step("Запускаем фильтрацию")
    public BlockLotPopUp filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();
        return this;
    }

    @Step("Выбираем причину блокировки")
    public BlockLotPopUp setBlockingReason(InventoryHoldStatus reason) {
        Select select = new Select(blockingReasonDropdown);
        select.selectByValue(reason.getCode());
        overlayBusy.waitUntilHidden();
        return this;
    }

    @Step("Выбираем первую строку в результатах поиска")
    public BlockLotPopUp selectFirstRow() {
        firstRowCheckBox.click();
        return this;
    }

    @Step("Сохраняем")
    public void save() {
        saveButton.click();
        switchToMainWindow();
    }
}
