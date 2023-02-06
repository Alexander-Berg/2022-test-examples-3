package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
abstract class BaseLogisticPointAvailabilityTest extends AbstractContextualTest {
    protected static final long LOGISTIC_POINT_1_ID = 1L;
    protected static final long LOGISTIC_POINT_1_PARTNER_ID = 10L;
    protected static final String LOGISTIC_POINT_1_NAME = "Склад 1";
    protected static final String LOGISTIC_POINT_1_PARTNER_NAME = "Партнёр склада 1";
    protected static final long LOGISTIC_POINT_2_ID = 2L;
    protected static final long LOGISTIC_POINT_2_PARTNER_ID = 20L;
    private static final String LOGISTIC_POINT_2_NAME = "Склад 2";
    private static final String LOGISTIC_POINT_2_PARTNER_NAME = "Партнёр склада 2";

    private static final Map<Long, LogisticPointData> LOGISTIC_POINT_DATA = Map.of(
        LOGISTIC_POINT_1_ID, new LogisticPointData(
            LOGISTIC_POINT_1_ID,
            LOGISTIC_POINT_1_NAME,
            LOGISTIC_POINT_1_PARTNER_ID
        ),
        LOGISTIC_POINT_2_ID, new LogisticPointData(
            LOGISTIC_POINT_2_ID,
            LOGISTIC_POINT_2_NAME,
            LOGISTIC_POINT_2_PARTNER_ID
        )
    );

    private static final Map<Long, String> PARTNER_NAMES = Map.of(
        LOGISTIC_POINT_1_PARTNER_ID, LOGISTIC_POINT_1_PARTNER_NAME,
        LOGISTIC_POINT_2_PARTNER_ID, LOGISTIC_POINT_2_PARTNER_NAME
    );

    @Autowired
    protected LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        when(lmsClient.getLogisticsPoint(anyLong())).thenAnswer(invocation ->
            Optional.of(invocation.<Long>getArgument(0))
                .map(LOGISTIC_POINT_DATA::get)
                .map(logisticPointData -> LogisticsPointResponse.newBuilder()
                    .id(logisticPointData.getLogisticPointId())
                    .name(logisticPointData.getLogisticPointName())
                    .partnerId(logisticPointData.getPartnerId())
                    .build())
        );

        when(lmsClient.getPartner(anyLong())).thenAnswer(invocation ->
            Optional.of(invocation.<Long>getArgument(0))
                .map(partnerId -> PartnerResponse.newBuilder()
                    .id(partnerId)
                    .name(PARTNER_NAMES.get(partnerId))
                    .build())
        );

        when(lmsClient.getLogisticsPoints(any())).thenAnswer(invocation ->
            invocation.<LogisticsPointFilter>getArgument(0).getIds().stream()
                .map(LOGISTIC_POINT_DATA::get)
                .map(data -> LogisticsPointResponse.newBuilder()
                    .id(data.getLogisticPointId())
                    .name(data.getLogisticPointName())
                    .partnerId(data.getPartnerId())
                    .build())
                .collect(Collectors.toList())
        );

        when(lmsClient.searchPartners(any())).thenAnswer(invocation ->
            invocation.<SearchPartnerFilter>getArgument(0).getIds().stream()
                .map(partnerId -> PartnerResponse.newBuilder()
                    .id(partnerId)
                    .name(PARTNER_NAMES.get(partnerId))
                    .build())
                .collect(Collectors.toList())
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    protected void verifyGetLogisticPoint(long logisticPointId) {
        verify(lmsClient).getLogisticsPoint(logisticPointId);
    }

    protected void verifyGetLogisticPoints(Set<Long> logisticPointIds) {
        if (!logisticPointIds.isEmpty()) {
            verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(logisticPointIds).build());
        }
    }

    protected void verifyGetPartner(long partnerId) {
        verify(lmsClient).getPartner(partnerId);
    }

    protected void verifyGetPartners(Set<Long> partnerIds) {
        if (!partnerIds.isEmpty()) {
            verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(partnerIds).build());
        }
    }

    @Value
    private static class LogisticPointData {
        long logisticPointId;
        String logisticPointName;
        long partnerId;
    }
}
