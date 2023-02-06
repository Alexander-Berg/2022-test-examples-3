package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.replenishment;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.PropertyLoader;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.scheduler.SchedulerSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Replenishment")
@Epic("Selenium Tests")
public class ReplenishmentTest extends AbstractUiTest {

    private final long STORER_KEY = 1559;
    private final String STORER_SKU = "REPLTESTITEM1";
    private final int TOTAL_QTY = 10;

    private final Item ITEM = Item.builder()
            .sku(STORER_SKU)
            .vendorId(STORER_KEY)
            .article(STORER_SKU)
            .quantity(TOTAL_QTY)
            .build();

    private static final SchedulerSteps schedulerSteps = new SchedulerSteps();
    private String areaKey;
    private String palleteZone;
    private String storageCell;
    private String bufferCell;
    private String knownSku;
    private String knownLot;
    private List<String> knownSerials;
    private String storagePallet;
    private String pickingCart;

    @BeforeEach
    @Step("Подготовка: инициализация состояния окружения для теста")
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зона паллетного хранения
        palleteZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ячейки
        bufferCell = DatacreatorSteps.Location().createTurnoverReplBuffer(palleteZone);
        storageCell = DatacreatorSteps.Location().createPalletStorageCell(palleteZone);

        storagePallet = DatacreatorSteps.Label().createContainer(ContainerIdType.PLT);
        pickingCart = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        createItemsForReplenishment();

        knownSku = ApiSteps.Stocks().getRovByStorerKeyAndManufaturerSku(STORER_KEY, STORER_SKU);
    }

    @AfterEach
    @Step("Очистка данных после теста")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(bufferCell);
        DatacreatorSteps.Location().deleteCell(storageCell);

        // удаление зоны
        DatacreatorSteps.Location().deletePutawayZone(palleteZone);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey);
    }

    @RetryableTest
    @DisplayName("Пополнение по оборачиваемости: дважды спускаем одну и ту же палету, отбор разными способами")
    @ResourceLock("Пополнение по оборачиваемости: дважды спускаем одну и ту же палету, отбор разными способами")
    public void replenishmentSequentalCyclesTest() {
        replenishmentFirstPickPartAndReturn();
        checkBalancesAfterPick(3);
        replenishmentSecondPickPallete();
        checkBalancesAfterPick(10);
    }

    @RetryableTest
    @DisplayName("Пополнение по оборачиваемости: отбор палеты поштучно и тут же передумали, отобрали все")
    @ResourceLock("Пополнение по оборачиваемости: отбор палеты поштучно и тут же передумали, отобрали все")
    public void replenishmentChangeMyMindPickTest() {
        replenishmentPickMix();
        checkBalancesAfterPick(10);
    }

    @Step("Создание товаров в паллетном хранении")
    private void createItemsForReplenishment() {
        uiSteps.Login().PerformLogin();
        Assertions.assertFalse(
                uiSteps.Balances().isEnoughItemsAvailable(storageCell, ITEM.getArticle(), 1)
        ,"В только что созданной ячейке не должно быть товаров на момент начала тестов");

        Inbound inbound = ApiSteps.Inbound().createInbound(ITEM.getArticle());

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(inbound.getFulfillmentId(), 1);

        uiSteps.Login().PerformLogin();
        String pallet = uiSteps.Receiving().findPalletOfInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(ITEM, pallet, storagePallet);

        uiSteps.Login().PerformLogin();
        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        knownLot = uiSteps.Balances().findLotByNzn(storagePallet);

        uiSteps.Login().PerformLogin();
        knownSerials = uiSteps.Balances().findByNznAndSupSku(STORER_SKU, storagePallet);

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(storagePallet, storageCell);
    }

    @Step("Проверка, что где лежит после пополнения на {sumPicked} шт.")
    private void checkBalancesAfterPick(int sumPicked) {

        uiSteps.Login().PerformLogin();
        List<String> serialsOnPallet = uiSteps.Balances().findByNznAndSupSku(STORER_SKU, storagePallet);

        Assertions.assertEquals(TOTAL_QTY - sumPicked, serialsOnPallet.size(),
                String.format("На паллете %s ожидали остаток %d, а фактически там %d",
                        storagePallet, TOTAL_QTY - sumPicked, serialsOnPallet.size()));

        uiSteps.Login().PerformLogin();
        List<String> serialsOnTransitCart = uiSteps.Balances().findByNznAndSupSku(STORER_SKU, pickingCart);
        Assertions.assertEquals(sumPicked, serialsOnTransitCart.size(),
                String.format("В корзине %s ожидали %d, а фактически там %d",
                        pickingCart, sumPicked, serialsOnTransitCart.size()));

    }

    @Step("Кейс: пополнение (3шт) с возвратом непустой паллеты на хранение")
    private void replenishmentFirstPickPartAndReturn() {
        schedulerSteps.createReplenishmentTasks(areaKey, storageCell, storagePallet,
                new SkuId(Long.toString(STORER_KEY), knownSku), knownLot, 3,
                true);
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment()
                .moveDown(areaKey, storageCell, storagePallet, bufferCell);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().startPicking(areaKey, bufferCell, storagePallet, pickingCart)
                .inputSerialNumber(knownSerials.get(0))
                .inputSerialNumber(knownSerials.get(1))
                .inputSerialNumber(knownSerials.get(2));

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment()
                .moveUp(areaKey, bufferCell, storagePallet, storageCell);
    }

    @Step("Кейс: поштучно пополняем по заданию и на полпути передумываем, забираем всё")
    private void replenishmentPickMix() {
        schedulerSteps.createReplenishmentTasks(areaKey, storageCell, storagePallet,
                new SkuId(Long.toString(STORER_KEY), knownSku), knownLot, 5,
                false);
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().moveDown(areaKey, storageCell, storagePallet, bufferCell);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().startPicking(areaKey, bufferCell, storagePallet, pickingCart)
                .inputSerialNumber(knownSerials.get(0))
                .inputSerialNumber(knownSerials.get(1))
                .inputSerialNumber(knownSerials.get(2))
                .pickWholePallete()
                .verifyPickAllConfirmShown()
                .confirmPickingAll();
    }

    @Step("Кейс: пополнение (7шт), паллета остаётся пустая")
    private void replenishmentSecondPickPallete() {
        schedulerSteps.createReplenishmentTasks(areaKey, storageCell, storagePallet,
                new SkuId(Long.toString(STORER_KEY), knownSku), knownLot, 7,
                false);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().moveDown(areaKey, storageCell, storagePallet, bufferCell);

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().startPicking(areaKey, bufferCell, storagePallet, pickingCart)
                .pickWholePallete()
                .verifyPickAllConfirmShown()
                .confirmPickingAll();

        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().validateNoMoveTasksTurnover(areaKey);
    }
}
