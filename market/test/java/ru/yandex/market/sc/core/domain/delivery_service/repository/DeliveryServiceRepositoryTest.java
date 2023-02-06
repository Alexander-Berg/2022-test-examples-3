package ru.yandex.market.sc.core.domain.delivery_service.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.deliveryService;

/**
 * @author valter
 */
@EmbeddedDbTest
class DeliveryServiceRepositoryTest {

    @Autowired
    DeliveryServiceRepository deliveryServiceRepository;

    @Test
    void save() {
        var expected = deliveryService();
        assertThat(deliveryServiceRepository.save(expected)).isEqualTo(expected);
    }

}
