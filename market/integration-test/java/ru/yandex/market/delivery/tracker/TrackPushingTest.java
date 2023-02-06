package ru.yandex.market.delivery.tracker;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.controller.DeliveryTrackerController;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackQueueProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты на ручку {@link DeliveryTrackerController#pushDeliveryTracks}.
 */
class TrackPushingTest extends AbstractContextualTest {

    @Autowired
    private PushTrackQueueProducer pushTrackQueueProducer;

    void setUp() {
        doNothing().when(pushTrackQueueProducer).enqueue(anySet());
        doNothing().when(pushTrackQueueProducer).enqueue(anyLong());
    }

    /**
     * Дергаем ручку, не передав туда айдишки.
     * Должны получить четырехсотку.
     */
    @Test
    void failPushWithoutIds() throws Exception {
        String contentAsString = httpOperationWithResult(
            post("/track/push"),
            status().is4xxClientError()
        );

        assertions()
            .assertThat(contentAsString)
            .contains("MissingServletRequestParameterException")
            .contains("ids");
    }

    /**
     * Добавляем в очередь entity_id=ORDER_1.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    void pushSuccessful() throws Exception {
        httpOperationWithResult(
            post("/track/push")
                .param("ids", "1"),
            status().is2xxSuccessful()
        );

        verify(pushTrackQueueProducer).enqueue(Collections.singleton(1L));
    }
}
