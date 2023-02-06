package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.endtoend;

import java.util.List;
import java.util.Random;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ApiClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.IrisClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Iris")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class IrisTest extends AbstractUiTest {

    private final ApiClient ApiClient = new ApiClient();
    private final IrisClient irisClient = new IrisClient();

    @Property("wms.ui.receiving.table")
    private String table;

    @Property("wms.ui.receiving.printer")
    private String printer;

    private Inbound inbound;
    private final Long VENDOR_ID_FROM_MDM = Long.valueOf(2020100702);
    private final String vendor_sku = "IV202010070201";
    private final Long VENDOR_ID = 1559L;
    public final String mobileTable = "DIMMOB_1";

    @Step("Создаем поставку товара: {sku} и принимаем её на паллету")
    private String createInboundAndPlaceToPallet(String requestFilePath, String sku) {
        inbound = ApiSteps.Inbound().createInbound(requestFilePath, sku);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(inbound.getFulfillmentId(), 1);
        uiSteps.Login().PerformLogin();
        return uiSteps.Receiving().findPalletOfInbound(inbound.getFulfillmentId());
    }


    @RetryableTest
    @DisplayName("Тест обмера: Приёмка нового одноместного товара и проверка пуша в IRIS")
    @ResourceLock("Тест обмера: Приёмка нового одноместного товара и проверка пуша в IRIS")
    void receiveNewItemMeasurementToIrisTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID)
                .article(UniqueId.getStringUUID())
                .name("Item Name")
                .build();

        String length = "11";
        String width = "12";
        String height = "13";
        String weight = "1.3";

        String pallet = createInboundAndPlaceToPallet(
                "wms/wrapRequests/createInboundWithoutKorobytes.xml",
                item.getArticle()
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

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        ApiSteps.Inbound().assertItemKorobytes(VENDOR_ID, item.getArticle(), length, width, height, weight);

        ApiSteps.Iris().checkIrisPushVGH(length, width, height, weight, VENDOR_ID, item.getArticle( ));
    }


    @RetryableTest
    @Disabled ("Пока не работает тестовая интеграция с ириской")
    @DisplayName("Тест обмера: Приёмка старого одноместного товара, пуш в IRIS и проверка что новые ВГХ доехали в ирис через мдм")
    @ResourceLock("Тест обмера: Приёмка старого одноместного товара, пуш в IRIS и проверка что новые ВГХ доехали в ирис через мдм")
    void receiveNewItemMeasurementToIrisAndWaitForResponseTest() {
        Item item = Item.builder()
                .vendorId(VENDOR_ID_FROM_MDM)
                .article(vendor_sku)
                .name("Item Name")
                .build();

        final Random random = new Random();
        String length = "1"+ random.nextInt(6);
        String width = "1"+ random.nextInt(6);
        String height = "1"+ random.nextInt(6);
        String weight = "1.3"+ random.nextInt(10);


        String pallet = createInboundAndPlaceToPallet(
                "wms/wrapRequests/createInboundWithoutKorobytesNeedMeasurement.xml",
                item.getArticle()
        );
        String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.OBM);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(item, pallet, cartId, false);
        uiSteps.Receiving().closeCart();

        uiSteps.Login().PerformLogin();
        List<String> snList = uiSteps.Balances().findByNznAndSupSku(item.getArticle(), cartId);

        tsdSteps.Login().PerformLogin();

        tsdSteps.Inbound().reMeasureItem(cartId, snList.get(0), length, width, height, weight);

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        ApiSteps.Iris().checkIrisPushVGH(length, width, height, weight, VENDOR_ID_FROM_MDM, item.getArticle( ));

        ApiSteps.Iris().waitTrustworthyInfoAppear(VENDOR_ID_FROM_MDM, item.getArticle( ), length, width, height, weight);


    }

}
