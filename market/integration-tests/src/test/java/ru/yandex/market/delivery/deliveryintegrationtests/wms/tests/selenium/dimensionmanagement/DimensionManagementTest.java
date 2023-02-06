package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.dimensionmanagement;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import java.util.List;

@DisplayName("Selenium: Измерение ВГХ товара")
@Epic("Selenium Tests")
@Slf4j
public class DimensionManagementTest extends AbstractUiTest {

    private final Long VENDOR_ID = 1559L;
    public final String mobileTable = "DIMMOB_1";

    @RetryableTest
    @DisplayName("Тест обмера: Приёмка нового одноместного товара на мобильной станции")
    @ResourceLock("Тест обмера: Приёмка нового одноместного товара на мобильной станции")
    public void receiveNewItemMeasurementTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(UniqueId.getStringUUID())
                .name("Item Name")
                .build();

        String length = "11";
        String width = "12";
        String height = "13";
        String weight = "1.3";

        String pallet = processSteps.Incoming().createInboundAndPlaceToPallet(
                "wms/servicebus/putInboundRegistry/putInboundRegistryWithoutKorobytes.xml",
                item.getArticle(),
                VENDOR_ID
        );
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.OBM);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(item, pallet, cartId, false);
        uiSteps.Receiving().closeCart();

        uiSteps.Login().PerformLogin();
        List<String> snList = uiSteps.Balances().findByNznAndSupSku(item.getArticle(), cartId);

        uiSteps.Login().PerformLogin();
        uiSteps.DimensionManagement().measureOneBoxItemInMobileStation(mobileTable, snList.get(0),
                length, width, height, weight);

        ApiSteps.Inbound().assertItemKorobytes(VENDOR_ID, item.getArticle(), length, width, height, weight);
    }

    @RetryableTest
    @DisplayName("Тест обмера: Приёмка нового многоместного товара на мобильной станции")
    @ResourceLock("Тест обмера: Приёмка нового многоместного товара на мобильной станции")
    public void receiveMnogoboxItemMeasurementTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(UniqueId.getStringUUID())
                .name("Item Name")
                .build();

        String length1 = "11";
        String width1 = "12";
        String height1 = "13";
        String weight1 = "1.3";

        String length2 = "15";
        String width2 = "10";
        String height2 = "13";
        String weight2 = "2";

        double totalWeight = Double.parseDouble(weight1) + Double.parseDouble(weight2);

        String pallet = processSteps.Incoming().createInboundAndPlaceToPallet(
                "wms/servicebus/putInboundRegistry/putInboundRegistryWithMultiBox.xml",
                item.getArticle(),
                VENDOR_ID
        );
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.OBM);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receivePallet(pallet);
        uiSteps.Receiving().receiveBom(item, item.getName() + " BOM1", cartId);
        uiSteps.Receiving().receiveBom(item, item.getName() + " BOM2", cartId);
        uiSteps.Receiving().closeCart();

        uiSteps.Login().PerformLogin();
        List<String> snList = uiSteps.Balances().findByNznAndSupSku(item.getArticle(), cartId);

        uiSteps.Login().PerformLogin();
        uiSteps.DimensionManagement().measureTwoBoxItemInMobileStation(mobileTable, snList.get(0), snList.get(1),
                length1, width1, height1, weight1, length2, width2, height2, weight2);

        ApiSteps.Inbound().assertItemKorobytes(
                VENDOR_ID,
                item.getArticle(),
                length2,
                width1,
                height1,
                String.valueOf(totalWeight));
    }

}
