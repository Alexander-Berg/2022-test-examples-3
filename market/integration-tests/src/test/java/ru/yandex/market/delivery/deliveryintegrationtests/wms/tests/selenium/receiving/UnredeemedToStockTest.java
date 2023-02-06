package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;
import java.util.Map;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.AnomalyType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.logistic.api.model.common.UnitCountType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Приёмка невыкупов на сток")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class UnredeemedToStockTest extends AbstractUiTest {

    public static int CIS_CARGO_TYPE_REQUIRED = 980;
    public static int CIS_CARGO_TYPE_OPTIONAL = 990;
    private final int CIS_HANDLE_MODE_ENABLED = 1;

    private static final String UNREDEEMED_BOX_PREFIX = "P000";
    private static final String UNREDEEMED_PALLET_PREFIX = "SC_LOT_";

    private static final String EXPIRED = "EXPIRED";
    private static final String ITEM_NAME = "e2e unredeemed test";

    private static final int VENDOR_ID = 1559;

    private static final String NO_CIS_EXPECTED = "";

    @RetryableTest
    @DisplayName("Приёмка годного невыкупа по УИТу")
    @ResourceLock("Приёмка годного невыкупа по УИТу")
    void receiveValidUnredeemedItemByUit() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.UNREDEEMED);

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

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(uit)
                .name(ITEM_NAME)
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);

        receiveUnredeemedItemByPalletInitial(item, box, false, inbound, UnitCountType.FIT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Приёмка годного невыкупа по EAN")
    @ResourceLock("Приёмка годного невыкупа по EAN")
    void receiveValidUnredeemedItemByEan() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.UNREDEEMED);

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

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(barcode)
                .name(ITEM_NAME)
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);

        receiveUnredeemedItemByPalletInitial(item, box, false, inbound, UnitCountType.FIT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Приёмка годного невыкупа по УИТу с обязательным Cis")
    @ResourceLock("Приёмка годного невыкупа по УИТу с обязательным Cis")
    void receiveValidUnredeemedItemByUitWithCis() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String barcode = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String box = generateUnredeemedBox();
        final String pallet = generateUnredeemedPallet();
        final String order = RandomUtil.randomStringNumbersOnly(10);
        final String testNumber = "1";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> generatedCis = RandomUtil.generateCis(gtin);
        final String cisDeclared = generatedCis.getLeft();
        final String cisActual = generatedCis.getRight();

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(sku)
                .article(uit)
                .name(ITEM_NAME)
                .checkCis(1)
                .instances(Map.of("CIS", cisActual))
                .build();

        ApiSteps.Inbound().putUnredeemedInboundRegistryWithCis(
                inbound,
                item,
                box,
                order,
                uit,
                pallet,
                barcode,
                cisDeclared,
                CIS_HANDLE_MODE_ENABLED,
                CIS_CARGO_TYPE_REQUIRED
        );

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);

        receiveUnredeemedItemByPalletInitial(item, box, false, inbound, UnitCountType.FIT, cisDeclared);
    }

    @RetryableTest
    @DisplayName("Приёмка годного невыкупа с опциональным CIS, который не соответствует заявленному в поставке")
    @ResourceLock("Приёмка годного невыкупа с опциональным CIS, который не соответствует заявленному в поставке")
    void receiveValidUnredeemedItemWithOptionalNotDeclaredCis() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String barcode = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String box = generateUnredeemedBox();
        final String pallet = generateUnredeemedPallet();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        final String gtin1 = "1".repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String gtin2 = "2".repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String cisDeclared = RandomUtil.generateCis(gtin1).getLeft();
        final String cisActual = RandomUtil.generateCis(gtin2).getRight();

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(sku)
                .article(uit)
                .name(ITEM_NAME)
                .checkCis(1)
                .instances(Map.of("CIS", cisActual))
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_OPTIONAL_CIS))
                .build();

        ApiSteps.Inbound().putUnredeemedInboundRegistryWithCis(
                inbound,
                item,
                box,
                order,
                uit,
                pallet,
                barcode,
                cisDeclared,
                CIS_HANDLE_MODE_ENABLED,
                CIS_CARGO_TYPE_OPTIONAL
        );

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);

        receiveUnredeemedItemByPalletInitial(item, box, false, inbound, UnitCountType.FIT, null);
    }

    @RetryableTest
    @DisplayName("Приёмка повреждённого невыкупа в брак")
    @ResourceLock("Приёмка повреждённого невыкупа в брак")
    void returnOneDamagedItem() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.UNREDEEMED);

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

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(uit)
                .name(ITEM_NAME)
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);

        receiveUnredeemedItemByPalletInitial(item, box, true, inbound, UnitCountType.DEFECT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Приёмка покоробочного невыкупа")
    @ResourceLock("Приёмка покоробочного невыкупа")
    void receiveUnredeemedByBox() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String barcode = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String box = generateUnredeemedBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(sku)
                .article(uit)
                .name(ITEM_NAME)
                .build();

        ApiSteps.Inbound().putUnredeemedInboundRegistryByBox(
                inbound,
                item,
                box,
                order,
                uit,
                barcode
        );

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(box);
        receiveUnredeemedItemByBoxInitial(item, box, false, inbound, UnitCountType.FIT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Приёмка автодопоставки невыкупа")
    @ResourceLock("Приёмка автодопоставки невыкупа")
    void returnAutoAdditionalItem() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final Inbound additionalInbound = ApiSteps.Inbound().putAdditionalInbound(
                inbound.getYandexId(),
                inbound.getPartnerId(),
                InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String barcode = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String box = generateUnredeemedBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(sku)
                .article(uit)
                .name(ITEM_NAME)
                .build();

        ApiSteps.Inbound().putUnredeemedInboundRegistryByBox(
                additionalInbound,
                item,
                box,
                order,
                uit,
                barcode
        );

        receiveUnredeemedItemByPalletInitial(item, box, false, additionalInbound, UnitCountType.FIT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Приёмка попалетного невыкупа с потерянными коробками")
    @ResourceLock("Приёмка попалетного невыкупа с потерянными коробками")
    void closeWithVerificationWithLostBox() {
        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String barcode = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String first_box = generateUnredeemedBox();
        final String second_box = generateUnredeemedBox();
        final String pallet = generateUnredeemedPallet();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(sku)
                .article(uit)
                .name("e2e unredeemed test")
                .build();

        ApiSteps.Inbound().putUnredeemedInboundRegistryByPalletWithTwoBoxes(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                first_box,
                second_box,
                order,
                uit,
                sku,
                String.valueOf(VENDOR_ID),
                pallet,
                barcode);

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);
        receiveUnredeemedItemWithTwoBoxByPalletInitial(item, first_box, second_box, false, inbound, UnitCountType.FIT,
                NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Невыкуп товара, просроченного по ОСГ на выход")
    @ResourceLock("Невыкуп товара, просроченного по ОСГ на выход")
    void unredeemedExpiredItem() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.UNREDEEMED);

        final String sku = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String box = generateUnredeemedBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);
        final String pallet = UNREDEEMED_PALLET_PREFIX + UniqueId.getString();

        ApiSteps.Inbound().putUnredeemedInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box,
                order,
                uit,
                sku,
                String.valueOf(VENDOR_ID),
                pallet,
                UniqueId.getString(),
                true);

        int daysToExpiration = -2;

        final Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .sku(sku)
                .article(uit)
                .name(ITEM_NAME)
                .shelfLife(true)
                .expDate(DateUtil.currentUtcDatePlusDays(daysToExpiration))
                .toExpireDaysQuantity(String.valueOf(daysToExpiration + 1))
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(pallet);

        uiSteps.Login().PerformLogin();

        var cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Receiving()
                .receiveUnredeemedExpiredItem(item, box, cartId)
                .closePallet();

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInbound(getInboundResponse, EXPIRED, NO_CIS_EXPECTED);
    }

    private void receiveUnredeemedItemByPalletInitial(final Item item,
                                                      final String box,
                                                      final boolean isDamaged,
                                                      final Inbound inbound,
                                                      final UnitCountType itemTypeExpected,
                                                      final String cisExpected) {
        var cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveReturnItemFromBoxWithAttrPage(item, box, cartId, isDamaged)
                .closePallet();

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInbound(getInboundResponse, itemTypeExpected.getName(), cisExpected);
    }

    private void receiveUnredeemedItemWithTwoBoxByPalletInitial(final Item item,
                                                                final String firstBox,
                                                                final String secondBox,
                                                                final boolean isDamaged,
                                                                final Inbound inbound,
                                                                final UnitCountType itemTypeExpected,
                                                                final String cisExpected) {
        var cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveReturnItemFromBoxWithAttrPage(item, firstBox, cartId, isDamaged)
                .closePallet();
        processSteps.Incoming().approveCloseInboundWithWarning(inbound.getFulfillmentId());

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInbound(getInboundResponse, itemTypeExpected.getName(), cisExpected);
    }

    private void receiveUnredeemedItemByBoxInitial(final Item item,
                                                   final String box,
                                                   final boolean isDamaged,
                                                   final Inbound inbound,
                                                   final UnitCountType itemTypeExpected,
                                                   final String cisExpected) {
        var cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveReturnItem(item, box, cartId, isDamaged)
                .closePallet();

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInbound(getInboundResponse, itemTypeExpected.getName(), cisExpected);
    }

    private static String generateUnredeemedBox() {
        return UNREDEEMED_BOX_PREFIX + UniqueId.getString();
    }

    private static String generateUnredeemedPallet() {
        return UNREDEEMED_PALLET_PREFIX + UniqueId.getString();
    }
}
