package ru.yandex.market.logistics.utilizer.repo;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.entity.SystemProperty;

class SystemPropertyJpaRepositoryTest extends AbstractContextualTest {
    @Autowired
    SystemPropertyJpaRepository systemPropertyJpaRepository;

    @Test
    void persistAndLoad() {
        SystemProperty newProperty = SystemProperty.builder()
                .name("name")
                .value("value")
                .build();

        systemPropertyJpaRepository.save(newProperty);
        Optional<SystemProperty> result = systemPropertyJpaRepository.findByName("name");

        softly.assertThat(result.isPresent());
    }
}
