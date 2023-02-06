package ru.yandex.market.sc.core.domain.inbound;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.sc.core.domain.inbound.model.GetInboundStatusResponse;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundStatusHistoryTest {

    private final TestFactory testFactory;
    private SortingCenter sortingCenter;
    private final InboundQueryService inboundQueryService;
    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setupMockClock(clock);
    }

    @Test
    void getInboundStatusHistoryAndInboundStatus() {
        String externalId = "inboundExtId";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(externalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        var statusHistory = inboundQueryService.getInboundStatusHistory(List.of(externalId));
        var statuses = inboundQueryService.getInboundsStatuses(List.of(externalId));
        assertThat(statusHistory.keySet()).hasSize(1);
        assertThat(statusHistory.get(externalId)).hasSize(1);
        assertThat(statuses).hasSize(1);
        var lastStatusFromHistory = statusHistory.get(externalId).get(0);
        assertThat(DateTime.fromLocalDateTime(lastStatusFromHistory.getUpdatedAt()))
                .isEqualTo(DateTime.fromLocalDateTime(statuses.get(0).getUpdatedAt()));
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.HOURS));
        testFactory.acceptInbound(inbound.getId());
        statusHistory = inboundQueryService.getInboundStatusHistory(List.of(externalId));
        statuses = inboundQueryService.getInboundsStatuses(List.of(externalId));
        assertThat(statusHistory.get(externalId)).hasSize(2);
        lastStatusFromHistory = statusHistory.get(externalId)
                .stream().max(Comparator.comparing(GetInboundStatusResponse::getUpdatedAt)).get();
        assertThat(DateTime.fromLocalDateTime(lastStatusFromHistory.getUpdatedAt()))
                .isEqualTo(DateTime.fromLocalDateTime(statuses.get(0).getUpdatedAt()));
    }

}
