package ru.yandex.market.checkout.util.report;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import ru.yandex.market.common.report.model.json.credit.InstallmentsInfo;
import ru.yandex.market.common.report.model.json.credit.MonthlyPayment;

public final class BnplFactory {

    private BnplFactory() {
    }

    @Nonnull
    public static MonthlyPayment payment(@Nonnull Number value) {
        var payment = new MonthlyPayment();
        payment.setCurrency("RUB");
        payment.setValue(BigDecimal.valueOf(value.doubleValue()));
        return payment;
    }

    @Nonnull
    public static InstallmentsInfo installments(double term, @Nonnull MonthlyPayment payment) {
        var installmentsInfo = new InstallmentsInfo();
        installmentsInfo.setTerm(term);
        installmentsInfo.setMonthlyPayment(payment);
        return installmentsInfo;
    }
}
