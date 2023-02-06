package ru.yandex.market.ff.repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.CoefficientMeasurement;

public class CoefficientMeasurementRepositoryTest extends IntegrationTest {

    @Autowired
    private CoefficientMeasurementRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/coefficient_measurement/before.xml")
    void shouldSuccessCoefficientForRequestTypeSupply() {
        assertions(RequestType.SUPPLY, BigDecimal.ONE.setScale(2));
    }

    @Test
    @DatabaseSetup("classpath:repository/coefficient_measurement/before.xml")
    void shouldSuccessCoefficientForRequestTypeCrossDoc() {
        assertions(RequestType.CROSSDOCK, BigDecimal.valueOf(1.5).setScale(2));
    }

    private void assertions(RequestType requestType, BigDecimal expectedValue) {
        Optional<CoefficientMeasurement> optionalCoefficient = repository.findOneByRequestType(requestType);

        assertions.assertThat(optionalCoefficient.isPresent()).isTrue();

        CoefficientMeasurement actualCoefficient = optionalCoefficient.get();
        assertions.assertThat(actualCoefficient.getCoefficient()).isEqualTo(expectedValue);
    }
}
