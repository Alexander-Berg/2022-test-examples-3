package ru.yandex.market.api.user.order;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.user.order.credit.CreditInformation;
import ru.yandex.market.api.user.order.helper.CapiCreditInformationGeneratorHelper;
import ru.yandex.market.api.user.order.helper.CheckouterCreditInformationGeneratorHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

public class CreditInformationConverterTest extends BaseTest {

    @Inject
    CreditInformationConverter creditInformationConverter;

    @Test
    public void testToCheckouter() {
        CreditInformation capiCreditInformation = CapiCreditInformationGeneratorHelper.create();

        ru.yandex.market.checkout.checkouter.credit.CreditInformation checkouterCreditInformation =
                creditInformationConverter.toCheckouter(capiCreditInformation);

        assertThat(checkouterCreditInformation.getPriceForCreditAllowed(), is(BigDecimal.TEN));
        assertThat(checkouterCreditInformation.getCreditMonthlyPayment(), is(BigDecimal.ONE));
        assertThat(checkouterCreditInformation.getCreditErrors(), hasSize(1));
        assertThat(checkouterCreditInformation.getCreditErrors(), contains(
                allOf(
                        hasProperty("errorCode", is("123")),
                        hasProperty("invalidItems", contains(
                                hasProperty("wareMd5", is("369b54a144b1d549f15168298c875245"))
                        ))
                )
        ));
        assertThat(checkouterCreditInformation.getOptions(), hasSize(2));
        assertThat(checkouterCreditInformation.getOptions(), contains(
                allOf(
                        hasProperty("term", is("3")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(ru.yandex.common.util.currency.Currency.RUR)),
                                hasProperty("value", is("100"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("6")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(ru.yandex.common.util.currency.Currency.RUR)),
                                hasProperty("value", is("1000"))
                        ))
                )
        ));
        assertThat(checkouterCreditInformation.getSelected(), allOf(
                hasProperty("term", is("6")),
                hasProperty("monthlyPayment", allOf(
                        hasProperty("currency", is(ru.yandex.common.util.currency.Currency.RUR)),
                        hasProperty("value", is("1000"))
                ))
        ));
    }

    @Test
    public void testToCheckouterWhenNullFields() {
        CreditInformation capiCreditInformation = new CreditInformation();
        capiCreditInformation.setPriceForCreditAllowed(null);
        capiCreditInformation.setCreditMonthlyPayment(null);
        capiCreditInformation.setCreditErrors(null);
        capiCreditInformation.setOptions(null);
        capiCreditInformation.setSelected(null);

        ru.yandex.market.checkout.checkouter.credit.CreditInformation checkouterCreditInformation =
                creditInformationConverter.toCheckouter(capiCreditInformation);

        assertThat(checkouterCreditInformation, allOf(
                hasProperty("priceForCreditAllowed", nullValue()),
                hasProperty("creditMonthlyPayment", nullValue()),
                hasProperty("creditErrors", nullValue()),
                hasProperty("options", nullValue()),
                hasProperty("selected", nullValue())
        ));
    }

    @Test
    public void testToCapiWhenNullFields() {
        ru.yandex.market.checkout.checkouter.credit.CreditInformation checkouterCreditInformation =
                new ru.yandex.market.checkout.checkouter.credit.CreditInformation();
        checkouterCreditInformation.setPriceForCreditAllowed(null);
        checkouterCreditInformation.setCreditMonthlyPayment(null);
        checkouterCreditInformation.setCreditErrors(null);
        checkouterCreditInformation.setOptions(null);
        checkouterCreditInformation.setSelected(null);

        CreditInformation capiCreditInformation = creditInformationConverter.toCapi(checkouterCreditInformation);

        assertThat(capiCreditInformation, allOf(
                hasProperty("priceForCreditAllowed", nullValue()),
                hasProperty("creditMonthlyPayment", nullValue()),
                hasProperty("creditErrors", nullValue()),
                hasProperty("options", nullValue()),
                hasProperty("selected", nullValue())
        ));
    }

    @Test
    public void testToCapi() {
        ru.yandex.market.checkout.checkouter.credit.CreditInformation checkouterCreditInformation =
                CheckouterCreditInformationGeneratorHelper.create();

        CreditInformation capiCreditInformation = creditInformationConverter.toCapi(checkouterCreditInformation);

        assertThat(capiCreditInformation.getPriceForCreditAllowed(), is(BigDecimal.TEN));
        assertThat(capiCreditInformation.getCreditMonthlyPayment(), is(BigDecimal.ONE));
        assertThat(capiCreditInformation.getCreditErrors(), hasSize(1));
        assertThat(capiCreditInformation.getCreditErrors(), contains(
                allOf(
                        hasProperty("errorCode", is("123")),
                        hasProperty("invalidItems", contains(
                                hasProperty("wareMd5", is("369b54a144b1d549f15168298c875245"))
                        ))
                )
        ));
        assertThat(capiCreditInformation.getOptions(), hasSize(2));
        assertThat(capiCreditInformation.getOptions(), contains(
                allOf(
                        hasProperty("term", is("3")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("100"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("6")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("1000"))
                        ))
                )
        ));
        assertThat(capiCreditInformation.getSelected(), allOf(
                hasProperty("term", is("6")),
                hasProperty("monthlyPayment", allOf(
                        hasProperty("currency", is(Currency.RUR)),
                        hasProperty("value", is("1000"))
                ))
        ));
    }
}
