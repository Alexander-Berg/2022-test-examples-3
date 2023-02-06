package ru.yandex.travel.orders.repository.finances;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.finances.BankOrder;
import ru.yandex.travel.orders.entities.finances.BankOrderDetail;
import ru.yandex.travel.orders.entities.finances.BankOrderPayment;
import ru.yandex.travel.orders.entities.finances.BankOrderStatus;
import ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType;
import ru.yandex.travel.orders.entities.finances.BillingTransactionType;
import ru.yandex.travel.orders.entities.finances.OebsPaymentStatus;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class BankOrderRepositoryTest {
    @Autowired
    private BankOrderRepository bankOrderRepository;
    @Autowired
    private BankOrderDetailRepository bankOrderDetailRepository;

    @Test
    public void testReconciled() {
        BankOrderPayment payment = BankOrderPayment.fromId("123");

        long contractId = 1000L;
        BankOrderDetail detail = new BankOrderDetail();
        detail.setContractId(contractId);
        detail.setCurrency(ProtoCurrencyUnit.RUB);
        detail.setSum(BigDecimal.TEN);
        detail.setPaymentType(BillingTransactionPaymentType.COST);
        detail.setTransactionType(BillingTransactionType.PAYMENT);
        detail.setBankOrderPayment(payment);
        detail.setPaymentTime(LocalDate.of(2020, 9, 30));
        bankOrderDetailRepository.saveAndFlush(detail);

        BankOrder bankOrder = generateOrder(payment, "1", OebsPaymentStatus.RECONCILED);
        bankOrderRepository.saveAndFlush(bankOrder);
        BankOrder bankOrder2 = generateOrder(payment, "2", OebsPaymentStatus.TRANSMITTED);
        bankOrderRepository.saveAndFlush(bankOrder2);

        var balance = bankOrderRepository.getReconciledAmountForContractIdInPeriod(
                contractId,
                LocalDate.of(2020, 9, 1),
                LocalDate.of(2020, 10, 1)
        );
        assertThat(balance).isEqualTo(BigDecimal.TEN.doubleValue());
    }

    private BankOrder generateOrder(BankOrderPayment payment, String bankOrderId, OebsPaymentStatus oebsStatus) {
        BankOrder bankOrder = new BankOrder();
        bankOrder.setServiceId(123);
        bankOrder.setBankOrderId(bankOrderId);
        bankOrder.setEventtime(LocalDate.of(2020, 9, 30));
        bankOrder.setBankOrderPayment(payment);
        bankOrder.setStatus(BankOrderStatus.DONE);
        bankOrder.setOebsPaymentStatus(oebsStatus);
        bankOrder.setSum(BigDecimal.TEN);
        bankOrder.setTrantime(LocalDateTime.now());
        return bankOrder;
    }
}
