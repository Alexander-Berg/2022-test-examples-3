package ru.yandex.market.core.balance;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

class DefaultOverdraftServiceFunctionalTest extends FunctionalTest {

    @Autowired
    private OverdraftService overdraftService;

    /**
     * Проверяет логику обновления овердрафтного баланса клиентов, а также
     * вычисления и сохранения платежей по овердрафту.
     */
    @Test
    @DbUnitDataSet(after = "DefaultOverdraftServiceFunctionalTest.after.csv")
    void shouldUpdateOverdraftBalanceAndInsertOverdraftPayments() {
        // Клиент уходит в минус, платежа нет
        overdraftService.store(new ClientOverdraft(1, 10, 5, LocalDate.of(2013, 2, 4), 1));
        // Клиент уходит в минус, платежа нет
        overdraftService.store(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 2));
        // Клиент уходит в минус, платежа нет
        overdraftService.store(new ClientOverdraft(2, 20, 8, LocalDate.of(2013, 2, 4), 1));
        // Платеж 7 фишкоцентов для clientId 1
        overdraftService.store(new ClientOverdraft(1, 10, 2, LocalDate.of(2013, 2, 4), 3));
        // Платеж 6 фишкоцентов для clientId 2
        overdraftService.store(new ClientOverdraft(2, 10, 2, LocalDate.of(2013, 2, 4), 2));
        // Платеж 2 фишкоцентов для clientId 1
        overdraftService.store(new ClientOverdraft(1, 10, 0, LocalDate.of(2013, 2, 4), 4));
        // Платеж 2 фишкоцентов для clientId 2
        overdraftService.store(new ClientOverdraft(2, 10, 0, LocalDate.of(2013, 2, 4), 3));
        // Клиент уходит в минус, платежа нет
        overdraftService.store(new ClientOverdraft(1, 10, 5, LocalDate.of(2013, 2, 4), 5));
        // Клиент уходит в минус, платежа нет
        overdraftService.store(new ClientOverdraft(2, 10, 9, LocalDate.of(2013, 2, 4), 4));
        // Платеж не засчитается (и баланс не будет обновлен), т.к. updateId не увеличился
        overdraftService.store(new ClientOverdraft(2, 10, 3, LocalDate.of(2013, 2, 4), 4));
        // Увеличился limit, spent не изменился - это не платеж
        overdraftService.store(new ClientOverdraft(1, 30, 5, LocalDate.of(2013, 2, 4), 6));
        // Spent не изменился, это не платеж
        overdraftService.store(new ClientOverdraft(1, 30, 5, LocalDate.of(2013, 2, 4), 7));
    }
}
