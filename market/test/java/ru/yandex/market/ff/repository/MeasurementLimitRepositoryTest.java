package ru.yandex.market.ff.repository;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.MeasurementLimit;
import ru.yandex.market.ff.model.enums.MeasurementLimitType;

public class MeasurementLimitRepositoryTest extends IntegrationTest {

    private static final long SERVICE_ID = 147L;

    @Autowired
    private MeasurementLimitRepository measurementLimitRepository;

    @Test
    @DatabaseSetup("classpath:repository/measurement_limits/before.xml")
    void shouldSuccessGetLimitForRequestTypeSupplyAndOneShopRequest() {
        assertions(RequestType.SUPPLY,  MeasurementLimitType.ON_ONE_SHOP_REQUEST, 100);
    }

    @Test
    @DatabaseSetup("classpath:repository/measurement_limits/before.xml")
    void shouldSuccessGetLimitForRequestTypeCrossDocAndOneShopRequest() {
        assertions(RequestType.CROSSDOCK, MeasurementLimitType.ON_ONE_SHOP_REQUEST, 150);
    }

    @Test
    @DatabaseSetup("classpath:repository/measurement_limits/before.xml")
    void shouldSuccessGetLimitForRequestTypeSupplyAndDailyOnAllShopRequests() {
        assertions(RequestType.SUPPLY, MeasurementLimitType.DAILY_ON_ALL_SHOP_REQUESTS, 1250);
    }

    @Test
    @DatabaseSetup("classpath:repository/measurement_limits/before.xml")
    void shouldSuccessGetLimitForRequestTypeCrossDocAndDailyOnAllShopRequests() {
        assertions(RequestType.CROSSDOCK, MeasurementLimitType.DAILY_ON_ALL_SHOP_REQUESTS, 1500);
    }

    private void assertions(RequestType requestType, MeasurementLimitType limitType, long expectedLimitValue) {
        Optional<MeasurementLimit> optionalActualLimit =
                measurementLimitRepository.findOne(
                        SERVICE_ID,
                        requestType.getId(),
                        limitType.getId());

        assertions.assertThat(optionalActualLimit.isPresent()).isTrue();

        MeasurementLimit actualLimit = optionalActualLimit.get();
        assertions.assertThat(actualLimit.getMeasurementCount()).isEqualTo(expectedLimitValue);
    }
}
