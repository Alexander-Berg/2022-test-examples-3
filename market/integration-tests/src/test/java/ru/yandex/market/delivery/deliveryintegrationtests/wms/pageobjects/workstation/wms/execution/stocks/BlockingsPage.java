package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.blockingstopcontextmenu.BlockLotPopUp;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.blockingstopcontextmenu.BlockingsTopContextMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class BlockingsPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(BlockingsPage.class);

    private final BlockingsTopContextMenu topContextMenu;

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$x57bm_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации: Партия")
    @FindBy(xpath = "//input[@attribute='LOT']")
    private HtmlElement batchField;

    @Name("Поле фильтрации: НЗН")
    @FindBy(xpath = "//input[@attribute='ID']")
    private HtmlElement nznField;

    @Name("Поле фильтрации: Ячейка")
    @FindBy(xpath = "//input[@attribute='LOC']")
    private HtmlElement cellField;

    public BlockingsPage(WebDriver driver) {
        super(driver);
        topContextMenu = new BlockingsTopContextMenu(driver);
    }

    @Step("Вводим Номер партии")
    public BlockingsPage inputBatchNumber(String batchNumber) {
        batchField.sendKeys(batchNumber);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Запускаем фильтрацию")
    public void filterButtonClick() {
        overlayBusy.waitUntilHidden();
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Получаем статусы партии")
    public List<String> getLotStatuses() {
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        List<WebElement> resultStatuses =
                driver.findElements(By.xpath("//td[@id = '$x57bm_cell_0_5']"));
        resultStatuses.addAll(driver.findElements(By.xpath("//td[@id = '$x57bm_cell_1_5']")));
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);

        return resultStatuses.stream().map(WebElement::getText).collect(Collectors.toList());
    }

    @Step("Заблокировать товары по партии")
    public void blockByLot(String lot, InventoryHoldStatus reason) {
        BlockLotPopUp blockLotPopUp = topContextMenu.create().blockByLot();
        blockLotPopUp
                .inputLot(lot)
                .filterButtonClick()
                .selectFirstRow()
                .setBlockingReason(reason)
                .save();
    }
}
