package ru.yandex.market.pvz.core.domain.configuration.middle_mile;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.configuration.ConfigurationProviderSource;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParams.DEFAULT_COURIER_DELIVERY_SERVICE_ID;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ConfigurationMiddleMileCommandServiceTest {

    private static final String KEY = "KEY";
    private static final String VALUE = "54321";

    private final TestPickupPointFactory testPickupPointFactory;
    private final TestPickupPointCourierMappingFactory pickupPointCourierMappingFactory;

    private final ConfigurationProviderSource configurationProviderSource;

    private final ConfigurationMiddleMileCommandService configurationMiddleMileCommandService;

    @Test
    void setBooleanTrueForMiddleMile() {
        configurationMiddleMileCommandService.setValue(DEFAULT_COURIER_DELIVERY_SERVICE_ID, KEY, String.valueOf(true));

        assertThat(configurationProviderSource.getForMiddleMile(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .isBooleanEnabled(KEY)).isTrue();
    }

    @Test
    void setAndThenResetForMiddleMile() {
        configurationMiddleMileCommandService.setValue(DEFAULT_COURIER_DELIVERY_SERVICE_ID, KEY, String.valueOf(true));
        configurationMiddleMileCommandService.setValue(DEFAULT_COURIER_DELIVERY_SERVICE_ID, KEY, String.valueOf(false));

        assertThat(configurationProviderSource.getForMiddleMile(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .isBooleanEnabled(KEY)).isFalse();
    }

    @Test
    void setTheSameValuesForDifferentMiddleMiles() {
        configurationMiddleMileCommandService.setValue(DEFAULT_COURIER_DELIVERY_SERVICE_ID, KEY, VALUE);
        configurationMiddleMileCommandService.setValue(DEFAULT_COURIER_DELIVERY_SERVICE_ID + 1, KEY, VALUE + 1);

        assertThat(configurationProviderSource.getForMiddleMile(DEFAULT_COURIER_DELIVERY_SERVICE_ID).getValue(KEY))
                .hasValue(VALUE);
        assertThat(configurationProviderSource.getForMiddleMile(DEFAULT_COURIER_DELIVERY_SERVICE_ID + 1).getValue(KEY))
                .hasValue(VALUE + 1);
    }

    @Test
    void getValueForMiddleMileByPickupPointId() {
        PickupPoint pickupPoint = testPickupPointFactory.createPickupPoint();
        pickupPointCourierMappingFactory.create(
                TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParamsBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());
        configurationMiddleMileCommandService.setValue(DEFAULT_COURIER_DELIVERY_SERVICE_ID, KEY, String.valueOf(true));

        assertThat(configurationProviderSource.getForMiddleMileByPickupPointId(pickupPoint.getId())
                .isBooleanEnabled(KEY)).isTrue();
    }

    @Test
    void noValueForMiddleMileByPickupPointIdBecauseOfEmptyMapping() {
        PickupPoint pickupPoint = testPickupPointFactory.createPickupPoint();
        configurationMiddleMileCommandService.setValue(DEFAULT_COURIER_DELIVERY_SERVICE_ID, KEY, String.valueOf(true));
        assertThat(configurationProviderSource.getForMiddleMileByPickupPointId(pickupPoint.getId())
                .isBooleanEnabled(KEY)).isFalse();
    }
}
