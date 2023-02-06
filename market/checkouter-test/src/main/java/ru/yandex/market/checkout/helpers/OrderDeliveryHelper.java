package ru.yandex.market.checkout.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryServiceCustomerInfoList;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TracksList;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class OrderDeliveryHelper extends MockMvcAware {

    public OrderDeliveryHelper(WebApplicationContext webApplicationContext,
                               TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public Order updateOrderDelivery(Long orderId, Delivery delivery) throws Exception {
        return updateOrderDelivery(orderId, ClientInfo.SYSTEM, delivery);
    }

    public Order updateOrderDelivery(long orderId,
                                     ClientInfo clientInfo,
                                     Delivery delivery) throws Exception {
        return performApiRequest(buildRequest(orderId, clientInfo, delivery, Collections.emptyMap()), Order.class);
    }

    public ErrorCodeException updateOrderDeliveryFailed(long orderId,
                                                        ClientInfo clientInfo,
                                                        Delivery delivery) throws Exception {
        ResultActions response = mockMvc.perform(buildRequest(orderId, clientInfo, delivery, Collections.emptyMap()));
        response.andExpect(status().is4xxClientError());
        return testSerializationService.deserializeCheckouterObject(
                response.andReturn().getResponse().getContentAsString(),
                ErrorCodeException.class
        );
    }

    public Order updateOrderDelivery(Order order,
                                     ClientInfo clientInfo,
                                     Delivery delivery) throws Exception {
        return performApiRequest(
                buildRequest(order.getId(), clientInfo, delivery, Collections.emptyMap()),
                Order.class
        );
    }

    public ResultActions updateOrderDeliveryForActions(long orderId,
                                                       ClientInfo clientInfo,
                                                       Delivery delivery) throws Exception {
        return performApiRequest(buildRequest(orderId, clientInfo, delivery, Collections.emptyMap()));
    }

    public MockHttpServletRequestBuilder buildRequest(long orderId,
                                                      ClientInfo clientInfo,
                                                      Delivery delivery,
                                                      Map<String, String> params) {
        String content = testSerializationService.serializeCheckouterObject(delivery);
        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/delivery", orderId);
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

        for (Map.Entry<String, String> e : params.entrySet()) {
            builder = builder.param(e.getKey(), e.getValue());
        }

        return builder
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(content);
    }

    public Track addTrack(long orderId,
                          long parcelId,
                          Track track,
                          ClientInfo clientInfo,
                          ResultActionsContainer container) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(track);
        MockHttpServletRequestBuilder builder = post(
                "/orders/{order-id}/delivery/parcels/{parcel-id}/track",
                orderId, parcelId
        );
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

        return performApiRequest(
                builder
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(content),
                Track.class,
                container);
    }

    public Track addTrack(long orderId, long parcelId, Track track, ClientInfo clientInfo) throws Exception {
        return addTrack(orderId, parcelId, track, clientInfo, null);
    }

    public Track addTrack(Order order, Track track, ClientInfo clientInfo) throws Exception {
        return addTrack(order.getId(), order.getDelivery().getParcels().get(0).getId(), track, clientInfo, null);
    }

    public List<Track> putTrack(
            long orderId,
            long parcelId,
            List<Track> tracks,
            ClientInfo clientInfo
    ) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(new TracksList(tracks));
        MockHttpServletRequestBuilder builder = put(
                "/orders/{order-id}/delivery/parcels/{parcel-id}/tracks/update",
                orderId, parcelId
        );
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);


        return performApiRequest(builder
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(content),
                TracksList.class,
                null)
                .getTracks();
    }

    public void putTrackWithException(
            long orderId,
            long parcelId,
            List<Track> tracks,
            ClientInfo clientInfo
    ) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(new TracksList(tracks));
        MockHttpServletRequestBuilder builder = put(
                "/orders/{order-id}/delivery/parcels/{parcel-id}/tracks/update",
                orderId, parcelId
        );
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);


        performApiRequest(builder
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(content),
                TracksList.class,
                new ResultActionsContainer().andExpect(status().isBadRequest()));
    }

    public DeliveryServiceCustomerInfoList getDeliveryServiceInfo(
            List<Long> deliveryServiceIds
    ) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(deliveryServiceIds);
        MockHttpServletRequestBuilder builder = get("/delivery-service-info");
        return performApiRequest(
                builder
                        .param("deliveryServiceIds", Joiner.on(",").join(deliveryServiceIds)),
                DeliveryServiceCustomerInfoList.class
        );
    }

}
