package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.endtoend;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ApiOrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Cis;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Stock;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Transfer;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import static org.hamcrest.Matchers.is;

@Resource.Classpath("wms/test.properties")
@DisplayName("EndToEnd: Receiving return of item with cis, it's transfer and order")
@Epic("EndToEnd Tests")
@Slf4j
public class TransferWithCisTest extends AbstractUiTest {
    private final int CIS_HANDLE_MODE_ENABLED = 1;
    public static int CIS_CARGO_TYPE_REQUIRED = 980;
    private static final String UNREDEEMED_BOX_PREFIX = "P000";
    private static final String UNREDEEMED_PALLET_PREFIX = "SC_LOT_";
    private final String INBOUND_CART = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
    private final String CONTAINER_LABEL = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
    private final String ITEM_NAME = "e2eTransferItem";
    private final Cis CIS = Cis.of("04482716484736");
    private final String ITEM_ARTICLE = UniqueId.getString();
    private final Item DECLARED_ITEM = Item.builder()
            .sku(ITEM_ARTICLE)
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .name(ITEM_NAME)
            .checkCis(1)
            .instances(Map.of("CIS", CIS.getWithoutCryptoPart()))
            .build();
    private final Item ACTUAL_ITEM = Item.builder()
            .sku(ITEM_ARTICLE)
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .name(ITEM_NAME)
            .checkCis(1)
            .instances(Map.of("CIS", CIS.getFull()))
            .build();
    private String areaKey;
    private String pickingZone;
    private String packingZone;
    private String shippingZone;
    private String storageCell;
    private String otherStorageCell;
    private String consolidationCell;
    private String singlesConsolidationCell;
    private String packingTable;
    private String shippingCell;
    private String shippingDoor;
    private String droppingCell;
    private Order order;
    private WaveId waveId;

    @BeforeEach
    @Step("Подготовка: инициализация данных для теста")
    public void setUp() {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        packingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        shippingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ячейки отбора, консолидации и стол упаковки
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        otherStorageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        consolidationCell = DatacreatorSteps.Location().createConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // линия консолидации синглов
        singlesConsolidationCell = DatacreatorSteps.Location().createSinglesConsolidationCell(pickingZone);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingStandardCell(shippingZone);
    }

    @AfterEach
    @Step("Очистка данных после теста")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(consolidationCell);
        DatacreatorSteps.Location().deleteCell(singlesConsolidationCell);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(storageCell);
        DatacreatorSteps.Location().deleteCell(otherStorageCell);
        DatacreatorSteps.Location().deleteCell(shippingCell);
        DatacreatorSteps.Location().deleteCell(shippingDoor);

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

    @Step("Принимаем возвратную поставку и размещаем товар в ячейку отбора")
    private void receiveReturnInboundAndMoveItemToStorageCell() {
        final String boxId = generateUnredeemedBox();
        final String palletId = generateUnredeemedPallet();

        Inbound returnInbound = createCisUnredeemedInbound(boxId, palletId);

        uiSteps.Login().PerformLogin();
        processSteps.Incoming()
                .initiallyReceiveReturnContainersWithoutQualityAttributes(palletId);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveReturnItemFromBoxWithAttrPage(ACTUAL_ITEM, boxId, INBOUND_CART, false)
                .closePallet();
        ApiSteps.Inbound().verifyInboundStatusIs(returnInbound, InboundStatus.ACCEPTANCE.getId());

        uiSteps.Login().PerformLogin();
        final String lot = uiSteps.Balances().findLotByNzn(INBOUND_CART);
        final List<String> lotStatuses = DatacreatorSteps.Items().getStatusesByLot(lot);
        uiSteps.Items().verifyLotStatuses(
                Collections.singletonList(InventoryHoldStatus.CIS_QUAR.getCode()), lotStatuses
        );

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(INBOUND_CART, storageCell);
    }

    private Inbound createCisUnredeemedInbound(final String box, final String pallet) {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final String barcode = UniqueId.getString();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        ApiSteps.Inbound().putUnredeemedInboundRegistryWithCis(
                inbound,
                DECLARED_ITEM,
                box,
                order,
                DECLARED_ITEM.getArticle(),
                pallet,
                barcode,
                DECLARED_ITEM.getInstances().get("CIS"),
                CIS_HANDLE_MODE_ENABLED,
                CIS_CARGO_TYPE_REQUIRED
        );
        return inbound;
    }

    @Step("Создаем трансфер с блокировки CIS_QUAR на годный сток")
    private void createTransferFromCisQuarToFit() {
        Transfer transfer = ApiSteps.Transfer().createTransferWithCis(DECLARED_ITEM, Stock.CisQuar, Stock.Fit, 1);
        ApiSteps.Transfer().waitTransferStatusIs(transfer, Transfer.STATUS_COMPLETED);

        Allure.step("Проверяем детали трансфера", () -> {
            String prefix = "root.response.transferDetails.transferDetailsItems.transferDetailsItem.";
            String expectedActualField = "1";
            String expectedDeclaredField = "1";
            ValidatableResponse response = ApiSteps.Transfer().getTransferDetails(transfer);
            response
                    .body(prefix + "unitId.id",
                            is(DECLARED_ITEM.getArticle()))
                    .body(prefix + "unitId.vendorId",
                            is(String.valueOf(DECLARED_ITEM.getVendorId())))
                    .body(prefix + "unitId.article",
                            is(DECLARED_ITEM.getArticle()))
                    .body(prefix + "actual",
                            is(expectedActualField))
                    .body(prefix + "declared",
                            is(expectedDeclaredField))
                    .body(prefix + "instances.instance.partialIds.partialId.idType",
                            is("CIS"))
                    .body(prefix + "instances.instance.partialIds.partialId.value",
                            is(DECLARED_ITEM.getInstances().get("CIS")));
        });
    }

    @RetryableTest
    @DisplayName("Тест трансфера: Трансфер для товаров с CIS")
    @ResourceLock("Тест трансфера: Трансфер для товаров с CIS")
    public void receiveCisQuarItemThenShipItInOrder() {
        receiveReturnInboundAndMoveItemToStorageCell();
        //За время работы трансфера сессия успевает протухнуть,
        //лучше переоткрыть браузер самим
        closeBrowser();
        createTransferFromCisQuarToFit();
        order = ApiSteps.Order().createTodayOrder(ACTUAL_ITEM);
        openNewBrowser();

        waveId = processSteps.Outgoing().createAndForceStartSingleWaveManually(order, singlesConsolidationCell);

        List<String> itemSerials = processSteps.Outgoing().pickSingleOrderAssignmentRefusingNextTask(areaKey, CONTAINER_LABEL);
        processSteps.Outgoing().consolidateWaveContainerToLine(CONTAINER_LABEL, singlesConsolidationCell);
        List<ParcelId> packs = processSteps.Outgoing().packNonsortOrder(CONTAINER_LABEL, itemSerials, packingTable);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
        processSteps.Outgoing().shipOrder(order, dropId, shippingDoor, shippingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }

    private static String generateUnredeemedBox() {
        return UNREDEEMED_BOX_PREFIX + UniqueId.getString();
    }

    private static String generateUnredeemedPallet() {
        return UNREDEEMED_PALLET_PREFIX + UniqueId.getString();
    }

}
