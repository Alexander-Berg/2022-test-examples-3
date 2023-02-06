package ru.yandex.market.api.user.order.credit.creditoptionselector;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.user.order.credit.CreditOption;
import ru.yandex.market.api.user.order.installments.MonthlyPayment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class CreditOptionSelectorImplTest extends BaseTest {

    CreditOptionSelector creditOptionSelector;

    @Before
    public void setup() {
        creditOptionSelector = new CreditOptionSelectorImpl();
    }

    @Test
    public void testChooseDefaultSelectedCreditOption() {
        ImmutableList<CreditOption> creditOptions = ImmutableList.of(
                new CreditOption("3", new MonthlyPayment(Currency.RUR, "600")),
                new CreditOption("6", new MonthlyPayment(Currency.RUR, "3900")),
                new CreditOption("12", new MonthlyPayment(Currency.RUR, "300")),
                new CreditOption("24", new MonthlyPayment(Currency.RUR, "5000"))
        );

        CreditOption creditOption = creditOptionSelector.chooseDefaultSelectedCreditOption(creditOptions);

        assertEquals("6", creditOption.getTerm());
        assertEquals(Currency.RUR, creditOption.getMonthlyPayment().getCurrency());
        assertEquals("3900", creditOption.getMonthlyPayment().getValue());
    }

    @Test
    public void testChooseDefaultSelectedCreditOptionAllValuesLessThanBorder() {
        ImmutableList<CreditOption> creditOptions = ImmutableList.of(
                new CreditOption("3", new MonthlyPayment(Currency.RUR, "200")),
                new CreditOption("6", new MonthlyPayment(Currency.RUR, "100")),
                new CreditOption("12", new MonthlyPayment(Currency.RUR, "500")),
                new CreditOption("24", new MonthlyPayment(Currency.RUR, "300"))
        );

        CreditOption creditOption = creditOptionSelector.chooseDefaultSelectedCreditOption(creditOptions);

        assertEquals("12", creditOption.getTerm());
        assertEquals(Currency.RUR, creditOption.getMonthlyPayment().getCurrency());
        assertEquals("500", creditOption.getMonthlyPayment().getValue());
    }

    @Test
    public void testChooseDefaultSelectedCreditOptionAllValuesGreaterThanBorder() {
        ImmutableList<CreditOption> creditOptions = ImmutableList.of(
                new CreditOption("3", new MonthlyPayment(Currency.RUR, "7000")),
                new CreditOption("6", new MonthlyPayment(Currency.RUR, "5500")),
                new CreditOption("12", new MonthlyPayment(Currency.RUR, "4500")),
                new CreditOption("24", new MonthlyPayment(Currency.RUR, "6000"))
        );

        CreditOption creditOption = creditOptionSelector.chooseDefaultSelectedCreditOption(creditOptions);

        assertEquals("3", creditOption.getTerm());
        assertEquals(Currency.RUR, creditOption.getMonthlyPayment().getCurrency());
        assertEquals("7000", creditOption.getMonthlyPayment().getValue());
    }

    @Test
    public void testChooseDefaultSelectedCreditOptionEmpty() {
        ImmutableList<CreditOption> creditOptions = ImmutableList.of();

        CreditOption creditOption = creditOptionSelector.chooseDefaultSelectedCreditOption(creditOptions);

        assertNull(creditOption);
    }
}
