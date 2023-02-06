package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.RegistryItemWrapper;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.PrinterInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TableInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dropping.BbxdConfirmPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving.GateInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.BarcodeInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.CartInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.ChooseConveyorZonePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.CloseContainerInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.ReceiptInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.ShelfLifePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.SkuWarehouseDamagePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin.DiscrepanciesReport;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin.SuppliesListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.logistic.api.model.common.NonconformityType;

@Resource.Classpath({"wms/wms.properties"})
public class Receiving {
    private final WebDriver driver;
    private final MenuPage menuPage;
    private final NotificationDialog notificationDialog;
    private final InboundTable inboundTable;

    @Property("wms.ui.receiving.printer")
    private String printer;
    private final long closePuoTimeout = 20;

    public Receiving(WebDriver driver, InboundTable inboundTable) {
        this.driver = driver;

        PropertyLoader.newInstance().populate(this);
        this.menuPage = new MenuPage(driver);
        this.notificationDialog = new NotificationDialog(driver);
        this.inboundTable = inboundTable;
    }

    public BarcodeInputPage receiveItem(Item item, String pallet, String cart) {
        return receiveItem(item, pallet, cart, false);
    }

    public BarcodeInputPage receiveItem(RegistryItemWrapper item, String pallet, String cart) {
        return receiveItem(item, pallet, cart, false);
    }

    @Step("Первично принимаем товар на {amount} паллет")
    public void initialReceiveItem(String fullfilmentId, Integer amount) {
        enterPrinterAndGateInInitialReceiving()
                .enterInboundId(fullfilmentId)
                .enterPalletQty(amount.toString());
        Assertions.assertEquals("Было напечатано " + amount + " этикеток", notificationDialog.getMessage(),
                "Ошибка: При первичной приемке");
    }

    @Step("Первично принимаем товар по номеру поставки в Аксапте на {amount} паллет")
    public void initialReceiveItemByExternalRequestId(String externalRequestId, Integer amount) {
        enterPrinterAndGateInInitialReceiving()
                .enterExternalRequestId(externalRequestId)
                .enterPalletQty(amount.toString());
        Assertions.assertEquals("Было напечатано " + amount + " этикеток", notificationDialog.getMessage(),
                "Ошибка: При первичной приемке");
    }

    @Step("Первично принимаем возвратную поставку")
    public void initialReceiveReturnBox(String fulfillmentId, String containerID) {
        enterPrinterAndGateInInitialReceiving()
                .enterReturnInboundId(fulfillmentId)
                .enterContainerId(containerID);
    }

    @Step("Принимаем товар c паллеты {pallet} на тележку {cart}")
    public BarcodeInputPage receiveItem(Item item, String pallet, String cart, boolean isAnomaly) {
        return receiveItem(item, pallet, cart, isAnomaly, true);
    }

    @Step("Принимаем товар c паллеты {pallet} на тележку {cart}")
    public BarcodeInputPage receiveItem(RegistryItemWrapper item, String pallet, String cart, boolean isAnomaly) {
        return receiveItem(item, pallet, cart, isAnomaly, true);
    }

    @Step("Принимаем товар c паллеты {pallet} на тележку {cart}")
    public BarcodeInputPage receiveItem(Item item,
                                        String pallet,
                                        String cart,
                                        boolean isAnomaly,
                                        boolean allowQty) {
        enterTableAndPrinter();

        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(pallet)
                .clickForwardButton();

        return doReceive(item, cart, isAnomaly, allowQty, barcodeInputPage);
    }

    @Step("Принимаем товар c паллеты {pallet} на тележку {cart}")
    public BarcodeInputPage receiveItem(RegistryItemWrapper item,
                                        String pallet,
                                        String cart,
                                        boolean isAnomaly,
                                        boolean allowQty) {
        if (!driver.getCurrentUrl().endsWith("/ui/")) {
            menuPage();
        }
        Receiving receiving = new Receiving(driver, inboundTable);
        receiving.enterTableAndPrinter();

        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(pallet)
                .clickForwardButton();

        return doReceive(item, cart, isAnomaly, allowQty, barcodeInputPage);
    }

    @Step("Принимаем товар c паллеты {pallet} на тележку {cart}")
    public ReceiptInputPage receiveAnomaly(RegistryItemWrapper item,
                                           String pallet,
                                           String cart, NonconformityType nonconformityType) {
        enterTableAndPrinter();

        return new ReceiptInputPage(driver)
                .enterPallet(pallet)
                .clickForwardButton()
                .enterBarcode(item)
                .clickAnomaly()
                .selectAnomalyType(nonconformityType)
                .enterCart(cart)
                .closePallet();
    }

    @Step("Принимаем товары c паллеты {pallet} на тележку {cart}")
    public BarcodeInputPage receiveItems(List<Item> items,
                                         String pallet,
                                         String cart,
                                         boolean isAnomaly,
                                         boolean allowQty) {
        enterTableAndPrinter();
        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(pallet)
                .clickForwardButton();
        items.forEach(it -> doReceive(it, cart, isAnomaly, allowQty, barcodeInputPage));
        return barcodeInputPage;
    }

    @Step("Принимаем товар из возвратной коробки {box} на тележку {cart}")
    public BarcodeInputPage receiveReturnItem(Item item, String box, String cart, boolean isDamaged) {
        enterTableAndPrinter();

        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButton();

        return barcodeInputPage.receiveReturnInstance(item, cart, isDamaged);
    }

    @Step("Принимаем товар из возвратной коробки {box}, для которой нужно выбрать атрибуты качества, на тележку {cart}")
    public BarcodeInputPage receiveReturnItemFromBoxWithAttrPage(Item item, String box, String cart,
                                                                 boolean isDamaged) {
        enterTableAndPrinter();

        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButtonForPalletFlow()
                .clickSaveButtonForSecondaryPalletFlow();

        return barcodeInputPage.receiveReturnInstance(item, cart, isDamaged);
    }

    @Step("Принимаем товар из возвратной коробки {box} в аномальную тару {cart} с некорректным EAN")
    public BarcodeInputPage receiveReturnItemWithIncorrectEan(String box, String cart) {

        return enterPrinterTablePallet(box)
                .enterWrongBarcode(UUID.randomUUID().toString())
                .clickAnomaly()
                .enterCart(cart);
    }

    @Step("Принимаем товар из невыкупной коробки {box}, для которой нужно выбрать атрибуты качества, " +
            "в аномальную тару {cart} с некорректным EAN")
    public BarcodeInputPage receiveUnredeemedItemWithIncorrectEanFromBoxWithAttrPage(String box, String cart) {
        enterTableAndPrinter();

        return new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButtonForPalletFlow()
                .clickSaveButtonForSecondaryPalletFlow()
                .enterWrongBarcode(UUID.randomUUID().toString())
                .clickAnomaly()
                .enterCart(cart);
    }

    @Step("Принимаем товар, несоответствующий описанию, из возвратной коробки {box} в аномальную тару {cart}")
    public BarcodeInputPage receiveReturnItemWithIncorrectDescription(Item item, String box, String cart) {

        enterTableAndPrinter();

        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButton();

        return barcodeInputPage.receiveReturnInstanceWithIncorrectDescription(item)
                .enterCart(cart);
    }

    @Step("Принимаем товар из возвратной коробки {box} в анамальную тару {cart} c незаявленным IMEI")
    public BarcodeInputPage receiveReturnItemWithNotDeclaredIdentity(Item item, String box, String cart) {

        BarcodeInputPage barcodeInputPage = enterPrinterTablePallet(box);

        return barcodeInputPage.receiveReturnInstance(item, cart, false, true);
    }

    @Step("Принимаем невыкуп c незаявленным идентификатором из невыкупной коробки {box}, " +
            "для которой нужно выбрать атрибуты качества, в аномальную тару {cart}")
    public BarcodeInputPage receiveUnredeemedItemWithNotDeclaredIdentityFromBoxWithAttrPage(
            Item item, String box, String cart) {
        enterTableAndPrinter();

        return new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButtonForPalletFlow()
                .clickSaveButtonForSecondaryPalletFlow()
                .receiveReturnInstance(item, cart, false, true);
    }

    @Step("Принимаем товар из возвратной коробки {box} в аномальную тару {cart} с незаявленным штрихкодом")
    public BarcodeInputPage receiveReturnItemWithNotDeclaredEan(Item item, String box, String cart, boolean isDamaged) {

        return enterPrinterTablePallet(box)
                .enterNotDeclaredBarcode(item.getArticle())
                .clickAnomaly()
                .enterCart(cart);
    }

    @Step("Вводим принтер, стол и палету")
    public BarcodeInputPage enterPrinterTablePallet(String box) {
        enterTableAndPrinter();

        return new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButton();
    }

    @Step("Принимаем товар из возвратной коробки {box} сразу в аномальную тару {cart} (Без ШК)")
    public BarcodeInputPage receiveReturnItemAnomaly(String box, String cart) {

        return enterPrinterTablePallet(box)
                .clickAnomaly()
                .enterCart(cart);
    }

    @Step("Принимаем ещё один товар из возвратной коробки {box} на тележку {cart}")
    public BarcodeInputPage continueReceivingReturnItemFromEnteringPallet(Item item, String box, String cart,
                                                                          boolean isDamaged) {
        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButton();

        return new BarcodeInputPage(driver).receiveReturnInstance(item, cart, isDamaged);
    }

    @Step("Принимаем ещё один товар из возвратной коробки {box} на тележку {cart}")
    public BarcodeInputPage continueReceivingReturnItem(Item item, String box, String cart, boolean isDamaged) {
        return new BarcodeInputPage(driver).receiveReturnInstance(item, cart, isDamaged);
    }

    @Step("Принимаем еще одну единицу товара c паллеты {pallet} на тележку {cart}")
    public BarcodeInputPage continueReceiving(Item item,
                                              String cart,
                                              boolean isAnomaly,
                                              boolean allowQty) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        return doReceive(item, cart, isAnomaly, allowQty, barcodeInputPage);
    }

    @Step("Принимаем безУИТный товар c паллеты {fromPalletId} на паллету {toPalletId}")
    public void receiveVirtualUitItem(Item item, String fromPalletId, String toPalletId, Integer numOfBoxes,
                                      Integer quantityPerBox) {
        menuPage
                .inputReceivingVirtualUitPath()
                .enterTable(inboundTable.getStageCell())
                .enterPrinter(printer);
        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(fromPalletId)
                .clickForwardButton();

        barcodeInputPage.receiveVirtualUitInstance(item, toPalletId, numOfBoxes, quantityPerBox);
    }

    @Step("Принимаем безУИТный товар c паллеты {fromPalletId} на паллету {fromPalletId} и сортируем его")
    public BbxdConfirmPage receiveVirtualUitItemAndSort(Item item,
                                                        String fromPalletId,
                                                        Integer numOfBoxes,
                                                        Integer quantityPerBox,
                                                        String sortingTable) {
        menuPage
                .sortingOrderPath()
                .enterTable(sortingTable)
                .enterPrinter(printer);
        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(fromPalletId)
                .clickForwardButton();

        return barcodeInputPage.receiveVirtualUitAndStartSortingBbxd(item, numOfBoxes, quantityPerBox);
    }

    @Step("Принимаем безУИТный товар c паллеты {fromPalletId} на паллету {fromPalletId} и сортируем его")
    public void receiveVirtualUitItem(Item item,
                                      String fromPalletId,
                                      Integer numOfBoxes,
                                      Integer quantityPerBox,
                                      String table) {
        menuPage
                .sortingOrderPath()
                .enterTable(table)
                .enterPrinter(printer);
        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(fromPalletId)
                .clickForwardButton();

        barcodeInputPage.receiveVirtualUitBbxd(item, numOfBoxes, quantityPerBox);
    }

    @Step("Сортируем товар")
    public void sortBbxd(Integer numOfBoxes,
                         String dropId,
                         String sortingTable,
                         String fromPalletId) {
        menuPage
                .sortingOrderPath()
                .enterTable(sortingTable)
                .enterPrinter(printer);
        new ReceiptInputPage(driver)
                .enterPallet(fromPalletId);
        new BbxdConfirmPage(driver)
                .confirmAmount(numOfBoxes)
                .enterDropId(dropId);
    }

    @Step("Приемка на конвейере")
    public BarcodeInputPage receivingOnConveyor(Item item, String fromPalletId, String id) {
        enterTableAndPrinter();

        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(fromPalletId)
                .clickForwardButton();

        barcodeInputPage.receiveInstance(item, id, true);
        return barcodeInputPage;
    }

    @Step("Приемка на конвейере с вложенностью тар")
    public BarcodeInputPage receivingOnConveyorWithNesting(Item item, String fromPalletId, String flipboxId,
                                                           String id) {
        enterTableAndPrinter();

        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(fromPalletId)
                .clickForwardButton();

        barcodeInputPage.receiveInstanceWithNesting(item, flipboxId, id, true);
        return barcodeInputPage;
    }

    @Step("Принимаем паллету {pallet}")
    public BarcodeInputPage receivePallet(String pallet) {
        enterTableAndPrinter();
        BarcodeInputPage barcodeInputPage = new ReceiptInputPage(driver)
                .enterPallet(pallet)
                .clickForwardButton();
        return barcodeInputPage;
    }

    @Step("Принимаем часть многоместного товара {name}")
    public BarcodeInputPage receiveBom(Item item, String name, String cart) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver)
                .receiveMnogoboxInstance(item, name, cart);
        return barcodeInputPage;
    }

    @Step("Закрываем тару")
    public void closeCart() {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        barcodeInputPage.closeCart();
    }

    @Step("Выбираем дефолтную тару {cart} для паллеты {pallet} ")
    public BarcodeInputPage enterDefaultCart(Item item, String pallet, String cart) {
        enterTableAndPrinter();
        return new ReceiptInputPage(driver)
                .enterPallet(pallet)
                .clickDefaultCartButton()
                .enterCart(cart);
    }

    @Step("Проверяем, что находимся на странице BarcodeInputPage")
    public void isBarcodeInputPageDisplayed() {
        new BarcodeInputPage(driver);
    }

    @Step("Впервые принимаем единицу товара с СГ [Шаблон: Только дата окончания]")
    public BarcodeInputPage receiveItemWithExpDateFirstTime(Item item) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        barcodeInputPage.enterBarcode(item)
                .checkInfo(item)
                .enterQuantity(item.getQuantity());
        ShelfLifePage shelfLifePage = new ShelfLifePage(driver, item);
        return shelfLifePage.enterExpirationDate(item)
                .clickExpCheckBox()
                .confirm();
    }


    @Step("Впервые принимаем единицу товара с СГ в невыкупах [Шаблон: Только дата окончания]")
    public BarcodeInputPage receiveUnredeemedExpiredItem(Item item, String box, String cart) {
        enterTableAndPrinter();

        new ReceiptInputPage(driver)
                .enterPallet(box)
                .clickForwardButtonForPalletFlow()
                .clickSaveButtonForSecondaryPalletFlow()
                .enterBarcode(item)
                .checkInfo(item)
                .confirmWithoutChecks();

        new ShelfLifePage(driver, item)
                .enterExpirationDate(item)
                .clickExpCheckBox()
                .confirmAnomaly();

        new SkuWarehouseDamagePage(driver)
                .selectNotDamaged();

        final CartInputPage cartInputPage = new CartInputPage(driver);
        final String instance = cartInputPage.getInstance();
        cartInputPage.enterCart(cart);

        return new BarcodeInputPage(driver);
    }

    @Step("Повторно принимаем единицу товара с СГ. Вводим только дату окончания")
    public void receiveItemWithExpDateSecondTime(Item item) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        barcodeInputPage.enterBarcode(item)
                .enterQuantity(item.getQuantity());
        ShelfLifePage shelfLifePage = new ShelfLifePage(driver, item);
        shelfLifePage.checkToExpireDaysQuantity(item.getToExpireDaysQuantity())
                .enterExpirationDate(item)
                .confirm();
    }

    @Step("Впервые принимаем единицу товара с СГ [Шаблон: Дата производства + Дата окончания]")
    public void receiveItemWithCreationAndExpDateFirstTime(Item item) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        barcodeInputPage.enterBarcode(item)
                .checkInfo(item)
                .enterQuantity(item.getQuantity());
        ShelfLifePage shelfLifePage = new ShelfLifePage(driver, item);
        shelfLifePage.enterCreationDate(item)
                .enterExpirationDate(item)
                .confirm();
    }

    @Step("Впервые принимаем единицу товара с СГ [Шаблон: Дата окончания + срок годности]")
    public void receiveItemWithDurationAndExpDateFirstTime(Item item, String durationInMonths) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        barcodeInputPage.enterBarcode(item)
                .checkInfo(item)
                .enterQuantity(item.getQuantity());
        ShelfLifePage shelfLifePage = new ShelfLifePage(driver, item);
        shelfLifePage.enterDurationInMonths(durationInMonths)
                .enterExpirationDate(item)
                .confirm();
    }

    @Step("Впервые принимаем единицу товара с СГ [Шаблон: Дата производства + срок годности]")
    public void receiveItemWithDurationAndCreationDateFirstTime(Item item, String durationInMonths) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        barcodeInputPage.enterBarcode(item)
                .checkInfo(item)
                .enterQuantity(item.getQuantity());
        ShelfLifePage shelfLifePage = new ShelfLifePage(driver, item);
        shelfLifePage.enterDurationInMonths(durationInMonths)
                .enterCreationDate(item)
                .confirm();
    }

    @Step("Повторно принимаем единицу товара с СГ [Шаблон: Дата производства + срок годности]")
    public void receiveItemWithDurationAndCreationDateSecondTime(Item item) {
        BarcodeInputPage barcodeInputPage = new BarcodeInputPage(driver);
        barcodeInputPage.enterBarcode(item)
                .enterQuantity(item.getQuantity());
        ShelfLifePage shelfLifePage = new ShelfLifePage(driver, item);
        shelfLifePage.checkToExpireDaysQuantity(item.getToExpireDaysQuantity())
                .enterCreationDate(item)
                .confirm();
    }

    public void putItemOnConveyor(BarcodeInputPage barcodeInputPage, String totId) {
        barcodeInputPage.putItemOnConveyor();
        CloseContainerInputPage closeContainerInputPage = new CloseContainerInputPage(driver);
        closeContainerInputPage.enterContainer(totId);
        ChooseConveyorZonePage chooseConveyorZonePage = new ChooseConveyorZonePage(driver);
        chooseConveyorZonePage.clickFirstFloorZone();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Успех"),
                "Не появился диалог об успешном создании транспортного ордера");

        notificationDialog.waitUntilHidden();
    }


    @Step("Находим приемку в админке в ui")
    public SuppliesListPage findInboundInReceivingAdmin(String fulfillmentId) {
        return menuPage
                .inputReceivingAdminPath()
                .filterByReceiptKey(fulfillmentId);
    }

    @Step("Находим паллету ПУО-приёмки {fulfillmentId} в ui")
    public String findPalletOfInbound(String fulfillmentId) {
        return findInboundInReceivingAdmin(fulfillmentId)
                .openFirstReceipt()
                .switchToPalletReceivingDetailsTab()
                .selectPalletAndCloseTab();
    }

    @Step("Закрываем ПУО-приёмку в ui")
    public SuppliesListPage closeInboundOnSuppliesListPage() {
        new SuppliesListPage(driver)
                .selectFirstResult()
                .clickMoreActionsButton()
                .clickCloseReceiptButton();
        new ModalWindow(driver).clickSubmit();
        Assertions.assertTrue(
                notificationDialog.isPresentWithTitleCustomTimeout("ПУО приёмки закрыты", closePuoTimeout),
                "Не появился диалог с заголовком \"ПУО приёмки закрыты\"");

        notificationDialog.waitUntilHidden();
        return new SuppliesListPage(driver);
    }

    @Step("Закрываем ПУО-приёмку с проверкой в ui")
    public SuppliesListPage approveCloseInboundOnSuppliesListPage(SuppliesListPage suppliesListPage) {
        suppliesListPage
                .selectFirstResult()
                .clickMoreActionsButton()
                .clickApproveClosePuoButton();
        new ModalWindow(driver).clickSubmit();
        Assertions.assertTrue(
                notificationDialog.isPresentWithTitleCustomTimeout("ПУО приёмки закрыты с проверкой", closePuoTimeout),
                "Не появился диалог с заголовком \"ПУО приёмки закрыты с проверкой\"");

        notificationDialog.waitUntilHidden();
        return suppliesListPage;
    }

    @Step("Закрываем ПУО-приёмку с проверкой в ui с предупреждением")
    public SuppliesListPage approveCloseInboundWithWarning(SuppliesListPage suppliesListPage) {
        suppliesListPage
                .selectFirstResult()
                .clickMoreActionsButton()
                .clickApproveClosePuoButton();

        new ModalWindow(driver).clickSubmit();

        new ModalWindow(driver)
                .selectFirstResult()
                .clickForward();

        Assertions.assertTrue(
                notificationDialog.isPresentWithTitleCustomTimeout("Выбранные поставки закрыты с проверкой", closePuoTimeout),
                "Не появился диалог с заголовком \"ПУО приёмки закрыты с проверкой\"");

        notificationDialog.waitUntilHidden();
        return suppliesListPage;
    }

    @Step("Закрываем первичную ПУО-приёмку в ui")
    public void closeInitialInboundOnSuppliesListPage(SuppliesListPage suppliesListPage) {
        suppliesListPage
                .openFirstReceipt()
                .clickMoreActionsButton()
                .clickCloseInitialReceiptButton();

    }

    @Step("Отменяем приемку УИТа {uit}")
    public void cancelUitReceiving(String uit) {
        menuPage.inputCancelUitReceivingPath()
                .enterUitToCancel(uit)
                .deleteUit();
    }

    @Step("Открыть меню")
    public MenuPage menuPage() {
        driver.get(Urls.getMenuPage());
        return menuPage;
    }

    private BarcodeInputPage doReceive(Item item,
                                       String cart,
                                       boolean isAnomaly,
                                       boolean allowQty,
                                       BarcodeInputPage barcodeInputPage) {
        return isAnomaly ? barcodeInputPage.receiveInstanceAsAnomaly(item, cart) :
                barcodeInputPage.receiveInstance(item, cart, allowQty);
    }

    public ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving.ReceiptInputPage enterPrinterAndGateInInitialReceiving() {
        menuPage
                .inputInitialReceivingPath()
                .enterPrinter(printer);
        new GateInputPage(driver)
                .enterTable(inboundTable.getDoorCell());

        return new ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving.ReceiptInputPage(
                driver);
    }

    public DiscrepanciesReport openInitialReceivingDiscrepanciesReport(String fulfillmentId) {
        return menuPage
                .inputReceivingAdminPath()
                .filterByReceiptKey(fulfillmentId)
                .openFirstReceipt()
                .switchToSuppliesDetailsInformationTab()
                .openInitialReceivingDiscrepanciesReport(fulfillmentId);
    }


    @Step("Принимаем на паллету {pallet} товар с неверным ШК")
    public void receiveItemWithWrongBarcode(String pallet) {
        enterTableAndPrinter();
        new ReceiptInputPage(driver)
                .enterPallet(pallet)
                .clickForwardButton()
                .enterWrongBarcode(UUID.randomUUID().toString());
    }

    @Step("Переносим паллету {pallet} в локацию {loc}")
    public void moveReceivingPallet(String pallet, String loc) {
        menuPage
                .receivingPalletMovePath()
                .enterPallet(pallet)
                .enterCell(loc)
                .clickOk();

    }

    @Step("Связываем заказ с коробками")
    public void linkBoxWithOrderId(String boxId, String orderId) {
        menuPage.linkOrderIdToBoxPath()
                .enterBoxId(boxId)
                .enterOrderId(orderId);
    }

    private boolean tableSelectedAutomatically(String menuPageUrl) {
        WebDriverWait wait = new WebDriverWait(driver, WebDriverTimeout.MEDIUM_WAIT_TIMEOUT);
        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(menuPageUrl)));
        String currentUrl = driver.getCurrentUrl();
        Pattern pattern = Pattern.compile("printerInput.*");
        Matcher matcher = pattern.matcher(currentUrl);
        return matcher.find() || notificationDialog.IsPresentWithMessage("Автовыбор стола");
    }

    private void enterTableAndPrinter() {
        String menuPageUrl = driver.getCurrentUrl();
        menuPage
                .inputReceivingPath();
        if (tableSelectedAutomatically(menuPageUrl)) {
            new PrinterInputPage(driver)
                    .enterPrinter(printer);
        } else {
            new TableInputPage(driver)
                    .enterTable(inboundTable.getStageCell())
                    .enterPrinter(printer);
        }
    }

    private BarcodeInputPage doReceive(RegistryItemWrapper item,
                                       String cart,
                                       boolean isAnomaly,
                                       boolean allowQty,
                                       BarcodeInputPage barcodeInputPage) {
        return isAnomaly ? barcodeInputPage.receiveInstanceAsAnomaly(item, cart) :
                barcodeInputPage.receiveInstance(item, cart, allowQty);
    }

    private BarcodeInputPage receiveAnomaly(RegistryItemWrapper item,
                                            String cart,
                                            BarcodeInputPage barcodeInputPage) {
        return barcodeInputPage.receiveInstanceAsAnomaly(item, cart);
    }
}
