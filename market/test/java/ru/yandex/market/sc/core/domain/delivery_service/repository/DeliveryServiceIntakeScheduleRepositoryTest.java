package ru.yandex.market.sc.core.domain.delivery_service.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class DeliveryServiceIntakeScheduleRepositoryTest {

    @Autowired
    DeliveryServiceIntakeScheduleRepository deliveryServiceIntakeScheduleRepository;
    @Autowired
    TestFactory testFactory;

    @Test
    void save() {
        var expected = testFactory.deliveryServiceIntakeSchedule();
        assertThat(deliveryServiceIntakeScheduleRepository.save(expected)).isEqualTo(expected);
    }

}
