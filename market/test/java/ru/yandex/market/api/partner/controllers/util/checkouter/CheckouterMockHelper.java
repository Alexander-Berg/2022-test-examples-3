package ru.yandex.market.api.partner.controllers.util.checkouter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import ru.yandex.lang.NonNullApi;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@NonNullApi
public class CheckouterMockHelper {
    private final RestTemplate checkouterRestTemplate;
    private final String checkouterUrl;

    public CheckouterMockHelper(
            RestTemplate checkouterRestTemplate,
            String checkouterUrl
    ) {
        this.checkouterRestTemplate = checkouterRestTemplate;
        this.checkouterUrl = checkouterUrl;
    }

    public MockRestServiceServer getServer() {
        return MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    public void mockGetOrder(long orderId, long clientId, ResponseCreator responseCreator) {
        mockGetOrder(orderId, clientId)
                .andRespond(responseCreator);
    }

    public void mockGetOrder(long orderId, List<Long> clientIds, ResponseCreator responseCreator) {
        mockGetOrder(orderId, clientIds)
                .andRespond(responseCreator);
    }

    public ResponseActions mockGetOrder(long orderId, long clientId) {
        var server = getServer();
        return mockGetOrder(server, orderId, clientId);
    }

    public ResponseActions mockGetOrder(long orderId, List<Long> clientIds) {
        var server = getServer();
        return mockGetOrder(server, orderId, clientIds);
    }

    public ResponseActions mockGetOrder(MockRestServiceServer server, long orderId, long clientId) {
        return server
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(
                        String.format("%s/orders/%d?clientRole=SHOP&clientId=%d&shopId=&archived=false",
                                checkouterUrl, orderId, clientId
                        )));
    }

    public ResponseActions mockGetOrder(MockRestServiceServer server, long orderId, List<Long> clientIds) {
        String clientIdsQuery = clientIds.stream()
                .map(id -> "clientIds=" + id)
                .collect(Collectors.joining("&"));
        return server
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(
                        String.format("%s/orders/%d?clientRole=SHOP&clientId=&%s&shopId=&archived=false",
                                checkouterUrl, orderId, clientIdsQuery
                        )));
    }

    public void mockGetOrderWithChangeRequestReturnsBody(
            MockRestServiceServer server,
            long orderId,
            long clientId,
            String body) {
        server
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(
                        String.format("%s/orders/%d?clientRole=SHOP&clientId=%d&shopId=&archived=false" +
                                        "&partials=CHANGE_REQUEST",
                                checkouterUrl, orderId, clientId
                        )))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body));
    }

    public void mockChangeRequestStatusUpdate(
            MockRestServiceServer server,
            long orderId,
            long clientId,
            long cancellationId) {
        server
                .expect(method(HttpMethod.PATCH))
                .andExpect(requestTo(
                        String.format("%s/orders/%d/change-requests/%d?clientRole=SHOP&clientId=%d",
                                checkouterUrl, orderId, cancellationId, clientId
                        )))
                .andRespond(withSuccess());
    }

    public void mockGetOrderReturnsBody(long orderId, long clientId, String body) {
        mockGetOrder(orderId, clientId, withSuccess()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    }

    public void mockGetOrderReturnsBody(long orderId, List<Long> clientIds, String body) {
        mockGetOrder(orderId, clientIds, withSuccess()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    }

    public void mockGetBuyerPhoneVisibleOrder(long orderId, long clientId, String body) {
        getServer()
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(
                        String.format("%s/orders/%d?clientRole=SHOP&clientId=%d&shopId=&archived=false&partials=BUYER_PHONE",
                                checkouterUrl, orderId, clientId
                        )))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body));
    }

    public void mockGetOrders(
            long clientId,
            ResponseCreator responseCreator
    ) {
        mockGetOrders(clientId)
                .andRespond(responseCreator);
    }

    public ResponseActions mockGetOrders(long clientId) {
        var server = getServer();
        return mockGetOrders(clientId, server);
    }

    public ResponseActions mockGetOrders(long clientId, MockRestServiceServer server) {
        return server
                .expect(method(HttpMethod.POST))
                .andExpect(requestTo(
                        String.format("%s/get-orders?clientRole=SHOP&clientId=%d&shopId=&archived=false",
                                checkouterUrl, clientId
                        )));
    }

    public void mockGetOrdersReturnsBodyItems(long clientId, String... orderJsons) {
        mockGetOrdersReturnsBody(clientId, "{\"orders\":[" + String.join(",", orderJsons) + "]}");
    }

    public void mockGetOrdersReturnsBody(long clientId, String body) {
        mockGetOrders(clientId, withSuccess()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    }

    public void mockOrderStatusChange(
            long orderId,
            long clientId,
            OrderSubstatus orderSubstatus,
            ResponseCreator responseCreator) {
        var server = getServer();
        mockOrderStatusChange(server, orderId, clientId, orderSubstatus)
                .andRespond(responseCreator);
    }

    public ResponseActions mockOrderStatusChange(
            MockRestServiceServer server,
            long orderId,
            long clientId,
            OrderSubstatus orderSubstatus
    ) {
        var url = String.format(
                "%s/orders/%d/status?clientRole=SHOP&clientId=%d&shopId=&status=%s&substatus=%s",
                checkouterUrl, orderId, clientId, orderSubstatus.getStatus().name(), orderSubstatus.name()
        );
        return server
                .expect(method(HttpMethod.POST))
                .andExpect(requestTo(url));
    }

    public ResponseActions mockUpdateDeliveryTracks(
            MockRestServiceServer server,
            long orderId,
            long parcelId,
            long clientId) {
        var url = String.format(
                "%s/orders/%d/delivery/parcels/%d/tracks/update?clientRole=SHOP&clientId=%d",
                checkouterUrl, orderId, parcelId, clientId
        );
        return server
                .expect(method(HttpMethod.PUT))
                .andExpect(requestTo(url));
    }

    public ResponseActions mockEditOrder(
            MockRestServiceServer server,
            long orderId,
            long clientId,
            String color) {
        var url = String.format(
                "WHITE".equals(color) ?
                        "%s/orders/%d/edit?clientRole=SHOP&clientId=%d&rgb=%s" :
                        "%s/orders/%d/edit?clientRole=SHOP&clientId=%d&shopId=&rgb=%s",
                checkouterUrl, orderId, clientId, color
        );
        return server
                .expect(method(HttpMethod.POST))
                .andExpect(requestTo(url));
    }

    public void mockUpdateDeliveryTracks(
            MockRestServiceServer server,
            long orderId,
            long parcelId,
            long clientId,
            ResponseCreator responseCreator) {
        mockUpdateDeliveryTracks(server, orderId, parcelId, clientId).andRespond(responseCreator);
    }
}
