package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;


import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.StockType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: BigWithdrawal Order")
@Epic("Selenium Tests")
@Slf4j
public class BigWithdrawalOrderTest extends AbstractUiTest {

    private final String ITEM_ARTICLE = UniqueId.getString();
    private final Item ITEM = Item.builder()
            .sku(ITEM_ARTICLE)
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .quantity(1)
            .build();
    private String areaKey;
    private String pickingZone;
    private String packingZone;
    private String shippingZone;
    private String storageCell;
    private String withdrawalOversizeConsolidationCell;
    private String packingTable;
    private String shippingDoor;
    private String shippingWithdrawalCell;
    private String otherShippingWithdrawalCell;
    private String droppingCell;

    private WaveId waveId;

    @BeforeEach
    @Step("Подготовка: Создаем участок, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
    public void setUp() throws Exception {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        packingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        shippingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ячейки отбора, консолидации и стол упаковки
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        withdrawalOversizeConsolidationCell = DatacreatorSteps.Location()
                .createWithdrawalOversizeConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingWithdrawalCell = DatacreatorSteps.Location().createShippingWithdrawalCell(shippingZone);
        otherShippingWithdrawalCell = DatacreatorSteps.Location().createShippingWithdrawalCell(shippingZone);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(withdrawalOversizeConsolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(shippingDoor);
        DatacreatorSteps.Location().deleteCell(shippingWithdrawalCell);
        DatacreatorSteps.Location().deleteCell(otherShippingWithdrawalCell);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(packingZone);
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
        DatacreatorSteps.Location().deletePutawayZone(shippingZone);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey);

        if (waveId != null) {
            uiSteps.Login().PerformLogin();
            uiSteps.Wave().unreserveWave(waveId);
        }
    }

    @RetryableTest
    @Tag("AutostartReleaseSuite")
    @Tag("ShippingReleaseSuite")
    @Tag("AutostartMultitestingSuite")
    @Tag("ShippingMultitestingSuite")
    @DisplayName("Тест создания и запуска волны для больших изъятий с годного стока")
    @ResourceLock("Тест создания и запуска волны для больших изъятий с годного стока")
    public void bigWithdrawalFitWithManualWaveTest() {
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound = ApiSteps.Outbound().createOutbound(ITEM);
        Order withdrawalOrder = new Order(outbound.getYandexId(), outbound.getFulfillmentId());
        waveId = processSteps.Outgoing().createAndForceStartBigWithdrawalWaveManually(withdrawalOrder, withdrawalOversizeConsolidationCell);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveContainerToLine(containerLabel, withdrawalOversizeConsolidationCell);

        List<ParcelId> packs = processSteps.Outgoing().packOrderFromContainer(itemSerials, packingTable, containerLabel);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);

        processSteps.Outgoing().shipWithdrawals(outbound, dropId, shippingDoor, shippingWithdrawalCell);
    }

    @RetryableTest
    @Tag("AutostartReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("PickingReleaseSuite")
    @Tag("AutostartMultitestingSuite")
    @Tag("PackingMultitestingSuite")
    @Tag("PickingMultitestingSuite")
    @DisplayName("Тест создания и запуска волны для больших изъятий с бракованного стока")
    @ResourceLock("Тест создания и запуска волны для больших изъятий с бракованного стока")
    public void bigWithdrawalDefectWithManualWaveTest() {
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell);

        String lotForBlocking = DatacreatorSteps
                .Items()
                .getItemLotByArticleLoc(ITEM.getArticle(), storageCell);
        DatacreatorSteps.Items().placeHoldOnLot(lotForBlocking, InventoryHoldStatus.DAMAGE);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound = ApiSteps.Outbound().createOutbound(ITEM, StockType.DEFECT);
        Order withdrawalOrder = new Order(outbound.getYandexId(), outbound.getFulfillmentId());
        waveId = processSteps.Outgoing().createAndForceStartBigWithdrawalWaveManually(withdrawalOrder, withdrawalOversizeConsolidationCell);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveContainerToLine(containerLabel, withdrawalOversizeConsolidationCell);

        List<ParcelId> packs = processSteps.Outgoing().packOrderFromContainer(itemSerials, packingTable, containerLabel);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);

        processSteps.Outgoing().shipWithdrawals(outbound, dropId, shippingDoor, shippingWithdrawalCell);
    }

    @RetryableTest
    @Tag("ConsolidationReleaseSuite")
    @Tag("DroppingReleaseSuite")
    @Tag("ShippingReleaseSuite")
    @Tag("ConsolidationMultitestingSuite")
    @Tag("DroppingMultitestingSuite")
    @Tag("ShippingMultitestingSuite")
    @DisplayName("Тест создания и запуска волны для больших изъятий с просроченного стока")
    @ResourceLock("Тест создания и запуска волны для больших изъятий с просроченного стока")
    public void bigWithdrawalExpiredWithManualWaveTest() {
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell);

        String lotForBlocking = DatacreatorSteps
                .Items()
                .getItemLotByArticleLoc(ITEM.getArticle(), storageCell);
        DatacreatorSteps.Items().placeHoldOnLot(lotForBlocking, InventoryHoldStatus.EXPIRED);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound = ApiSteps.Outbound().createOutbound(ITEM, StockType.EXPIRED);
        Order withdrawalOrder = new Order(outbound.getYandexId(), outbound.getFulfillmentId());
        waveId = processSteps.Outgoing().createAndForceStartBigWithdrawalWaveManually(withdrawalOrder, withdrawalOversizeConsolidationCell);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveContainerToLine(containerLabel, withdrawalOversizeConsolidationCell);

        List<ParcelId> packs = processSteps.Outgoing().packOrderFromContainer(itemSerials, packingTable, containerLabel);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);

        processSteps.Outgoing().shipWithdrawals(outbound, dropId, shippingDoor, shippingWithdrawalCell);
    }

    @RetryableTest
    @Tag("AutostartReleaseSuite")
    @Tag("AutostartMultitestingSuite")
    @DisplayName("Тест создания и запуска волны для больших изъятий с годного стока для КГТ")
    @ResourceLock("Тест создания и запуска волны для больших изъятий с годного стока для КГТ")
    public void bigWithdrawalFitNonSortWithManualWaveTest() {
        processSteps.Incoming().acceptNonSortItemsAndPlaceThemToPickingCell(ITEM, storageCell);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound = ApiSteps.Outbound().createOutbound(ITEM);
        Order withdrawalOrder = new Order(outbound.getYandexId(), outbound.getFulfillmentId());
        waveId = processSteps.Outgoing().createAndForceStartBigWithdrawalWaveManually(withdrawalOrder, withdrawalOversizeConsolidationCell);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveContainerToLine(containerLabel, withdrawalOversizeConsolidationCell);

        List<ParcelId> packs = processSteps.Outgoing().packOrderFromContainer(itemSerials, packingTable, containerLabel);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);

        processSteps.Outgoing().shipWithdrawals(outbound, dropId, shippingDoor, shippingWithdrawalCell);
    }
}
