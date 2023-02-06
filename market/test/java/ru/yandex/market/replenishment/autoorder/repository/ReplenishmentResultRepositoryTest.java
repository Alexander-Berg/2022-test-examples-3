package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.ReplenishmentResult;
import ru.yandex.market.replenishment.autoorder.repository.postgres.ReplenishmentResultRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.ReplenishmentResultToPdbRepository;

import static org.junit.Assert.assertEquals;
@ContextConfiguration(classes = {ReplenishmentResultRepositoryTest.TransactionService.class})
public class ReplenishmentResultRepositoryTest extends FunctionalTest {

    @Service
    public static class TransactionService {

        private final ReplenishmentResultRepository repository;

        public TransactionService(ReplenishmentResultRepository repository) {
            this.repository = repository;
        }

        @Transactional
        public List<ReplenishmentResult> getReplenishmentResults() {
            return repository.findReplenishmentResultsForUpdatingInfo()
                    .sorted(Comparator.comparingLong(ReplenishmentResult::getIdLine))
                    .collect(Collectors.toList());
        }
    }

    @Autowired
    private ReplenishmentResultRepository repository;

    @Autowired
    private ReplenishmentResultToPdbRepository mybatisRepository;

    @Autowired
    private TransactionService transactionService;

    @Test
    @DbUnitDataSet(before = "ReplenishmentResultRepository.before.csv")
    public void getNotExportedReplenishmentResults() {
        List<ReplenishmentResult> replenishmentResults =
                repository.findByExportTimestampIsNull();
        assertEquals(1, replenishmentResults.size());
        assertEquals(replenishmentResults.get(0).getIdLine(), Long.valueOf(104L));
    }

    @Test
    @DbUnitDataSet(before = "ReplenishmentResultRepository.before.csv")
    public void getExportedReplenishmentResultsWithoutOrderInfo() {
        List<ReplenishmentResult> replenishmentResults = transactionService.getReplenishmentResults();

        assertEquals(2, replenishmentResults.size());
        assertEquals(replenishmentResults.get(0).getIdLine(), Long.valueOf(102L));
        assertEquals(replenishmentResults.get(1).getIdLine(), Long.valueOf(103L));
    }

    @Test
    @DbUnitDataSet(before = "ReplenishmentResultRepositoryTest.getCountByNotExported.before.csv")
    public void getCountByNotExported0() {
        final LocalDateTime dt = LocalDateTime.of(2019, 6, 11, 17, 50);
        assertEquals(0, mybatisRepository.getCountOfExportedWithoutAxInfo(dt));
    }

    @Test
    @DbUnitDataSet(before = "ReplenishmentResultRepositoryTest.getCountByNotExported.before.csv")
    public void getCountByNotExported1() {
        final LocalDateTime dt = LocalDateTime.of(2019, 6, 11, 17, 55);
        assertEquals(1, mybatisRepository.getCountOfExportedWithoutAxInfo(dt));
    }
}
