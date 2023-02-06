package ru.yandex.market.billing.factoring.dao;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;
import ru.yandex.market.billing.factoring.model.ClientFactor;
import ru.yandex.market.billing.factoring.model.ContractFactor;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.payment.PaymentOrderFactoring;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.billing.factoring.matcher.ContractFactorMatcher.hasContractId;
import static ru.yandex.market.billing.factoring.matcher.ContractFactorMatcher.hasFactor;

/**
 * Тест для {@link ClientFactorDao}
 */
public class ClientFactorDaoTest extends FunctionalTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 8, 1);

    @Autowired
    public ClientFactorDao clientFactorDao;


    @DisplayName("Получение признака факторинга с учётом добавления факторинга Альфа-Банка и Сбербанка")
    @Test
    @DbUnitDataSet(before = "ClientFactorDaoTest.testGetContractFactors.csv")
    public void testGetContractFactors() {
        List<ContractFactor> contractFactors = clientFactorDao.getContractFactors(TEST_DATE);

        assertEquals(7, contractFactors.size());

        assertThat(contractFactors,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasContractId(1L),
                                hasFactor(PaymentOrderFactoring.RAIFFEISEN)
                        ),
                        allOf(
                                hasContractId(1L),
                                hasFactor(PaymentOrderFactoring.ALFA)
                        ),
                        allOf(
                                hasContractId(1L),
                                hasFactor(PaymentOrderFactoring.SBER)
                        ),
                        allOf(
                                hasContractId(2L),
                                hasFactor(PaymentOrderFactoring.MARKET)
                        ),
                        allOf(
                                hasContractId(3L),
                                hasFactor(PaymentOrderFactoring.ALFA)
                        ),
                        allOf(
                                hasContractId(3L),
                                hasFactor(PaymentOrderFactoring.SBER)
                        ),
                        allOf(
                                hasContractId(4L),
                                hasFactor(PaymentOrderFactoring.RAIFFEISEN)
                        )));
    }

    @DisplayName("Изменение фактора для конкретного контракта и частоты выплат")
    @Test
    @DbUnitDataSet(
            before = "ClientFactorDaoTest.testPersistClientFactors.before.csv",
            after = "ClientFactorDaoTest.testPersistClientFactors.after.csv")
    public void testPersistClientFactors() {
        clientFactorDao.persistClientFactors(Set.of(
                new ClientFactor(null, PayoutFrequency.DAILY, PaymentOrderFactoring.MARKET),
                new ClientFactor(null, PayoutFrequency.WEEKLY, PaymentOrderFactoring.RAIFFEISEN),
                new ClientFactor(1L, PayoutFrequency.DAILY, PaymentOrderFactoring.MARKET),
                new ClientFactor(1L, PayoutFrequency.WEEKLY, PaymentOrderFactoring.RAIFFEISEN)
        ));
    }

    @DisplayName("Изменение фактора для конкретного контракта и частоты выплат")
    @Test
    @DbUnitDataSet(
            before = "ClientFactorDaoTest.testRemoveClientFactors.before.csv",
            after = "ClientFactorDaoTest.testRemoveClientFactors.after.csv")
    public void testRemoveClientFactors() {
        clientFactorDao.removeClientFactors(Arrays.asList(null, 1L));
    }
}
