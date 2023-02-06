package ru.yandex.market.api.user.order.helper;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.credit.CreditError;
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.checkouter.credit.CreditOption;
import ru.yandex.market.checkout.checkouter.credit.InvalidCreditOrderItem;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;

public class CheckouterCreditInformationGeneratorHelper {
    public static CreditInformation create() {
        CreditInformation creditInformation = new CreditInformation();

        creditInformation.setPriceForCreditAllowed(BigDecimal.TEN);
        creditInformation.setCreditMonthlyPayment(BigDecimal.ONE);

        CreditError creditError = new CreditError();
        creditError.setErrorCode("123");
        InvalidCreditOrderItem invalidCreditOrderItem = new InvalidCreditOrderItem();
        invalidCreditOrderItem.setWareMd5("369b54a144b1d549f15168298c875245");
        creditError.setInvalidItems(ImmutableList.of(invalidCreditOrderItem));
        creditInformation.setCreditErrors(ImmutableList.of(creditError));

        CreditOption firstCreditOption = new CreditOption("3", new MonthlyPayment(Currency.RUR, "100"));
        CreditOption secondCreditOption = new CreditOption("6", new MonthlyPayment(Currency.RUR, "1000"));
        creditInformation.setOptions(ImmutableList.of(firstCreditOption, secondCreditOption));

        creditInformation.setSelected(secondCreditOption);

        return creditInformation;
    }
}
