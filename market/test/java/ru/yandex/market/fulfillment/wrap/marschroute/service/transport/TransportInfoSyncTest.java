package ru.yandex.market.fulfillment.wrap.marschroute.service.transport;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.scheduled.TransportInfoSync;
import ru.yandex.market.fulfillment.wrap.marschroute.service.MarschrouteTransportService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransportInfoSyncTest {

    @Mock
    private MarschrouteTransportService transportService;

    @InjectMocks
    private TransportInfoSync sync;

    @Test
    void testProduce() {
        sync.produce();
        verify(transportService, times(1)).syncTransportInfo(any(LocalDate.class));
    }
}
