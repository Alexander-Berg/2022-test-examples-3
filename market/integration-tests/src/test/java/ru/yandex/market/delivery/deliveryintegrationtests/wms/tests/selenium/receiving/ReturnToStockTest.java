package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.restassured.path.xml.XmlPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InitialReceivingType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Приёмка возвратов на сток")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class ReturnToStockTest extends AbstractUiTest {

    private static final String ITEM_TYPE_FIT = "FIT";
    private static final String ITEM_TYPE_DEFECT = "DEFECT";
    private static final String NO_CIS_EXPECTED = "";

    private static final String RETURN_REASON_NOT_FIT = "DO_NOT_FIT";
    private static final String RETURN_REASON_BAD_QUALITY = "BAD_QUALITY";
    private static final int VENDOR_ID = 1559;

    private static final String RETURN_BOX_PREFIX = "VOZ_FF_";

    @RetryableTest
    @DisplayName("Возврат одного годного товара")
    @ResourceLock("Возврат одного годного товара")
    void returnOneValidItem() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final String sku = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String returnId = RandomUtil.randomStringNumbersOnly(6);
        final String box = generateReturnBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        ApiSteps.Inbound().putReturnInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box,
                order,
                returnId,
                RETURN_REASON_NOT_FIT,
                uit,
                sku,
                String.valueOf(VENDOR_ID));

        final Item item = Item.builder()
                .vendorId(1559)
                .article(uit)
                .name("e2e client return test")
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(box);

        receiveReturnedItem(item, box, false, inbound, ITEM_TYPE_FIT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Возврат одного бракованного товара")
    @ResourceLock("Возврат одного бракованного товара")
    void returnOneDamagedItem() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final String sku = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String returnId = RandomUtil.randomStringNumbersOnly(6);
        final String box = generateReturnBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        ApiSteps.Inbound().putReturnInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box,
                order,
                returnId,
                RETURN_REASON_BAD_QUALITY,
                uit,
                sku,
                String.valueOf(VENDOR_ID));

        final Item item = Item.builder()
                .vendorId(1559)
                .article(uit)
                .name("e2e client return test")
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(box);

        receiveReturnedItem(item, box, true, inbound, ITEM_TYPE_DEFECT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Возврат одного из двух одинаковых годных товаров с КИЗом и IMEI")
    @ResourceLock("Возврат одного из двух одинаковых годных товаров с КИЗом и IMEI")
    void returnOneOfTwoSimilarValidItemsInOrder() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final String sku = UniqueId.getString();

        final String firstUit = RandomUtil.generateUit();
        final String secondUit = RandomUtil.generateUit();

        final String gtin = "1".repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> firstCis = RandomUtil.generateCis(gtin);
        final String firstCisDeclared = firstCis.getLeft();
        final String firstCisActual = firstCis.getRight();
        final String secondCisDeclared = RandomUtil.generateCis(gtin).getLeft();

        final String firstImei = RandomUtil.generateImei();
        final String secondImei = RandomUtil.generateImei();

        final String returnId = RandomUtil.randomStringNumbersOnly(6);
        final String box = generateReturnBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        ApiSteps.Inbound().putReturnInboundRegistryWithIdentities(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box,
                order,
                returnId,
                RETURN_REASON_NOT_FIT,
                List.of(firstUit, secondUit),
                List.of(firstCisDeclared, secondCisDeclared),
                List.of(firstImei, secondImei),
                sku,
                String.valueOf(VENDOR_ID));

        final Item item = Item.builder()
                .vendorId(1559)
                .article(firstUit)
                .name("e2e client return test")
                .checkCis(1)
                .instances(Map.of("CIS", firstCisActual, "IMEI", firstImei))
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(box);

        receiveReturnedItem(item, box, false, inbound, ITEM_TYPE_FIT, firstCisDeclared);
    }

    @RetryableTest
    @DisplayName("Возврат двух годных товаров в одном возврате")
    @ResourceLock("Возврат двух годных товаров в одном возврате")
    void returnTwoValidItemsAtOnce() {
        final Inbound inbound =
                ApiSteps.Inbound().putInbound("wms/servicebus/putInbound/putInbound.xml", InboundType.RETURNS);

        final String sku = UniqueId.getString();
        final String uit1 = RandomUtil.generateUit();
        final String uit2 = RandomUtil.generateUit();
        final String returnId = RandomUtil.randomStringNumbersOnly(6);
        final String box = generateReturnBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        ApiSteps.Inbound().putReturnInboundRegistryWithTwoItemsInOneReturn(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box,
                order,
                returnId,
                RETURN_REASON_NOT_FIT,
                List.of(uit1, uit2),
                sku,
                String.valueOf(VENDOR_ID));
        var cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        final Item item1 = Item.builder()
                .vendorId(VENDOR_ID)
                .article(uit1)
                .name("e2e client return test")
                .build();
        final Item item2 = item1.toBuilder()
                .article(uit2)
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(box);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveReturnItem(item1, box, cartId, false);

        uiSteps.Receiving()
                .continueReceivingReturnItem(item2, box, cartId, false)
                .closePallet();

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, NO_CIS_EXPECTED);
    }


    @RetryableTest
    @DisplayName("Приемка обновляемой поставки")
    @ResourceLock("Приемка обновляемой поставки")
    void updatableReturn() {

        final Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.UPDATABLE_CUSTOMER_RETURN);

        final String sku = UniqueId.getString();
        final String uit = RandomUtil.generateUit();
        final String returnId = RandomUtil.randomStringNumbersOnly(6);
        final String box = generateReturnBox();
        final String order = RandomUtil.randomStringNumbersOnly(10);

        ApiSteps.Inbound().putUpdatableReturnInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box);

        final Item item = Item.builder()
                .vendorId(1559)
                .article(uit)
                .name("e2e client return test")
                .build();

        processSteps.Incoming().initiallyReceiveReturnContainersWithoutQualityAttributes(box);

        processSteps.Incoming().linkBoxWithOrderId(box, order);

        ApiSteps.Inbound().verifyInboundStatusIs(inbound, "25");
        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInboundUnitCountTypeInInitial(getInboundResponse, List.of(ITEM_TYPE_FIT), 1,
                InitialReceivingType.BY_BOX);

        ApiSteps.Inbound().putReturnInboundRegistry(
                String.valueOf(inbound.getYandexId()),
                inbound.getPartnerId(),
                box,
                order,
                returnId,
                ITEM_TYPE_FIT,
                uit,
                sku,
                String.valueOf(VENDOR_ID));

        receiveReturnedItem(item, box, false, inbound, ITEM_TYPE_FIT, NO_CIS_EXPECTED);
    }

    @Step("Принимаем возврат товара")
    private void receiveReturnedItem(final Item item,
                                     final String box,
                                     final boolean isDamaged,
                                     final Inbound inbound,
                                     final String itemTypeExpected,
                                     final String cisExpected) {
        var cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveReturnItem(item, box, cartId, isDamaged)
                .closePallet();

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
        ApiSteps.Inbound().checkGetInbound(getInboundResponse, itemTypeExpected, cisExpected);
    }

    private static String generateReturnBox() {
        return RETURN_BOX_PREFIX + UniqueId.getString();
    }
}
