package ru.yandex.market.sc.core.domain.logistic_point.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.logistic_point.model.TargetLogisticPoint;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
class TargetLogisticPointRepositoryTest {

    @Autowired
    TestFactory testFactory;

    @Autowired
    TargetLogisticPointRepository targetLogisticPointRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    void save() {
        var expected = new TargetLogisticPoint(10L, "name", "address");
        assertThat(targetLogisticPointRepository.save(expected)).isEqualTo(expected);
    }

    @Test
    void saveTwiceOnConflictDoNotUpdate() {
        var lp1 = new TargetLogisticPoint(101L, "name1", "address1");
        targetLogisticPointRepository.save(lp1);
        var lp2 = new TargetLogisticPoint(lp1.getId(), "name2", "address2");
        transactionTemplate.execute(status -> {
            targetLogisticPointRepository.save(lp2);
            return null;
        });
        transactionTemplate.execute(status -> {
            var actualLp = targetLogisticPointRepository.findById(lp1.getId());
            assertThat(actualLp.isEmpty()).isFalse();
            assertThat(actualLp.get().getAddress()).isEqualTo(lp1.getAddress());
            assertThat(actualLp.get().getName()).isEqualTo(lp1.getName());
            return null;
        });
    }
}
