package ru.yandex.market.pvz.core.domain.configuration.pickup_point;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.configuration.ConfigurationProviderSource;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ConfigurationPickupPointCommandServiceTest {

    private static final String KEY = "KEY";
    private static final String VALUE = "54321";

    private final TestPickupPointFactory testPickupPointFactory;

    private final ConfigurationProviderSource configurationProviderSource;
    private final ConfigurationPickupPointCommandService configurationPickupPointCommandService;

    @Test
    void testSetBooleanTrueForPickupPoint() {
        PickupPoint pickupPoint = testPickupPointFactory.createPickupPoint();
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), KEY, String.valueOf(true));
        assertThat(configurationProviderSource.getForPickupPoint(pickupPoint.getId()).isBooleanEnabled(KEY)).isTrue();
    }

    @Test
    void testSetAndThenResetForPickupPoint() {
        PickupPoint pickupPoint = testPickupPointFactory.createPickupPoint();
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), KEY, String.valueOf(true));
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), KEY, String.valueOf(false));
        assertThat(configurationProviderSource.getForPickupPoint(pickupPoint.getId()).isBooleanEnabled(KEY)).isFalse();
    }

    @Test
    void testSetTheSameValuesForDifferentPickupPoints() {
        PickupPoint pickupPoint1 = testPickupPointFactory.createPickupPoint();
        PickupPoint pickupPoint2 = testPickupPointFactory.createPickupPoint();

        configurationPickupPointCommandService.setValue(pickupPoint1.getId(), KEY, VALUE);
        configurationPickupPointCommandService.setValue(pickupPoint2.getId(), KEY, VALUE + 1);

        assertThat(configurationProviderSource.getForPickupPoint(pickupPoint1.getId()).getValue(KEY))
                .hasValue(VALUE);
        assertThat(configurationProviderSource.getForPickupPoint(pickupPoint2.getId()).getValue(KEY))
                .hasValue(VALUE + 1);
    }

    @Test
    void testLikeSearch() {
        PickupPoint pickupPoint = testPickupPointFactory.createPickupPoint();

        configurationPickupPointCommandService.setValue(pickupPoint.getId(), "KE", VALUE);
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), "KEY", VALUE);
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), "KEY122", VALUE);
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), "KEY123", VALUE);
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), "KAY123", VALUE);

        ConfigurationProvider confProvider = configurationProviderSource.getForPickupPoint(pickupPoint.getId());
        assertThat(confProvider.getValuesLike("KE%")).containsExactlyInAnyOrderEntriesOf(Map.of(
                "KEY123", VALUE,
                "KEY122", VALUE,
                "KEY", VALUE,
                "KE", VALUE
        ));

        assertThat(confProvider.getValuesLike("KE_")).containsExactlyInAnyOrderEntriesOf(Map.of(
                "KEY", VALUE
        ));

        assertThat(confProvider.getValuesLike("%12_")).containsExactlyInAnyOrderEntriesOf(Map.of(
                "KEY123", VALUE,
                "KEY122", VALUE,
                "KAY123", VALUE
        ));
    }
}
