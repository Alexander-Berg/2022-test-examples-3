package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.restassured.path.xml.XmlPath;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Box;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InitialReceivingType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;

@DisplayName("Selenium: Initial Receiving - Первичная Приёмка")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class InitialReceivingTest extends AbstractUiTest {
    private static final String UNREDEEMED_BOX_PREFIX = "P000";
    private static final String UNREDEEMED_PALLET_PREFIX = "SC_LOT_";
    private static final int VENDOR_ID = 1559;
    private static final String RETURN_BOX_PREFIX = "VOZ_FF_";
    private static final String UNIQUE_BOX_PREFIX = "U_";

    private final String skuPrefix = "InitialReceivingTestItem";
    private static final String COUNT_TYPE_FIT = "FIT";
    private static final String COUNT_TYPE_NOT_ACCEPTABLE = "NOT_ACCEPTABLE";
    private static final String COUNT_TYPE_UNDEFINED = "UNDEFINED";

    @Property("wms.ui.receiving.printer")
    private String printer;
    private String fulfillmentId;

    @BeforeEach
    void setUp() {
        createInbound();
    }

    @Step("Создаем поставку товаров")
    private void createInbound() {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry("wms/servicebus/putInboundRegistry/putInboundRegistry.xml",
                inbound,
                skuPrefix,
                1559,
                false
        );
        fulfillmentId = inbound.getFulfillmentId();
    }

    @RetryableTest
    @DisplayName("Первичная приёмка товара")
    @ResourceLock("Первичная приёмка товара")
    public void initialReceiveBasic() {
        log.info("Printer: " + printer);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(fulfillmentId, 1);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().findPalletOfInbound(fulfillmentId);
    }

    @RetryableTest
    @DisplayName("Первичная приемка двух возвратных коробок без недостачи")
    @ResourceLock("Первичная приемка двух возвратных коробок без недостачи")
    public void returnBoxInitialReceivingWithoutShortage() {
        final List<Order> orders = generateOrderList(2);
        final Order firstOrder = orders.get(0);
        final Order secondOrder = orders.get(1);
        final Box firstBox = createSimpleBox(firstOrder.getBoxId());
        final Box secondBox = createSimpleBox(secondOrder.getBoxId());
        final Inbound inbound = createReturnInboundWithTwoBoxes(firstOrder, secondOrder, List.of(firstBox, secondBox));

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .enterPrinterAndGateInInitialReceiving()
                .enterReturnInboundId(inbound.getFulfillmentId())
                .enterContainerId(firstOrder.getBoxId())
                .clickSaveButtonForInitialPalletFlow()
                .enterContainerId(secondOrder.getBoxId())
                .chooseQualityAttribute("111")
                .chooseQualityAttribute("105")
                .clickSaveButtonForInitialPalletFlow()
        ;
        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().openInitialReceivingDiscrepanciesReport(inbound.getFulfillmentId())
                .checkBoxDiscrepancies(secondBox.getBoxId(), List.of("Разрыв пленки", "Манипуляционные знаки"))
                .verifyNoShortageInReport();
    }

    @RetryableTest
    @DisplayName("Первичная приемка двух возвратных коробок с недостачей")
    @ResourceLock("Первичная приемка двух возвратных коробок с недостачей")
    public void returnBoxInitialReceivingWithShortage() {
        final List<Order> orders = generateOrderList(2);
        final Order firstOrder = orders.get(0);
        final Order secondOrder = orders.get(1);
        final Box firstBox = createSimpleBox(firstOrder.getBoxId());
        final Box secondBox = createSimpleBox(secondOrder.getBoxId());
        final Inbound inbound = createReturnInboundWithTwoBoxes(firstOrder, secondOrder, List.of(firstBox, secondBox));

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(firstBox.getBoxId());
        processSteps.Incoming().closeInitialInbound(inbound.getFulfillmentId());
        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().openInitialReceivingDiscrepanciesReport(inbound.getFulfillmentId())
                .verifyShortageInReport();
    }

    @RetryableTest
    @DisplayName("Первичная приемка неизвестной  коробки")
    @ResourceLock("Первичная приемка неизвестной  коробки")
    public void returnBoxInitialReceivingUnknownBox() {
        final List<Order> orders = generateOrderList(3);
        final Order firstOrder = orders.get(0);
        final Order secondOrder = orders.get(1);
        final Box firstBox = createSimpleBox(firstOrder.getBoxId());
        final Box secondBox = createSimpleBox(secondOrder.getBoxId());
        final Box unknownBox = createSimpleBox(UNIQUE_BOX_PREFIX + orders.get(2).getBoxId());
        List<Box> boxes = List.of(firstBox, secondBox);
        final Inbound inbound = createReturnInboundWithTwoBoxes(firstOrder, secondOrder, boxes);
        processSteps.Incoming().initiallyReceiveReturnBoxesAndUndefinedBoxWithoutQualityAttributes(
                inbound.getFulfillmentId(), List.of(firstBox.getBoxId(), secondBox.getBoxId(), unknownBox.getBoxId()));

        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");
        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInboundUnitCountTypeInInitial(getInboundResponse,
                List.of(COUNT_TYPE_FIT, COUNT_TYPE_FIT, COUNT_TYPE_UNDEFINED),
                3, InitialReceivingType.BY_BOX);

    }

    @RetryableTest
    @DisplayName("Первичная приемка дропшиповой  коробки")
    @ResourceLock("Первичная приемка дропшиповой  коробки")
    public void returnBoxInitialReceivingNotAcceptableBox() {
        final List<Order> orders = generateOrderList(2);
        final Order firstOrder = orders.get(0);
        final Order secondOrder = orders.get(1);
        final Box firstBox = createSimpleBox(firstOrder.getBoxId());
        final Box secondBox = createBoxWithCountType(secondOrder.getBoxId(), COUNT_TYPE_NOT_ACCEPTABLE);
        List<Box> boxes = List.of(firstBox, secondBox);
        final Inbound inbound = createReturnInboundWithTwoBoxes(firstOrder, secondOrder, boxes);
        processSteps.Incoming().initiallyReceiveUnredeemedNotAllowedBoxesWithoutQualityAttributes(
                inbound.getFulfillmentId(), boxes);
        processSteps.Incoming().closeInitialInbound(inbound.getFulfillmentId());

        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");
        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInboundUnitCountTypeInInitial(getInboundResponse, List.of(COUNT_TYPE_FIT), 1,
                InitialReceivingType.BY_BOX);
    }


    @RetryableTest
    @DisplayName("Первичная приемка возвратов c задублированым реестром")
    @ResourceLock("Первичная приемка возвратов c задублированым реестром")
    public void returnDuplicateBoxesInitialReceiving() {
        final List<Order> orders = generateOrderList(2);
        final Order firstOrder = orders.get(0);
        final Order secondOrder = orders.get(1);
        final Box firstBox = createSimpleBox(firstOrder.getBoxId());
        final Box secondBox = createSimpleBox(secondOrder.getBoxId());
        List<Box> boxes = List.of(firstBox, secondBox);

        final Inbound inbound = createReturnInboundWithTwoBoxes(firstOrder, secondOrder, boxes);
        //создаем дубликат реестра
        createReturnInboundWithTwoBoxes(firstOrder, secondOrder, boxes);

        processSteps.Incoming()
                .initiallyReceiveReturnDuplicateContainersWithoutQualityAttributes(inbound.getFulfillmentId(),
                        List.of(firstBox.getBoxId(), secondBox.getBoxId()));

        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");
        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInboundUnitCountTypeInInitial(getInboundResponse, List.of(COUNT_TYPE_FIT,
                COUNT_TYPE_FIT), 2, InitialReceivingType.BY_BOX);
    }

    @RetryableTest
    @DisplayName("Первичная приемка возвратной палеты")
    @ResourceLock("Первичная приемка возвратной палеты")
    public void returnInitialReceivingByPallet() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String barcode = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String box = generateUnredeemedBox();
        final String pallet = generateUnredeemedPallet();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        ApiSteps.Inbound().putUnredeemedInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box,
                order,
                uit,
                sku,
                String.valueOf(VENDOR_ID),
                pallet,
                barcode,
                false);
        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);

        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");
        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInboundUnitCountTypeInInitial(getInboundResponse, List.of(COUNT_TYPE_FIT), 1,
                InitialReceivingType.BY_PALLET);
    }

    //todo завязаться на новую ошибку MARKETWMS-16369
    @RetryableTest
    @DisplayName("Не разрешать принимать палету после завершения первички")
    @ResourceLock("Не разрешать принимать палету после завершения первички")
    public void prohibitInitialReceivingByPallet() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String barcode = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String box = generateUnredeemedBox();
        final String firstPallet = generateUnredeemedPallet();
        final String secondPallet = generateUnredeemedPallet();
        final String order = RandomUtil.randomStringNumbersOnly(10);
        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(sku)
                .article(uit)
                .name("e2e initial receiving by pallet test")
                .build();

        ApiSteps.Inbound().putUnredeemedInboundRegistryWithTwoPallets(
                inbound,
                item,
                box,
                order,
                uit,
                List.of(firstPallet, secondPallet),
                barcode);
        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(firstPallet);
        processSteps.Incoming().closeInitialInbound(inbound.getFulfillmentId());
        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");
        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInboundUnitCountTypeInInitial(getInboundResponse, List.of(COUNT_TYPE_FIT), 1,
                InitialReceivingType.BY_PALLET);

        processSteps.Incoming().tryToInitiallyReceivePallet(inbound.getFulfillmentId(), secondPallet);
    }

    private List<Order> generateOrderList(int numberOfOrders) {
        List<Order> orderList = new ArrayList<>();
        for (int i = 0; i < numberOfOrders; i++) {
            Order order = Order.builder()
                    .orderId(RandomUtil.randomStringNumbersOnly(10))
                    .uit(RandomUtil.generateUit())
                    .returnId(RandomUtil.randomStringNumbersOnly(6))
                    .build();
            order.setBoxId(RETURN_BOX_PREFIX + UniqueId.getString());
            orderList.add(order);
        }

        return orderList;
    }

    private Box createSimpleBox(String boxId) {
        return createBoxWithCountType(boxId, COUNT_TYPE_FIT);
    }

    private Box createBoxWithCountType(String boxId, String countType) {
        return Box.builder()
                .boxId(boxId)
                .countType(countType)
                .build();
    }

    private Inbound createReturnInboundWithTwoBoxes(Order firstOrder, Order secondOrder, List<Box> boxes) {
        final Item firstItem = Item.builder()
                .vendorId(1559)
                .article(firstOrder.getUit())
                .name("return initial receiving test first item ")
                .build();
        final Item secondItem = Item.builder()
                .vendorId(1559)
                .article(secondOrder.getUit())
                .name("return initial receiving test second item")
                .build();

        final Inbound inbound = ApiSteps.Inbound().putInbound(
                "wms/servicebus/putInbound/putInbound.xml",
                InboundType.RETURNS
        );

        ApiSteps.Inbound().putReturnInboundRegistryWithTwoBoxes(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                List.of(firstOrder.getOrderId(), secondOrder.getOrderId()),
                List.of(firstOrder.getReturnId(), secondOrder.getOrderId()),
                List.of("DO_NOT_FIT", "BAD_QUALITY"),
                List.of(firstOrder.getUit(), secondOrder.getUit()),
                List.of(firstItem.getArticle(), secondItem.getArticle()),
                String.valueOf(firstItem.getVendorId()),
                boxes
        );

        return inbound;
    }

    private static String generateUnredeemedBox() {
        return UNREDEEMED_BOX_PREFIX + UniqueId.getString();
    }

    private static String generateUnredeemedPallet() {
        return UNREDEEMED_PALLET_PREFIX + UniqueId.getString();
    }

    @Data
    @Builder
    private static class Order {
        private String orderId;
        private String boxId;
        private String returnId;
        private String uit;
    }
}
