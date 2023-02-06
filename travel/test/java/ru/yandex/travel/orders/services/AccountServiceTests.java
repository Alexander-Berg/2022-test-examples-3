package ru.yandex.travel.orders.services;

import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.Matchers;
import org.javamoney.moneta.Money;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.Account;
import ru.yandex.travel.orders.entities.WellKnownAccount;
import ru.yandex.travel.orders.repository.AccountRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class AccountServiceTests {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    @Transactional
    public void testWellKnownAccountCreation() {
        accountService.createWellKnownAccs();
        List<Account> accountList = accountRepository.findAll();

        Assert.assertThat(accountList.size(), Matchers.equalTo(WellKnownAccount.values().length));
    }

    @Test
    @Transactional
    public void testTransferMoney() {
        accountService.createWellKnownAccs();
        accountService.transferMoney(WellKnownAccount.YANDEX.getUuid(), WellKnownAccount.TRUST.getUuid(),
                BigDecimal.valueOf(5L), WellKnownAccount.YANDEX.getCurrency());

        assertThat(accountRepository.getCurrentBalanceForAccount(WellKnownAccount.YANDEX.getUuid())).isEqualTo(BigDecimal.valueOf(-5L));
        assertThat(accountRepository.getCurrentBalanceForAccount(WellKnownAccount.TRUST.getUuid())).isEqualTo(BigDecimal.valueOf(5L));
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    public void testIllegalArgumentExceptionThrownOnWrongCurrency() {
        accountService.createWellKnownAccs();
        Account accountTo = accountService.createAccount(ProtoCurrencyUnit.USD);

        accountService.transferMoney(WellKnownAccount.YANDEX.getUuid(), accountTo.getId(), BigDecimal.valueOf(5L),
                ProtoCurrencyUnit.USD);
    }

    @Test
    @Transactional
    public void testBalanceOfEmptyAccount() {
        Account account = accountService.createAccount(ProtoCurrencyUnit.USD);
        Money balance = accountService.getAccountBalance(account.getId());
        assertThat(balance.getNumber().longValueExact()).isZero();
        assertThat(balance.getCurrency().getCurrencyCode()).isEqualTo("USD");

    }
}
