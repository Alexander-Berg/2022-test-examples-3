package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Lottable08;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.configuration.ProductPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks.BalancesPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks.BlockingsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks.MovingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.stocks.SequentialInventoryPage;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;

@Resource.Classpath({"wms/infor.properties"})
public class Items extends AbstractWSSteps {
    private static final Logger log = LoggerFactory.getLogger(Items.class);

    private final SequentialInventoryPage sequentialInventoryPage;
    private final BalancesPage balancesPage;
    private final BlockingsPage blockingsPage;
    private final MovingPage movingPage;
    private final ProductPage productPage;

    public Items(WebDriver drvr) {
        super(drvr);
        sequentialInventoryPage = new SequentialInventoryPage(driver);
        balancesPage = new BalancesPage(driver);
        movingPage = new MovingPage(driver);
        blockingsPage = new BlockingsPage(driver);
        productPage = new ProductPage(driver);
    }

    @Step("Находим серийники айтемов по НЗН и Артикулу поставщика")
    public List<String> findByNznAndSupSku(String supplierSku, String nzn) {
        log.info("Getting item list by NZN and supplierSku");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().sequentialInventory();

        sequentialInventoryPage
                .inputNzn(nzn)
                .inputSupplierSku(supplierSku)
                .filterButtonClick();

        return sequentialInventoryPage.getSerialsFromFilterResults();
    }


    @Step("Находим УИТы по ячейке {cell} и Артикулу поставщика")
    public List<String> findByCellAndSupSku(String supplierSku, String cell) {
        log.info("Getting Serials list by Cell and supplierSku");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().sequentialInventory();

        sequentialInventoryPage
                .inputCellNumber(cell)
                .inputSupplierSku(supplierSku)
                .filterButtonClick();

        return sequentialInventoryPage.getSerialsFromFilterResults();
    }

    @Step("Находим серийники заданного количества айтемов по НЗН и Артикулу поставщика")
    public  List<String> findIdCountByNznAndSupSku(String supplierSku, String nzn, int count) {
        log.info("Getting item list by NZN and supplierSku");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().sequentialInventory();

        sequentialInventoryPage
                .inputNzn(nzn)
                .inputSupplierSku(supplierSku)
                .filterButtonClick();

        return sequentialInventoryPage.getSerialsFromFilterResults(count);
    }

    @Step("Находим серийники айтемов по ячейке и номеру партии")
    public List<String> findByCellVendorAndBatchNumber(
            String cellId,
            String batchNumber,
            int maxResults
    ) {
        log.info("Getting item list by cell and batch number");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().sequentialInventory();

        sequentialInventoryPage
                .inputCellNumber(cellId)
                .inputBatchNumber(batchNumber)
                .filterButtonClick();

        //Нужно отсортировать на случай нескольких партий, чтобы получить самую свежую
        if (sequentialInventoryPage.resultsOnPage() > 1) {
            sequentialInventoryPage
                    .sortByLot()
                    .checkSortUp()
                    .sortByLot()
                    .checkSortDown();
        }

        return sequentialInventoryPage.getSerialsFromFilterResults(maxResults);
    }

    @Step("Получаем НЗН айтемов по серийнику")
    public List<ParcelId> findNznBySerial(List<String> serials) {
        log.info("Getting item nzns by serials");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().sequentialInventory();

        String searchString = String.join(" или ", serials);

        sequentialInventoryPage
                .inputSerialNumber(searchString)
                .filterButtonClick();

        return sequentialInventoryPage.getNznsFromFilterResults()
                .stream()
                .map(ParcelId::new)
                .collect(Collectors.toList());
    }

    @Step("Узнаем количество доступного товара")
    public int getAvailableItemsNumber(String cellId, String supplierSku, int expectedAmount) {
        log.info("Getting number of items available for order");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().balances();

        balancesPage
                .inputCellNumber(cellId)
                .inputSupplierSku(supplierSku)
                .inputLottable08(Lottable08.YES)
                .filterButtonClick();

        return balancesPage.getAvailableAmount(expectedAmount);
    }

    @Step("Проверяем наличие товара")
    public boolean isEnoughItemsAvailable(String cellId, String supplierSku, int expectedAmount) {
        log.info("Checking if there is enough items available for order");

        int actualAmount = getAvailableItemsNumber(cellId, supplierSku, expectedAmount);

        log.info("actualAmount: {}, expectedAmount: {}", actualAmount, expectedAmount);
        return actualAmount >= expectedAmount;
    }

    @Step("Провереям отсутсвие товара по серийному номеру")
    public void checkItemAbsent(String sn){
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().sequentialInventory();
        sequentialInventoryPage
                .inputSerialNumber(sn)
                .filterButtonClick();
        Assertions.assertEquals(0,sequentialInventoryPage.getSerialsFromFilterResults().size());
    }

    @Step("Находим партию айтемов по НЗН")
    public String findLotByNzn(String nzn) {
        log.info("Getting item list by NZN");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().balances();

        balancesPage
                .inputNzn(nzn)
                .filterButtonClick();

        return balancesPage.getBatchFromFilterResults();
    }

    @Deprecated
    @Step("Находим список статусов по номеру партии")
    public List<String> findStatusesByLot(String batchId) {
        log.info("Getting status by batchId");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().blockings();

        blockingsPage
                .inputBatchNumber(batchId)
                .filterButtonClick();

        return blockingsPage.getLotStatuses();
    }

    @Step("Получаем Ровку айтема")
    public String getItemRov(Item item) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().configuration().product();
        productPage.inputSupplierSku(item.getArticle());
        productPage.inputSupplierId(item.getVendorId());
        productPage.filterButtonClick();
        return productPage.getRovFromFilterResults();
    }

    @Step("Перемещение всех товаров товара с тележки на полку через балансы")
    public void moveBalanceFromCartToCell(String cartId, String cellId) {
        log.info("Moving items from cart {} to cell {}", cartId, cellId);
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().transfers();

        movingPage
                .inputNzn(cartId)
                .filterButtonClick()
                .moveFilteredItemsToCell(cellId);
    }

    @Step("Перемещение {count} товара(ов) с тележки на полку через балансы")
    public void moveBalanceCountFromCartToCell(String cartId, String cellId, int count) {
        log.info("Moving items from cart {} to cell {}", cartId, cellId);
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().transfers();

        movingPage
                .inputNzn(cartId)
                .filterButtonClick()
                .moveFilteredItemsCountToCell(cellId, count);
    }

    @Step("Перемещение одного СКУ с тележки {cartId} на полку {cellId} через балансы")
    public void moveItemBalanceFromCartToCell(Item item, String cartId, String cellId) {
        log.info("Moving items from cart {} to cell {}", cartId, cellId);
        String rov = getItemRov(item);
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().transfers();

        movingPage
                .inputRov(rov)
                .inputNzn(cartId)
                .filterButtonClick()
                .moveFilteredItemsToCell(cellId);
    }

    @Step("Отключаем чекбокс \"Требуется ручная настройка\"")
    public void switchOffManualSetUpRequiredCheckbox(Item item) {
        log.info("Switching off requirement of manual set up");
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().configuration().product();
        String rov = getItemRov(item);
        productPage
                .inputRov(rov)
                .filterButtonClick()
                .openRovDetails()
                .openIncomingTab()
                .clickManualSetUpRequiredCheckbox()
                .saveChanges();
    }

    @Step("Блокируем товары по партии")
    public void blockByLot(String lot, InventoryHoldStatus reason) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().stocks().blockings();
        blockingsPage.blockByLot(lot, reason);
    }
}
