package ru.yandex.travel.orders.repository;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.Account;
import ru.yandex.travel.orders.entities.AccountTransaction;
import ru.yandex.travel.orders.entities.FxContext;
import ru.yandex.travel.orders.entities.FxRate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class AccountRepositoryTests {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Test
    public void testTransactionCreation() {
        Account accountFrom = Account.createAccount(ProtoCurrencyUnit.RUB);
        Account accountTo = Account.createAccount(ProtoCurrencyUnit.RUB);
        accountRepository.save(accountFrom);
        accountRepository.save(accountTo);
        AccountTransaction transaction = AccountTransaction.builder(new FxContext(ProtoCurrencyUnit.RUB))
                .addTransfer(accountFrom, accountTo, BigDecimal.valueOf(2L))
                .build();
        accountTransactionRepository.saveAndFlush(transaction);

        AccountTransaction readTransaction = accountTransactionRepository.getOne(transaction.getId());

        assertThat(readTransaction).isNotNull();
        assertThat(readTransaction.getAccountRecords().size()).isEqualTo(2);
    }

    @Test
    public void testAccountBalanceFormula() {
        Account accountFrom1 = Account.createAccount(ProtoCurrencyUnit.RUB);
        Account accountFrom2 = Account.createAccount(ProtoCurrencyUnit.RUB);
        Account accountTo = Account.createAccount(ProtoCurrencyUnit.RUB);
        accountRepository.save(accountFrom1);
        accountRepository.save(accountFrom2);
        accountRepository.save(accountTo);
        AccountTransaction transaction1 = AccountTransaction.builder(new FxContext(ProtoCurrencyUnit.RUB))
                .addTransfer(accountFrom1, accountTo, BigDecimal.valueOf(2L))
                .build();
        AccountTransaction transaction2 = AccountTransaction.builder(new FxContext(ProtoCurrencyUnit.RUB))
                .addTransfer(accountFrom2, accountTo, BigDecimal.valueOf(3L))
                .build();
        accountTransactionRepository.save(transaction1);
        accountTransactionRepository.save(transaction2);

        assertThat(accountRepository.getCurrentBalanceForAccount(accountFrom1.getId())).isEqualTo(BigDecimal.valueOf(-2L));
        assertThat(accountRepository.getCurrentBalanceForAccount(accountFrom2.getId())).isEqualTo(BigDecimal.valueOf(-3L));
        assertThat(accountRepository.getCurrentBalanceForAccount(accountTo.getId())).isEqualTo(BigDecimal.valueOf(5L));
    }

    @Test
    public void testAccountBalanceWithDifferentCurrencies() {
        Account accountFrom1 = Account.createAccount(ProtoCurrencyUnit.RUB);
        Account accountFrom2 = Account.createAccount(ProtoCurrencyUnit.RUB);
        Account accountTo = Account.createAccount(ProtoCurrencyUnit.USD);
        accountRepository.save(accountFrom1);
        accountRepository.save(accountFrom2);
        accountRepository.save(accountTo);
        FxRate fxRate = new FxRate();
        fxRate.putIfAbsent(ECurrency.C_USD, BigDecimal.valueOf(2L, 2));
        AccountTransaction transaction = AccountTransaction.builder(new FxContext(ProtoCurrencyUnit.RUB, fxRate))
                .addTransfer(accountFrom1, accountTo, BigDecimal.valueOf(10000L))
                .addTransfer(accountFrom2, accountTo, BigDecimal.valueOf(5000L))
                .build();
        accountTransactionRepository.save(transaction);

        assertThat(accountRepository.getCurrentBalanceForAccount(accountFrom1.getId())).isEqualByComparingTo(BigDecimal.valueOf(-10000L));
        assertThat(accountRepository.getCurrentBalanceForAccount(accountFrom2.getId())).isEqualByComparingTo(BigDecimal.valueOf(-5000L));
        assertThat(accountRepository.getCurrentBalanceForAccount(accountTo.getId())).isEqualByComparingTo(BigDecimal.valueOf(300L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentExceptionThrownNoExchangeRate() {
        Account accountFrom = Account.createAccount(ProtoCurrencyUnit.RUB);
        Account accountTo = Account.createAccount(ProtoCurrencyUnit.USD);
        accountRepository.save(accountFrom);
        accountRepository.save(accountTo);
        AccountTransaction transaction = AccountTransaction.builder(new FxContext(ProtoCurrencyUnit.RUB))
                .addTransfer(accountFrom, accountTo, BigDecimal.valueOf(10L))
                .build();
        accountTransactionRepository.save(transaction);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentExceptionThrownWrongFromCurrency() {
        Account accountFrom = Account.createAccount(ProtoCurrencyUnit.USD);
        Account accountTo = Account.createAccount(ProtoCurrencyUnit.RUB);
        accountRepository.save(accountFrom);
        accountRepository.save(accountTo);
        AccountTransaction transaction = AccountTransaction.builder(new FxContext(ProtoCurrencyUnit.RUB))
                .addTransfer(accountFrom, accountTo, BigDecimal.valueOf(10L))
                .build();
        accountTransactionRepository.save(transaction);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failOnTransactionCreation() {
        AccountTransaction tr = AccountTransaction.builder(new FxContext(ProtoCurrencyUnit.RUB)).build();
    }

}
