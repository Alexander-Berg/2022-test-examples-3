package ru.yandex.market.fintech.creditbroker.integration.db;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fintech.creditbroker.exception.CreditApplicationNotFoundException;
import ru.yandex.market.fintech.creditbroker.helper.PaymentIdGenerator;
import ru.yandex.market.fintech.creditbroker.mapper.application.CreditApplicationMapper;
import ru.yandex.market.fintech.creditbroker.model.CreditApplication;
import ru.yandex.market.fintech.creditbroker.model.CreditApplicationStatus;
import ru.yandex.market.fintech.creditbroker.model.OrderInfo;
import ru.yandex.market.fintech.creditbroker.model.OrderItem;
import ru.yandex.market.fintech.creditbroker.model.PartnerName;
import ru.yandex.market.fintech.creditbroker.model.SigningType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreditApplicationDBTest
//        extends AbstractFunctionalTest
{

    @Autowired
    private CreditApplicationMapper creditApplicationMapper;

    @Test
    @Disabled
    public void shouldInsertAndReadCreditApplication() {
        UUID id = UUID.randomUUID();
        CreditApplication creditApplication = new CreditApplication()
                .setId(id)
                .setStatus(CreditApplicationStatus.APPROVED)
                .setCreatedAt(LocalDateTime.now().minusMinutes(30).truncatedTo(ChronoUnit.MILLIS))
                .setUpdatedAt(LocalDateTime.now().minusMinutes(5).truncatedTo(ChronoUnit.MILLIS))
                .setCommitted(true)
                .setCreditDurationInMonths(3)
                .setOtpCheckPassed(true)
                .setOtpTrackId("123-123")
                .setBankLink("link123")
                .setWebhookUrl("url1234")
                .setExpiryAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .setFirstName("FN")
                .setLastName("LN")
                .setMiddleName("MN")
                .setLoanNumber("9877")
                .setPaymentId(PaymentIdGenerator.getNext())
                .setUid(6544L)
                .setOrderInfo(new OrderInfo()
                        .setOrderIds(List.of("1", "2"))
                        .setAmount(BigDecimal.TEN)
                        .setItems(List.of(new OrderItem().setName("item1").setPrice(BigDecimal.ONE))))
                .setSigningType(SigningType.BANK)
                .setPartnerNames(List.of(new PartnerName().setPartnerName("comp1").setInn("123-634")));

        creditApplicationMapper.insert(creditApplication);
        CreditApplication foundCreditApplication =
                creditApplicationMapper.findById(id).orElseThrow(() -> new CreditApplicationNotFoundException(id));

        // set null to BSonIgnored fields
        creditApplication.setFirstName(null);
        creditApplication.setMiddleName(null);
        creditApplication.setLastName(null);
        creditApplication.setLoanNumber(null);
        creditApplication.setSigningType(null);
        creditApplication.setSigningType(null);
        assertEquals(creditApplication, foundCreditApplication);
    }

    @Test
    @Disabled
    public void insertNullShouldThrowException() {
        assertThrows(NullPointerException.class, () -> creditApplicationMapper.insert(null), "creditApplication " +
                "cannot be null");
    }

    @Test
    @Disabled
    public void findByPaymentIdTest() {
        String paymentId = PaymentIdGenerator.getNext();
        CreditApplication creditApplication = new CreditApplication()
                .setPaymentId(paymentId)
                .setStatus(CreditApplicationStatus.APPROVED);

        UUID id = creditApplicationMapper.insert(creditApplication);

        CreditApplication foundCreditApplication = creditApplicationMapper.findByPaymentId(paymentId)
                .orElseThrow(() -> new CreditApplicationNotFoundException(id));
        assertEquals(creditApplication, foundCreditApplication);
    }

    @Test
    @Disabled
    public void updateTest() {
        CreditApplication creditApplication = new CreditApplication()
                .setPaymentId(PaymentIdGenerator.getNext())
                .setStatus(CreditApplicationStatus.INPROGRESS);
        UUID id = creditApplicationMapper.insert(creditApplication);

        creditApplication.setStatus(CreditApplicationStatus.APPROVED);

        CreditApplication updatedCreditApplication = creditApplicationMapper.update(creditApplication);
        assertEquals(updatedCreditApplication, creditApplication);
        assertNotSame(updatedCreditApplication, creditApplication);
    }

}
