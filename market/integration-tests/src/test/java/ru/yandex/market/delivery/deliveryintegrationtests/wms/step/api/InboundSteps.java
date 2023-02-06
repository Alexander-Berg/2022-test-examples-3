package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.qameta.allure.Step;
import io.restassured.path.xml.XmlPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.RadiatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Box;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InitialReceivingType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.logistic.api.model.common.RegistryType;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.api.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetInboundRequest;
import ru.yandex.market.logistic.api.model.fulfillment.request.PutInboundRegistryRequest;
import ru.yandex.market.logistic.api.model.fulfillment.request.PutInboundRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundRegistryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundResponse;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.hamcrest.Matchers.is;

@Slf4j
public class InboundSteps {
    private static final ServiceBus SERVICE_BUS = new ServiceBus();
    private static final DatacreatorClient DATA_CREATOR = new DatacreatorClient();
    private static final RadiatorClient RADIATOR_CLIENT = new RadiatorClient();
    private static final String ITEM_TYPE_INCORRECT_CIS = "INCORRECT_CIS";
    private static final String ITEM_TYPE_INCORRECT_IMEI = "INCORRECT_IMEI";
    private static final String ITEM_TYPE_INCORRECT_SN = "INCORRECT_SERIAL_NUMBER";
    private static final String ITEM_TYPE_NON_COMPLIENT = "NON_COMPLIENT";
    private static final String YM_ITEM_MAX_LENGTH = "YM_ITEM_MAX_LENGTH";
    private static final Set<String> NON_COMPLIENT_ITEM_TYPES = Set.of(
            ITEM_TYPE_INCORRECT_CIS, ITEM_TYPE_INCORRECT_IMEI, ITEM_TYPE_INCORRECT_SN
    );


    protected InboundSteps() {
    }


    @Step("Создаем поставку товаров без СГ на завтра")
    public Inbound createInbound(String sku) {
        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                sku,
                SERVICE_BUS.getToken(),
                false
        );

        return SERVICE_BUS.createInbound(body);
    }

    @Step("Создаем поставку товаров без СГ на завтра")
    public Inbound createNonSortInbound(String sku) {
        var maxLength = DATA_CREATOR.getSettingValue(YM_ITEM_MAX_LENGTH);
        var itemLength = Integer.parseInt(maxLength) + 10;

        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInboundCustomLength.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                sku,
                SERVICE_BUS.getToken(),
                false
        ).replaceAll("LENGTH_PLACEHOLDER", String.valueOf(itemLength));

        return SERVICE_BUS.createInbound(body);
    }

    @Step("Создаем поставку на завтра")
    public Inbound createInbound(String sku, Boolean isShelfLife) {
        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                sku,
                SERVICE_BUS.getToken(),
                isShelfLife,
                false
        );
        return SERVICE_BUS.createInbound(body);
    }

    @Step("Создаем поставку на завтра")
    public Inbound createInbound(Boolean isShelfLife, String... sku) {
        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                sku,
                SERVICE_BUS.getToken(),
                isShelfLife,
                false
        );
        return SERVICE_BUS.createInbound(body);
    }

    @Step("Создаем поставку на завтра")
    public Inbound createInbound(String requestFilePath, String sku) {
        String body = FileUtil.bodyStringFromFile(requestFilePath,
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                sku,
                SERVICE_BUS.getToken(),
                false
        );
        return SERVICE_BUS.createInbound(body);
    }

    public Inbound putInbound(InboundType inboundType) {
        return putInbound("wms/servicebus/putInbound/putInbound.xml", inboundType);
    }

    @Step("Создаем поставку на завтра")
    public Inbound putInbound(String requestFilePath, InboundType inboundType) {
        String body = FileUtil.bodyStringFromFile(
                requestFilePath,
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                inboundType.getCode()
        );
        return SERVICE_BUS.putInbound(body);
    }

    @Step("Создаем допоставку")
    public Inbound putAdditionalInbound(
            long externalParentId,
            String internalParentId,
            InboundType inboundType) {
        String body = FileUtil.bodyStringFromFile(
                        "wms/servicebus/putInbound/putInboundAdditional.xml")
                .replaceAll("TOKEN_PLACEHOLDER", SERVICE_BUS.getToken())
                .replaceAll("HASH_PLACEHOLDER", UniqueId.getStringUUID())
                .replaceAll("CURR_YANDEX_ID_PLACEHOLDER", String.valueOf(UniqueId.get()))
                .replaceAll("INBOUND_TYPE_PLACEHOLDER", String.valueOf(inboundType.getCode()))
                .replaceAll("INTERVAL_PLACEHOLDER", DateUtil.tomorrowDateTime())
                .replaceAll("PARENT_YANDEX_ID_PLACEHOLDER", String.valueOf(externalParentId))
                .replaceAll("PARENT_PARTNER_ID_PLACEHOLDER", internalParentId);
        return SERVICE_BUS.putInbound(body);
    }

    @Step("Создаем реестр поставки")
    public void putInboundRegistry(String requestFilePath,
                                   Inbound inbound,
                                   String sku,
                                   long vendorId,
                                   boolean isShelfLife) {
        String body = FileUtil.bodyStringFromFile(requestFilePath,
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                inbound.getYandexId(),
                inbound.getPartnerId(),
                sku,
                vendorId,
                isShelfLife
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр поставки")
    public void putInboundRegistry(String requestFilePath,
                                   Inbound inbound,
                                   String uit,
                                   String sku,
                                   long vendorId,
                                   String lot,
                                   String palletId,
                                   boolean isShelfLife) {
        String body = FileUtil.bodyStringFromFile(requestFilePath,
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                inbound.getYandexId(),
                inbound.getPartnerId(),
                uit,
                sku,
                vendorId,
                lot,
                palletId,
                isShelfLife
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем поставку на завтра")
    public PutInboundResponse putInbound() {
        String yandexId = UniqueId.getString();
        log.info("yandexId: {}", yandexId);
        PutInboundRequest putInboundRequest = new PutInboundRequest(
                ru.yandex.market.logistic.api.model.common.Inbound.builder(
                                new ResourceId(yandexId, null),
                                InboundType.DEFAULT,
                                new DateTimeInterval(OffsetDateTime.now().plusDays(1),
                                        OffsetDateTime.now().plusDays(1)))
                        .build(), null);

        RequestWrapper<PutInboundRequest> request =
                new RequestWrapper<>(
                        new Token(SERVICE_BUS.getToken()), null, UniqueId.getStringUUID(), putInboundRequest);
        return SERVICE_BUS.putInbound(request);
    }

    @Step("Создаем поставку из Аксапты")
    public Inbound putInboundWithExternalRequestId(InboundType inboundType,
                                                   String externalRequestId) {
        String body = FileUtil.bodyStringFromFile(
                "wms/servicebus/putInbound/putInboundWithExternalRequestId.xml",
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                inboundType.getCode(),
                externalRequestId);
        return SERVICE_BUS.putInbound(body);
    }


    @Step("Создаем реестр поставки")
    public PutInboundRegistryResponse putInboundRegistry(PutInboundResponse inbound,
                                                         List<RegistryItem> items,
                                                         RegistryType registryType) {
        ResourceId resourceId = ResourceId.builder()
                .setYandexId(inbound.getInboundId().getYandexId())
                .setPartnerId(inbound.getInboundId().getPartnerId())
                .build();
        InboundRegistry registry = InboundRegistry.builder(resourceId, resourceId, registryType)
                .setItems(items)
                .setBoxes(null)
                .setDate(new DateTime("2021-04-29T00:00:00+03:00"))
                .build();

        PutInboundRegistryRequest putInboundRegistryRequest = new PutInboundRegistryRequest(registry);


        RequestWrapper<PutInboundRegistryRequest> requestWrapper = new RequestWrapper<>();
        requestWrapper.setToken(new Token(SERVICE_BUS.getToken()));
        requestWrapper.setRequest(putInboundRegistryRequest);
        return SERVICE_BUS.putInboundRegistry(requestWrapper);
    }

    @Step("Получаем информацию о поставке через getInbound")
    public GetInboundResponse getInbound(ResourceId resourceId) {
        log.info("Calling ServiceBus getInbound with yandexId = {}, partnerId = {}",
                resourceId.getYandexId(), resourceId.getPartnerId());
        GetInboundRequest getInboundRequest = new GetInboundRequest(resourceId);
        RequestWrapper<GetInboundRequest> wrappedRequest =
                new RequestWrapper<>(new Token(SERVICE_BUS.getToken()), null, null, getInboundRequest);
        return SERVICE_BUS.getInbound(wrappedRequest);
    }

    @Step("Создаем реестр возвратной поставки")
    public void putReturnInboundRegistry(String yandexId,
                                         String partnerId,
                                         String boxId,
                                         String orderId,
                                         String returnId,
                                         String returnReasonId,
                                         String uit,
                                         String sku,
                                         String vendorId) {
        String body = FileUtil.bodyStringFromFile(
                "wms/servicebus/putInboundRegistry/putInboundRegistryReturn.xml",
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                yandexId,
                partnerId,
                boxId,
                orderId,
                uit,
                returnReasonId,
                sku,
                vendorId,
                returnId
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр обновляемой возвратной поставки")
    public void putUpdatableReturnInboundRegistry(String yandexId,
                                         String partnerId,
                                         String boxId) {
        String body = FileUtil.bodyStringFromFile(
                "wms/servicebus/putInboundRegistry/putInboundRegistryUpdatableReturn.xml",
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                yandexId,
                partnerId,
                boxId
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр возвратной поставки с идентификаторами")
    public void putReturnInboundRegistryWithIdentities(
            String yandexId,
            String partnerId,
            String boxId,
            String orderId,
            String returnId,
            String returnReasonId,
            List<String> uitList,
            List<String> cisList,
            List<String> imeiList,
            String sku,
            String vendorId) {
        String body = FileUtil.bodyStringFromFile(
                "wms/servicebus/putInboundRegistry/putInboundRegistryReturnWithIdentities.xml",
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                yandexId,
                partnerId,
                boxId,
                orderId,
                uitList.get(0),
                uitList.get(1),
                cisList.get(0),
                cisList.get(1),
                imeiList.get(0),
                imeiList.get(1),
                returnReasonId,
                sku,
                vendorId,
                returnId
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр возвратной поставки с идентификаторами")
    public void putReturnInboundRegistryWithTwoIMEI(
            String yandexId,
            String partnerId,
            List<String> boxIdList,
            List<String> orderIdList,
            List<String> returnIdList,
            List<String> returnReasonIdList,
            List<String> imeiList,
            List<Item> itemList) {
        String body = FileUtil.bodyStringFromFile(
                        "wms/servicebus/putInboundRegistry/putInboundRegistryReturnWithTwoImei.xml")
                .replaceAll("TOKEN_PLACEHOLDER", SERVICE_BUS.getToken())
                .replaceAll("HASH_PLACEHOLDER", UniqueId.getStringUUID())
                .replaceAll("YANDEX_ID_PLACEHOLDER", yandexId)
                .replaceAll("PARTNER_ID_PLACEHOLDER", partnerId)
                .replaceAll("FIRST_BOX_ID_PLACEHOLDER", boxIdList.get(0))
                .replaceAll("SECOND_BOX_ID_PLACEHOLDER", boxIdList.get(1))
                .replaceAll("FIRST_ORDER_ID_PLACEHOLDER", orderIdList.get(0))
                .replaceAll("SECOND_ORDER_ID_PLACEHOLDER", orderIdList.get(1))
                .replaceAll("FIRST_ORDER_RETURN_ID_PLACEHOLDER", returnIdList.get(0))
                .replaceAll("SECOND_ORDER_RETURN_ID_PLACEHOLDER", returnIdList.get(1))
                .replaceAll("FIRST_ORDER_RETURN_REASON_ID_PLACEHOLDER", returnReasonIdList.get(0))
                .replaceAll("SECOND_ORDER_RETURN_REASON_ID_PLACEHOLDER", returnReasonIdList.get(1))
                .replaceAll("FIRST_ITEM_UIT_PLACEHOLDER", itemList.get(0).getArticle())
                .replaceAll("SECOND_ITEM_UIT_PLACEHOLDER", itemList.get(1).getArticle())
                .replaceAll("FIRST_ITEM_ARTICLE_PLACEHOLDER", itemList.get(0).getSku())
                .replaceAll("SECOND_ITEM_ARTICLE_PLACEHOLDER", itemList.get(1).getSku())
                .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(itemList.get(0).getVendorId()))
                .replaceAll("FIRST_ITEM_IMEI_PLACEHOLDER", imeiList.get(0))
                .replaceAll("SECOND_ITEM_IMEI_PLACEHOLDER", imeiList.get(1));
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр возвратной поставки с двумя товарами в одном возврате")
    public void putReturnInboundRegistryWithTwoItemsInOneReturn(
            String yandexId,
            String partnerId,
            String boxId,
            String orderId,
            String returnId,
            String returnReasonId,
            List<String> uitList,
            String sku,
            String vendorId) {
        String body = FileUtil.bodyStringFromFile(
                "wms/servicebus/putInboundRegistry/putInboundRegistryReturnWithTwoItems.xml",
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                yandexId,
                partnerId,
                boxId,
                orderId,
                uitList.get(0),
                uitList.get(1),
                returnReasonId,
                sku,
                vendorId,
                returnId
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр возвратной поставки с двумя возвратными коробками")
    public void putReturnInboundRegistryWithTwoBoxes(
            String yandexId,
            String partnerId,
            List<String> orderIdList,
            List<String> returnIdList,
            List<String> returnReasonIdList,
            List<String> uitList,
            List<String> itemArticleList,
            String vendorId,
            List<Box> boxes) {
        String body = FileUtil.bodyStringFromFile(
                        "wms/servicebus/putInboundRegistry/putInboundRegistryReturnWithTwoBoxes.xml")
                .replaceAll("TOKEN_PLACEHOLDER", SERVICE_BUS.getToken())
                .replaceAll("HASH_PLACEHOLDER", UniqueId.getStringUUID())
                .replaceAll("YANDEX_ID_PLACEHOLDER", yandexId)
                .replaceAll("PARTNER_ID_PLACEHOLDER", partnerId)
                .replaceAll("FIRST_BOX_COUNT_TYPE_PLACEHOLDER", boxes.get(0).getCountType())
                .replaceAll("SECOND_BOX_COUNT_TYPE_PLACEHOLDER", boxes.get(1).getCountType())
                .replaceAll("FIRST_BOX_ID_PLACEHOLDER", boxes.get(0).getBoxId())
                .replaceAll("SECOND_BOX_ID_PLACEHOLDER", boxes.get(1).getBoxId())
                .replaceAll("FIRST_ORDER_ID_PLACEHOLDER", orderIdList.get(0))
                .replaceAll("SECOND_ORDER_ID_PLACEHOLDER", orderIdList.get(1))
                .replaceAll("FIRST_ORDER_RETURN_ID_PLACEHOLDER", returnIdList.get(0))
                .replaceAll("SECOND_ORDER_RETURN_ID_PLACEHOLDER", returnIdList.get(1))
                .replaceAll("FIRST_ORDER_RETURN_REASON_ID_PLACEHOLDER", returnReasonIdList.get(0))
                .replaceAll("SECOND_ORDER_RETURN_REASON_ID_PLACEHOLDER", returnIdList.get(1))
                .replaceAll("FIRST_ITEM_UIT_PLACEHOLDER", uitList.get(0))
                .replaceAll("SECOND_ITEM_UIT_PLACEHOLDER", uitList.get(1))
                .replaceAll("FIRST_ITEM_ARTICLE_PLACEHOLDER", itemArticleList.get(0))
                .replaceAll("SECOND_ITEM_ARTICLE_PLACEHOLDER", itemArticleList.get(1))
                .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(vendorId));

        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр возвратной поставки с двумя товарами в двух коробках в одном возврате")
    public void putReturnInboundRegistryWithTwoBarcodesInTwoBoxesInOneReturn(
            String yandexId,
            String partnerId,
            List<String> boxIdList,
            List<String> orderIdList,
            List<String> returnIdList,
            List<String> returnReasonIdList,
            List<String> uitList,
            List<Item> itemList) {
        String body = FileUtil.bodyStringFromFile(
                        "wms/servicebus/putInboundRegistry/putInboundRegistryReturnWithTwoBarcodes.xml")
                .replaceAll("TOKEN_PLACEHOLDER", SERVICE_BUS.getToken())
                .replaceAll("HASH_PLACEHOLDER", UniqueId.getStringUUID())
                .replaceAll("YANDEX_ID_PLACEHOLDER", yandexId)
                .replaceAll("PARTNER_ID_PLACEHOLDER", partnerId)
                .replaceAll("FIRST_BOX_ID_PLACEHOLDER", boxIdList.get(0))
                .replaceAll("SECOND_BOX_ID_PLACEHOLDER", boxIdList.get(1))
                .replaceAll("FIRST_ORDER_ID_PLACEHOLDER", orderIdList.get(0))
                .replaceAll("SECOND_ORDER_ID_PLACEHOLDER", orderIdList.get(1))
                .replaceAll("FIRST_ORDER_RETURN_ID_PLACEHOLDER", returnIdList.get(0))
                .replaceAll("SECOND_ORDER_RETURN_ID_PLACEHOLDER", returnIdList.get(1))
                .replaceAll("FIRST_ORDER_RETURN_REASON_ID_PLACEHOLDER", returnReasonIdList.get(0))
                .replaceAll("SECOND_ORDER_RETURN_REASON_ID_PLACEHOLDER", returnReasonIdList.get(1))
                .replaceAll("FIRST_ITEM_UIT_PLACEHOLDER", uitList.get(0))
                .replaceAll("SECOND_ITEM_UIT_PLACEHOLDER", uitList.get(1))
                .replaceAll("FIRST_ITEM_ARTICLE_PLACEHOLDER", itemList.get(0).getSku())
                .replaceAll("SECOND_ITEM_ARTICLE_PLACEHOLDER", itemList.get(1).getSku())
                .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(itemList.get(0).getVendorId()))
                .replaceAll("FIRST_BARCODE_PLACEHOLDER", itemList.get(0).getArticle())
                .replaceAll("SECOND_BARCODE_PLACEHOLDER", itemList.get(1).getArticle());
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр поставки невыкупов с КИЗом")
    public Inbound putUnredeemedInboundRegistryWithCis(
            Inbound inbound,
            Item item,
            String boxId,
            String orderId,
            String uit,
            String palletId,
            String barcode,
            String cisDeclared,
            int cisHandleMode,
            int cisCargoType) {
        String body = FileUtil.bodyStringFromFile(
                        "wms/servicebus/putInboundRegistry/putInboundRegistryUnredeemedWithCis.xml")
                .replaceAll("TOKEN_PLACEHOLDER", SERVICE_BUS.getToken())
                .replaceAll("HASH_PLACEHOLDER", UniqueId.getStringUUID())
                .replaceAll("YANDEX_ID_PLACEHOLDER", String.valueOf(inbound.getYandexId()))
                .replaceAll("PARTNER_ID_PLACEHOLDER", String.valueOf(inbound.getPartnerId()))
                .replaceAll("PALLET_ID_PLACEHOLDER", palletId)
                .replaceAll("BOX_ID_PLACEHOLDER", boxId)
                .replaceAll("ORDER_ID_PLACEHOLDER", orderId)
                .replaceAll("UIT_PLACEHOLDER", uit)
                .replaceAll("CIS_PLACEHOLDER", cisDeclared)
                .replaceAll("ARTICLE_PLACEHOLDER", item.getArticle())
                .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(item.getVendorId()))
                .replaceAll("BARCODE_PLACEHOLDER", barcode)
                .replaceAll("CIS_HANDLE_MODE_PLACEHOLDER", String.valueOf(cisHandleMode))
                .replaceAll("CIS_CARGO_TYPE_PLACEHOLDER", String.valueOf(cisCargoType))
                .replaceAll("NAME_PLACEHOLDER", item.getName());

        SERVICE_BUS.putInboundRegistry(body);
        return inbound;
    }

    @Step("Создаем реестр поставки невыкупов с КИЗом")
    public Inbound putUnredeemedInboundRegistryByBox(
            Inbound inbound,
            Item item,
            String boxId,
            String orderId,
            String uit,
            String barcode) {
        String body = FileUtil.bodyStringFromFile(
                        "wms/servicebus/putInboundRegistry/putInboundRegistryUnredeemedByBox.xml")
                .replaceAll("TOKEN_PLACEHOLDER", SERVICE_BUS.getToken())
                .replaceAll("HASH_PLACEHOLDER", UniqueId.getStringUUID())
                .replaceAll("YANDEX_ID_PLACEHOLDER", String.valueOf(inbound.getYandexId()))
                .replaceAll("PARTNER_ID_PLACEHOLDER", String.valueOf(inbound.getPartnerId()))
                .replaceAll("BOX_ID_PLACEHOLDER", boxId)
                .replaceAll("ORDER_ID_PLACEHOLDER", orderId)
                .replaceAll("UIT_PLACEHOLDER", uit)
                .replaceAll("ARTICLE_PLACEHOLDER", item.getArticle())
                .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(item.getVendorId()))
                .replaceAll("BARCODE_PLACEHOLDER", barcode)
                .replaceAll("NAME_PLACEHOLDER", item.getName());

        SERVICE_BUS.putInboundRegistry(body);
        return inbound;
    }

    @Step("Создаем реестр поставки невыкупов с палетой и одной коробкой")
    public void putUnredeemedInboundRegistry(String yandexId,
                                             String partnerId,
                                             String boxId,
                                             String orderId,
                                             String uit,
                                             String article,
                                             String vendorId,
                                             String palletId,
                                             String barcode,
                                             boolean hasLifeTime) {
        String body = FileUtil.bodyStringFromFile(
                "wms/servicebus/putInboundRegistry/putInboundRegistryUnredeemedByPallet.xml",
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                yandexId,
                partnerId,
                boxId,
                orderId,
                uit,
                article,
                vendorId,
                palletId,
                barcode,
                hasLifeTime
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр поставки невыкупов с палетой и двумя коробками")
    public void putUnredeemedInboundRegistryByPalletWithTwoBoxes(String yandexId,
                                             String partnerId,
                                             String firstBoxId,
                                             String secondBoxId,
                                             String orderId,
                                             String uit,
                                             String article,
                                             String vendorId,
                                             String palletId,
                                             String barcode) {
        String body = FileUtil.bodyStringFromFile(
                "wms/servicebus/putInboundRegistry/putInboundRegistryUnredeemedByPalletWithTwoBoxes.xml",
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                yandexId,
                partnerId,
                firstBoxId,
                orderId,
                uit,
                article,
                vendorId,
                palletId,
                barcode,
                secondBoxId
        );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр поставки невыкупов с двумя палетами")
    public void putUnredeemedInboundRegistryWithTwoPallets(Inbound inbound,
                                                           Item item,
                                                           String boxId,
                                                           String orderId,
                                                           String uit,
                                                           List<String> palletIds,
                                                           String barcode) {
        String body = FileUtil.bodyStringFromFile(
                        "wms/servicebus/putInboundRegistry/putInboundRegistryUnredeemedWithTwoPallets.xml")
                .replaceAll("TOKEN_PLACEHOLDER", SERVICE_BUS.getToken())
                .replaceAll("HASH_PLACEHOLDER", UniqueId.getStringUUID())
                .replaceAll("YANDEX_ID_PLACEHOLDER", String.valueOf(inbound.getYandexId()))
                .replaceAll("PARTNER_ID_PLACEHOLDER", String.valueOf(inbound.getPartnerId()))
                .replaceAll("FIRST_PALLET_ID_PLACEHOLDER", palletIds.get(0))
                .replaceAll("SECOND_PALLET_ID_PLACEHOLDER", palletIds.get(1))
                .replaceAll("BOX_ID_PLACEHOLDER", boxId)
                .replaceAll("ORDER_ID_PLACEHOLDER", orderId)
                .replaceAll("UIT_PLACEHOLDER", uit)
                .replaceAll("ARTICLE_PLACEHOLDER", item.getArticle())
                .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(item.getVendorId()))
                .replaceAll("BARCODE_PLACEHOLDER", barcode)
                .replaceAll("NAME_PLACEHOLDER", item.getName()
                );
        SERVICE_BUS.putInboundRegistry(body);
    }

    @Step("Создаем реестр допоставки")
    public void putAdditionalInboundRegistry(String requestFilePath,
                                             String yandexId,
                                             String partnerId,
                                             String vendorId,
                                             String sku,
                                             String consigmentId,
                                             String cis,
                                             String gtin) {
        String body = FileUtil.bodyStringFromFile(
                requestFilePath,
                SERVICE_BUS.getToken(),
                UniqueId.getStringUUID(),
                yandexId,
                partnerId,
                vendorId,
                sku,
                consigmentId,
                cis,
                gtin
        );
        SERVICE_BUS.putInboundRegistry(body);

    }

    @Step("Создаем реестр с карготипом и КИЗами")
    public Inbound putInboundRegistryWithCargotypeAndCis(Inbound inbound, long vendorId, String article,
                                                         int count, String barcode, int cargotype,
                                                         @Nullable String declaredCis, int cisHandleMode,
                                                         int imeiCount, int snCount) {
        String body;
        if (declaredCis == null) {
            body = FileUtil.bodyStringFromFile(
                    "wms/servicebus/putInboundRegistry/putInboundRegistryWithCargotype.xml",
                    SERVICE_BUS.getToken(),
                    UniqueId.getStringUUID(),
                    inbound.getYandexId(),
                    inbound.getPartnerId(),
                    vendorId,
                    article,
                    barcode,
                    count,
                    cargotype,
                    imeiCount,
                    snCount,
                    cisHandleMode
            );
        } else {
            body = FileUtil.bodyStringFromFile(
                    "wms/servicebus/putInboundRegistry/putInboundRegistryWithDeclaredCis.xml",
                    SERVICE_BUS.getToken(),
                    UniqueId.getStringUUID(),
                    inbound.getYandexId(),
                    inbound.getPartnerId(),
                    vendorId,
                    article,
                    barcode,
                    count,
                    cargotype,
                    imeiCount,
                    snCount,
                    cisHandleMode,
                    declaredCis
            );
        }
        SERVICE_BUS.putInboundRegistry(body);
        return inbound;
    }

    @Step("Получаем отгруженный со склада товар и его УИТ")
    public Item getShippedItem() {
        return DATA_CREATOR.getShippedItem();
    }

    @Step("Получаем отгруженный со склада товар с КИЗом и его УИТ")
    public Item getShippedItemWithCis() {
        return DATA_CREATOR.getShippedItemWithCis();
    }

    @Step("Проверяем, что статус поставки равен: {status}")
    public void verifyInboundStatusIs(Inbound inbound, String status) {
        SERVICE_BUS.getInboundStatus(inbound)
                .body("root.response.inboundsStatus.inboundStatus.statusCode", is(status));
    }

    @Step("Проверяем, что в истории поставки есть статус: {status}")
    public void verifyInboundHistoryHasStatus(Inbound inbound, String status) {
        SERVICE_BUS.getInboundHistory(inbound)
                .body("root.response.inboundStatusHistory.history.inboundStatus.find " +
                        "{it.statusCode == " + status + "}.statusCode", is(status));
    }

    @Step("Проверяем, что ВГХ товара сохранились правильно")
    public void assertItemKorobytes(Long vendorId,
                                    String article,
                                    String length,
                                    String width,
                                    String height,
                                    String weight
    ) {
        String prefix = "root.response.itemReferences.itemReference.item.";
        RADIATOR_CLIENT.getReferenceItems(vendorId, article)
                .body(prefix + "korobyte.width", is(width))
                .body(prefix + "korobyte.height", is(height))
                .body(prefix + "korobyte.length", is(length))
                .body(prefix + "korobyte.weightGross", is(weight))
                .body(prefix + "korobyte.weightNet", is(weight))
                .body(prefix + "korobyte.weightTare", is("0.0"));

    }

    @Step("Получаем информацию о поставке через getInbound")
    public XmlPath getInbound(long yandexId, String partnerId) {
        log.info("Calling ServiceBus getInbound with yandexId = {}, partnerId = {}", yandexId, partnerId);

        String requestBody = FileUtil.bodyStringFromFile("wms/servicebus/getInbound.xml",
                partnerId, yandexId, SERVICE_BUS.getToken());

        return SERVICE_BUS.getInbound(requestBody);
    }

    @Step("Получаем информацию о поставке через getInbound")
    public XmlPath getInbound(String yandexId, String partnerId) {
        log.info("Calling ServiceBus getInbound with yandexId = {}, partnerId = {}", yandexId, partnerId);

        String requestBody = FileUtil.bodyStringFromFile("wms/servicebus/getInbound.xml",
                partnerId, yandexId, SERVICE_BUS.getToken());

        return SERVICE_BUS.getInbound(requestBody);
    }

    private String getPathToContainer(InitialReceivingType initialReceivingType) {
        switch (initialReceivingType) {
            case BY_BOX -> {
                return ".boxes.box";
            }
            case BY_PALLET -> {
                return ".pallets.pallet";
            }
            default -> {
                return "";
            }
        }
    }

    @Step("Проверяем UnitCountType и количество принятых грузомест в реестре первичной приемки")
    public void checkGetInboundUnitCountTypeInInitial(XmlPath getInboundResponse,
                                                      List<String> expectedUnitCountTypes, int expectedContainerQty,
                                                      InitialReceivingType initialReceivingType) {
        String containerPath = getPathToContainer(initialReceivingType);
        String registryPath = "root.response.registries.registry[regId]";
        String registryCountPath = registryPath + containerPath + ".unitInfo.counts.count";


        int registrySize = getInboundResponse.getInt("root.response.registries.registry.size()");

        for (int regId = 0; regId < registrySize; regId++) {
            int registryType = getInboundResponse.param("regId", regId).getInt(registryPath + ".registryType");

            // Проверяем только initial-реестр
            if (registryType != 12) {
                continue;
            }
            int boxesSizeActual = getInboundResponse.param("regId", regId)
                    .getInt(registryPath + containerPath + ".size()");
            Assert.assertEquals(String.format("Ожидаемое количество принятых грузомест %d не совпало с фактическим %d",
                    expectedContainerQty, boxesSizeActual), expectedContainerQty, boxesSizeActual);
            List<String> boxTypes = getInboundResponse.param("regId", regId)
                    .getList(registryCountPath + ".countType");
            Assertions.assertIterableEquals(expectedUnitCountTypes, boxTypes,
                    String.format("Ожидаемый тип грузомест %s не совпал с фактическим %s", expectedUnitCountTypes,
                            boxTypes));

        }
    }

    @Step("Проверяем тип стока и фактически принятый идентификатор")
    public void checkGetInbound(XmlPath getInboundResponse, List<String> expectedItemTypeList,
                                List<String> expectedCisList) {
        String registryPath = "root.response.registries.registry[regId]";
        String registryCountPath = registryPath + ".items.item.unitInfo.counts.count";
        int registrySize = getInboundResponse.getInt("root.response.registries.registry.size()");

        for (int regId = 0; regId < registrySize; regId++) {
            int registryType = getInboundResponse.param("regId", regId).getInt(registryPath + ".registryType");

            // initial-реестры не проверяем
            if (registryType == 4 || registryType == 12) {
                continue;
            }

            List<String> convertedItemTypes = convertItemTypes(registryType, expectedItemTypeList);
            List<String> itemTypes = getInboundResponse.param("regId", regId)
                    .getList(registryCountPath + ".countType");

            Assertions.assertIterableEquals(convertedItemTypes, itemTypes,
                    String.format("Ожидаемый тип товара %s не совпал с фактическим %s", itemTypes, convertedItemTypes));

            List<String> cisList = getInboundResponse.param("regId", regId)
                    .getList(registryCountPath + ".unitIds.unitId.partialIds.partialId.value");
            Assertions.assertIterableEquals(expectedCisList, cisList,
                    String.format("Ожидаемый идентификатор %s не совпал с фактическим %s", cisList, expectedCisList));
        }
    }

    public void checkGetInbound(XmlPath getInboundResponse, String expectedItemType, String expectedCis) {
        List<String> expectedCisList = Stream.of(expectedCis)
                .filter(cis -> !StringUtils.isEmpty(cis))
                .collect(Collectors.toList());

        List<String> expectedItemTypeList = Stream.of(expectedItemType)
                .filter(itemType -> !StringUtils.isEmpty(itemType))
                .collect(Collectors.toList());

        checkGetInbound(getInboundResponse, expectedItemTypeList, expectedCisList);
    }

    public void checkGetInboundForSecondaryReturn(XmlPath getInboundResponse,
                                                  String expectedNonconformityAttributes) {

        List<String> expectedNonconformityAttributesList = Stream.of(expectedNonconformityAttributes)
                .filter(attr -> !StringUtils.isEmpty(attr))
                .collect(Collectors.toList());

        checkGetInboundSecondaryReturn(getInboundResponse, expectedNonconformityAttributesList);
    }

    @Step("Проверяем Nonconformity attributes для вторичной приемки клиентсокго возврата")
    public void checkGetInboundSecondaryReturn(XmlPath getInboundResponse,
                                               List<String> expectedNonconformityAttributesList
    ) {

        String registryPath = "root.response.registries.registry[regId]";
        String registryCountPath = registryPath + ".items.item.unitInfo.counts.count";
        int registrySize = getInboundResponse.getInt("root.response.registries.registry.size()");

        for (int regId = 0; regId < registrySize; regId++) {
            int registryType = getInboundResponse.param("regId", regId).getInt(registryPath + ".registryType");

            // Проверяем только фактический реестр вторичной приемки возвратной поставки
            if (registryType != 9) {
                continue;
            }

            if (!expectedNonconformityAttributesList.isEmpty()) {
                List<String> nonconformityAttributes = getInboundResponse.param("regId", regId)
                        .getList(registryCountPath + ".nonconformityAttributes");
                Assertions.assertIterableEquals(expectedNonconformityAttributesList, nonconformityAttributes,
                        String.format("Ожидаемый тип товара %s не совпал с фактическим %s",
                                expectedNonconformityAttributesList,
                                nonconformityAttributes));
            }
        }
    }


    private List<String> convertItemTypes(int registryType, List<String> itemTypeList) {
        if (registryType == 1) {
            return itemTypeList;
        }

        return itemTypeList.stream()
                .map(itemType -> {
                    if (isNonComplient(itemType)) {
                        return ITEM_TYPE_NON_COMPLIENT;
                    }
                    return itemType;
                })
                .collect(Collectors.toList());
    }

    private boolean isNonComplient(String itemType) {
        return NON_COMPLIENT_ITEM_TYPES.contains(itemType);
    }
}
