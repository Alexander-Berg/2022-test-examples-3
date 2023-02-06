package ru.yandex.market.pvz.core.domain.pickup_point.timezone;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointTimezoneCommandServiceTest {

    private static final int MOSCOW_ID = 213;
    private static final int MOSCOW_OFFSET = 3;

    private final PickupPointTimezoneCommandService pickupPointTimezoneCommandService;
    private final PickupPointTimezoneQueryService pickupPointTimezoneQueryService;

    @Test
    public void saveTzOffset() {
        var created = new PickupPointTimezone((long) MOSCOW_ID, MOSCOW_OFFSET);
        pickupPointTimezoneCommandService.create(created.getRegionId(), created.getTimeOffset());
        var found = pickupPointTimezoneQueryService.findTzOffset(created.getRegionId());
        assertThat(found).isPresent();
        assertThat(found.get().getTimeOffset()).isEqualTo(created.getTimeOffset());
    }
}
