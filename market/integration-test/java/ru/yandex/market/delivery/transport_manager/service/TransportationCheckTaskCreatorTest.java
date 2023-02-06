package ru.yandex.market.delivery.transport_manager.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.checker.TransportationCheckerProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.checker.TransportationCheckTaskCreator;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;

import static org.mockito.Mockito.mock;

class TransportationCheckTaskCreatorTest extends AbstractContextualTest {

    @Autowired
    private TransportationMapper transportationMapper;

    private TransportationCheckerProducer transportationCheckerProducer = mock(TransportationCheckerProducer.class);
    private TransportationCheckTaskCreator transportationCheckTaskCreator;

    @Autowired
    private TransportationService transportationService;

    @Autowired
    private TransportationStatusService transportationStatusService;

    @BeforeEach
    void initMock() {
        transportationCheckTaskCreator = new TransportationCheckTaskCreator(
            transportationService,
            transportationStatusService,
            transportationCheckerProducer
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    void testLookup() {
        transportationCheckTaskCreator.lookup();

        Mockito.verify(transportationCheckerProducer, Mockito.times(2))
            .enqueue(Mockito.any(EnqueueParams.class));

        softly.assertThat(transportationMapper.getById(1L).getStatus())
            .isEqualTo(TransportationStatus.SCHEDULED);

        softly.assertThat(transportationMapper.getById(2L).getStatus())
            .isEqualTo(TransportationStatus.CHECK_PREPARED);

        softly.assertThat(transportationMapper.getById(3L).getStatus())
            .isEqualTo(TransportationStatus.CHECK_PREPARED);
    }
}
