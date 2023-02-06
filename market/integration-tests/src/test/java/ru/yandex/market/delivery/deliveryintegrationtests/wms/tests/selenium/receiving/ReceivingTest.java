package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Receiving - Новая Приёмка")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class ReceivingTest extends AbstractUiTest {

    private final String skuPrefix = UniqueId.getString();

    @Property("wms.ui.receiving.printer")
    private String printer;

    private String pallet;
    private String cartId;

    public ReceivingTest() {
        PropertyLoader.newInstance().populate(this);
    }

    @BeforeEach
    void setUp() {
        pallet = createInbound();
        cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
    }

    @Step("Создаем поставку товаров")
    private String createInbound() {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry("wms/servicebus/putInboundRegistry/putInboundRegistryWithImeiAndSn.xml",
                inbound,
                skuPrefix,
                1559,
                false
        );
        return processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
    }

    @RetryableTest
    @DisplayName("Приёмка товара")
    @ResourceLock("Приёмка товара")
    void receiveBasic() {

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(1559)
                        .article(skuPrefix + "1")
                        .name("Without lifetime")
                        .build(),
                pallet,
                cartId,
                false
        );
    }

    @RetryableTest
    @DisplayName("Приёмка товара с IMEI")
    @ResourceLock("Приёмка товара с IMEI")
    void receiveIMEI() {

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(1559)
                        .article(skuPrefix + "2")
                        .name("Item with IMEI")
                        .checkImei(1)
                        .build(),
                pallet,
                cartId,
                false
        );
    }

    @RetryableTest
    @DisplayName("Приёмка товара с SN")
    @ResourceLock("Приёмка товара с SN")
    void receiveSN() {

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(1559)
                        .article(skuPrefix + "3")
                        .name("Item with SN")
                        .checkSn(1)
                        .build(),
                pallet,
                cartId,
                false
        );
    }

    @RetryableTest
    @DisplayName("Приёмка товара с 2 IMEI и 1 SN")
    @ResourceLock("Приёмка товара с 2 IMEI и 1 SN")
    void receiveImeiAndSn() {

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(1559)
                        .article(skuPrefix + "4")
                        .name("Item with 2 IMEI and 1 SN")
                        .checkImei(2)
                        .checkSn(1)
                        .build(),
                pallet,
                cartId,
                false
        );
    }

    @RetryableTest
    @DisplayName("Приёмка товара с дублирующим штрих кодом")
    @ResourceLock("Приёмка товара с дублирующим штрих кодом")
    void receiveDuplicateSku() {

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(1559)
                        .article(skuPrefix + "5")
                        .name("First duplicate item")
                        .hasDuplicates(true)
                        .build(),
                pallet,
                cartId,
                false
        );
    }

    @RetryableTest
    @DisplayName("Приёмка товара с некорректным штрихкодом")
    @ResourceLock("Приёмка товара с некорректным штрихкодом")
    void enterWrongBarcode() {

        InboundTable table = DatacreatorSteps.Location().createInboundTable();

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItemWithWrongBarcode(pallet);

        DatacreatorSteps.Location().deleteInboundTable(table);
    }
}
