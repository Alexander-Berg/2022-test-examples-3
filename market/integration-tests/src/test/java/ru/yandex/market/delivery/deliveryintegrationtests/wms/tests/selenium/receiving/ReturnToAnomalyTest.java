package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.AnomalyType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;


@DisplayName("Selenium: Приёмка возвратов аномалий")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class ReturnToAnomalyTest extends AnomalySeleniumTest {

    private static final String NO_BARCODE = "NO_BARCODE"; // ReceiveItemType: UNRECOGNIZABLE
    private static final String UNKNOWN_SKU = "UNKNOWN_SKU"; // ReceiveItemType: UNKNOWN_EAN
    private static final String MISGRADING = "MISGRADING"; // ReceiveItemType: NOT_DECLARED_FOR_RETURN
    private static final String MISMATCHING_DESCRIPTION = "MISMATCHING_DESCRIPTION"; // ReceiveItemType: UNDESCRIBED
    private static final String NOT_DECLARED_IMEI = "NOT_DECLARED_IMEI";

    private static final String RETURN_REASON_NOT_FIT = "DO_NOT_FIT";
    private static final String RETURN_BOX_PREFIX = "VOZ_FF_";
    private static final int VENDOR_ID = 1559;

    private String cartIdAN;

    @BeforeEach
    void setUpCartAN() {
        cartIdAN = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
    }

    @RetryableTest
    @DisplayName("Возврат товара без ШК. Прием в аномалии.")
    @ResourceLock("Возврат товара без ШК. Прием в аномалии.")
    void returnUnrecognizable() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final ReturnedOrder order = generateOrderList(1).get(0);

        ApiSteps.Inbound().putReturnInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                order.getBoxId(),
                order.getOrderId(),
                order.getReturnId(),
                RETURN_REASON_NOT_FIT,
                order.getUit(),
                order.getSku(),
                String.valueOf(VENDOR_ID));

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(order.getUit())
                .name("e2e client return test")
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(
                order.getBoxId());
        receiveReturnedItemWithoutBarcode(item, order.getBoxId(), inbound, NO_BARCODE);
    }

    @RetryableTest
    @DisplayName("Возврат товара с нераспознанным EAN")
    @ResourceLock("Возврат товара с нераспознанным EAN")
    void returnUnknownEan() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final ReturnedOrder order = generateOrderList(1).get(0);

        ApiSteps.Inbound().putReturnInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                order.getBoxId(),
                order.getOrderId(),
                order.getReturnId(),
                RETURN_REASON_NOT_FIT,
                order.getUit(),
                order.getSku(),
                String.valueOf(VENDOR_ID));

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(order.getUit())
                .name("e2e client return test")
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(order.getBoxId());
        receiveReturnedItemWithIncorrectEan(item, order.getBoxId(), inbound, UNKNOWN_SKU);
    }

    @RetryableTest
    @DisplayName("Возврат товара незаявленного к поставке")
    @ResourceLock("Возврат товара незаявленного к поставке")
    void returnNotDeclaredForReturn() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final List<ReturnedOrder> orders = generateOrderList(2);
        final ReturnedOrder order1 = orders.get(0);
        final ReturnedOrder order2 = orders.get(1);
        final String barcode1 = RandomUtil.randomStringNumbersOnly(12);
        final String barcode2 = RandomUtil.randomStringNumbersOnly(12);

        final Item item1 = Item.builder()
                .vendorId(VENDOR_ID)
                .article(barcode1)
                .sku(order1.getSku())
                .name("e2e client return test")
                .build();
        final Item item2 = item1.toBuilder()
                .article(barcode2)
                .sku(order2.getSku())
                .build();

        ApiSteps.Inbound().putReturnInboundRegistryWithTwoBarcodesInTwoBoxesInOneReturn(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                List.of(order1.getBoxId(), order2.getBoxId()),
                List.of(order1.getOrderId(), order2.getOrderId()),
                List.of(order1.getReturnId(), order2.getReturnId()),
                List.of(RETURN_REASON_NOT_FIT, RETURN_REASON_NOT_FIT),
                List.of(order1.getUit(), order2.getUit()),
                List.of(item1, item2))
        ;

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(
                List.of(order1.getBoxId(), order2.getBoxId()));
        receiveReturnedItemWithNotDeclaredEan(item1, item2, order1.getBoxId(), order2.getBoxId(), inbound, MISGRADING);

    }

    @RetryableTest
    @DisplayName("Возврат товара несоответствующего описанию")
    @ResourceLock("Возврат товара несоответствующего описанию")
    void returnUndescribed() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final ReturnedOrder order = generateOrderList(1).get(0);

        ApiSteps.Inbound().putReturnInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                order.getBoxId(),
                order.getOrderId(),
                order.getReturnId(),
                RETURN_REASON_NOT_FIT,
                order.getUit(),
                order.getSku(),
                String.valueOf(VENDOR_ID));

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(order.getUit())
                .name("e2e client return test")
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(order.getBoxId());
        receiveReturnedItemWithIncorrectDescription(item, order.getBoxId(), inbound, MISMATCHING_DESCRIPTION);
    }

    @RetryableTest
    @DisplayName("Возврат товара c незаявленным в поставке IMEI")
    @ResourceLock("Возврат товара c незаявленным в поставке IMEI")
    void returnNotDeclaredIMEI() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final List<ReturnedOrder> orders = generateOrderList(2);
        final ReturnedOrder order1 = orders.get(0);
        final ReturnedOrder order2 = orders.get(1);
        final String imei1 = RandomUtil.generateImei();
        final String imei2 = RandomUtil.generateImei();

        final Item item1 = Item.builder()
                .vendorId(VENDOR_ID)
                .article(order1.getUit())
                .sku(order1.getSku())
                .name("e2e client return test")
                .checkImei(1)
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_IMEI))
                .instances(Map.of("IMEI", imei2)) //IMEI не соответствующий заявленному
                .build();

        final Item item2 = item1.toBuilder()
                .article(order2.getUit())
                .sku(order2.getSku())
                .instances(Map.of("IMEI", imei2))
                .anomalyTypes(Set.of())
                .build();

        ApiSteps.Inbound().putReturnInboundRegistryWithTwoIMEI(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                List.of(order1.getBoxId(), order2.getBoxId()),
                List.of(order1.getOrderId(), order2.getOrderId()),
                List.of(order1.getReturnId(), order2.getReturnId()),
                List.of(RETURN_REASON_NOT_FIT, RETURN_REASON_NOT_FIT),
                List.of(imei1, imei2),
                List.of(item1, item2)
        );

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(
                List.of(order1.getBoxId(), order2.getBoxId()));

        receiveReturnedItemWithNotDeclaredIMEI(item1, item2, order1.getBoxId(), order2.getBoxId(), inbound,
                NOT_DECLARED_IMEI);

    }

    private void receiveReturnedItemWithoutBarcode(final Item item,
                                                   final String box,
                                                   final Inbound inbound,
                                                   final String itemTypeExpected) {
        uiSteps.Login().PerformLogin();

        uiSteps.Receiving()
                .receiveReturnItemAnomaly(box, cartIdAN)
                .closePalletWithDiscrepancies();

        verifyAnomalyInboundResponse(cartIdAN, inbound, itemTypeExpected);
    }

    private void receiveReturnedItemWithIncorrectEan(final Item item,
                                                     final String box,
                                                     final Inbound inbound,
                                                     final String itemTypeExpected) {
        uiSteps.Login().PerformLogin();

        uiSteps.Receiving()
                .receiveReturnItemWithIncorrectEan(box, cartIdAN)
                .closePalletWithDiscrepancies();

        verifyAnomalyInboundResponse(cartIdAN, inbound, itemTypeExpected);
    }

    private void receiveReturnedItemWithNotDeclaredEan(final Item item1,
                                                       final Item item2,
                                                       final String box1,
                                                       final String box2,
                                                       final Inbound inbound,
                                                       final String itemTypeExpected) {
        final String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();

        uiSteps.Receiving()
                .receiveReturnItemWithNotDeclaredEan(item1, box2, cartIdAN, false)
                .closePalletWithDiscrepancies();

        uiSteps.Receiving()
                .continueReceivingReturnItemFromEnteringPallet(item1, box1, cartId, false)
                .closePallet();

        verifyAnomalyInboundResponse(cartIdAN, inbound, itemTypeExpected);
    }

    private void receiveReturnedItemWithIncorrectDescription(final Item item,
                                                             final String box,
                                                             final Inbound inbound,
                                                             final String itemTypeExpected) {
        uiSteps.Login().PerformLogin();

        uiSteps.Receiving()
                .receiveReturnItemWithIncorrectDescription(item, box, cartIdAN)
                .closePallet();

        verifyAnomalyInboundResponse(cartIdAN, inbound, itemTypeExpected);
    }

    private void receiveReturnedItemWithNotDeclaredIMEI(final Item item1,
                                                        final Item item2,
                                                        final String box1,
                                                        final String box2,
                                                        final Inbound inbound,
                                                        final String itemTypeExpected) {
        final String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();

        uiSteps.Receiving()
                .receiveReturnItemWithNotDeclaredIdentity(item1, box1, cartIdAN)
                .closePallet();

        uiSteps.Receiving()
                .continueReceivingReturnItemFromEnteringPallet(item2, box2, cartId, false)
                .closePallet();

        verifyAnomalyInboundResponse(cartIdAN, inbound, itemTypeExpected);
    }

    private void verifyAnomalyInboundResponse(String cartId, Inbound inbound, String itemTypeExpected) {
        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                cartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());

        ApiSteps.Inbound().checkGetInboundForSecondaryReturn(getInboundResponse, itemTypeExpected);
    }

    private List<ReturnedOrder> generateOrderList(int numberOfOrders) {
        List<ReturnedOrder> orderList = new ArrayList<>();
        for (int i = 0; i < numberOfOrders; i++) {
            ReturnedOrder order = ReturnedOrder.builder()
                    .orderId(RandomUtil.randomStringNumbersOnly(10))
                    .uit(RandomUtil.generateUit())
                    .returnId(RandomUtil.randomStringNumbersOnly(6))
                    .sku(UniqueId.getString())
                    .build();
            order.setBoxId(RETURN_BOX_PREFIX + UniqueId.getString());
            orderList.add(order);
        }

        return orderList;
    }

    @Data
    @Builder
    private static class ReturnedOrder {
        private String orderId;
        private String boxId;
        private String returnId;
        private String uit;
        private String sku;
    }

}
