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

@DisplayName("Selenium: Pick and pack ID")
@Epic("Selenium Tests")
@Slf4j
public class PickIdTest extends AbstractUiTest {

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
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone, false);
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
    @Tag("PickingReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("PickingMultitestingSuite")
    @Tag("PackingMultitestingSuite")
    @DisplayName("Тест отбора и упаковки по НЗН")
    @ResourceLock("Тест отбора и упаковки по НЗН")
    public void pickAndPackId() {
        String inboundCartLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell, inboundCartLabel);

        String lotForBlocking = DatacreatorSteps
                .Items()
                .getItemLotByArticleLoc(ITEM.getArticle(), storageCell);
        DatacreatorSteps.Items().placeHoldOnLot(lotForBlocking, InventoryHoldStatus.PLAN_UTILIZATION);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound = ApiSteps.Outbound().createOutbound(ITEM, StockType.PLAN_UTILIZATION);
        Order withdrawalOrder = new Order(outbound.getYandexId(), outbound.getFulfillmentId());
        waveId = processSteps.Outgoing().createAndForceStartBigWithdrawalWaveManually(withdrawalOrder, withdrawalOversizeConsolidationCell);

        processSteps.Outgoing().pickSingleOutboundAssignment(areaKey, inboundCartLabel, containerLabel);
        processSteps.Outgoing().consolidateWaveContainerToLine(inboundCartLabel, withdrawalOversizeConsolidationCell);

        List<ParcelId> packs = processSteps.Outgoing().packId(inboundCartLabel, packingTable);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);

        processSteps.Outgoing().shipWithdrawals(outbound, dropId, shippingDoor, shippingWithdrawalCell);
    }
}
