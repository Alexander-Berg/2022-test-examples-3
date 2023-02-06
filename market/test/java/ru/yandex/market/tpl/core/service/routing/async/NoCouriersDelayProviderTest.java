package ru.yandex.market.tpl.core.service.routing.async;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.async.queue.QueueRoutingRequestGroupPayload;
import ru.yandex.market.tpl.core.domain.routing.async.queue.delay.RoutingLogRecordResolver;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogRecord;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRoutingService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class NoCouriersDelayProviderTest {

    public static final long ROUTING_REQUEST_GROUP_ID = 10L;
    public static final LocalDate SHIFT_DATE = LocalDate.now();
    public static final long SORTING_CENTER_ID = 1L;
    @Mock
    private RoutingLogRecordResolver routingLogRecordResolver;
    @Mock
    private UserScheduleRoutingService userScheduleRoutingService;
    @Mock
    private PartnerRepository<SortingCenter> sortingCenterRepository;
    @InjectMocks
    private NoCouriersDelayProvider delayProvider;

    @Test
    void isApply_isTrue() {
        //given
        when(routingLogRecordResolver.tryResolveByGroupId(ROUTING_REQUEST_GROUP_ID))
                .thenReturn(buildRequestRecord());
        SortingCenter sc = new SortingCenter();
        when(sortingCenterRepository.findByIdOrThrow(SORTING_CENTER_ID)).thenReturn(sc);
        when(userScheduleRoutingService.calculateShiftEndsForRouting(SHIFT_DATE, sc))
                .thenReturn(List.of());
        //then
        assertThat(delayProvider.isApply(new QueueRoutingRequestGroupPayload("req", ROUTING_REQUEST_GROUP_ID)))
                .isTrue();
    }

    @Test
    void isApply_isFalse() {
        //given
        when(routingLogRecordResolver.tryResolveByGroupId(ROUTING_REQUEST_GROUP_ID))
                .thenReturn(buildRequestRecord());
        SortingCenter sc = new SortingCenter();
        when(sortingCenterRepository.findByIdOrThrow(SORTING_CENTER_ID)).thenReturn(sc);
        when(userScheduleRoutingService.calculateShiftEndsForRouting(SHIFT_DATE, sc))
                .thenReturn(List.of(new UserScheduleRule()));
        //then
        assertThat(delayProvider.isApply(new QueueRoutingRequestGroupPayload("req", ROUTING_REQUEST_GROUP_ID)))
                .isFalse();
    }

    @NotNull
    private RoutingLogRecord buildRequestRecord() {
        return new RoutingLogRecord(1L, "requestId", SHIFT_DATE, SORTING_CENTER_ID, RoutingMockType.REAL,
                RoutingProfileType.GROUP,
                Instant.now(), Instant.now(), RoutingResultStatus.QUEUE_ROUTING_REQUEST_GROUP, null);
    }
}
