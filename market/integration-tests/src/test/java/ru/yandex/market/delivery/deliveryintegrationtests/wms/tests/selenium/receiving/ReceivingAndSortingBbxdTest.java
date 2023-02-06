package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.PutawayZoneType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;

@DisplayName("Selenium: Receiving/Dropping - Приёмка и сортировка без уит'ов")
@Epic("Selenium Tests")
@Slf4j
public class ReceivingAndSortingBbxdTest extends AbstractUiTest {

    private static final int NUM_OF_BOXES = 1;
    private static final int QUANTITY_PER_BOX = 1;
    private static final String CARRIER_CODE = "107";
    private static final String VIRTUAL_UIT_TEST_ITEM = "ReceivingVirtualUitTestItem";
    private static final int VENDOR_ID = 465852;

    private String palletId;
    private String dropId;
    private String dropLoc;
    private String sortingLoc;
    private String sortingTable;
    private Outbound outbound;
    private String sortingZone;
    private String shippingZone;
    private String shippingDoor;
    private String shippingCell;

    @BeforeEach
    void setUp() {
        setupTopology();
        DatacreatorSteps.Tasks().deleteBbxdTasks(user.getLogin());
        Inbound inbound = createInbound();
        outbound = createOutbound(inbound.getYandexId());
        palletId = processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
    }

    @Step("Создаем топологию")
    private void setupTopology() {
        DatacreatorSteps.Location().setCarrierPriority(CARRIER_CODE, 1000);

        areaKey = DatacreatorSteps.Location().createArea();

        shippingZone = DatacreatorSteps.Location().createPutawayZoneWithType(areaKey, PutawayZoneType.BBXD_SHIP);
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingBbxdCell(shippingZone);

        sortingZone = DatacreatorSteps.Location().createPutawayZoneWithType(areaKey, PutawayZoneType.BBXD_SORTER);
        dropLoc = DatacreatorSteps.Location().createBbxdDroppingCell(sortingZone);
        sortingLoc = DatacreatorSteps.Location().createBbxdSortingCell(sortingZone);
        sortingTable = DatacreatorSteps.Location().createInboundTable(sortingZone).getStageCell();

        dropId = DatacreatorSteps.Label().createDrop(CARRIER_CODE);
    }

    @Step("Создаем поставку")
    private Inbound createInbound() {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.BBXD_SUPPLY);
        ApiSteps.Inbound().putInboundRegistry("wms/servicebus/putInboundRegistry/putInboundRegistryBbxd.xml", inbound,
                VIRTUAL_UIT_TEST_ITEM, VENDOR_ID, false);
        return inbound;
    }

    @Step("Создаем изъятие")
    private Outbound createOutbound(long receiptYandexId) {
        long yandexId = UniqueId.get();
        String interval = String.format("%1$s/%1$s", DateUtil.currentDateTime());

        Outbound outbound = ApiSteps.Outbound().putOutboundBbxd(yandexId, interval, receiptYandexId, CARRIER_CODE);
        ApiSteps.Outbound().putOutboundRegistry(outbound.getYandexId(), outbound.getFulfillmentId(),
                VIRTUAL_UIT_TEST_ITEM, VENDOR_ID);
        return outbound;
    }

    @RetryableTest
    @DisplayName("Приемка и сортировка BBXD вместе")
    @ResourceLock("Приемка и сортировка BBXD вместе")
    void receiveAndSortCombined() {
        uiSteps.Login().PerformLogin();
        log.info(inboundTable.toString());
        uiSteps.Receiving().moveReceivingPallet(palletId, sortingLoc);

        uiSteps.Login().PerformLogin();
        uiSteps.Dropping().assignDropWithLoc(dropId, dropLoc);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveVirtualUitItemAndSort(getItem(), palletId, NUM_OF_BOXES, QUANTITY_PER_BOX, sortingTable)
                .confirmAmount(NUM_OF_BOXES)
                .enterDropId(dropId);

        uiSteps.Login().PerformLogin();
        processSteps.Outgoing()
                .shipOrder(new Order(outbound.getYandexId(), outbound.getFulfillmentId()), new DropId(dropId),
                        shippingDoor, shippingCell);
        processSteps.Outgoing().waitOrderStatusIs(outbound.getFulfillmentId(), OrderStatus.SHIPPED);
    }

    @RetryableTest
    @DisplayName("Приемка и сортировка BBXD отдельно")
    @ResourceLock("Приемка и сортировка BBXD отдельно")
    void receiveAndSortSeparated() {
        uiSteps.Login().PerformLogin();
        uiSteps.Dropping().assignDropWithLoc(dropId, dropLoc);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveVirtualUitItem(getItem(), palletId, NUM_OF_BOXES, QUANTITY_PER_BOX,
                        inboundTable.getStageCell());

        uiSteps.Login().PerformLogin();
        log.info(inboundTable.toString());
        uiSteps.Receiving().moveReceivingPallet(palletId, sortingLoc);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().sortBbxd(NUM_OF_BOXES, dropId, sortingTable, palletId);

        uiSteps.Login().PerformLogin();
        processSteps.Outgoing()
                .shipOrder(new Order(outbound.getYandexId(), outbound.getFulfillmentId()), new DropId(dropId),
                        shippingDoor, shippingCell);
        processSteps.Outgoing().waitOrderStatusIs(outbound.getFulfillmentId(), OrderStatus.SHIPPED);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(dropLoc);
        DatacreatorSteps.Location().deleteCell(sortingLoc);
        DatacreatorSteps.Location().deleteCell(sortingTable);
        DatacreatorSteps.Location().deleteCell(shippingDoor);
        DatacreatorSteps.Location().deleteCell(shippingCell);
        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(sortingZone);
        DatacreatorSteps.Location().deletePutawayZone(shippingZone);
        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey);

        //удаляем задание на сортировку
        //в случае ошибки в тесте оно зависнет и может зааффектить ретрай
        DatacreatorSteps.Tasks().deleteBbxdTasks(user.getLogin());
    }

    private Item getItem() {
        return Item.builder()
                .vendorId(VENDOR_ID)
                .article(VIRTUAL_UIT_TEST_ITEM)
                .name("Item Name")
                .build();
    }
}
