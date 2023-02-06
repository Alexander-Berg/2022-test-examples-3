package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SorterExit;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.consolidation.order.UitPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.deliverySorting.SortingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing.PackingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing.PrinterPackingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing.TablePackingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.CartInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.CheckItemCardPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.CheckLocationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.ContainerInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.ContainerLabelInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.DropContainerInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.FinishPickingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.LocationInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.MoveItemPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.WorkingAreaInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.precons.ConsLineInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.precons.ContainerConsInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping.PlaceDropIdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave.OrdersListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation.Inbound;

@Resource.Classpath({"wms/infor.properties"})
public class Order {
    private static final Logger log = LoggerFactory.getLogger(Inbound.class);

    private WebDriver driver;
    private MenuPage menuPage;
    private NotificationDialog notificationDialog;

    @Property("infor.printerid")
    private String printerId;

    public Order(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
        this.notificationDialog = new NotificationDialog(driver);
        PropertyLoader.newInstance().populate(this);
    }

    public List<String> pickAndFinishAssignment() {
        List<String> pickedItems = pickAssignmentItems();
        finishAssignmentAndRefuseNextTask();
        return pickedItems;
    }

    public void pickAndFinishAssignment(String id) {
        pickIdWithAssignmentItems(id);
        finishAssignmentAndRefuseNextTask();
    }

    public List<String> pickAndFinishAssignmentAndTakeNextTask() {
        List<String> pickedItems = pickAssignmentItems();
        finishAssignmentAndAcceptNextTask();
        return pickedItems;
    }

    @Step("Сбрасываем тару отбора в PICKTO и отказываемся от следующего назначения")
    public void finishAssignmentAndRefuseNextTask() {
        new FinishPickingPage(driver)
                .enterPicktoLoc("PICKTO")
                .refuseNextAssignmentInCurrentArea();
    }

    @Step("Сбрасываем тару отбора в PICKTO и берем от следующее назначения")
    public void finishAssignmentAndAcceptNextTask() {
        new FinishPickingPage(driver)
                .enterPicktoLoc("PICKTO")
                .acceptNextAssignmentInCurrentArea();
    }

    @Step("Отбираем товары назначения")
    public List<String> pickAssignmentItems() {
        List<String> allAssignmentSerials = new ArrayList<>();

        while (pickingContinues()) {
            CheckItemCardPage chosenCellPage = new CheckLocationPage(driver).enterCellId();
            List<String> serialsPickedFromCell = chosenCellPage.enterSerialNumbers();
            allAssignmentSerials = Stream.of(allAssignmentSerials, serialsPickedFromCell).flatMap(Collection::stream)
                    .collect(Collectors.toList());
            log.info("Serials picked from current cell {}", serialsPickedFromCell);
            log.info("List of all serials picked {}", allAssignmentSerials);
        };

        return allAssignmentSerials;
    }

    @Step("Отбираем товары назначения по НЗН")
    public void pickIdWithAssignmentItems(String id) {
        while (pickingContinues()) {
            CheckItemCardPage chosenCellPage = new CheckLocationPage(driver).enterCellId();
            chosenCellPage.enterId(id);
            log.info("ID picked from current cell {}", id);
        };
    }

    private Boolean pickingContinues() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("checkLocationPage") || currentUrl.contains("moveItemCardPage");
    }

    /**
     * returns Что и где зашортили [cellName, lotNameEnding]
     */
    @Step("Отбираем назначение, но делаем шортирование")
    public Pair<String, String> doShortWhilePickingAssignmentItems() {
        return new CheckLocationPage(driver).enterCellId().doShortageOfItem(true);
    }

    @Step("Отбираем множественные назначения")
    public void pickMultiAssignment(Map<String, List<String>> containerItemsMap) {
        for (String containerLabel : containerItemsMap.keySet()) {
            MoveItemPage moveItemPage = new LocationInputPage(driver)
                    .enterCellId();
            int amountToPickFromThisCell = moveItemPage.getAmountToPickFromThisCell();
            for (int i = 0; i < amountToPickFromThisCell; i++) {
                moveItemPage
                        .enterSerialNumber(containerItemsMap.get(containerLabel))
                        .enterCart(containerLabel);
            }

            log.info("Picked serial numbers: {} in container: {}",
                    containerItemsMap.get(containerLabel), containerLabel);
        }
    }

    @Step("Предконсолидация одного контейнера")
    public void preConsOneCart(String containerLabel, String consolidationCell) {
        menuPage
                .inputPreconsPath();
        new ContainerConsInputPage(driver)
                .enterConsCart(containerLabel);
        new ConsLineInputPage(driver)
                .enterConsLine(consolidationCell);

    }

    @Step("Предконсолидация одного контейнера на предлагаемую линию")
    public void preConsOneCart(String containerLabel) {
        menuPage
                .inputPreconsPath();
        new ContainerConsInputPage(driver)
                .enterConsCart(containerLabel);
        new ConsLineInputPage(driver)
                .enterProposedConsLine();

    }

    @Step("Проверяем, что нет начатых отборов")
    public void verifyNoComplectationStarted(String area) {
        log.info("Validating that there is no started complectation");

        menuPage
                .inputPickingPath()
                .enterWorkingArea(area);

        Assertions.assertTrue(new WorkingAreaInputPage(driver).noComplectationStartedNotificationPresent(),
                "Ошибка: Не должно быть начатых отборов");
    }

    @Step("Получаем назначение")
    public void getAssignment(String areaKey, String containerLabel) {
        menuPage
                .inputPickingPath()
                .enterWorkingArea(areaKey);
        enterPickingCart(containerLabel);
    }

    @Step("Продолжаем взятое ранее назначение")
    public void resumeAssignment(String containerLabel) {
        menuPage.inputPickingPathWithActiveAssignment();
        enterPickingCart(containerLabel);
    }

    @Step("Вводим тару для отбора")
    public void enterPickingCart(String containerLabel) {
        new ContainerLabelInputPage(driver).enterCart(containerLabel);
    }

    @Step("Проверяем, что назначение выдалось из той же зоны")
    public void checkPutawayZoneOfAssignment(String putawayZoneKey) {
        new ContainerLabelInputPage(driver).checkArea(putawayZoneKey);
    }

    @Step("Ожидаем назначение, упорно пытаемся получить")
    public void getAssignmentWithRetry(String areaKey, String containerLabel) {
        WorkingAreaInputPage page = menuPage.inputPickingPath();
        String urlChooseArea = driver.getCurrentUrl();
        Retrier.retry(() -> {
            //Если в цикле уже сменится раздел, то не надо спрашивать назначение
            if (driver.getCurrentUrl().equals(urlChooseArea)) {
                page.enterWorkingArea(areaKey);
            }
            new ContainerLabelInputPage(driver).enterCart(containerLabel);
        }, Retrier.RETRIES_MEDIUM, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    @Step("Ожидаем назначение, упорно пытаемся получить")
    public void getWithdrawalAssignmentWithRetry(String areaKey, String containerLabel) {
        WorkingAreaInputPage page = menuPage.inputPickingWithdrawalPath();
        String urlChooseArea = driver.getCurrentUrl();
        Retrier.retry(() -> {
            //Если в цикле уже сменится раздел, то не надо спрашивать назначение
            if (driver.getCurrentUrl().equals(urlChooseArea)) {
                page.enterWorkingArea(areaKey);
            }
            new ContainerLabelInputPage(driver).enterCart(containerLabel);
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    @Step("Получаем назначение на изъятие")
    public void getWithdrawalAssignment(String areaKey, String containerLabel) {
        menuPage
                .inputPickingWithdrawalPath()
                .enterWorkingArea(areaKey);
        new ContainerLabelInputPage(driver).enterCart(containerLabel);
    }

    @Step("Получаем множественные назначения")
    public void getMultiAssignments(String areaKey, Set<String> containers) {
        menuPage
                .inputMultiPickingPath()
                .enterWorkingArea(areaKey);
        new ContainerInputPage(driver)
                .enterCarts(containers)
                .clickStartPickingButton();
    }

    @Step("Выполняем консолидацию по заказам")
    public void orderConsolidation(String sortingStation, String sortingCell, String containerLabel,
                                   List<String> itemSerials) {
        Map<String, String> itemSerialToSortingCellsMapping = mapItemSerialsToSortingCell(itemSerials, sortingCell);
        orderConsolidationBase(sortingStation, containerLabel, itemSerialToSortingCellsMapping, Collections.emptyList());
    }

    @Step("Выполняем консолидацию по заказам, часть шортируем")
    public void orderConsolidationWithShortage(String sortingStation, String sortingCell, String containerLabel,
                                               List<String> itemSerials, List<String> itemSerialsForShortage) {
        Map<String, String> itemSerialToSortingCellsMapping = mapItemSerialsToSortingCell(itemSerials, sortingCell);
        orderConsolidationBase(sortingStation, containerLabel, itemSerialToSortingCellsMapping, itemSerialsForShortage);
    }

    @Step("Выполняем консолидацию по нескольким заказам в разные ячейки")
    public void orderConsolidation(
            String sortingStation,
            String containerLabel,
            Map<String, String> itemSerialToSortingCellsMapping) {
        orderConsolidationBase(sortingStation, containerLabel, itemSerialToSortingCellsMapping, Collections.emptyList());
    }

    @Step("Выполняем консолидацию по не скольким заказам в разные ячейки, часть шортируем")
    public void orderConsolidationWithShortage(
            String sortingStation,
            String containerLabel,
            Map<String, String> itemSerialToSortingCellsMapping,
            List<String> itemSerialsForShortage) {
        orderConsolidationBase(sortingStation, containerLabel, itemSerialToSortingCellsMapping, itemSerialsForShortage);
    }

    private void orderConsolidationBase(
            String sortingStation,
            String containerLabel,
            Map<String, String> itemSerialToSortingCellsMapping,
            List<String> itemSerialsForShortage) {
        UitPage uitPage = menuPage
                .inputConsolidationOrderPath()
                .enterSortStation(sortingStation)
                .enterContainer(containerLabel);

        int scannedCount = 0;
        for (var entry : itemSerialToSortingCellsMapping.entrySet()) {
            String itemSerial = entry.getKey();
            String sortingCell = entry.getValue();
            if (itemSerialsForShortage.contains(itemSerial)) {
                continue;
            }

            uitPage
                    .enterUIT(itemSerial)
                    .enterCell(sortingCell, ++scannedCount == itemSerialToSortingCellsMapping.size());
        }

        if (!itemSerialsForShortage.isEmpty()) {
            uitPage.doShortageOfItems();
        }
    }

    @Step("Упаковываем товары")
    public void packGoods(String packingTable, List<String> snList, String containerLabel) {
        enterPackingTableAndPrinter(packingTable)
                .enterContainerIfNecessary(containerLabel)
                .enterUits(snList);
    }

    @Step("Упаковываем товары из нескольких контейнеров")
    public void packGoodsFromSeveralContainers(String packingTable, Map<String, List<String>> containerToUitsMap) {
        PackingPage packingPage = enterPackingTableAndPrinter(packingTable);
        for (int i = 0; i < containerToUitsMap.size(); i++) {
            String suggestedContainer = packingPage.enterSuggestedContainer();
            packingPage.enterUitsAndCheckForNextTask(containerToUitsMap.get(suggestedContainer));
        }
    }

    @Step("Упаковываем товары из ячейки {putwallCell}, и шортируем")
    public void packGoodsFromCellShortage(String packingTable, List<String> snList, String putwallCell, boolean isLastTask) {
        enterPackingTableAndPrinter(packingTable)
                .pickTaskForCell(putwallCell)
                .enterUitsAndDoShortage(snList, isLastTask);
    }

    @Step("Упаковываем товары")
    public void packGoodsFromContainer(String packingTable, List<String> snList, String containerLabel) {
        menuPage.inputPackingPath();
        new TablePackingPage(driver)
                .enterWorkingArea(packingTable)
                .enterPrinterForPackId(printerId)
                .enterContainer(containerLabel)
                .enterUit(snList);
    }

    @Step("Упаковываем НЗН")
    public void packId(String packingTable, String id) {
        menuPage.inputPackingPath();
        new TablePackingPage(driver)
                .enterWorkingArea(packingTable)
                .enterPrinterForPackId(printerId)
                .enterContainer(id)
                .packId(id);
    }

    @Step("Упаковываем товары из ячейки {putwallCell}")
    public void packGoodsFromCell(String packingTable, List<String> snList, String putwallCell) {
        enterPackingTableAndPrinter(packingTable)
                .pickTaskForCell(putwallCell)
                .enterUits(snList);
    }

    @Step("Упаковываем товары")
    public void packGoods(String packingTable, Map<String, String> itemSerialsToSortingCellsMapping) {
        enterPackingTableAndPrinter(packingTable)
                .enterUits(itemSerialsToSortingCellsMapping);
    }

    @Step("Упаковываем товары по одному в коробку")
    public void packGoodsOnePerPack(String packingTable, List<String> snList) {
        enterPackingTableAndPrinter(packingTable)
                .enterUitOnePerPack(snList);
    }

    @Step("Перекладываем в отмененку")
    public void moveCancelledItems(String packingTable, List<String> stuckItems, String containerForCancelledLabel) {
        enterPackingTableAndPrinter(packingTable)
                .moveCancelledItems(stuckItems, containerForCancelledLabel);
    }

    public DropId deliverySorting(ParcelId parcelId, String droppingCell) {
        return deliverySorting(List.of(parcelId), droppingCell);
    }

    @Step("Сортируем по СД")
    public DropId deliverySorting(List<ParcelId> parcelIds, String droppingCell) {
        log.info("Sorting by carrier");
        String dropId = "DRP" + parcelIds.get(0).getId().substring(3);
        SortingPage sortingPage = menuPage
                .inputDeliverySortingPath()
                .enterContainer(dropId)
                .enterDroppingCell(droppingCell);
        for (int i = 0; i < parcelIds.size(); i++) {
            sortingPage.enterParcel(parcelIds.get(i).getId());
        }
        return new DropId(dropId);
    }

    @Step("Сбрасываем контейнеры")
    public void dropContainers(Map<String, List<String>> containerItemsMap) {
        new CartInputPage(driver)
                .clickDropContainerButton();
        List<String> containerLabels = new ArrayList<>(containerItemsMap.keySet());

        for (int i = 0; i < containerLabels.size(); i++) {
            String containerLabel = containerLabels.get(i);
            new DropContainerInputPage(driver)
                    .enterCart(containerLabel, i == containerLabels.size() - 1);

        }
    }

    @Step("Сбрасываем контейнеры для синглов")
    public void dropContainersSingleOrders() {
        new CartInputPage(driver)
                .clickForwardButton();
    }

    public void checkSorterOrderLocation(String parcelId, SorterExit targetSorterLocation) {
        menuPage
                .inputSorterOrderManagementPath()
                .checkSorterOrderLocation(parcelId, targetSorterLocation.getSorterExitKey());
    }

    @Step("Размещаем в ячейку для отгрузки")
    public void shippingPlacement(DropId dropId, String shippingCell) {
        menuPage.inputShippingPath()
                .enterDropId(dropId.getId())
                .enterCell(shippingCell);
    }

    @Step("Размещаем в ячейку для отгрузки")
    public void shippingPlacement(List<DropId> dropIds, List<String> shippingCells) {
        Map<DropId, String> dropIdsWithShippingCells = new HashMap<>();
        for (var i = 0; i < dropIds.size(); i++) {
            dropIdsWithShippingCells.put(dropIds.get(i), shippingCells.get(i));
        }

        PlaceDropIdPage placeDropIdPage = menuPage.inputShippingPath();
        for (var  entry : dropIdsWithShippingCells.entrySet()) {
            DropId dropId = entry.getKey();
            String shippingCell = entry.getValue();
            placeDropIdPage
                    .enterDropId(dropId.getId())
                    .enterCell(shippingCell);
        }
    }

    @Step("Привязываем СД к воротам")
    public void connectDoorToCarrier(String door, String carrier) {
        menuPage.inputShippingDoorToDeliveryPath()
                .appendDeliveryIdForDoor(door, carrier);
    }

    @Step("Создаем отгрузку (стандартная)")
    public Long createShipmentStandard(String vehicle, String carrier, String door, String scheduledDepartureTime) {
        return menuPage.inputShipControlPath()
                .createStandardShipment(vehicle, carrier, door, scheduledDepartureTime);
    }

    @Step("Создаем отгрузку (изъятие)")
    public Long createShipmentWithdrawal(String vehicle, String withdrawalId, String door) {
        return menuPage.inputShipControlPath()
                .createWithdrawalShipment(vehicle, withdrawalId, door);
    }

    @Step("Создаем отгрузку (изъятие)")
    public Long createShipmentWithdrawal(String vehicle, Collection<String> withdrawalIds, String door) {
        return menuPage.inputShipControlPath()
                .createWithdrawalShipment(vehicle, withdrawalIds, door);
    }

    @Step("Отгружаем дропку")
    public void newShipping(String door, DropId dropId) {
        menuPage.inputNewShippingPath()
                .enterGate(door)
                .enterDropId(dropId.getId())
                .enterFinishGate(door);
    }

    @Step("Отгружаем дропки")
    public void newShipping(String door, Collection<DropId> dropIds) {
        List<String> ids = dropIds.stream()
                .map(DropId::getId)
                .collect(Collectors.toList());
        menuPage.inputNewShippingPath()
                .enterGate(door)
                .enterDropIds(ids)
                .enterFinishGate(door);
    }

    @Step("Отгружаем по номеру заказа")
    public void shipOrderByOrderKey(String orderKey) {
        menuPage.inputShippingOrderPath()
                .shipOrderByOrderKey(orderKey);
    }

    @Step("Завершаем отгрузку")
    public void closeShipment(Long shipmentId) {
        menuPage.inputShipControlPath()
                .closeShipment(shipmentId);
    }

    @Step("Проверяем, что у заказа {orderId} статус {status}")
    public void verifyOrderStatus(String orderId, OrderStatus status) {
        log.info("Verifying order {} status is {}", orderId, status);
        OrdersListPage ordersListPage = menuPage
                .inputOrdersListPath();
        OrderStatus actualOrderStatus = Retrier.retry(() -> ordersListPage.resetFiltersClick()
                .inputOrderId(orderId)
                .getOrderStatus(), Retrier.RETRIES_TINY, Retrier.RETRIES_SMALL, TimeUnit.SECONDS);
        checkOrderStatus(status, actualOrderStatus);
    }

    @Step("Ждём, пока статус заказа {orderId} станет {status}")
    public void waitOrderStatusIs(String orderId, OrderStatus status) {
        // Триггер смены статусов на тестингах настроен на интервал 1 минуту
        log.info("Verifying order {} status is {}", orderId, status);
        OrdersListPage ordersListPage = menuPage.inputOrdersListPath();
        Retrier.retry(() -> {
            OrderStatus actualOrderStatus = ordersListPage
                    .resetFiltersClick()
                    .inputOrderId(orderId)
                    .getOrderStatus();
            checkOrderStatus(status, actualOrderStatus);
        }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    @Step("Ждём, когда у заказа {orderId} totalQty станет {totalQty}")
    public void waitOrderTotalQtyIs(String orderId, int totalQty) {
        log.info("Verifying order {} totalQty is {}", orderId, totalQty);
        OrdersListPage ordersListPage = menuPage.inputOrdersListPath();
        Retrier.retry(() -> {
            int actualOrderTotalQty = ordersListPage
                    .resetFiltersClick()
                    .inputOrderId(orderId)
                    .getOrderTotalQty();
            Assertions.assertEquals(totalQty, actualOrderTotalQty,
                    String.format("Не дождались, чтобы у заказа %s число всех товаров стало %d, в таблице число %d",
                            orderId, totalQty, actualOrderTotalQty));
        }, Retrier.RETRIES_MEDIUM, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
    }


    @Step("Ждём, пока статус заказов {orderIds} станет {status}")
    public void waitOrderStatusIs(Collection<String> orderIds, OrderStatus status) {
        log.info("Verifying orders {} status is {}", orderIds, status);
        OrdersListPage ordersListPage = menuPage.inputOrdersListPath();
        for (String orderId : orderIds) {
            Retrier.retry(() -> {
                OrderStatus actualOrderStatus = ordersListPage
                        .resetFiltersClick()
                        .inputOrderId(orderId)
                        .getOrderStatus();
                checkOrderStatus(status, actualOrderStatus);
            }, Retrier.RETRIES_MEDIUM, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
        }
    }

    private boolean tableSelectedAutomatically(String menuPageUrl) {
        WebDriverWait wait = new WebDriverWait(driver, WebDriverTimeout.MEDIUM_WAIT_TIMEOUT);
        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(menuPageUrl)));
        String currentUrl = driver.getCurrentUrl();
        Pattern pattern = Pattern.compile("printerInput.*");
        Matcher matcher = pattern.matcher(currentUrl);
        return matcher.find() || notificationDialog.IsPresentWithMessage("Автовыбор стола");
    }

    private PackingPage enterPackingTableAndPrinter(String packingTable) {
        String menuPageUrl = driver.getCurrentUrl();
        menuPage
                .inputPackingPath();
        if (tableSelectedAutomatically(menuPageUrl)) {
            log.info("Automatically selected packing table {}", packingTable);
            return new PrinterPackingPage(driver)
                    .enterPrinter(printerId);
        } else {
            return new TablePackingPage(driver)
                    .enterWorkingArea(packingTable)
                    .enterPrinter(printerId);
        }
    }

    public int getCountOfItemsToPickInAssignment() {
        return new CheckLocationPage(driver).getLeftItemsCount();
    }

    public String getOrderFulfilmentIdByRowIndex(long yandexId, int rowIndex) {
        OrdersListPage ordersListPage = menuPage
                .inputOrdersListPath();
        return Retrier.retry(() -> ordersListPage
                        .resetFiltersClick()
                        .inputExternalOrderId(yandexId)
                        .getOrderFulfillmentIdByRowIndex(rowIndex),
                Retrier.RETRIES_TINY, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
    }

    private void checkOrderStatus(OrderStatus status, OrderStatus actualOrderStatus){
        Assertions.assertEquals(status, actualOrderStatus,
                String.format("Ожидали, что заказ будет в статусе %s, но по факту оказался %s",
                        status, actualOrderStatus));
    }

    private static Map<String, String> mapItemSerialsToSortingCell(List<String> itemSerials, String sortingCell) {
        return itemSerials.stream().collect(Collectors.toMap(Function.identity(), x -> sortingCell));
    }
}
