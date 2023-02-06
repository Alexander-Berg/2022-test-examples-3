package ru.yandex.market.checkout.helpers;

import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebTestHelper
public class AddTrackHelper extends MockMvcAware {

    public AddTrackHelper(WebApplicationContext webApplicationContext,
                          TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public void addTrack(long orderId, long parcelId, Track track, ClientInfo clientInfo) throws Exception {
        performApiRequest(post("/orders/{orderId}/delivery/parcels/{parcelId}/track", orderId, parcelId)
                .param(CheckouterClientParams.CLIENT_ROLE, clientInfo.getRole().name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(clientInfo.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(testSerializationService.serializeCheckouterObject(track)));
    }
}
