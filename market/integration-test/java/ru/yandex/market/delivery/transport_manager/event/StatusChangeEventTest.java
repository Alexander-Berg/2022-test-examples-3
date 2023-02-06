package ru.yandex.market.delivery.transport_manager.event;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.StatusChangeEventConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.TransportationStatusService;

@DatabaseSetup("/repository/event/transportation_with_deps.xml")
public class StatusChangeEventTest extends AbstractContextualTest {
    @Autowired
    private TransportationStatusService transportationStatusService;
    @Autowired
    private TransportationMapper transportationMapper;
    @Autowired
    private StatusChangeEventConfig.TestTransportationStatusChangeEventApplicationListener applicationListener;

    @AfterEach
    void tearDown() {
        applicationListener.setShouldFail(false);
        applicationListener.setEvent(null);
    }

    @Test
    void listenSuccessfully() {
        applicationListener.setShouldFail(false);

        transportationStatusService.setTransportationStatus(1, TransportationStatus.SCHEDULED);
        checkTransportationEvent();

        // Статус изменился
        softly.assertThat(transportationMapper.getStatus(1L))
            .isEqualTo(TransportationStatus.SCHEDULED);
    }

    @Test
    void listenWithFailure() {
        applicationListener.setShouldFail(true);

        softly.assertThatThrownBy(() ->
            transportationStatusService.setTransportationStatus(1, TransportationStatus.SCHEDULED));
        checkTransportationEvent();

        // Статус не изменился, так как исключение в слушателе вызвало откат транзакции
        softly.assertThat(transportationMapper.getStatus(1L))
            .isEqualTo(TransportationStatus.NEW);
    }

    private void checkTransportationEvent() {
        softly.assertThat(applicationListener.getEvent()).isNotNull();
        softly.assertThat(applicationListener.getEvent().getEntityType()).isEqualTo(EntityType.TRANSPORTATION);
        softly.assertThat(applicationListener.getEvent().getEntityId()).isEqualTo(1);
        softly.assertThat(applicationListener.getEvent().getOldStatus()).isEqualTo(TransportationStatus.NEW);
        softly.assertThat(applicationListener.getEvent().getNewStatus()).isEqualTo(TransportationStatus.SCHEDULED);
    }
}
