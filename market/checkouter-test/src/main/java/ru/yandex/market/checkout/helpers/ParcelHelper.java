package ru.yandex.market.checkout.helpers;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.providers.ParcelPatchRequestProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * @author mmetlov
 */
@WebTestHelper
public class ParcelHelper extends MockMvcAware {

    @Autowired
    private TestSerializationService serializationService;

    public ParcelHelper(WebApplicationContext webApplicationContext,
                        TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }


    public ResultActions updateParcelDeliveredAt(Order order, Parcel parcel, Instant deliveredAt) throws Exception {
        return updateParcelForActions(
                order.getId(),
                parcel.getId(),
                ParcelPatchRequestProvider.getDeliveredAtUpdateRequest(deliveredAt),
                ClientInfo.SYSTEM
        );
    }


    public ResultActions updateCancellationRequestStatus(long orderId,
                                                         long parcelId,
                                                         CancellationRequestStatus newStatus,
                                                         ClientInfo clientInfo) throws Exception {
        MockHttpServletRequestBuilder builder = post(
                "/orders/{orderId}/delivery/parcels/{parcelId}/cancellation-request/status",
                orderId,
                parcelId
        ).contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(serializationService.serializeCheckouterObject(newStatus));

        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

        return performApiRequest(builder);
    }

    public ResultActions updateParcelForActions(long orderId, long parcelId, ParcelPatchRequest request,
                                                ClientInfo clientInfo) throws Exception {
        MockHttpServletRequestBuilder builder = patch(
                "/orders/{orderId}/delivery/parcels/{parcelsId}", orderId, parcelId
        )
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(serializationService.serializeCheckouterObject(request));

        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

        return performApiRequest(builder);
    }
}
