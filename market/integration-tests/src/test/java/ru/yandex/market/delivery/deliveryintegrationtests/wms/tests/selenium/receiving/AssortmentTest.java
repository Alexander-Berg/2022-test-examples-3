package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.List;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Приёмка ассортиментного товара")
@Epic("Selenium Tests")
public class AssortmentTest extends AbstractUiTest {
    private final int vendor = 1559;

    private String skuPrefix;
    private String cartId;

    @BeforeEach
    void setUp() {
        skuPrefix = UniqueId.getString();
        cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
    }

    @RetryableTest
    @DisplayName("Приёмка ассортиментного товара с одинаковым штрих-кодом")
    @ResourceLock("Приёмка ассортиментного товара с одинаковым штрих-кодом")
    void receiveAssortmentSkuWithSameBarcode() {
        XmlPath result = putInboundAndReceive(
                "wms/servicebus/putInboundRegistry/putInboundRegistryAssortmentWithSameBarcode.xml",
                List.of(Item.builder()
                                .vendorId(vendor)
                                .article(skuPrefix)
                                .name("Barbie blonde")
                                .hasDuplicates(true)
                                .quantity(1)
                                .build(),
                        Item.builder()
                                .vendorId(vendor)
                                .article(skuPrefix)
                                .name("Barbie brunette")
                                .hasDuplicates(true)
                                .quantity(2)
                                .build(),
                        Item.builder()
                                .vendorId(vendor)
                                .article(skuPrefix)
                                .name("Barbie ginger")
                                .hasDuplicates(true)
                                .quantity(1)
                                .build()
                )
        );
        ApiSteps.Inbound().checkGetInbound(result, "FIT", "");
    }

    @RetryableTest
    @DisplayName("Приёмка ассортиментного товара с разным штрих-кодом")
    @ResourceLock("Приёмка ассортиментного товара с разным штрих-кодом")
    void receiveAssortmentSkuWithDifferentBarcode() {
        var result = putInboundAndReceive(
                "wms/servicebus/putInboundRegistry/putInboundRegistryAssortmentWithDifferentBarcode.xml",
                List.of(Item.builder()
                                .vendorId(vendor)
                                .article(skuPrefix + "1")
                                .name("Barbie blonde")
                                .hasDuplicates(false)
                                .quantity(1)
                                .build(),
                        Item.builder()
                                .vendorId(vendor)
                                .article(skuPrefix + "2")
                                .name("Barbie brunette")
                                .hasDuplicates(false)
                                .quantity(2)
                                .build(),
                        Item.builder()
                                .vendorId(vendor)
                                .article(skuPrefix + "3")
                                .name("Barbie ginger")
                                .hasDuplicates(false)
                                .quantity(1)
                                .build()
                )
        );
        ApiSteps.Inbound().checkGetInbound(result, "FIT", "");
    }

    private XmlPath putInboundAndReceive(String registryFile, List<Item> items) {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry(
                registryFile,
                inbound,
                skuPrefix,
                vendor,
                false
        );
        var pallet = processSteps.Incoming().placeInboundToPallet(inbound.getPartnerId());
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveItems(items, pallet, cartId, false, true)
                .closePallet();
        processSteps.Incoming().approveCloseInbound(inbound.getPartnerId());
        return ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
    }
}
