package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import static org.hamcrest.Matchers.is;


@DisplayName("Selenium: Receiving - Новая Приёмка")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class ReceivingDefaultCartTest extends AbstractUiTest {

    @Property("wms.ui.receiving.table")
    private String table;

    @Property("wms.ui.receiving.printer")
    private String printer;

    private Inbound inbound;
    private final Long VENDOR_ID = 1559L;
    private final String STORAGE_CELL = "4-02";
    private final String inboundExternalRequestIdPrefix = "Зп-";
    private final String externalRequestId = generateExternalRequestId();

    private final ServiceBus serviceBus = new ServiceBus();

    @Step("Создаем поставку товаров: {sku} СГ: {isShelfLife}, и принимаем её на паллету")
    private String createInboundAndPlaceToPallet(String sku, boolean isShelfLife) {
        inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry("wms/servicebus/putInboundRegistry/putInboundRegistry.xml",
                inbound,
                sku,
                1559,
                isShelfLife
        );

        return processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
    }

    @Step("Проверяем детали  поставки")
    private void verifyInboundDetails(Inbound inbound,
                                      String declared,
                                      String actual,
                                      String defect,
                                      String surplus) {
        log.info("Checking return inbound details");
        String bodyPath = "root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.";
        serviceBus.getInboundDetails(inbound)
                .body(bodyPath + "declared", is(declared))
                .body(bodyPath + "actual", is(actual))
                .body(bodyPath + "defect", is(defect))
                .body(bodyPath + "surplus", is(surplus));
    }

    @RetryableTest
    @DisplayName("Приёмка товара в дефолтную тару с последующим размещением товара")
    @ResourceLock("Приёмка товара в дефолтную тару с последующим размещением товара")
    void receiveItemIntoDefaultCartTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article("ReceivingTestItem5")
                .name("Item Name")
                .build();
        String pallet = createInboundAndPlaceToPallet(item.getArticle(), item.isShelfLife());
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().enterDefaultCart(item, pallet, cartId)
                .enterBarcode(item)
                .checkInfo(item)
                .enterQuantity(item.getQuantity());
        uiSteps.Receiving().isBarcodeInputPageDisplayed();

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(cartId, STORAGE_CELL);
    }

    @RetryableTest
    @DisplayName("Приёмка нового товара с СГ. Только дата окончания")
    @ResourceLock("Приёмка нового товара с СГ. Только дата окончания")
    void receiveItemWithExpDateTest() {
        int daysToExpiration = 90;
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(UniqueId.getStringUUID())
                .name("Item Name")
                .shelfLife(true)
                .expDate(DateUtil.currentUtcDatePlusDays(daysToExpiration))
                .toExpireDaysQuantity(String.valueOf(daysToExpiration + 1))
                .build();
        String pallet = createInboundAndPlaceToPallet(item.getArticle(), item.isShelfLife());
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().enterDefaultCart(item, pallet, cartId);
        uiSteps.Receiving().receiveItemWithExpDateFirstTime(item);
        uiSteps.Receiving().receiveItemWithExpDateSecondTime(item);

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(cartId, STORAGE_CELL);

        verifyInboundDetails(inbound, "100", "2", "0", "0");
    }

    @RetryableTest
    @DisplayName("Приёмка нового товара с СГ. Дата окончания + срок годности")
    @ResourceLock("Приёмка нового товара с СГ. Дата окончания + срок годности")
    void receiveItemWithDurationAndExpDateTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(UniqueId.getStringUUID())
                .name("Item Name")
                .shelfLife(true)
                .expDate(DateUtil.currentDatePlusDays(90))
                .toExpireDaysQuantity("365")
                .build();
        String pallet = createInboundAndPlaceToPallet(item.getArticle(), item.isShelfLife());
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().enterDefaultCart(item, pallet, cartId);
        uiSteps.Receiving().receiveItemWithDurationAndExpDateFirstTime(item, "12");
        uiSteps.Receiving().receiveItemWithExpDateSecondTime(item);

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(cartId, STORAGE_CELL);

        verifyInboundDetails(inbound, "100", "2", "0", "0");
    }

    @RetryableTest
    @DisplayName("Приёмка нового товара с СГ. Дата производства + срок годности")
    @ResourceLock("Приёмка нового товара с СГ. Дата производства + срок годности")
    void receiveItemWithDurationAndCreationDateTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(UniqueId.getStringUUID())
                .name("Item Name")
                .shelfLife(true)
                .creationDate(DateUtil.currentDateMinusDays(90))
                .toExpireDaysQuantity("365")
                .build();
        String pallet = createInboundAndPlaceToPallet(item.getArticle(), item.isShelfLife());
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().enterDefaultCart(item, pallet, cartId);
        uiSteps.Receiving().receiveItemWithDurationAndCreationDateFirstTime(item, "12");
        uiSteps.Receiving().receiveItemWithDurationAndCreationDateSecondTime(item);

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(cartId, STORAGE_CELL);

        verifyInboundDetails(inbound, "100", "2", "0", "0");
    }

    @RetryableTest
    @DisplayName("Приёмка нового товара с СГ. Дата производства + Дата окончания")
    @ResourceLock("Приёмка нового товара с СГ. Дата производства + Дата окончания")
    void receiveItemWithCreationAndExpirationDateTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(UniqueId.getStringUUID())
                .name("Item Name")
                .shelfLife(true)
                .creationDate(DateUtil.currentDateMinusDays(120))
                .expDate(DateUtil.currentDatePlusDays(90))
                .toExpireDaysQuantity("210")
                .build();

        String pallet = createInboundAndPlaceToPallet(item.getArticle(), item.isShelfLife());
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().enterDefaultCart(item, pallet, cartId);
        uiSteps.Receiving().receiveItemWithCreationAndExpDateFirstTime(item);
        uiSteps.Receiving().receiveItemWithExpDateSecondTime(item);

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(cartId, STORAGE_CELL);

        verifyInboundDetails(inbound, "100", "2", "0", "0");
    }

    @Step("Создаем поставку товаров: {sku} СГ: {isShelfLife} по номеру в Аксапте, и принимаем её на паллету")
    private String createNewInboundWithExternalId(String sku, boolean isShelfLife) {
        inbound = ApiSteps.Inbound().putInboundWithExternalRequestId(InboundType.DEFAULT, externalRequestId);
        ApiSteps.Inbound().putInboundRegistry("wms/servicebus/putInboundRegistry/putInboundRegistry.xml",
                inbound,
                sku,
                1559,
                isShelfLife
        );
        String externalRequestNumbers = externalRequestId.substring(3, 12);
        return processSteps.Incoming().placeInboundByExternalRequestIdToPallet(inbound.getFulfillmentId(), externalRequestNumbers, 1);
    }

    @RetryableTest
    @DisplayName("Приёмка товара в дефолтную тару с последующим размещением товара по номеру поставки в Аксапте")
    @ResourceLock("Приёмка товара в дефолтную тару с последующим размещением товара по номеру поставки в Аксапте")
    void receiveItemIntoDefaultCartWithExternalTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article("ReceivingTestItem5")
                .name("Item Name")
                .build();
        String pallet = createNewInboundWithExternalId(item.getArticle(), item.isShelfLife());
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().enterDefaultCart(item, pallet, cartId)
                .enterBarcode(item)
                .checkInfo(item)
                .enterQuantity(item.getQuantity());
        uiSteps.Receiving().isBarcodeInputPageDisplayed();

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(cartId, STORAGE_CELL);

    }

    private String generateExternalRequestId() {
        return inboundExternalRequestIdPrefix + RandomUtil.randomStringNumbersOnly(9);
    }
}


