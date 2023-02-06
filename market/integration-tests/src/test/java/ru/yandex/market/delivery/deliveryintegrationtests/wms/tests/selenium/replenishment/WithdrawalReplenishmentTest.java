package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.replenishment;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.StockType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemStatus;

import java.util.List;

@DisplayName("Selenium: Replenishment")
@Epic("Selenium Tests")
@Slf4j
public class WithdrawalReplenishmentTest extends AbstractUiTest {


    private final String SKU_1 = UniqueId.getString();
    private final String SKU_2 = UniqueId.getString();
    private final Integer STORER = 1559;
    private final Item PICKING_ITEM_1 = Item.builder()
            .sku(SKU_1)
            .vendorId(STORER)
            .article(SKU_1)
            .quantity(1)
            .build();
    private final Item STORAGE_ITEM_1 = Item.builder()
            .sku(SKU_1)
            .vendorId(STORER)
            .article(SKU_1)
            .quantity(1)
            .build();
    private final Item TOTAL_ITEM_1 = Item.builder()
            .sku(SKU_1)
            .vendorId(STORER)
            .article(SKU_1)
            .quantity(PICKING_ITEM_1.getQuantity() + STORAGE_ITEM_1.getQuantity())
            .build();

    private final Item PICKING_ITEM_2 = Item.builder()
            .sku(SKU_2)
            .vendorId(STORER)
            .article(SKU_2)
            .quantity(1)
            .build();
    private final Item STORAGE_ITEM_2 = Item.builder()
            .sku(SKU_2)
            .vendorId(STORER)
            .article(SKU_2)
            .quantity(1)
            .build();
    private final Item TOTAL_ITEM_2 = Item.builder()
            .sku(SKU_2)
            .vendorId(STORER)
            .article(SKU_2)
            .quantity(PICKING_ITEM_2.getQuantity() + STORAGE_ITEM_2.getQuantity())
            .build();

    private String areaKey;
    private String pickingZone;
    private String palletZone;
    private String pickingCell;
    private String bufferCell;
    private String bufferCell2;
    private String palletCell;
    private String palletCell2;
    private WaveId waveId;


    @BeforeEach
    @Step("Подготовка: Создаем участок, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
    public void setUp() throws Exception {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        palletZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // Ячейки
        pickingCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        bufferCell = DatacreatorSteps.Location().createWithdrawalReplBuffer(palletZone);
        bufferCell2 = DatacreatorSteps.Location().createWithdrawalReplBuffer(palletZone);
        palletCell = DatacreatorSteps.Location().createPalletStorageCell(palletZone);
        palletCell2 = DatacreatorSteps.Location().createPalletStorageCell(palletZone);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(pickingCell);
        DatacreatorSteps.Location().deleteCell(bufferCell);
        DatacreatorSteps.Location().deleteCell(bufferCell2);
        DatacreatorSteps.Location().deleteCell(palletCell);
        DatacreatorSteps.Location().deleteCell(palletCell2);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
        DatacreatorSteps.Location().deletePutawayZone(palletZone);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey);

        if (waveId != null) {
            uiSteps.Login().PerformLogin();
            uiSteps.Wave().unreserveWave(waveId);
        }
    }


    @RetryableTest
    @Tag("ReplenishmentReleaseSuite")
    @DisplayName("Тест: волна годного стока больших изъятий -> пополнение -> удачный резерв")
    @ResourceLock("Тест: волна годного стока больших изъятий -> пополнение -> удачный резерв")
    public void bigWithdrawalFitWithManualWaveTest() {
        // Принимаем товар
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(PICKING_ITEM_1, pickingCell);
        String palletId = processSteps.Incoming()
                .acceptItemsAndPlaceThemToPalletCell(STORAGE_ITEM_1, palletCell, STORAGE_ITEM_1.getQuantity());

        // Изъятия Создаем волну, которой не хватает товаров в отборе, нажимаем пополнить
        Outbound outbound = ApiSteps.Outbound().createOutbound(TOTAL_ITEM_1);
        Order withdrawalOrder = new Order(outbound.getYandexId(), outbound.getFulfillmentId());
        waveId = processSteps.Outgoing().createWaveManuallyForReplenishment(withdrawalOrder);

        // Пополнение Создаем таски
        processSteps.Outgoing().createWithdrawalReplenishmentTasks(withdrawalOrder.getFulfillmentId());

        // Пополнение Выполняем таски
        String pickingCart = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().moveDownWithdrawal(areaKey, palletCell, palletId, bufferCell);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().startPickingWithdrawal(areaKey, bufferCell, palletId, pickingCart)
                .pickWholePallete()
                .confirmPickingAll();

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().validateNoMoveTasksWithdrawal(areaKey);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().waitOrderStatusIs(withdrawalOrder.getFulfillmentId(), ProblemStatus.COMPLETE);

        // Размещаем товары в ячейку с типом PICK
        uiSteps.Login().PerformLogin();
        processSteps.Incoming().placeContainerToCell(pickingCart, pickingCell);

        // Изъятия Резервируем волну, которой должно хватить товаров
        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().waitWaveStatusIs(waveId.getId(), WaveStatus.RESERVED);
    }

    @RetryableTest
    @Tag("ReplenishmentReleaseSuite")
    @DisplayName("Тест: волна бракованного стока больших изъятий -> пополнение -> удачный резерв")
    @ResourceLock("Тест: волна бракованного стока больших изъятий -> пополнение -> удачный резерв")
    public void bigWithdrawalDamageWithManualWaveTest() {
        // Принимаем товар
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(PICKING_ITEM_1, pickingCell);
        String lotPicking = DatacreatorSteps.Items().getItemLotByArticleLoc(PICKING_ITEM_1.getArticle(), pickingCell);
        DatacreatorSteps.Items().placeHoldOnLot(lotPicking, InventoryHoldStatus.DAMAGE);

        String palletId = processSteps.Incoming()
                .acceptItemsAndPlaceThemToPalletCell(STORAGE_ITEM_1, palletCell, STORAGE_ITEM_1.getQuantity());
        String lotPallet = DatacreatorSteps.Items().getItemLotByArticleLoc(STORAGE_ITEM_1.getArticle(), palletCell);
        DatacreatorSteps.Items().placeHoldOnLot(lotPallet, InventoryHoldStatus.DAMAGE);

        processSteps.Incoming()
                .acceptItemsAndPlaceThemToPalletCell(STORAGE_ITEM_1, palletCell2, STORAGE_ITEM_1.getQuantity());
        String lotPallet2 = DatacreatorSteps.Items().getItemLotByArticleLoc(STORAGE_ITEM_1.getArticle(), palletCell2);
        DatacreatorSteps.Items().placeHoldOnLot(lotPallet2, InventoryHoldStatus.DAMAGE);
        DatacreatorSteps.Items().placeHoldOnLot(lotPallet2, InventoryHoldStatus.PLAN_UTILIZATION);

        // Изъятия Создаем волну, которой не хватает товаров в отборе, нажимаем пополнить
        Outbound outbound = ApiSteps.Outbound().createOutbound(TOTAL_ITEM_1, StockType.DEFECT);
        Order withdrawalOrder = new Order(outbound.getYandexId(), outbound.getFulfillmentId());
        waveId = processSteps.Outgoing().createWaveManuallyForReplenishment(withdrawalOrder);

        // Пополнение Создаем таски
        processSteps.Outgoing().createWithdrawalReplenishmentTasks(withdrawalOrder.getFulfillmentId());
        // Пополнение Выполняем таски
        String pickingCart = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().moveDownWithdrawal(areaKey, palletCell, palletId, bufferCell);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().startPickingWithdrawal(areaKey, bufferCell, palletId, pickingCart)
                .pickWholePallete()
                .confirmPickingAll();

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().validateNoMoveTasksWithdrawal(areaKey);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().waitOrderStatusIs(withdrawalOrder.getFulfillmentId(), ProblemStatus.COMPLETE);

        // Размещаем товары в ячейку с типом PICK
        uiSteps.Login().PerformLogin();
        processSteps.Incoming().placeContainerToCell(pickingCart, pickingCell);

        // Изъятия Создаем волну, которой должно хватить товаров
        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().waitWaveStatusIs(waveId.getId(), WaveStatus.RESERVED);
    }

    @RetryableTest
    @Tag("ReplenishmentReleaseSuite")
    @DisplayName("Тест: волна годного стока изъятий -> пополнение -> удачный резерв")
    @ResourceLock("Тест: волна годного стока изъятий -> пополнение -> удачный резерв")
    public void withdrawalFitWithManualWaveTest() {
        // Принимаем товар
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(PICKING_ITEM_1, pickingCell);
        String palletId1 = processSteps.Incoming()
                .acceptItemsAndPlaceThemToPalletCell(STORAGE_ITEM_1, palletCell, STORAGE_ITEM_1.getQuantity());
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(PICKING_ITEM_2, pickingCell);
        String palletId2 = processSteps.Incoming()
                .acceptItemsAndPlaceThemToPalletCell(STORAGE_ITEM_2, palletCell2, STORAGE_ITEM_2.getQuantity());


        // Изъятия Создаем волну, которой не хватает товаров в отборе, нажимаем пополнить
        Outbound outbound1 = ApiSteps.Outbound().createOutbound(TOTAL_ITEM_1);
        Outbound outbound2 = ApiSteps.Outbound().createOutbound(TOTAL_ITEM_2);
        Order withdrawalOrder1 = new Order(outbound1.getYandexId(), outbound1.getFulfillmentId());
        Order withdrawalOrder2 = new Order(outbound2.getYandexId(), outbound2.getFulfillmentId());
        List<Order> orders = List.of(withdrawalOrder1, withdrawalOrder2);
        List<String> orderIds = orders.stream().map(Order::getFulfillmentId).toList();

        waveId = processSteps.Outgoing().createWaveManuallyForReplenishment(orders);
        // Пополнение Создаем таски
        processSteps.Outgoing().createWithdrawalReplenishmentTasks(orderIds);

        // Пополнение Выполняем таски
        String pickingCart = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().moveDownWithdrawal(areaKey, palletCell, palletId1, bufferCell);
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().moveDownWithdrawal(areaKey, palletCell2, palletId2, bufferCell2);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().startPickingWithdrawal(areaKey, bufferCell, palletId1, pickingCart)
                .pickWholePallete()
                .confirmPickingAll();
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().startPickingWithdrawal(areaKey, bufferCell2, palletId2, pickingCart)
                .pickWholePallete()
                .confirmPickingAll();

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().validateNoMoveTasksWithdrawal(areaKey);

        // Пополнение Проверяем что выполнилось
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().waitOrderStatusIs(orderIds, ProblemStatus.COMPLETE);

        // Размещаем товары в ячейку с типом PICK
        uiSteps.Login().PerformLogin();
        processSteps.Incoming().placeContainerToCell(pickingCart, pickingCell);

        // Изъятия Создаем волну, которой должно хватить товаров
        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().waitWaveStatusIs(waveId.getId(), WaveStatus.RESERVED);
    }
}
