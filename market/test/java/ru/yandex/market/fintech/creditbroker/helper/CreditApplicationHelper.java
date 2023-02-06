package ru.yandex.market.fintech.creditbroker.helper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.fintech.creditbroker.mapper.application.CreditApplicationMapper;
import ru.yandex.market.fintech.creditbroker.model.CreditApplication;
import ru.yandex.market.fintech.creditbroker.model.OrderInfo;

@TestComponent
public final class CreditApplicationHelper {

    @Autowired
    private CreditApplicationMapper creditApplicationMapper;
    @Autowired
    private Clock clock;

    public CreditApplication create() {
        CreditApplication creditApplication = new CreditApplication()
                .setPaymentId(PaymentIdGenerator.getNext())
                .setOrderInfo(new OrderInfo().setOrderIds(List.of(OrderIdGenerator.getNext())))
                .setOtpCheckPassed(false)
                .setExpiryAt(LocalDateTime.now(clock).plusMinutes(30L));
        creditApplicationMapper.insert(creditApplication);
        return creditApplication;
    }

}
