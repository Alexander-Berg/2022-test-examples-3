package ru.yandex.market.checkout.helpers;

import java.util.Collections;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.NotificationResults;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class NotifyTracksHelper extends MockMvcAware {

    public NotifyTracksHelper(WebApplicationContext webApplicationContext,
                              TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public void notifyTracks(DeliveryTrack deliveryTrack) throws Exception {
        notifyTracksForActions(deliveryTrack)
                .andExpect(status().isOk());

    }

    public NotificationResults notifyTracksForResult(DeliveryTrack deliveryTrack) throws Exception {
        return performApiRequest(post("/notify-tracks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testSerializationService.serializeCheckouterObject(
                                Collections.singletonList(deliveryTrack))),
                NotificationResults.class);
    }

    public ResultActions notifyTracksForActions(DeliveryTrack deliveryTrack) throws Exception {
        MockHttpServletRequestBuilder request = post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testSerializationService.serializeCheckouterObject(Collections.singletonList(deliveryTrack)));

        return mockMvc.perform(request)
                .andDo(log());
    }
}
