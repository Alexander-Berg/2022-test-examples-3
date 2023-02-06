package ru.yandex.market.pvz.core.domain.pickup_point;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.pvz.core.domain.pickup_point.timezone.PickupPointTimezoneCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.timezone.PickupPointTimezoneQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.timezone.PickupPointTimezoneService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointTimezoneServiceTest {

    private static final int SARATOV_ID = 194;
    private static final String SARATOV_NAME = "Europe/Saratov";
    private static final int SARATOV_OFFSET_IN_SECONDS = 14400;
    private static final int SARATOV_OFFSET_IN_HOURS = 4;

    private static final int MOSCOW_ID = 213;
    private static final int MOSCOW_OFFSET_IN_HOURS = 3;

    private final PickupPointTimezoneService pickupPointTimezoneService;
    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointQueryService pickupPointQueryService;
    private final PickupPointTimezoneCommandService pickupPointTimezoneCommandService;
    private final PickupPointTimezoneQueryService pickupPointTimezoneQueryService;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpGeobase httpGeobase;

    @BeforeEach
    void setUp() {
        when(httpGeobase.getTimezone(eq(SARATOV_ID)))
                .thenReturn(SARATOV_NAME);
        when(httpGeobase.getTzInfo(eq(SARATOV_NAME)).getOffset())
                .thenReturn(SARATOV_OFFSET_IN_SECONDS);
    }

    @Test
    public void updateTzOffsetWithTzValueFromDB() {
        pickupPointTimezoneCommandService.create(MOSCOW_ID, MOSCOW_OFFSET_IN_HOURS);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointTimezoneService.updateTzOffset(pickupPoint.getId(), MOSCOW_ID);
        var updated = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(updated.getTimeOffset()).isEqualTo(MOSCOW_OFFSET_IN_HOURS);
    }

    @Test
    public void updateTzOffset() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointTimezoneService.updateTzOffset(pickupPoint.getId(), SARATOV_ID);

        var updated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        var timeOffset = pickupPointTimezoneQueryService.findTzOffset(SARATOV_ID);

        assertThat(timeOffset).isPresent();
        assertThat(updated.getTimeOffset()).isEqualTo(SARATOV_OFFSET_IN_HOURS);
        assertThat(timeOffset.get().getTimeOffset()).isEqualTo(SARATOV_OFFSET_IN_HOURS);

    }

}
