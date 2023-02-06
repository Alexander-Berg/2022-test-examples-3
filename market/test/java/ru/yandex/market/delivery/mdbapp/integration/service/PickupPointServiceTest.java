package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Optional;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.delivery.mdbapp.components.storage.domain.PickupPoint;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PickupPointRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PICKUP_POINT_EXTERNAL_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.newPickupPoint;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pickupPoint;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pvzLogisticsPointResponse;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointServiceTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Captor
    public ArgumentCaptor<PickupPoint> pickupPointArgumentCaptor;

    @Mock
    PickupPointRepository pickupPointRepository;

    @InjectMocks
    PickupPointService service;

    @Test
    public void findByPvzMarketId__shouldReturnPickupPoint() {
        // given:
        doReturn(Optional.of(pickupPoint()))
            .when(pickupPointRepository)
            .findByPvzMarketId(anyString());

        // when:
        final Optional<PickupPoint> actual = service.findByPvzMarketId(PICKUP_POINT_EXTERNAL_ID);

        // then:
        verify(pickupPointRepository).findByPvzMarketId(PICKUP_POINT_EXTERNAL_ID);
        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get()).isEqualToComparingFieldByField(pickupPoint());
    }

    @Test
    public void findByPvzMarketId__shouldReturnEmpty() {
        // given:
        doReturn(Optional.empty())
            .when(pickupPointRepository)
            .findByPvzMarketId(anyString());

        // when:
        final Optional<PickupPoint> actual = service.findByPvzMarketId(PICKUP_POINT_EXTERNAL_ID);

        // then:
        verify(pickupPointRepository).findByPvzMarketId(PICKUP_POINT_EXTERNAL_ID);
        softly.assertThat(actual).isEmpty();
    }

    @Test
    public void getOrCreate__shouldReturnFromRepository_whenHasInDB() {
        // given:
        doReturn(Optional.of(pickupPoint()))
            .when(pickupPointRepository)
            .findByPvzMarketId(anyString());

        // when:
        final PickupPoint actual = service.getOrCreate(pvzLogisticsPointResponse());

        // then:
        verify(pickupPointRepository).findByPvzMarketId(PICKUP_POINT_EXTERNAL_ID);
        verifyZeroInteractions(pickupPointRepository);
        softly.assertThat(actual).isEqualToComparingFieldByField(pickupPoint());
    }

    @Test
    public void getOrCreate__shouldReturnNewCreatedPickupPoint_whenHasNoOneInDB() {
        // given:
        doReturn(Optional.empty())
            .when(pickupPointRepository)
            .findByPvzMarketId(anyString());
        doReturn(pickupPoint())
            .when(pickupPointRepository)
            .save(any(PickupPoint.class));

        // when:
        final PickupPoint actual = service.getOrCreate(pvzLogisticsPointResponse());

        // then:
        verify(pickupPointRepository).findByPvzMarketId(PICKUP_POINT_EXTERNAL_ID);
        verify(pickupPointRepository).save(pickupPointArgumentCaptor.capture());
        softly.assertThat(pickupPointArgumentCaptor.getValue()).isEqualToComparingFieldByField(newPickupPoint());
        softly.assertThat(actual).isEqualToComparingFieldByField(pickupPoint());
    }
}
