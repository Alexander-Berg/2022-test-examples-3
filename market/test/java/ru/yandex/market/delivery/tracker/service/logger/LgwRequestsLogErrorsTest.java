package ru.yandex.market.delivery.tracker.service.logger;

import java.nio.charset.Charset;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerEntityId;
import ru.yandex.market.delivery.tracker.client.tracking.common.TrackingService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.MappedDeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.TrackRequestMeta;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.delivery.tracker.service.tracking.DeliveryServiceFetcher;
import ru.yandex.market.delivery.tracker.service.tracking.DeliveryTracker;
import ru.yandex.market.delivery.tracker.service.tracking.StatusListUpdatingChecker;
import ru.yandex.market.delivery.tracker.service.tracking.processor.history.HistoryProcessor;
import ru.yandex.market.delivery.tracker.service.tracking.processor.status.StatusResponseProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LgwRequestsLogErrorsTest {

    @RegisterExtension
    JUnitJupiterSoftAssertions assertions = new JUnitJupiterSoftAssertions();

    @Mock
    private HistoryProcessor historyResponseProcessor;

    @Mock
    private StatusResponseProcessor statusResponseProcessor;

    @Mock
    private TrackingService apiClient;

    @Mock
    private TrackingFailureLogger failureLogger;

    @Mock
    private DeliveryServiceFetcher deliveryServiceFetcher;

    private Clock clock = Clock.fixed(
        LocalDate.parse("2019-01-01").atStartOfDay().toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC
    );
    private DeliveryTracker deliveryTracker;
    private StatusListUpdatingChecker statusListUpdatingChecker;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @BeforeEach
    void setUp() {
        deliveryTracker = new DeliveryTracker(
            historyResponseProcessor,
            apiClient,
            deliveryServiceFetcher,
            failureLogger,
            clock
        );
        statusListUpdatingChecker = new StatusListUpdatingChecker(
            statusResponseProcessor,
            apiClient,
            deliveryServiceFetcher,
            failureLogger,
            clock
        );
    }

    @Test
    void testGetOrderHistory4xx() {
        byte[] responseBody = "Some history 4xx error text".getBytes();
        when(apiClient.getHistory(anyLong(), any(TrackerEntityId.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, null, responseBody,
                Charset.defaultCharset()
            ));

        deliveryTracker.track(new DeliveryTrackMeta(), getTrackRequestMeta(), SurveyType.MANUAL, 1L);

        verify(failureLogger).logFailure(
            anyLong(),
            any(DeliveryTrackMeta.class),
            any(TrackRequestMeta.class),
            stringCaptor.capture(),
            isNull()
        );

        String error = stringCaptor.getValue();
        assertions.assertThat(error).isEqualTo("Some history 4xx error text");
    }

    @Test
    void testGetOrdersStatus4xx() {
        byte[] responseBody = "Some status 4xx error text".getBytes();
        when(apiClient.getStatus(anyLong(), anyList()))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, null, responseBody,
                Charset.defaultCharset()
            ));

        DeliveryTrackMeta track = new DeliveryTrackMeta();
        track.setId(2L);
        MappedDeliveryTrackMeta trackMeta = new MappedDeliveryTrackMeta();
        trackMeta.setBatchId(1L);
        trackMeta.setDeliveryTrackMeta(track);

        statusListUpdatingChecker.getBatchesWithNewStatuses(List.of(trackMeta), getTrackRequestMeta());

        verify(failureLogger).logFailure(
            anyLong(),
            any(List.class),
            any(TrackRequestMeta.class),
            stringCaptor.capture(),
            isNull()
        );

        String error = stringCaptor.getValue();
        assertions.assertThat(error).isEqualTo("Some status 4xx error text");
    }

    private TrackRequestMeta getTrackRequestMeta() {
        return new TrackRequestMeta()
            .setServiceId(1L)
            .setToken("token")
            .setUrl("url")
            .setName("name")
            .setType(RequestType.ORDER_HISTORY)
            .setVersion(ApiVersion.DS);
    }
}
