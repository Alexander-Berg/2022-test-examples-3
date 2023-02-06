package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.Map;
import java.util.Set;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.AnomalyType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;

@DisplayName("Selenium: Приёмка в аномалии товаров c SN")
@Epic("Selenium Tests")
@Slf4j
public class ReceivingAnomalySNTest extends AnomalySeleniumTest {

    private static final String REQUEST_TEMPLATE_PATH =
            "wms/servicebus/putInboundRegistry/putInboundRegistryWithImeiAndSn.xml";

    private static final String SKU_PREFIX = UniqueId.getString();

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с SN не проходящим проверку регулярным выражением")
    @ResourceLock("Принимаем в аномалии товар с SN не проходящим проверку регулярным выражением")
    public void badSN() {
        String snItemBarcode = SKU_PREFIX + "3";
        Item item = Item.builder()
                .vendorId(1559)
                .article(snItemBarcode)
                .name("Item with SN")
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_SERIAL_NUMBER))
                .instances(Map.of("SN", "bad_sn"))
                .checkSn(1)
                .build();

        XmlPath getInboundResponse = receiveAnomalyItem(item, SKU_PREFIX, anomalyPlacementLoc,
                REQUEST_TEMPLATE_PATH, areaKey);

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, "INCORRECT_SERIAL_NUMBER", "bad_sn");
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с неуникальным SN")
    @ResourceLock("Принимаем в аномалии товар с неуникальным SN")
    public void duplicatedSN() {
        String sn = RandomStringUtils.randomAlphanumeric(10);
        String snItemBarcode = SKU_PREFIX + "3";

        Item goodItem = Item.builder()
                .vendorId(1559)
                .article(snItemBarcode)
                .name("Item with SN")
                .checkSn(1)
                .instances(Map.of("SN", sn))
                .build();

        receiveItemOnStock(goodItem, SKU_PREFIX, REQUEST_TEMPLATE_PATH);

        Item anomalyItem = Item.builder()
                .vendorId(1559)
                .article(snItemBarcode)
                .name("Item with SN")
                .checkSn(1)
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_SERIAL_NUMBER))
                .instances(Map.of("SN", sn))
                .build();

        XmlPath getInboundResponse = receiveAnomalyItem(anomalyItem, SKU_PREFIX,
                anomalyPlacementLoc, REQUEST_TEMPLATE_PATH, areaKey);

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, "INCORRECT_SERIAL_NUMBER", sn);
    }
}
