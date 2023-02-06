package ru.yandex.travel.orders.repository.finances;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.finances.BankOrderDetail;
import ru.yandex.travel.orders.entities.finances.BankOrderPayment;
import ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType;
import ru.yandex.travel.orders.entities.finances.BillingTransactionType;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class BankOrderDetailRepositoryTest {
    @Autowired
    private BankOrderDetailRepository bankOrderDetailRepository;

    @Test
    public void testCountBalance() {
        long contractId = 1000L;
        BankOrderDetail detail = new BankOrderDetail();
        detail.setContractId(contractId);
        detail.setCurrency(ProtoCurrencyUnit.RUB);
        detail.setSum(BigDecimal.TEN);
        detail.setPaymentType(BillingTransactionPaymentType.COST);
        detail.setTransactionType(BillingTransactionType.PAYMENT);
        detail.setBankOrderPayment(BankOrderPayment.fromId("123"));
        detail.setPaymentTime(LocalDate.of(2020, 9, 30));
        bankOrderDetailRepository.saveAndFlush(detail);

        var checkDetail = bankOrderDetailRepository.findAll().get(0);
        assertThat(checkDetail.getContractId()).isEqualTo(contractId);
        assertThat(checkDetail.getId()).isEqualTo(1);
        assertThat(checkDetail.getVersion()).isEqualTo(0);
        assertThat(checkDetail.getSum()).isEqualTo(BigDecimal.TEN);

        var balance = bankOrderDetailRepository.getBalanceByContractIdAtDate(contractId, LocalDate.now());
        assertThat(balance).isEqualTo(BigDecimal.TEN.doubleValue());
    }

    @Test
    public void testCountNullBalance() {
        var balance = bankOrderDetailRepository.getBalanceByContractIdAtDate(2L, LocalDate.now());
        assertThat(balance).isNull();
    }
}
