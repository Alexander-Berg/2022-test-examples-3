package ru.yandex.market.sc.core.domain.route.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class RouteRepositoryTest {

    @Autowired
    RouteRepository routeRepository;
    @Autowired
    TestFactory testFactory;

    @Test
    void save() {
        var expected = testFactory.route();
        assertThat(routeRepository.save(expected)).isEqualTo(expected);
    }
}
