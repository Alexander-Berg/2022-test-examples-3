package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.StockType;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetInboundRequest;
import ru.yandex.market.logistic.api.model.fulfillment.request.PutInboundRegistryRequest;
import ru.yandex.market.logistic.api.model.fulfillment.request.PutInboundRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundRegistryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundResponse;

import java.util.List;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Resource.Classpath({"wms/infor.properties", "wms/wrapinfor.properties"})
public class ServiceBus extends AbstractRestClient {
    private static final int REQUEST_TIMEOUT = 15000;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", REQUEST_TIMEOUT)
                    .setParam("http.socket.timeout", REQUEST_TIMEOUT)
                    .setParam("http.connection-manager.timeout", REQUEST_TIMEOUT)
            )
            .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

    private final Logger log = LoggerFactory.getLogger(ServiceBus.class);

    @Property("infor.token")
    private String inforToken;

    @Property("infor.host")
    private String host;

    @Property("infor.servicebus")
    private String servicebus;

    private final String servicebusHost;

    public ServiceBus() {
        PropertyLoader.newInstance().populate(this);

        servicebusHost = host + servicebus;
    }

    private ValidatableResponse baseRequest(String reqBody, String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        return RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(servicebusHost)
                .contentType(ContentType.XML)
                .header("Content-Type", "text/xml")
                .body(reqBody)
                .log().ifValidationFails()
                .when()
                .post(path)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK)
                .body("root.requestState.isError", is("false"));
    }

    private ValidatableResponse baseJsonRequest(String reqBody, String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        return RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(servicebusHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .body(reqBody)
                .log().ifValidationFails()
                .when()
                .post(path)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK);
    }

    public Order createOrder(long yandexId, List<Item> itemList, String shipmentDate) {
        log.info("Calling ServiceBus createOrder. yandexId = {}, shipmentDate = {}", yandexId, shipmentDate);

        String hash = UniqueId.getStringUUID();
        String items = "";

        for (Item item: itemList
             ) {
            items = items + FileUtil.bodyStringFromFile("wms/servicebus/item.xml")
                    .replaceAll("ARTICLE_PLACEHOLDER", item.getArticle())
                    .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(item.getVendorId()))
                    .replaceAll("COUNT_PLACEHOLDER", String.valueOf(item.getQuantity()))
                    .replaceAll("REMOVABLE_IF_ABSENT_PLACEHOLDER", String.valueOf(item.isRemovableIfAbsent()));
        }

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/createOrder.xml")
                .replaceAll("HASH_PLACEHOLDER", hash)
                .replaceAll("TOKEN_PLACEHOLDER", inforToken)
                .replaceAll("YANDEX_ID_PLACEHOLDER", String.valueOf(yandexId))
                .replaceAll("ITEM_PLACEHOLDER", items)
                .replaceAll("SHIPMENT_DATE_PLACEHOLDER", shipmentDate);

        ValidatableResponse resp = baseRequest(reqString, "/api/logistic/createOrder")
                .body("root.response.orderId.yandexId",
                        is(
                                String.valueOf(yandexId)
                        )
                );

        String fulfillmentId = resp.extract().xmlPath().getString("root.response.orderId.fulfillmentId");
        String partnerId = resp.extract().xmlPath().getString("root.response.orderId.partnerId");
        Assertions.assertEquals(
                fulfillmentId,
                partnerId,
                String.format("Expected partnerId to be equal to fulfillmentId. Received, " +
                        "partnerId: %s, fulfillmentId: %s", partnerId, fulfillmentId)
        );

        log.info("OrderSteps created. yandexId = {}, fulfillmentId = {}", yandexId, fulfillmentId);

        return new Order(yandexId, fulfillmentId);
    }

    public ValidatableResponse getOrder(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getOrder");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/getOrder.request.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getOrder")
                .body("root.response.order.orderId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.order.orderId.partnerId",
                        is(fulfillmentId))
                .body("root.response.order.orderId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse putReferenceItems(String body) {
        log.info("Calling ServiceBus putReferenceItems. body = {}", body);
        return baseRequest(body, "/api/logistic/putReferenceItems");
    }

    public Inbound createInbound(long yandexId, String intervalDate, String sku) {
        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                yandexId,
                intervalDate,
                sku,
                inforToken,
                false
        );

        return createInbound(reqString);
    }

    public Inbound createInbound(long yandexId, String intervalDate, String filePath,String sku) {
        String reqString = FileUtil.bodyStringFromFile(filePath,
                UniqueId.getStringUUID(),
                yandexId,
                intervalDate,
                sku,
                inforToken,
                false
        );

        return createInbound(reqString);
    }

    public Inbound createInbound(String body) {
        final XmlPath xmlPath = baseRequest(body, "/api/logistic/createInbound")
                .body("root.response.inboundId.yandexId", not(emptyOrNullString()))
                .body("root.response.inboundId.fulfillmentId", not(emptyOrNullString()))
                .body("root.response.inboundId.partnerId", not(emptyOrNullString()))
                .extract()
                .xmlPath();

        final long yandexId = xmlPath.getLong("root.response.inboundId.yandexId");
        final String fulfillmentId = xmlPath.getString("root.response.inboundId.fulfillmentId");
        final String partnerId = xmlPath.getString("root.response.inboundId.partnerId");

        return new Inbound(yandexId, fulfillmentId, partnerId);
    }

    public Inbound putInbound(String body) {
        final XmlPath xmlPath = baseRequest(body, "/api/logistic/putInbound")
                .body("root.response.inboundId.yandexId", not(emptyOrNullString()))
                .body("root.response.inboundId.partnerId", not(emptyOrNullString()))
                .extract()
                .xmlPath();

        final long yandexId = xmlPath.getLong("root.response.inboundId.yandexId");
        final String fulfillmentId = xmlPath.getString("root.response.inboundId.partnerId");
        final String partnerId = xmlPath.getString("root.response.inboundId.partnerId");

        log.info("Created inbound yandexId = {}, fulfillmentId = {}", yandexId, fulfillmentId);
        return new Inbound(yandexId, fulfillmentId, partnerId);
    }

    public PutInboundResponse putInbound(RequestWrapper<PutInboundRequest> req) {
        String body = createReqBody(req);

        String res = baseRequest(body, "/api/logistic/putInbound")
                .body("root.response.inboundId.yandexId", not(emptyOrNullString()))
                .body("root.response.inboundId.partnerId", not(emptyOrNullString()))
                .extract()
                .asString();

        ResponseWrapper<PutInboundResponse> wrappedPutInboundResponse = createWrappedResponse(
                res,
                new TypeReference<>() {});

        return wrappedPutInboundResponse.getResponse();
    }

    public ValidatableResponse putInboundRegistry(String body) {
        return baseRequest(body, "/api/logistic/putInboundRegistry");
    }

    public PutInboundRegistryResponse putInboundRegistry(RequestWrapper<PutInboundRegistryRequest> req) {
        String body = createReqBody(req);

        String res = baseRequest(body, "/api/logistic/putInboundRegistry")
                .body("root.response.registryId.yandexId", not(emptyOrNullString()))
                .body("root.response.registryId.partnerId", not(emptyOrNullString()))
                .extract()
                .asString();

        ResponseWrapper<PutInboundRegistryResponse> wrappedResponse = createWrappedResponse(
                res,
                new TypeReference<>() {});

        return wrappedResponse.getResponse();
    }

    public String getToken() {
        return inforToken;
    }

    public ValidatableResponse putReferenceItems(Item item) {
        return putReferenceItems(item.getVendorId(), item.getArticle());
    }

    /**
     * Узнать по manufacturersku нашу внутреннюю sku
     */
    public String mapArticleToNativeSku(long vendorId, String article){
        String template = "{\"manufacturerSkus\":[{\"storerKey\":%d,\"manufacturerSku\":\"%s\"}]}";
        return baseJsonRequest(String.format(template, vendorId, article),"/wms/mapSku")
                .body("mappedUnits[0].manufacturerSku.storerKey",
                        is((int)vendorId))
                .body("mappedUnits[0].manufacturerSku.manufacturerSku",
                        is(article)).extract().jsonPath().getString("mappedUnits[0].skuId.sku");
    }

    public ValidatableResponse putReferenceItems(long vendorId, String article) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/putReferenceItems.xml",
                hash,
                vendorId,
                article,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/putReferenceItems");
    }

    public ValidatableResponse putReferenceItems(long vendorId, String article, String name) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile(
                "wms/tests/api/servicebus/putReferenceItemsWithRandomName/putReferenceItemsWithRandomName.xml",
                hash,
                vendorId,
                article,
                name,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/putReferenceItems");
    }

    public ValidatableResponse cancelInbound(Inbound inboundToCancel) {
        return cancelInbound(inboundToCancel.getYandexId(), inboundToCancel.getFulfillmentId());
    }

    public ValidatableResponse cancelInbound(long yandexId, String fulfillmentId) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/cancelInbound.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/cancelInbound");
    }

    public ValidatableResponse getOutbound(Outbound outbound) {
        return getOutbound(outbound.getYandexId(), outbound.getFulfillmentId());
    }

    public ValidatableResponse getOutbound(long yandexId, String fulfillmentId) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/getOutbound.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getOutbound");
    }

    public Outbound createOutbound(long yandexId, List<Item> itemList, String shipmentDate) {
        return createOutbound(yandexId, itemList, shipmentDate, StockType.FIT);
    }

    public Outbound createOutbound(long yandexId, List<Item> itemList, String shipmentDate, StockType stock) {

        log.info("Calling ServiceBus createOutbound");

        String hash = UniqueId.getStringUUID();
        String consignments = "";

        for (Item item: itemList) {
            consignments = consignments + FileUtil.bodyStringFromFile("wms/wrapRequests/consignment.xml")
                    .replaceAll("ARTICLE_PLACEHOLDER", item.getArticle())
                    .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(item.getVendorId()))
                    .replaceAll("COUNT_PLACEHOLDER", String.valueOf(item.getQuantity()));
        }

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/createOutbound.xml")
                .replaceAll("HASH_PLACEHOLDER", hash)
                .replaceAll("TOKEN_PLACEHOLDER", inforToken)
                .replaceAll("YANDEX_ID_PLACEHOLDER", String.valueOf(yandexId))
                .replaceAll("CONSIGNMENT_PLACEHOLDER", consignments)
                .replaceAll("SHIPMENT_DATE_PLACEHOLDER", shipmentDate)
                .replaceAll("STOCK_TYPE_PLACEHOLDER", stock.getValue());

        ValidatableResponse resp = baseRequest(reqString, "/api/logistic/createOutbound")
                .body("root.response.outboundId.yandexId",
                        is(String.valueOf(yandexId))
                );

        String fulfillmentId = resp.extract().xmlPath().getString("root.response.outboundId.fulfillmentId");
        String partnerId = resp.extract().xmlPath().getString("root.response.outboundId.partnerId");

        log.info("Outbound created. yandexId = {}, fulfillmentId = {}, partnerId = {}", yandexId, fulfillmentId, partnerId);

        return new Outbound(yandexId, fulfillmentId, partnerId);
    }

    public ValidatableResponse putOutbound(Outbound outbound) {
        return putOutbound(outbound.getYandexId(), String.format("%1$s/%1$s", DateUtil.currentDateTime()), 0);
    }

    public ValidatableResponse putOutbound(long yandexId) {
        return putOutbound(yandexId, String.format("%1$s/%1$s", DateUtil.currentDateTime()), 0);
    }

    public ValidatableResponse putOutbound(long yandexId, String interval, int type) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/putOutbound.xml",
                hash,
                yandexId,
                interval,
                inforToken,
                type
        );

        return baseRequest(reqString, "/api/logistic/putOutbound");
    }

    public ValidatableResponse putOutbound(long yandexId, String interval) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/putOutbound.xml",
                hash,
                yandexId,
                interval,
                inforToken,
                0
        );

        return baseRequest(reqString, "/api/logistic/putOutbound");
    }

    public Outbound putOutbound(long yandexId, String interval, String filePath) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile(filePath,
                hash,
                yandexId,
                interval,
                inforToken
        );

        ValidatableResponse resp = baseRequest(reqString, "/api/logistic/putOutbound")
                .body("root.response.outboundId.yandexId",
                        is(String.valueOf(yandexId))
                );

        String fulfillmentId = resp.extract().xmlPath().getString("root.response.outboundId.partnerId");

        log.info("Outbound created. yandexId = {}, fulfillmentId = {}", yandexId, fulfillmentId);

        return new Outbound(yandexId, fulfillmentId);
    }

    public Outbound putOutboundBbxd(long yandexId, String interval, long externalReceiptKey, String carrierCode) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/putOutboundBbxd.xml",
                hash,
                yandexId,
                interval,
                inforToken,
                externalReceiptKey,
                carrierCode
        );

        ValidatableResponse resp = baseRequest(reqString, "/api/logistic/putOutbound")
                .body("root.response.outboundId.yandexId",
                        is(String.valueOf(yandexId))
                );

        String fulfillmentId = resp.extract().xmlPath().getString("root.response.outboundId.partnerId");

        log.info("Outbound created. yandexId = {}, fulfillmentId = {}", yandexId, fulfillmentId);

        return new Outbound(yandexId, fulfillmentId);
    }

    public ValidatableResponse putOutboundRegistry(long yandexId, String partnerId, String interval, String article, int vendor) {
        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/putOutboundRegistry.xml",
                hash,
                yandexId,
                partnerId,
                inforToken,
                interval,
                article,
                vendor
        );

        return baseRequest(reqString, "/api/logistic/putOutboundRegistry");
    }

    public XmlPath getInbound(String requestBody) {
        return baseRequest(requestBody, "/api/logistic/getInbound")
                .extract()
                .xmlPath();
    }

    public GetInboundResponse getInbound(RequestWrapper<GetInboundRequest> req) {
        String body = createReqBody(req);

        String res = baseRequest(body, "/api/logistic/getInbound").extract().asString();

        ResponseWrapper<GetInboundResponse> wrappedResponse = createWrappedResponse(res, new TypeReference<>() {});

        return wrappedResponse.getResponse();
    }

    public ValidatableResponse getTransferStatus(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getTransferStatus");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getTransferStatus.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getTransfersStatus")
                .body("root.response.transfersStatus.transferStatus.transferId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.transfersStatus.transferStatus.transferId.partnerId",
                        is(String.valueOf(fulfillmentId)))
                .body("root.response.transfersStatus.transferStatus.transferId.fulfillmentId",
                        is(String.valueOf(fulfillmentId)));
    }

    public ValidatableResponse getTransferHistory(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getTransferHistory");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getTransferHistory.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getTransferHistory")
                .body("root.response.transferStatusHistory.transferId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.transferStatusHistory.transferId.partnerId",
                        is(String.valueOf(fulfillmentId)))
                .body("root.response.transferStatusHistory.transferId.fulfillmentId",
                        is(String.valueOf(fulfillmentId)));
    }

    public ValidatableResponse getTransferDetails(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getTransferDetails");

        String hash = UniqueId.getStringUUID();
        String prefix = "root.response.transferDetails.transferId.";

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getTransferDetails.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getTransferDetails")
                .body(prefix + "yandexId",
                        is(String.valueOf(yandexId)))
                .body(prefix + "partnerId",
                        is(String.valueOf(fulfillmentId)))
                .body(prefix + "fulfillmentId",
                        is(String.valueOf(fulfillmentId)));
    }

    public ValidatableResponse createTransfer(long transferId, long inbYandexId, String inbFfId, Item item,  int stockFrom, int stockTo, int count) {
        log.info("Calling WrapInfor createTransfer");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/createTransfer.xml",
                hash,
                transferId,
                inbYandexId,
                inbFfId,
                stockFrom,
                stockTo,
                count,
                item.getSku(),
                item.getVendorId(),
                item.getArticle(),
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/createTransfer")
                .body("root.response.transferId.yandexId",
                        is(String.valueOf(transferId)));
    }

    public ValidatableResponse createTransferWithCis(long transferId, Item item,  int stockFrom, int stockTo, int count) {
        log.info("Calling WrapInfor createTransfer with Cis");

        String hash = UniqueId.getStringUUID();
        String identitiesXml = createTransferIdentitiesXml(item);

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/createTransferWithCis.xml",
                hash,
                transferId,
                stockFrom,
                stockTo,
                count,
                item.getSku(),
                item.getVendorId(),
                item.getArticle(),
                inforToken,
                identitiesXml
        );

        return baseRequest(reqString, "/api/logistic/createTransfer")
                .body("root.response.transferId.yandexId",
                        is(String.valueOf(transferId)));
    }

    private String createTransferIdentitiesXml(Item item) {
        String identitiesXml = "";
        String xmlTemplate = "<partialId><idType>CIS</idType><value>%s</value></partialId>";

        for (String instanceKey : item.getInstances().keySet()) {
            identitiesXml += String.format(xmlTemplate, item.getInstances().get(instanceKey));
        }

        return identitiesXml;
    }

    public ValidatableResponse cancelOrder(Order order) {
        return cancelOrder(order.getYandexId(), order.getFulfillmentId());
    }

    public ValidatableResponse cancelOrder(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus cancelOrder. yandexId = {}, fulfillmentId = {}", yandexId, fulfillmentId);

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/cancelOrder.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/cancelOrder");
    }

    public ValidatableResponse getOutboundDetails(Outbound outbound) {
        return getOutboundDetails(outbound.getYandexId(), outbound.getFulfillmentId());
    }

    public ValidatableResponse getOutboundDetails(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getOutboundDetails");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/getOutboundDetails.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString,"/api/logistic/getOutboundDetails")
                .body("root.response.outboundDetails.outboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.outboundDetails.outboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.outboundDetails.outboundId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse updateInbound(Inbound inbound, String intervalDate) {
        return updateInbound(inbound.getYandexId(), inbound.getFulfillmentId(), intervalDate);
    }

    public ValidatableResponse updateInbound(long yandexId, String fulfillmentId, String intervalDate) {
        log.info("Calling ServiceBus updateInbound");

        String hash = UniqueId.getStringUUID();
        String prefix = "root.response.inboundId.";

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/updateInbound.xml",
                hash,
                yandexId,
                fulfillmentId,
                intervalDate,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/updateInbound")
                .body(prefix + "yandexId",
                        is(String.valueOf(yandexId)))
                .body(prefix + "fulfillmentId",
                        is(String.valueOf(fulfillmentId)))
                .body(prefix + "partnerId",
                        is(String.valueOf(fulfillmentId)));

    }

    public ValidatableResponse getInboundStatus(Inbound inbound) {
        return getInboundStatus(inbound.getYandexId(), inbound.getFulfillmentId());
    }

    public ValidatableResponse getInboundStatus(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getInboundsStatus");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getInboundsStatus.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getInboundStatuses")
                .body("root.response.inboundsStatus.inboundStatus.inboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.inboundsStatus.inboundStatus.inboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.inboundsStatus.inboundStatus.inboundId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse getInboundHistory(Inbound inbound) {
        return getInboundHistory(inbound.getYandexId(), inbound.getFulfillmentId());
    }

    public ValidatableResponse getInboundHistory(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getInboundHistory");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/getInboundHistory.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getInboundStatusHistory")
                .body("root.response.inboundStatusHistory.inboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.inboundStatusHistory.inboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.inboundStatusHistory.inboundId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse getReturnInboundDetails(Inbound inbound) {
        return getReturnInboundDetails(inbound.getYandexId(), inbound.getFulfillmentId());
    }

    public ValidatableResponse getReturnInboundDetails(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getReturnInboundDetails");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getReturnInboundDetails.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getReturnInboundDetails")
                .body("root.response.returnInboundDetails.inboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.returnInboundDetails.inboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.returnInboundDetails.inboundId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse getInboundDetails(Inbound inbound) {
        return getInboundDetails(inbound.getYandexId(), inbound.getFulfillmentId());
    }

    public ValidatableResponse getInboundDetails(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getInboundDetails");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getInboundDetails.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getInboundDetails")
                .body("root.response.inboundDetails.inboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.inboundDetails.inboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.inboundDetails.inboundId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse putRegistryWithCustomItems(Outbound outbound, List<Item> itemList, String shipmentDate) {
        return putRegistryWithCustomItems(outbound, itemList, shipmentDate, StockType.FIT);
    }

    public ValidatableResponse putRegistryWithCustomItems(Outbound outbound, List<Item> itemList, String shipmentDate, StockType stock) {

        String hash = UniqueId.getStringUUID();
        String items = "";
        String interval = String.format("%1$s/%1$s", shipmentDate);

        for (Item item: itemList) {
            items = items+ FileUtil.bodyStringFromFile("wms/servicebus/outboundItem.xml")
                    .replaceAll("ARTICLE_PLACEHOLDER", item.getArticle())
                    .replaceAll("VENDOR_ID_PLACEHOLDER", String.valueOf(item.getVendorId()))
                    .replaceAll("COUNT_PLACEHOLDER", String.valueOf(item.getQuantity()))
                    .replaceAll("STOCK_TYPE_PLACEHOLDER", stock.name());
        }

        log.info("Calling ServiceBus putOutboundRegistry");


        String reqString2 = FileUtil.bodyStringFromFile("wms/servicebus/putOutboundRegistryWithCustomItems.xml")
                .replaceAll("HASH_PLACEHOLDER", hash)
                .replaceAll("TOKEN_PLACEHOLDER", inforToken)
                .replaceAll("YANDEX_ID_PLACEHOLDER", String.valueOf(outbound.getYandexId()))
                .replaceAll("PARTNER_PLACEHOLDER", outbound.getPartnerId())
                .replaceAll("ITEM_PLACEHOLDER", items)
                .replaceAll("SHIPMENT_DATE_PLACEHOLDER", shipmentDate);

        return baseRequest(reqString2, "/api/logistic/putOutboundRegistry");
    }

    public ValidatableResponse cancelOutbound(Outbound outbound) {
        return cancelOutbound(outbound.getYandexId(), outbound.getFulfillmentId());
    }

    public ValidatableResponse cancelOutbound(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus cancelOutbound");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/cancelOutbound.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/cancelOutbound");
    }

    public ValidatableResponse getOutboundsStatus(Outbound outbound) {
        return getOutboundsStatus(outbound.getYandexId(), outbound.getFulfillmentId());
    }

    public ValidatableResponse getOutboundsStatus(long yandexId, String fulfillmentId) {
        log.info("Calling ServiceBus getOutboundsStatus");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/servicebus/getOutboundsStatus.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/logistic/getOutboundsStatus")
                .body("root.response.outboundsStatus.outboundStatus.outboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.outboundsStatus.outboundStatus.outboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.outboundsStatus.outboundStatus.outboundId.fulfillmentId",
                        is(fulfillmentId));
    }
}
