package ru.yandex.market.core.balance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.impl.JdbcOverdraftPaymentsRepository;

/**
 * @author a-kolchanov
 */
class JdbcOverdraftPaymentsRepositoryTest extends FunctionalTest {

    @Autowired
    private JdbcOverdraftPaymentsRepository repository;

    /**
     * Проверяет, что данные о платежах корректно вставляются в таблицу.
     */
    @Test
    @DbUnitDataSet(after = {"JdbcOverdraftPaymentsRepositoryTest.after.csv"})
    void shouldInsertPaymentsInTheTable() {
        repository.create(1, 100);
        repository.create(1, 60);
        repository.create(2, 5);
        repository.create(1, 200);
    }

}
