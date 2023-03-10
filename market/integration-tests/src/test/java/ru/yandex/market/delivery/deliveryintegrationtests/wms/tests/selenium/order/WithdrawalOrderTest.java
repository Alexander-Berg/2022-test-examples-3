package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;


import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Withdrawal Order")
@Epic("Selenium Tests")
@Slf4j
public class WithdrawalOrderTest extends AbstractUiTest {

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
    private String consolidationCell;
    private String packingTable;
    private String sortingStation;
    private String sortingCell;
    private String otherSortingCell;
    private String shippingDoor;
    private String shippingWithdrawalCell;
    private String otherShippingWithdrawalCell;
    private String droppingCell;

    private WaveId waveId;

    @BeforeEach
    @Step("????????????????????: ?????????????? ??????????????, ???????????? ????????????????????," +
            "??????????????????, ?????? ?????? ?????????????????? ?????????????? ?? ?????????????????? ?????????? ?????? ????????????")
    public void setUp() throws Exception {
        // ??????????????
        areaKey = DatacreatorSteps.Location().createArea();

        // ???????? ???????????? ?? ????????????????
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        packingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        shippingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ???????????? ????????????, ???????????????????????? ?? ???????? ????????????????
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        consolidationCell = DatacreatorSteps.Location().createConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // ?????????????????????????? ??????????????
        SortingStation sortingStationObj = DatacreatorSteps.Location().createSortingStation(pickingZone);
        sortingStation = sortingStationObj.getStation();
        sortingCell = sortingStationObj.getSortLocations().get(0);
        otherSortingCell = sortingStationObj.getSortLocations().get(1);
        DatacreatorSteps.Location().updateSortStationMode(sortingStation, AutoStartSortingStationMode.WITHDRAWALS);

        //???????????? ????????????????
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ???????????? ?????? ????????????????
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingWithdrawalCell = DatacreatorSteps.Location().createShippingWithdrawalCell(shippingZone);
        otherShippingWithdrawalCell = DatacreatorSteps.Location().createShippingWithdrawalCell(shippingZone);

        // ?????????????????? ??????????
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell, 2);
    }

    @AfterEach
    @Step("???????????? ???? ?????????? ????????????")
    public void tearDown() {
        // ???????????????? ?????????????????????????? ??????????????
        DatacreatorSteps.Location().deleteSotingStation(sortingStation);

        // ???????????????? ??????????
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(consolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(shippingDoor);
        DatacreatorSteps.Location().deleteCell(shippingWithdrawalCell);
        DatacreatorSteps.Location().deleteCell(otherShippingWithdrawalCell);

        // ???????????????? ??????
        DatacreatorSteps.Location().deletePutawayZone(packingZone);
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
        DatacreatorSteps.Location().deletePutawayZone(shippingZone);

        // ???????????????? ??????????????
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
    @DisplayName("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ?????????????? ??????????")
    @ResourceLock("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ?????????????? ??????????")
    public void withdrawalFitWithManualWaveTest() {
        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound1 = ApiSteps.Outbound().createOutbound(ITEM);
        Outbound outbound2 = ApiSteps.Outbound().createOutbound(ITEM);
        List<Outbound> outbounds = List.of(outbound1, outbound2);
        List<Order> withdrawalOrders = outbounds.stream()
                .map(outbound -> new Order(outbound.getYandexId(), outbound.getFulfillmentId()))
                .collect(Collectors.toList());

        waveId = processSteps.Outgoing().createAndForceStartWaveManually(withdrawalOrders, sortingStation);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);

        var itemSerialsToSortingCellsMapping = new HashMap<String, String>();
        itemSerialsToSortingCellsMapping.put(itemSerials.get(0), sortingCell);
        itemSerialsToSortingCellsMapping.put(itemSerials.get(1), otherSortingCell);
        processSteps.Outgoing().consolidateWaveAndOrders(itemSerialsToSortingCellsMapping, consolidationCell,
                containerLabel, sortingStation);

        List<ParcelId> packs = processSteps.Outgoing().packOrders(itemSerialsToSortingCellsMapping, packingTable);
        List<DropId> dropIds = List.of(
                processSteps.Outgoing().dropOrder(packs.get(0), droppingCell),
                processSteps.Outgoing().dropOrder(packs.get(1), droppingCell)
        );

        processSteps.Outgoing().shipWithdrawals(outbounds, dropIds, shippingDoor,
                List.of(shippingWithdrawalCell, otherShippingWithdrawalCell));
    }

    @RetryableTest
    @Tag("AutostartReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("PickingReleaseSuite")
    @Tag("AutostartMultitestingSuite")
    @Tag("PackingMultitestingSuite")
    @Tag("PickingMultitestingSuite")
    @DisplayName("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ???????????????????????? ??????????")
    @ResourceLock("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ???????????????????????? ??????????")
    public void withdrawalDefectWithManualWaveTest() {
        String lotForBlocking = DatacreatorSteps
                .Items()
                .getItemLotByArticleLoc(ITEM.getArticle(), storageCell);
        DatacreatorSteps.Items().placeHoldOnLot(lotForBlocking, InventoryHoldStatus.DAMAGE);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound1 = ApiSteps.Outbound().createOutbound(ITEM, StockType.DEFECT);
        Outbound outbound2 = ApiSteps.Outbound().createOutbound(ITEM, StockType.DEFECT);
        List<Outbound> outbounds = List.of(outbound1, outbound2);
        List<Order> withdrawalOrders = outbounds.stream()
                .map(outbound -> new Order(outbound.getYandexId(), outbound.getFulfillmentId()))
                .collect(Collectors.toList());

        waveId = processSteps.Outgoing().createAndForceStartWaveManually(withdrawalOrders, sortingStation);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);

        var itemSerialsToSortingCellsMapping = new HashMap<String, String>();
        itemSerialsToSortingCellsMapping.put(itemSerials.get(0), sortingCell);
        itemSerialsToSortingCellsMapping.put(itemSerials.get(1), otherSortingCell);
        processSteps.Outgoing().consolidateWaveAndOrders(itemSerialsToSortingCellsMapping, consolidationCell,
                containerLabel, sortingStation);

        List<ParcelId> packs = processSteps.Outgoing().packOrders(itemSerialsToSortingCellsMapping, packingTable);
        List<DropId> dropIds = List.of(
                processSteps.Outgoing().dropOrder(packs.get(0), droppingCell),
                processSteps.Outgoing().dropOrder(packs.get(1), droppingCell)
        );

        processSteps.Outgoing().shipWithdrawals(outbounds, dropIds, shippingDoor,
                List.of(shippingWithdrawalCell, otherShippingWithdrawalCell));
    }

    @RetryableTest
    @Tag("ConsolidationReleaseSuite")
    @Tag("DroppingReleaseSuite")
    @Tag("ShippingReleaseSuite")
    @Tag("ConsolidationMultitestingSuite")
    @Tag("DroppingMultitestingSuite")
    @Tag("ShippingMultitestingSuite")
    @DisplayName("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ?????????????????????????? ??????????")
    @ResourceLock("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ?????????????????????????? ??????????")
    public void withdrawalExpiredWithManualWaveTest() {
        String lotForBlocking = DatacreatorSteps
                .Items()
                .getItemLotByArticleLoc(ITEM.getArticle(), storageCell);
        DatacreatorSteps.Items().placeHoldOnLot(lotForBlocking, InventoryHoldStatus.EXPIRED);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound1 = ApiSteps.Outbound().createOutbound(ITEM, StockType.EXPIRED);
        Outbound outbound2 = ApiSteps.Outbound().createOutbound(ITEM, StockType.EXPIRED);
        List<Outbound> outbounds = List.of(outbound1, outbound2);
        List<Order> withdrawalOrders = outbounds.stream()
                .map(outbound -> new Order(outbound.getYandexId(), outbound.getFulfillmentId()))
                .collect(Collectors.toList());

        waveId = processSteps.Outgoing().createAndForceStartWaveManually(withdrawalOrders, sortingStation);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);

        var itemSerialsToSortingCellsMapping = new HashMap<String, String>();
        itemSerialsToSortingCellsMapping.put(itemSerials.get(0), sortingCell);
        itemSerialsToSortingCellsMapping.put(itemSerials.get(1), otherSortingCell);
        processSteps.Outgoing().consolidateWaveAndOrders(itemSerialsToSortingCellsMapping, consolidationCell,
                containerLabel, sortingStation);

        List<ParcelId> packs = processSteps.Outgoing().packOrders(itemSerialsToSortingCellsMapping, packingTable);

        List<DropId> dropIds = List.of(
                processSteps.Outgoing().dropOrder(packs.get(0), droppingCell),
                processSteps.Outgoing().dropOrder(packs.get(1), droppingCell)
        );
        processSteps.Outgoing().shipWithdrawals(outbounds, dropIds, shippingDoor,
                List.of(shippingWithdrawalCell, otherShippingWithdrawalCell));
    }

    @RetryableTest
    @Tag("AutostartReleaseSuite")
    @Tag("AutostartMultitestingSuite")
    @Disabled("https://st.yandex-team.ru/MARKETWMS-15941")
    @ResourceLock("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ?????????????? ?????????? ?????? ??????")
    @DisplayName("???????? ???????????????? ?? ?????????????? ?????????? ?????? ?????????????? ?? ?????????????? ?????????? ?????? ??????")
    public void withdrawalFitNonSortWithManualWaveTest() {
        processSteps.Incoming().acceptNonSortItemsAndPlaceThemToPickingCell(ITEM, storageCell);
        processSteps.Incoming().acceptNonSortItemsAndPlaceThemToPickingCell(ITEM, storageCell);

        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Outbound outbound1 = ApiSteps.Outbound().createOutbound(ITEM);
        Outbound outbound2 = ApiSteps.Outbound().createOutbound(ITEM);
        List<Outbound> outbounds = List.of(outbound1, outbound2);
        List<Order> withdrawalOrders = outbounds.stream()
                .map(outbound -> new Order(outbound.getYandexId(), outbound.getFulfillmentId()))
                .collect(Collectors.toList());

        waveId = processSteps.Outgoing().createAndForceStartWaveManually(withdrawalOrders, sortingStation);

        List<String> itemSerials = processSteps.Outgoing().pickWithdrawalAssignment(areaKey, containerLabel);

        //?? ???????????????? ?????? ???????????? ???????????????? ?????? ?????????????? ??????????
        var itemSerialsToSortingCellsMapping = new HashMap<String, String>();
        itemSerialsToSortingCellsMapping.put(itemSerials.get(0), sortingCell);
        itemSerialsToSortingCellsMapping.put(itemSerials.get(1), otherSortingCell);
        processSteps.Outgoing().consolidateWaveAndOrders(itemSerialsToSortingCellsMapping, consolidationCell,
                containerLabel, sortingStation);

        List<ParcelId> packs = processSteps.Outgoing().packOrders(itemSerialsToSortingCellsMapping, packingTable);

        List<DropId> dropIds = List.of(
                processSteps.Outgoing().dropOrder(packs.get(0), droppingCell),
                processSteps.Outgoing().dropOrder(packs.get(1), droppingCell)
        );

        processSteps.Outgoing().shipWithdrawals(outbounds, dropIds, shippingDoor,
                List.of(shippingWithdrawalCell, otherShippingWithdrawalCell));
    }
}
