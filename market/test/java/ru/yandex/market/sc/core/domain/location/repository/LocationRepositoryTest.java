package ru.yandex.market.sc.core.domain.location.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.location;

/**
 * @author valter
 */
@EmbeddedDbTest
class LocationRepositoryTest {

    @Autowired
    LocationRepository locationRepository;

    @Test
    void save() {
        var expected = location();
        assertThat(locationRepository.save(expected)).isEqualTo(expected);
    }

}
