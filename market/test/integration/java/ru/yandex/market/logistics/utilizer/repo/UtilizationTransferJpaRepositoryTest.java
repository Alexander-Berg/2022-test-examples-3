package ru.yandex.market.logistics.utilizer.repo;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.entity.UtilizationTransfer;
import ru.yandex.market.logistics.utilizer.domain.enums.StockType;

public class UtilizationTransferJpaRepositoryTest extends AbstractContextualTest {

    @Autowired
    private UtilizationTransferJpaRepository utilizationTransferJpaRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/utilization-transfer/1/db-state.xml")
    @ExpectedDatabase(value = "classpath:fixtures/repo/utilization-transfer/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void save() {
        UtilizationTransfer transfer = UtilizationTransfer.builder()
                .utilizationCycleId(1L)
                .warehouseId(172L)
                .stockType(StockType.DEFECT)
                .transferId(444444L)
                .build();
        utilizationTransferJpaRepository.save(transfer);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/utilization-transfer/2/db-state.xml")
    public void findAllByUtilizationCycleId() {
        List<Long> transferIds = utilizationTransferJpaRepository.findAllByUtilizationCycleId(2).stream()
                .map(UtilizationTransfer::getId)
                .collect(Collectors.toList());
        softly.assertThat(transferIds).containsExactlyInAnyOrder(2L, 3L);
    }
}
