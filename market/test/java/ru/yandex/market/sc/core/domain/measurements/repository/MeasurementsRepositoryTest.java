package ru.yandex.market.sc.core.domain.measurements.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.measurements;

/**
 * @author valter
 */
@EmbeddedDbTest
class MeasurementsRepositoryTest {

    @Autowired
    MeasurementsRepository measurementsRepository;

    @Test
    void save() {
        var expected = measurements();
        assertThat(measurementsRepository.save(expected)).isEqualTo(expected);
    }

}
