package ru.yandex.market.delivery.tracker.service.tracking;

import java.util.Optional;
import java.util.stream.IntStream;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import ru.yandex.market.delivery.tracker.dao.repository.TrackRequestMetaDao;
import ru.yandex.market.delivery.tracker.domain.entity.TrackRequestMeta;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.RequestMethodType;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackRequestMetaCachingProviderTest {

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    private final TrackRequestMetaDao trackRequestMetaDao = Mockito.mock(TrackRequestMetaDao.class);

    private final TrackRequestMetaCachingProvider cachingProvider =
        new TrackRequestMetaCachingProvider(trackRequestMetaDao);

    @Test
    void testGetRequestMeta() {
        long deliveryServiceId = 1L;
        RequestType requestType = RequestType.getByEntityTypeAndMethodType(EntityType.ORDER, RequestMethodType.HISTORY);
        ApiVersion apiVersion = ApiVersion.FF;
        TrackRequestMeta requestMeta = createTrackRequestMeta(deliveryServiceId, requestType, apiVersion);

        when(trackRequestMetaDao.getTrackRequestMeta(deliveryServiceId, requestType, apiVersion))
            .thenReturn(Optional.of(requestMeta));

        IntStream.iterate(0, i -> i + 1)
            .limit(2)
            .forEach(i -> {
                TrackRequestMeta actualMeta = cachingProvider
                    .getRequestMeta(deliveryServiceId, EntityType.ORDER, ApiVersion.FF);
                softly.assertThat(actualMeta).isEqualTo(requestMeta);
            });

        verify(trackRequestMetaDao, times(1))
                .getTrackRequestMeta(deliveryServiceId, requestType, apiVersion);
    }

    private TrackRequestMeta createTrackRequestMeta(long serviceId, RequestType requestType, ApiVersion apiVersion) {
        return new TrackRequestMeta()
            .setServiceId(serviceId)
            .setToken("")
            .setUrl("")
            .setName("")
            .setType(requestType)
            .setVersion(apiVersion);
    }
}
