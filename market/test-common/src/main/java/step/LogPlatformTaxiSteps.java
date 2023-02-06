package step;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import client.LogPlatformTaxiClient;
import com.fasterxml.jackson.databind.JsonNode;
import dto.responses.logplatform.StationParameters;
import dto.responses.logplatform.admin.request_get.RequestGetResponse;
import dto.responses.logplatform.admin.station_tag_list.TagsItem;
import dto.responses.logplatform.cancel_order.RequestIdDto;
import dto.responses.logplatform.create_order.ConfirmOrderResponse;
import dto.responses.logplatform.create_order.CreateOrderResponse;
import dto.responses.logplatform.create_order.OfferIdDto;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;
import toolkit.Retrofits;

public class LogPlatformTaxiSteps {

    private static final LogPlatformTaxiClient LOG_PLATFORM_TAXI_CLIENT = new LogPlatformTaxiClient(Retrofits.RETROFIT);

    @Step("Получение всех тегов с заказами со станции")
    public List<TagsItem> getAllTagsWithOrdersOnStation(String stationId) {
        return LOG_PLATFORM_TAXI_CLIENT
            .getTagsOfStation(stationId).getObjects().get(0)
            .getTags().stream().filter(tagsItem -> tagsItem.getCarriageInstant() != null)
            .collect(Collectors.toList());
    }

    @Step("Получение тега с заказами на указанную дату со станции")
    public String getOrdersOnStation(String stationId, LocalDate date) {
        return getAllTagsWithOrdersOnStation(stationId).stream().filter(tagsItem -> Instant
            .ofEpochMilli(tagsItem.getCarriageInstant() * 1000)
            .atZone(ZoneId.systemDefault()).toLocalDate().equals(date))
            .map(TagsItem::getTagId).collect(Collectors.toList()).get(0);
    }

    @Step("Удалить заказы со станции на указанную дату")
    public void deleteOrdersFromStation(String stationId, LocalDate date) {
        LOG_PLATFORM_TAXI_CLIENT.cleanTagAndDelete(getOrdersOnStation(stationId, date));
    }

    @Step("Получение всех request_id заказов на указанную дату со станций (станции)") //станция
    private List<String> getRequestIdsFromStations(LocalDate date, StationParameters params) {
        return LOG_PLATFORM_TAXI_CLIENT.getListOfStations(params).getStations().get(0).getRequestIds().get(date);
    }

    @Step("Активация тега с заказами на станции по request_id")
    public void activationOrdersOnStation(LocalDate date, StationParameters params) {
        LOG_PLATFORM_TAXI_CLIENT.activationTags(getRequestIdsFromStations(date, params).get(0));
    }

    @Step("Получение информации о пакете заказов на указанную дату")
    private RequestGetResponse getInformationOfPackOrders(LocalDate date, StationParameters params) {
        return LOG_PLATFORM_TAXI_CLIENT.getOrder(getRequestIdsFromStations(date, params).get(0));
    }

    @Step("Ждем, что у пакета заказов появится определенный статус в системе S7")
    public void verifyForStatusInS7Received(LocalDate date, StationParameters params, String statusS7) {
        Retrier.retry(
            () -> {
                Assertions.assertTrue(getInformationOfPackOrders(date, params).getModel().getExternalRequests().stream()
                    .filter(externalRequestsItem -> externalRequestsItem.getOperatorId().equals("S7"))
                    .findFirst().get().getEvents().stream()
                    .filter(event -> event.get("operator_id").equals("S7"))
                    .map(event -> event.get("operator_event_type"))
                    .anyMatch(event -> event.equals(statusS7)));
            }
        );
    }

    @Step("Создание и подтверждение заказа в лог платформе")
    public String createAndConfirmOrder(String corporateClientId, JsonNode createOrderRequest) {
        CreateOrderResponse createOrderResponse =
            LOG_PLATFORM_TAXI_CLIENT.createOrder(corporateClientId, createOrderRequest);
        OfferIdDto offerIdDto = createOrderResponse.getOffers().get(0);
        ConfirmOrderResponse confirmOrderResponse =
            LOG_PLATFORM_TAXI_CLIENT.confirmOrder(corporateClientId, offerIdDto);
        return confirmOrderResponse.getRequestId();
    }

    @Step("Отмена заказа в лог платформе")
    public void cancelOrder(String corporateClientId, String requestId) {
        LOG_PLATFORM_TAXI_CLIENT.cancelOrder(corporateClientId, new RequestIdDto(requestId));
    }

    @Step("Проверяем наличие статуса {eventStatus} в истории статусов заказа")
    public void verifyOrderHasStatus(String corporateClientId, String requestId, String eventStatus) {
        Retrier.retry(() ->
            Assertions.assertTrue(
                LOG_PLATFORM_TAXI_CLIENT.getOrderHistory(corporateClientId, requestId).getStateHistory().stream()
                    .anyMatch(event -> StringUtils.equals(event.getStatus(), eventStatus)),
                "Не найден статус " + eventStatus + " в истории заказа"
            )
        );
    }
}
