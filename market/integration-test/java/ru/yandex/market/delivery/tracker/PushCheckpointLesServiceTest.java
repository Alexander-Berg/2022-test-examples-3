package ru.yandex.market.delivery.tracker;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.delivery.tracker.domain.dto.TrackCheckpointsDto;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.service.pushing.PushCheckpointLesService;
import ru.yandex.market.delivery.tracker.service.tracking.DeliveryTrackService;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpyBean(LesProducer.class)
public class PushCheckpointLesServiceTest extends AbstractContextualTest {

    @Autowired
    private PushCheckpointLesService pushService;

    @Autowired
    private LesProducer lesProducer;

    @Autowired
    private DeliveryTrackService deliveryTrackService;

    /**
     * Пуш выбранного чекпоинта и проверка аргументов
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_two_checkpoints.xml")
    public void pushFilteredCheckpoint() {
        doNothing().when(lesProducer).send(any(Event.class), anyString());
        long trackId = 1;

        DeliveryTrackCheckpoint expected = deliveryTrackService.getDeliveryTrackInfo(trackId)
            .getDeliveryTrackCheckpoints().stream()
            .findAny().get();

        TrackCheckpointsDto dto = new TrackCheckpointsDto(trackId, Set.of(expected.unicityHash()));
        pushService.pushTrackCheckpoints(dto);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(lesProducer, times(1)).send(captor.capture(), eq("tracker_out"));
        Event argument = captor.getValue();

        assertions().assertThat(argument.getSource()).isEqualTo("tracker");
        assertions().assertThat(Long.valueOf(argument.getEventId())).isEqualTo(expected.getId());
        assertions().assertThat(argument.getTimestamp()).isEqualTo(expected.getCheckpointDate().getTime());
        assertions().assertThat(argument.getEventType()).isEqualTo(expected.getDeliveryCheckpointStatus().name());
        assertions()
            .assertThat(argument.getDescription())
            .isEqualTo(expected.getDeliveryCheckpointStatus().getDescription());
    }

}
