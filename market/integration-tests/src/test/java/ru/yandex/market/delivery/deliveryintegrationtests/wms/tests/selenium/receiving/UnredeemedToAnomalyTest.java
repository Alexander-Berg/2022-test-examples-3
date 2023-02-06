package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
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

@DisplayName("Selenium: Приёмка невыкупов в аномалии")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class UnredeemedToAnomalyTest extends AnomalySeleniumTest {

    public static int CIS_CARGO_TYPE_REQUIRED = 980;
    private final int CIS_HANDLE_MODE_ENABLED = 1;

    private static final String UNKNOWN_SKU = "UNKNOWN_SKU";
    private static final String NOT_DECLARED_CIS = "NOT_DECLARED_CIS";


    private static final String UNREDEEMED_BOX_PREFIX = "P000";
    private static final String UNREDEEMED_PALLET_PREFIX = "SC_LOT_";
    private static final String ITEM_NAME = "e2e unredeemed test";

    private static final int VENDOR_ID = 1559;

    private String cartIdAN;

    @BeforeEach
    void setUpCartAN() {
        cartIdAN = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
    }

    @RetryableTest
    @DisplayName("Невыкуп товара с неопознанным EAN")
    @ResourceLock("Невыкуп товара с неопознанным EAN")
    void unredeemedItemWithUnknownEan() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.UNREDEEMED);

        final UnredeemedOrder order = generateOrderList(1).get(0);

        ApiSteps.Inbound().putUnredeemedInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                order.getBoxId(),
                order.getOrderId(),
                order.getUit(),
                order.getSku(),
                String.valueOf(VENDOR_ID),
                order.getPalletId(),
                UniqueId.getString(),
                false);

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(order.getPalletId());

        uiSteps.Login().PerformLogin();

        uiSteps.Receiving()
                .receiveUnredeemedItemWithIncorrectEanFromBoxWithAttrPage(order.getBoxId(), cartIdAN)
                .closePalletWithDiscrepancies();

        verifyAnomalyInboundResponse(cartIdAN, inbound, UNKNOWN_SKU);
    }

    @RetryableTest
    @DisplayName("Невыкуп товара с обязательным КИЗом, который не совпадает с заявленным в поставке")
    @ResourceLock("Невыкуп товара с обязательным КИЗом, который не совпадает с заявленным в поставке")
    void unredeemedItemWithRequiredNotDeclaredCis() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.UNREDEEMED);

        final UnredeemedOrder order = generateOrderList(1).get(0);

        final String gtin1 = "1".repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String gtin2 = "2".repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String cisDeclared = RandomUtil.generateCis(gtin1).getLeft();
        final String cisActual = RandomUtil.generateCis(gtin2).getRight();

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(order.getSku())
                .article(order.getUit())
                .name(ITEM_NAME)
                .checkCis(1)
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_REQUIRED_CIS))
                .instances(Map.of("CIS", cisActual))
                .build();

        ApiSteps.Inbound().putUnredeemedInboundRegistryWithCis(
                inbound,
                item,
                order.getBoxId(),
                order.getOrderId(),
                order.getUit(),
                order.getPalletId(),
                UniqueId.getString(),
                cisDeclared,
                CIS_HANDLE_MODE_ENABLED,
                CIS_CARGO_TYPE_REQUIRED
        );

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(order.getPalletId());

        uiSteps.Login().PerformLogin();

        uiSteps.Receiving()
                .receiveUnredeemedItemWithNotDeclaredIdentityFromBoxWithAttrPage(item, order.getBoxId(), cartIdAN)
                .closePallet();

        verifyAnomalyInboundResponse(cartIdAN, inbound, NOT_DECLARED_CIS);
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

    private List<UnredeemedOrder> generateOrderList(int numberOfOrders) {
        List<UnredeemedOrder> orderList = new ArrayList<>();
        for (int i = 0; i < numberOfOrders; i++) {
            UnredeemedOrder order = UnredeemedOrder.builder()
                    .orderId(RandomUtil.randomStringNumbersOnly(10))
                    .uit(RandomUtil.generateUit())
                    .sku(UniqueId.getString())
                    .boxId(generateUnredeemedBox())
                    .palletId(generateUnredeemedPallet())
                    .build();
            orderList.add(order);
        }

        return orderList;
    }

    private static String generateUnredeemedBox() {
        return UNREDEEMED_BOX_PREFIX + UniqueId.getString();
    }

    private static String generateUnredeemedPallet() {
        return UNREDEEMED_PALLET_PREFIX + UniqueId.getString();
    }

    @Data
    @Builder
    private static class UnredeemedOrder {
        private String orderId;
        private String boxId;
        private String uit;
        private String sku;
        private String palletId;
    }
}
