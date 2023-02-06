package ru.yandex.market.delivery.tracker.service.logger;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.domain.enums.HasIntId.enumById;

class DsRequestsLoggerTest {

    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private DsRequestsLogger dsRequestsLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void logRequestWithEmptyData() {
        DeliveryTrackMeta trackMeta = new DeliveryTrackMeta();
        RequestType requestType = enumById(RequestType.class, 0);
        Date requestDate = null;
        DeliveryService deliveryService = null;

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("trackMethod", "ORDER_HISTORY")
            .put("deliveryServiceId", "0")
            .put("entityId", "")
            .put("orderId", "")
            .put("trackCode", "")
            .put("requestTs", "")
            .put("scheduledRequestTs", "")
            .put("updatedTs", "")
            .put("lastStatusRequestTs", "")
            .put("lastOrdersStatusRequestTs", "")
            .put("entityType", "")
            .put("deliveryServiceRole", "")
            .build();

        when(tskvLogger.formatDate(null)).thenReturn("");

        dsRequestsLogger.logRequest(trackMeta, requestType, requestDate, deliveryService);

        verify(tskvLogger).log(expectedTskvMap);
    }

    @Test
    void logRequestWithFullData() {
        Date nextRequestDate = Date.from(Instant.parse("2019-01-01T00:00:00Z"));
        Date lastUpdatedDate = Date.from(Instant.parse("2019-02-02T00:00:00Z"));
        Date lastOrdersStatusRequestDate = Date.from(Instant.parse("2019-03-03T00:00:00Z"));

        DeliveryTrackMeta trackMeta = new DeliveryTrackMeta();
        trackMeta.setDeliveryServiceId(1);
        trackMeta.setEntityId("OrderId_1");
        trackMeta.setTrackCode("TrackCode_1");
        trackMeta.setNextRequestDate(nextRequestDate);
        trackMeta.setLastUpdatedDate(lastUpdatedDate);
        trackMeta.setLastStatusRequestDate(lastOrdersStatusRequestDate);
        trackMeta.setEntityType(EntityType.ORDER);

        RequestType requestType = enumById(RequestType.class, 1);

        Date requestDate = Date.from(Instant.parse("2019-04-04T00:00:00Z"));

        DeliveryService deliveryService = new DeliveryService(
            1,
            "ds",
            DeliveryServiceType.DELIVERY,
            0
        );
        deliveryService.setActive(true);
        deliveryService.setRole(DeliveryServiceRole.DELIVERY);

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("trackMethod", "ORDER_STATUS")
            .put("deliveryServiceId", "1")
            .put("entityId", "OrderId_1")
            .put("orderId", "OrderId_1")
            .put("trackCode", "TrackCode_1")
            .put("requestTs", "2019-04-04 00:00:00")
            .put("scheduledRequestTs", "2019-01-01 00:00:00")
            .put("updatedTs", "2019-02-02 00:00:00")
            .put("lastStatusRequestTs", "2019-03-03 00:00:00")
            .put("lastOrdersStatusRequestTs", "2019-03-03 00:00:00")
            .put("entityType", "ORDER")
            .put("deliveryServiceRole", "DELIVERY")
            .build();

        when(tskvLogger.formatDate(nextRequestDate)).thenReturn("2019-01-01 00:00:00");
        when(tskvLogger.formatDate(lastUpdatedDate)).thenReturn("2019-02-02 00:00:00");
        when(tskvLogger.formatDate(lastOrdersStatusRequestDate)).thenReturn("2019-03-03 00:00:00");
        when(tskvLogger.formatDate(requestDate)).thenReturn("2019-04-04 00:00:00");

        dsRequestsLogger.logRequest(trackMeta, requestType, requestDate, deliveryService);

        verify(tskvLogger).log(expectedTskvMap);
    }
}
