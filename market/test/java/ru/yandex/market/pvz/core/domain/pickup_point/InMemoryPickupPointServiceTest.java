package ru.yandex.market.pvz.core.domain.pickup_point;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class InMemoryPickupPointServiceTest {

    private final TestPickupPointFactory pickupPointFactory;

    private final InMemoryPickupPointService inMemoryPickupPointService;

    @SpyBean
    private PickupPointQueryService pickupPointQueryService;

    @BeforeEach
    void cleanUp() {
        inMemoryPickupPointService.clean();
    }

    @Test
    void getPickupPointByPvzMarketIdFromDb() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        var actual = inMemoryPickupPointService.getByPvzMarketIdOrThrow(pickupPoint.getPvzMarketId());

        verify(pickupPointQueryService, times(1)).getSimpleByPvzMarketId(pickupPoint.getPvzMarketId());
        assertThat(actual.getId()).isEqualTo(pickupPoint.getId());
    }

    @Test
    void getPickupPointByPvzMarketIdFromCache() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        inMemoryPickupPointService.loadSync();

        var actual = inMemoryPickupPointService.getByPvzMarketIdOrThrow(pickupPoint.getPvzMarketId());

        verify(pickupPointQueryService, never()).getSimpleByPvzMarketId(pickupPoint.getPvzMarketId());
        assertThat(actual.getId()).isEqualTo(pickupPoint.getId());
    }
}
