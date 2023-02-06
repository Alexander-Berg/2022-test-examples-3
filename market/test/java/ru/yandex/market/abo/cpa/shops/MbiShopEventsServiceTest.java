package ru.yandex.market.abo.cpa.shops;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.shops.listener.CpaShopEnableListener;
import ru.yandex.market.core.moderation.event.Event;
import ru.yandex.market.core.moderation.event.EventList;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.monitoring.MonitoringUnit;
import ru.yandex.market.util.db.ConfigurationService;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author korolyov
 * 27.10.16
 */
public class MbiShopEventsServiceTest extends EmptyTest {
    @Autowired
    private MbiShopEventsService mbiShopEventsService;
    @Autowired
    private MonitoringUnit cpaShopEventsMonitoring;
    private long eventId = 0;
    private CpaShopEnableListener cpaShopEnableListener = spy(new CpaShopEnableListenerMock());

    @BeforeEach
    public void setUp() {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getValueAsLong(anyString())).thenReturn(-1L);

        MbiApiClient mbiApiClient = mock(MbiApiClient.class);
        when(mbiApiClient.getEvents(anyLong(), any(), any(), anyLong())).thenReturn(generateEventList(), generateEventList(), new EventList());

        mbiShopEventsService.setMbiApiClient(mbiApiClient);
        mbiShopEventsService.setCountersConfigurationService(configurationService);

        mbiShopEventsService.setShopEventsListeners(singletonList(cpaShopEnableListener));

        cpaShopEventsMonitoring.ok();
    }

    @Test
    public void receiveUpdatesGoodEventsTest() {
        doNothing().when(cpaShopEnableListener).process(any());
        mbiShopEventsService.receiveEvents();
        assertSame(cpaShopEventsMonitoring.getStatus(), MonitoringStatus.OK);
    }

    @Test
    public void receiveUpdatesBadEventsTest() {
        doNothing().when(cpaShopEnableListener).process(any());
        doThrow(new RuntimeException("ERROR")).when(cpaShopEnableListener).process(any());
        mbiShopEventsService.receiveEvents();

        assertSame(cpaShopEventsMonitoring.getStatus(), MonitoringStatus.CRITICAL);
        assertTrue(cpaShopEventsMonitoring.getMessage().contains("cpaShopEnableListener"));
    }

    private EventList generateEventList() {
        return new EventList(RND.longs(100)
                .mapToObj(id -> new Event(eventId++, id, null, null, null, Timestamp.from(Instant.now())))
                .collect(toList()));
    }

    /**
     * mockito версии ниже 2 не умеет в дефолтные методы - see https://stackoverflow.com/a/41396236.
     */
    private static class CpaShopEnableListenerMock extends CpaShopEnableListener {
    }
}
