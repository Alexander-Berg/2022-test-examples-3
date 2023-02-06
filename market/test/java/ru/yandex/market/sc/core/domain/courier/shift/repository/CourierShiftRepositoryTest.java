package ru.yandex.market.sc.core.domain.courier.shift.repository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class CourierShiftRepositoryTest {

    @Autowired
    CourierShiftRepository courierShiftRepository;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;

    @Test
    void save() {
        var courierShift = new CourierShift(
                testFactory.storedSortingCenter(),
                testFactory.storedCourier(),
                LocalDate.now(clock),
                LocalTime.now(clock)
        );
        assertThat(courierShiftRepository.save(courierShift))
                .isEqualToIgnoringGivenFields(courierShift, "id", "createdAt", "updatedAt");
    }

}
