package ru.yandex.market.sc.core.domain.cell.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class CellRepositoryTest {

    @Autowired
    CellRepository cellRepository;
    @Autowired
    TestFactory testFactory;

    @Test
    void save() {
        var expected = testFactory.cell();
        assertThat(cellRepository.save(expected)).isEqualTo(expected);
    }

}
