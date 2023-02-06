package ru.yandex.market.ff.service;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.AvailableMeasurementSize;
import ru.yandex.market.ff.model.entity.MeasurementLimit;

public class MeasurementLimitServiceTest extends IntegrationTest {

    private static final long SERVICE_ID = 100L;

    @Autowired
    private MeasurementLimitService limitService;

    @Test
    @DatabaseSetup("classpath:service/measurement_limits/before.xml")
    public void shouldGetSupplyLimitOnOneShopRequest() {
        MeasurementLimit limit = limitService.getOneShopRequestLimit(SERVICE_ID, RequestType.SUPPLY);

        assertions.assertThat(limit.getMeasurementCount()).isEqualTo(100L);
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_limits/before.xml")
    public void shouldGetDefaultLimitOnOneShopRequest() {
        MeasurementLimit limit = limitService.getOneShopRequestLimit(SERVICE_ID, RequestType.SHADOW_SUPPLY);

        assertions.assertThat(limit.getMeasurementCount()).isEqualTo(1000L);
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_limits/before.xml")
    public void shouldSuccessGetAvailableQuotaLimitForSupply() {
        final LocalDate date = LocalDate.of(2019, 11, 11);

        AvailableMeasurementSize availableSize =
                 limitService.getAvailableMeasureSizeQuota(RequestType.SUPPLY, SERVICE_ID, date);

        assertions.assertThat(availableSize).isNotNull();
        assertions.assertThat(availableSize.measureFit(100)).isTrue();
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_limits/before.xml")
    public void shouldGetAvailableQuotaLimitForCrossDoc() {
        final LocalDate date = LocalDate.of(2019, 11, 11);

        AvailableMeasurementSize availableSize =
                limitService.getAvailableMeasureSizeQuota(RequestType.CROSSDOCK, SERVICE_ID, date);

        assertions.assertThat(availableSize).isNotNull();
        assertions.assertThat(availableSize.measureFit(50)).isTrue();
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_limits/before.xml")
    public void shouldNotGetAvailableQuotaLimitForCrossDoc() {
        final LocalDate date = LocalDate.of(2019, 11, 11);

        AvailableMeasurementSize availableSize =
                limitService.getAvailableMeasureSizeQuota(RequestType.CROSSDOCK, SERVICE_ID, date);

        assertions.assertThat(availableSize).isNotNull();
        assertions.assertThat(availableSize.measureFit(51)).isFalse();
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_limits/before.xml")
    public void shouldGetAvailableQuotaLimitForCrossDocNextDate() {
        final LocalDate date = LocalDate.of(2019, 11, 12);

        AvailableMeasurementSize availableSize =
                limitService.getAvailableMeasureSizeQuota(RequestType.CROSSDOCK, SERVICE_ID, date);

        assertions.assertThat(availableSize).isNotNull();
        assertions.assertThat(availableSize.measureFit(551)).isFalse();
    }
}
