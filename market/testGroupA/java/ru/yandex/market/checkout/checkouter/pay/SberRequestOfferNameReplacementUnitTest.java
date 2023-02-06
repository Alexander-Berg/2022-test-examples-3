package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.market.checkout.checkouter.pay.strategies.prepay.CreditPaymentStrategyImpl.removeForbiddenSymbolsFromOfferName;

/**
 * @author : poluektov
 * date: 2019-07-05.
 */
public class SberRequestOfferNameReplacementUnitTest {

    @Test
    public void testCase1() {
        String offerName = removeForbiddenSymbolsFromOfferName("убери его: ' and or '' -- #");
        assertThat(offerName, equalTo("убери его:"));
    }

    @Test
    public void testCase2() {
        String offerName = removeForbiddenSymbolsFromOfferName("insert UPDATE order by DiStInCt and ПрЕвЕд МеДвEд " +
                "replace");
        assertThat(offerName, equalTo("ПрЕвЕд МеДвEд"));
    }

    @Test
    public void testCase3() {
        String offerName = removeForbiddenSymbolsFromOfferName("I like to drop the table and analyze my vacuum " +
                "cleaner");
        assertThat(offerName, equalTo("I to the table my cleaner"));
    }

    @Test
    public void testCase4() {
        String offerName = removeForbiddenSymbolsFromOfferName("DROPTABLE");
        assertThat(offerName, equalTo("DROPTABLE"));
    }
}
