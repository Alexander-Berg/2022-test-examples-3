package ru.yandex.market.tpl.core.domain.special_request;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestPoint;
import ru.yandex.market.tpl.core.domain.routing.RoutingAddressMapper;
import ru.yandex.market.tpl.core.domain.routing.logistic_request.Routable;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCollector;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType.LOCKER_INVENTORY;

@ExtendWith(MockitoExtension.class)
public class SpecialRequestCollectorTest {

    private final BigDecimal LATITUDE = BigDecimal.ONE;
    private final BigDecimal LONGITUDE = BigDecimal.ZERO;
    private final static String HOUSE = "10";

    @InjectMocks
    private SpecialRequestCollector specialRequestCollector;
    @Mock
    private Clock clock;
    @Mock
    private RoutingAddressMapper routingAddressMapper;
    @Mock
    private DsZoneOffsetCachingService dsZoneOffsetCachingService;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    public void setup() {
        ClockUtil.initFixed(clock);
    }

    @Test
    void checkRoutableMappingValid() {
        LocalDateTime intervalFrom = LocalDateTime.now(clock);
        LocalDateTime intervalTo = LocalDateTime.now(clock).plusHours(1);

        when(dsZoneOffsetCachingService.getOffsetForDs(any())).thenReturn(ZoneOffset.UTC);

        LogisticRequestPoint pointTo = LogisticRequestPoint.builder()
                .originalLatitude(LATITUDE)
                .originalLongitude(LONGITUDE)
                .city("Moscow")
                .street("Lenina")
                .house(HOUSE)
                .build();

        var specialRequest = mock(SpecialRequest.class);
        lenient().doReturn(21L).when(specialRequest).getId();
        lenient().doReturn("external-special-1").when(specialRequest).getExternalId();
        lenient().doReturn(intervalFrom).when(specialRequest).getIntervalFrom();
        lenient().doReturn(intervalTo).when(specialRequest).getIntervalTo();
        lenient().doReturn(LOCKER_INVENTORY).when(specialRequest).getSpecialRequestType();
        lenient().doReturn(pointTo).when(specialRequest).getPointTo();

        Routable routable = specialRequestCollector.convertToRoutable(specialRequest);

        assertThat(routable).isNotNull();
        assertThat(routable.getInterval().getStart()).isEqualTo(intervalFrom.toLocalTime());
        assertThat(routable.getInterval().getEnd()).isEqualTo(intervalTo.toLocalTime());
        assertThat(routable.getEntityId()).isEqualTo(specialRequest.getId());
        assertThat(routable.getRef()).isEqualTo(specialRequest.getExternalId());
        assertThat(routable.getRoutingRequestItemType()).isEqualTo(RoutingRequestItemType.LOCKER);
        assertThat(routable.getAddress().getHouse()).isEqualTo(HOUSE);
        assertThat(routable.getAddress().getLatitude().doubleValue()).isEqualTo(1.0);
        assertThat(routable.getAddress().getLongitude().doubleValue()).isEqualTo(0.0);
    }

}
