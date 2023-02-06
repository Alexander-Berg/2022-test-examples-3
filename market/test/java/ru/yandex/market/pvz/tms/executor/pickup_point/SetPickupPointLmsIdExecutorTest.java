package ru.yandex.market.pvz.tms.executor.pickup_point;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@TransactionlessEmbeddedDbTest
@Import({SetPickupPointLmsIdExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SetPickupPointLmsIdExecutorTest {

    private final LMSClient lmsClient;
    private final PickupPointQueryService pickupPointQueryService;
    private final TestPickupPointFactory pickupPointFactory;

    private final SetPickupPointLmsIdExecutor executor;

    @BeforeEach
    void setup() {
        when(lmsClient.getLogisticsPoints(any())).thenReturn(List.of());
    }

    @Test
    void testSet() {
        long lmsId = 123;

        PickupPoint pickupPoint = createPickupPointWithNullLmsId();
        long pickupPointId = pickupPoint.getId();
        long deliveryServiceId = pickupPoint.getLegalPartner().getDeliveryService().getId();

        LogisticsPointFilter filter = getLogisticsPointFilter(pickupPointId, deliveryServiceId);

        when(lmsClient.getLogisticsPoints(eq(filter)))
                .thenReturn(List.of(LogisticsPointResponse.newBuilder().id(lmsId).build()));

        executor.doRealJob(null);

        assertThat(pickupPointQueryService.getHeavy(pickupPointId).getLmsId()).isEqualTo(lmsId);
    }

    @Test
    void testSetOnlyCreatedInLms() {
        long lmsId = 15;

        PickupPoint pickupPoint1 = createPickupPointWithNullLmsId();
        PickupPoint pickupPoint2 = createPickupPointWithNullLmsId();

        long deliveryServiceId = pickupPoint1.getLegalPartner().getDeliveryService().getId();
        LogisticsPointFilter filter = getLogisticsPointFilter(pickupPoint1.getId(), deliveryServiceId);

        when(lmsClient.getLogisticsPoints(eq(filter)))
                .thenReturn(List.of(LogisticsPointResponse.newBuilder().id(lmsId).build()));

        executor.doRealJob(null);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint1.getId()).getLmsId())
                .isEqualTo(lmsId);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint2.getId()).getLmsId())
                .isNull();
    }

    private PickupPoint createPickupPointWithNullLmsId() {
        return pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .lmsId(null)
                        .build())
                .build());
    }

    private LogisticsPointFilter getLogisticsPointFilter(long pickupPointId, long deliveryServiceId) {
        return LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(deliveryServiceId))
                .externalIds(Set.of(String.valueOf(pickupPointId)))
                .build();
    }

}
