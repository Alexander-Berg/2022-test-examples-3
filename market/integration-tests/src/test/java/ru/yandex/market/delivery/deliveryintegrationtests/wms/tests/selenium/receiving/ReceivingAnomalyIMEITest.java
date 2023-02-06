package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.Map;
import java.util.Set;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.AnomalyType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;

@DisplayName("Selenium: Приёмка в аномалии товаров c IMEI")
@Epic("Selenium Tests")
@Slf4j
public class ReceivingAnomalyIMEITest extends AnomalySeleniumTest {

    private static final String SKU_PREFIX = UniqueId.getString();

    private static final String REQUEST_TEMPLATE_PATH =
            "wms/servicebus/putInboundRegistry/putInboundRegistryWithImeiAndSn.xml";

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с IMEI не проходящим проверку регулярным выражением")
    @ResourceLock("Принимаем в аномалии товар с IMEI не проходящим проверку регулярным выражением")
    public void createAndReceiveInboundWithRequiredInValidCis() {
        String imeiItemBarcode = SKU_PREFIX + "2";

        Item item = Item.builder()
                .vendorId(1559)
                .article(imeiItemBarcode)
                .name("Item with IMEI")
                .checkImei(1)
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_IMEI))
                .instances(Map.of("IMEI", "bad_imei"))
                .build();

        XmlPath getInboundResponse = receiveAnomalyItem(item, SKU_PREFIX, anomalyPlacementLoc,
                REQUEST_TEMPLATE_PATH, areaKey);

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, "INCORRECT_IMEI", "bad_imei");
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с IMEI не проходящим валидацию на сервере (Mod10)")
    @ResourceLock("Принимаем в аномалии товар с IMEI не проходящим валидацию на сервере (Mod10)")
    public void invalidMod10Imei() {
        String imeiItemBarcode = SKU_PREFIX + "2";

        Item item = Item.builder()
                .vendorId(1559)
                .article(imeiItemBarcode)
                .name("Item with IMEI")
                .checkImei(1)
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_IMEI))
                .instances(Map.of("IMEI", "00000000001000000"))
                .build();

        XmlPath getInboundResponse = receiveAnomalyItem(item, SKU_PREFIX, anomalyPlacementLoc,
                REQUEST_TEMPLATE_PATH, areaKey);

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, "INCORRECT_IMEI", "00000000001000000");
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с неуникальным IMEI")
    @ResourceLock("Принимаем в аномалии товар с неуникальным IMEI")
    public void duplicatedIMEI() {
        String imei = RandomUtil.generateImei();
        String imeiItemBarcode = SKU_PREFIX + "2";

        Item goodItem = Item.builder()
                .vendorId(1559)
                .article(imeiItemBarcode)
                .name("Item with IMEI")
                .checkImei(1)
                .instances(Map.of("IMEI", imei))
                .build();

        receiveItemOnStock(goodItem, SKU_PREFIX, REQUEST_TEMPLATE_PATH);

        Item anomalyItem = Item.builder()
                .vendorId(1559)
                .article(imeiItemBarcode)
                .name("Item with IMEI")
                .checkImei(1)
                .anomalyTypes(Set.of(AnomalyType.INCORRECT_IMEI))
                .instances(Map.of("IMEI", imei))
                .build();

        XmlPath getInboundResponse = receiveAnomalyItem(anomalyItem, SKU_PREFIX,
                anomalyPlacementLoc, REQUEST_TEMPLATE_PATH, areaKey);

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, "INCORRECT_IMEI", imei);
    }


}
