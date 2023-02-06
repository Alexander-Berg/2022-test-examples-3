package ru.yandex.market.core.balance;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.impl.JdbcOverdraftRepository;

/**
 * @author zoom
 */
@DbUnitDataSet
class JdbcOverdraftRepositoryTest extends FunctionalTest {

    @Autowired
    JdbcOverdraftRepository repository;

    @Test
    void shouldReturnNullWhenOverdraftNotFound() {
        Assertions.assertFalse(repository.get(1).isPresent());
    }

    @Test
    @DbUnitDataSet(before = {"JdbcOverdraftRepositoryTest.csv"})
    void shouldReturnExistingClientOverdraft() {
        Optional<ClientOverdraft> actual = repository.get(2);
        ClientOverdraft expectedOverdraft =
                new ClientOverdraft(2, 123, 120, LocalDate.of(2020, 2, 3), 21);
        Assertions.assertEquals(expectedOverdraft, actual.get());
    }

    @Test
    void shouldCreateNewOverdraft() {
        ClientOverdraft overdraft = new ClientOverdraft(1, 345, 20, LocalDate.of(2013, 3, 12), 222);
        repository.create(overdraft);
        Assertions.assertEquals(overdraft, repository.get(1).get());
    }

    @Test
    void shouldCreateNewOverdraftWithUndefinedPaymentDeadline() {
        ClientOverdraft overdraft = new ClientOverdraft(1, 345, 20, null, 222);
        repository.create(overdraft);
        Assertions.assertEquals(overdraft, repository.get(1).get());
    }

    @Test
    @DbUnitDataSet(before = {"JdbcOverdraftRepositoryTest.csv"})
    void shouldUpdateOverdraft() {
        ClientOverdraft overdraft = new ClientOverdraft(2, 345, 20, LocalDate.of(2013, 3, 12), 222);
        repository.update(overdraft);
        Assertions.assertEquals(overdraft, repository.get(2).get());
    }

    @Test
    @DbUnitDataSet(before = {"JdbcOverdraftRepositoryTest.csv"})
    void shouldUpdateOverdraftWithUndefinedPaymentDeadline() {
        ClientOverdraft overdraft = new ClientOverdraft(2, 345, 20, null, 222);
        repository.update(overdraft);
        Assertions.assertEquals(overdraft, repository.get(2).get());
    }

    @Test
    @DbUnitDataSet(before = {"JdbcOverdraftRepositoryTest.csv"})
    void shouldNotChangeWhenUpdateIdIsLessThanStoredOne() {
        int clientId = 2;
        ClientOverdraft overdraftOld = repository.get(clientId).get();
        ClientOverdraft overdraft = new ClientOverdraft(clientId, 345, 20, LocalDate.of(2013, 3, 12), 2);
        repository.update(overdraft);
        ClientOverdraft overdraftNew = repository.get(clientId).get();
        Assertions.assertEquals(overdraftOld, overdraftNew, "Овердрафт не должен измениться, если update_id меньше текущего");
    }
}
