package ru.yandex.market.tpl.internal.controller.internal;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.tpl.core.service.delivery.tracker.DeliveryTrackNotifyService;
import ru.yandex.market.tpl.core.service.delivery.tracker.dto.NotificationResult;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author kukabara
 */
@WebLayerTest(DeliveryTrackerNotificationController.class)
class DeliveryTrackerNotificationControllerTest extends BaseShallowTest {

    private static final long NOT_FOUND_TRACKER_ID = 400L;

    @MockBean
    private DeliveryTrackNotifyService deliveryTrackNotifyService;

    @Test
    void notifyTracks() throws Exception {
        when(deliveryTrackNotifyService.notifyTracks(anyList())).thenAnswer(invocation -> {
            List<DeliveryTrack> tracks = invocation.getArgument(0);
            return tracks.stream()
                    .map(t -> t.getDeliveryTrackMeta().getId() == NOT_FOUND_TRACKER_ID ?
                            NotificationResult.notFound(t.getDeliveryTrackMeta().getId()) :
                            NotificationResult.ok(t.getDeliveryTrackMeta().getId()))
                    .collect(Collectors.toList());

        });

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("tracker/request_notify_tracks.json"))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("tracker/response_notify_tracks.json")));
    }
}
