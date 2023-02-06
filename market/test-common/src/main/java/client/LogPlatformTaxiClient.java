package client;

import api.LogPlatformTaxiApi;
import com.fasterxml.jackson.databind.JsonNode;
import dto.responses.logplatform.StationParameters;
import dto.responses.logplatform.admin.request_get.RequestGetResponse;
import dto.responses.logplatform.admin.station_list.StationListResponse;
import dto.responses.logplatform.admin.station_tag_list.StationTagListResponse;
import dto.responses.logplatform.cancel_order.RequestIdDto;
import dto.responses.logplatform.create_order.ConfirmOrderResponse;
import dto.responses.logplatform.create_order.CreateOrderResponse;
import dto.responses.logplatform.create_order.OfferIdDto;
import dto.responses.logplatform.order_history.OrderStatusHistoryResponse;
import io.qameta.allure.Step;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import toolkit.Retrofits;

import ru.yandex.common.util.collections.CollectionUtils;

import static toolkit.Retrofits.RETROFIT;
import static toolkit.Retrofits.RETROFIT_XML;

@Slf4j
@Resource.Classpath("delivery/logplatformtaxi.properties")
public class LogPlatformTaxiClient {

    private final LogPlatformTaxiApi logPlatformTaxiApi;
    @Property("logplatformtaxi.host")
    private String host;

    public LogPlatformTaxiClient(Retrofits retrofit) {
        PropertyLoader.newInstance().populate(this);
        retrofit = retrofit == RETROFIT ? RETROFIT : RETROFIT_XML;
        logPlatformTaxiApi = retrofit.getRetrofit(host).create(LogPlatformTaxiApi.class);
    }

    //todo для дебага, удалить
//    public LogPlatformTaxiClient() {
//        PropertyLoader.newInstance().populate(this);
//        logPlatformTaxiApi = RETROFIT.getRetrofit(host).create(LogPlatformTaxiApi.class);
//    }

    @SneakyThrows
    @Step("Получение списка станций по условиям")
    public StationListResponse getListOfStations(StationParameters params) {
        log.debug("Getting list of stations...");

        Response<StationListResponse> response = logPlatformTaxiApi.getListOfStations(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM),
            params.getLimit(),
            params.getDump(),
            String.valueOf(params.getCapacity()),
            params.getStationId(),
            params.getOperatorId()
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос получения списка станций");
        Assertions.assertNotNull(response, "Пустой ответ: получение списка станций");
        return response.body();
    }

    @SneakyThrows
    @Step("Получения списка тегов указанной станции")
    public StationTagListResponse getTagsOfStation(String stationId) {
        log.debug("Getting tags of station...");

        Response<StationTagListResponse> response = logPlatformTaxiApi.getTagsOfStation(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM), stationId
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос получения тегов станции");
        Assertions.assertNotNull(response, "Пустой ответ: получение списка станций");
        return response.body();
    }

    @SneakyThrows
    @Step("Освобождение тега и последующее удаление")
    public void cleanTagAndDelete(String tag) {
        log.debug("Clean and delete tag...");

        Response<ResponseBody> response = logPlatformTaxiApi.cleanTagAndDelete(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM), tag
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос освобождения тега и его удаления");
        Assertions.assertNotNull(response.body(), "Не удалось получить объект response");
    }

    @SneakyThrows
    @Step("Активация тегов")
    public void activationTags(String requestId) {
        log.debug("Activation tags by request_id...");

        Response<ResponseBody> response = logPlatformTaxiApi.findAndActivationTags(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM),
            10L,
            requestId
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос активации тегов");
        Assertions.assertNotNull(response.body(), "Не удалось получить объект activationTags");
    }

    @SneakyThrows
    @Step("Получение информации по заказу")
    public RequestGetResponse getOrder(String requestId) {
        log.debug("Getting information by request...");

        Response<RequestGetResponse> response = logPlatformTaxiApi.getInformationByRequest(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM),
            requestId
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос получения информации по заказу");
        Assertions.assertNotNull(response.body(), "Пустой ответ: получение информации по заказу");
        return response.body();
    }

    @SneakyThrows
    @Step("Создание заказа")
    public CreateOrderResponse createOrder(String corporateClientId, JsonNode createOrderRequest) {
        Response<CreateOrderResponse> response = logPlatformTaxiApi.createOrder(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM),
            corporateClientId,
            createOrderRequest
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос создания заказа");
        Assertions.assertNotNull(response.body(), "Пустой ответ: создание заказа");
        Assertions.assertFalse(CollectionUtils.isEmpty(response.body().getOffers()), "Пустой список офферов в заказе");
        return response.body();
    }

    @SneakyThrows
    @Step("Подтверждение заказа")
    public ConfirmOrderResponse confirmOrder(String corporateClientId, OfferIdDto offerIdDto) {
        Response<ConfirmOrderResponse> response = logPlatformTaxiApi.confirmOrder(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM),
            corporateClientId,
            offerIdDto
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос подтверждения заказа");
        Assertions.assertNotNull(response.body(), "Пустой ответ: подтверждение заказа");
        return response.body();
    }

    @SneakyThrows
    public void cancelOrder(String corporateClientId, RequestIdDto requestIdDto) {
        Response<ResponseBody> response = logPlatformTaxiApi.cancelOrder(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM),
            corporateClientId,
            requestIdDto
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос отмены заказа");
    }

    @SneakyThrows
    public OrderStatusHistoryResponse getOrderHistory(String corporateClientId, String requestId) {
        Response<OrderStatusHistoryResponse> response = logPlatformTaxiApi.getOrderStatusHistory(
            TVM.INSTANCE.getServiceTicket(TVM.PLATFORM),
            corporateClientId,
            requestId
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось выполнить запрос получения истории статусов заказа");
        Assertions.assertNotNull(response.body(), "Пустой ответ: история статусов заказа");
        return response.body();
    }
}
