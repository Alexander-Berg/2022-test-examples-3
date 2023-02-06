package ru.yandex.market.promoboss.dao.history;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.AbstractDaoTest;

@ContextConfiguration(classes = AuditDao.class)
@DbUnitDataSet(before = "AuditTestData.csv")
class AuditDaoTest extends AbstractDaoTest {

    @Autowired
    private AuditDao auditDao;

    @Test
    void getChangesTotal() {
        Assertions.assertEquals(6, auditDao.getChangesTotal("cf_104547", 1));
    }

    @Test
    void getTransactions() {
        List<BigDecimal> expectedResult = List.of(
                BigDecimal.valueOf(123L),
                BigDecimal.valueOf(1236L),
                BigDecimal.valueOf(1235L),
                BigDecimal.valueOf(1234L),
                BigDecimal.valueOf(124L)
        );
        List<BigDecimal> actualResult = auditDao.getTransactions("cf_104547", 1, 5, 0);
        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getTransactions_emptyResult() {
        List<BigDecimal> actualResult = auditDao.getTransactions("cf_111111", 11, 5, 0);
        Assertions.assertEquals(0, actualResult.size());
    }
}
