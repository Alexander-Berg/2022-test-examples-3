package ru.yandex.market.common.report.parser.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.market.common.report.model.json.credit.BnplDenial;
import ru.yandex.market.common.report.model.json.credit.CreditInfo;
import ru.yandex.market.common.report.model.json.credit.CreditOffer;
import ru.yandex.market.common.report.model.json.credit.CreditTerm;
import ru.yandex.market.common.report.model.json.credit.MonthlyPayment;
import ru.yandex.market.common.report.model.json.credit.YandexBnplInfo;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.report.model.json.BnplDenialReason.TOO_CHEAP;

public class CreditInfoJsonParserTest {

    @Test
    public void shouldParseCreditDenialWithoutBlackList() throws IOException {
        CreditInfoJsonParser parser = new CreditInfoJsonParser();
        CreditInfo creditInfo = parser.parse(CreditInfoJsonParserTest.class.getResourceAsStream(
                "/files/credit_info_denial_without_blacklist.json"
        ));

        assertThat(creditInfo.getCreditDenial(), notNullValue());
        assertThat(creditInfo.getCreditDenial().getReason(), is("TOO_CHEAP"));
        assertThat(creditInfo.getCreditDenial().getThreshold(), is(BigDecimal.valueOf(2000)));

        List<String> blackList = creditInfo.getCreditDenial().getBlackListOffers();
        assertThat(blackList, is(nullValue()));
    }

    @Test
    public void shouldParseCreditDenial() throws IOException {
        CreditInfoJsonParser parser = new CreditInfoJsonParser();
        CreditInfo creditInfo = parser.parse(CreditInfoJsonParserTest.class.getResourceAsStream(
                "/files/credit_info_denial.json"
        ));

        assertThat(creditInfo.getCreditDenial(), notNullValue());
        assertThat(creditInfo.getCreditDenial().getReason(), is("TOO_CHEAP"));
        assertThat(creditInfo.getCreditDenial().getThreshold(), is(BigDecimal.valueOf(2000)));

        List<String> blackList = creditInfo.getCreditDenial().getBlackListOffers();
        assertThat(blackList, is(notNullValue()));
        assertThat(blackList, hasSize(3));
    }

    @Test
    public void shouldParseActualDeliveryPlace() throws IOException {
        CreditInfoJsonParser parser = new CreditInfoJsonParser();
        CreditInfo result = parser.parse(CreditInfoJsonParserTest.class.getResourceAsStream
                ("/files/credit_info.json"));

        assertNotNull(result);
        assertThat(result.getCreditDenial(), nullValue());
        assertThat(result.getCreditInfoResult(), notNullValue());
        assertThat(result.getCreditInfoResult().getTermRange().getMin(), is(6));
        assertThat(result.getCreditInfoResult().getTermRange().getMax(), is(24));
        assertThat(result.getCreditInfoResult().getInitialPayment(), is(BigDecimal.valueOf(300)));
        assertThat(result.getCreditInfoResult().getMonthlyPayment(), is(BigDecimal.valueOf(605)));
        assertThat(result.getCreditInfoResult().getBestOptionId(), is("abc456"));

        assertThat(result.getCreditOptions(), hasSize(2));
        assertThat(result.getCreditOptions().get(0).getId(), is("abc123"));
        assertThat(result.getCreditOptions().get(0).getBank(), is("Сбербанк"));
        assertThat(result.getCreditOptions().get(0).getTerm(), is(12));
        assertThat(result.getCreditOptions().get(0).getRate(), is(BigDecimal.valueOf(16.5)));
        assertThat(result.getCreditOptions().get(0).getInitialPaymentPercent(), is(BigDecimal.valueOf(20.5)));

        assertThat(result.getCreditOptions().get(1).getId(), is("abc456"));
        assertThat(result.getCreditOptions().get(1).getBank(), is("другойбанк"));
        assertThat(result.getCreditOptions().get(1).getTerm(), is(6));
        assertThat(result.getCreditOptions().get(1).getRate(), is(BigDecimal.valueOf(14.5)));
        assertThat(result.getCreditOptions().get(1).getInitialPaymentPercent(), is(BigDecimal.valueOf(20.5)));

        assertThat(result.getGlobalRestrictions(), notNullValue());
        assertThat(result.getGlobalRestrictions().getMinPrice(), is(BigDecimal.valueOf(3500)));

        assertThat(result.getCreditOffers(), nullValue());
    }

    @Test
    public void shouldParseYandexBnplInfoPlace() throws IOException {
        CreditInfoJsonParser parser = new CreditInfoJsonParser();
        CreditInfo result = parser.parse(CreditInfoJsonParserTest.class.getResourceAsStream
                ("/files/credit_info_with_offers.json"));

        assertNotNull(result);
        assertThat(result.getCreditDenial(), nullValue());
        assertThat(result.getCreditInfoResult(), notNullValue());
        assertThat(result.getCreditInfoResult().getTermRange().getMin(), is(6));
        assertThat(result.getCreditInfoResult().getTermRange().getMax(), is(24));
        assertThat(result.getCreditInfoResult().getInitialPayment(), is(BigDecimal.valueOf(300)));
        assertThat(result.getCreditInfoResult().getMonthlyPayment(), is(BigDecimal.valueOf(605)));
        assertThat(result.getCreditInfoResult().getBestOptionId(), is("abc456"));

        assertThat(result.getCreditOptions(), hasSize(2));
        assertThat(result.getCreditOptions().get(0).getId(), is("abc123"));
        assertThat(result.getCreditOptions().get(0).getBank(), is("Сбербанк"));
        assertThat(result.getCreditOptions().get(0).getTerm(), is(12));
        assertThat(result.getCreditOptions().get(0).getRate(), is(BigDecimal.valueOf(16.5)));
        assertThat(result.getCreditOptions().get(0).getInitialPaymentPercent(), is(BigDecimal.valueOf(20.5)));

        assertThat(result.getCreditOptions().get(1).getId(), is("abc456"));
        assertThat(result.getCreditOptions().get(1).getBank(), is("другойбанк"));
        assertThat(result.getCreditOptions().get(1).getTerm(), is(6));
        assertThat(result.getCreditOptions().get(1).getRate(), is(BigDecimal.valueOf(14.5)));
        assertThat(result.getCreditOptions().get(1).getInitialPaymentPercent(), is(BigDecimal.valueOf(20.5)));

        assertThat(result.getGlobalRestrictions(), notNullValue());
        assertThat(result.getGlobalRestrictions().getMinPrice(), is(BigDecimal.valueOf(3500)));

        List<CreditOffer> creditOffers = result.getCreditOffers();
        assertThat(creditOffers, notNullValue());
        assertThat(creditOffers, equalTo(getExpectedCreditOffers()));
    }

    @Test
    public void shouldParseCreditInfoTermsPlace() throws IOException {
        CreditInfoJsonParser parser = new CreditInfoJsonParser();
        CreditInfo result = parser.parse(CreditInfoJsonParserTest.class.getResourceAsStream
                ("/files/credit_info_with_credit_terms.json"));

        assertNotNull(result);
        assertThat(result.getCreditDenial(), nullValue());
        assertThat(result.getCreditInfoResult(), notNullValue());
        assertThat(result.getCreditInfoResult().getTermRange().getMin(), is(6));
        assertThat(result.getCreditInfoResult().getTermRange().getMax(), is(24));
        assertThat(result.getCreditInfoResult().getInitialPayment(), is(BigDecimal.valueOf(300)));
        assertThat(result.getCreditInfoResult().getMonthlyPayment(), is(BigDecimal.valueOf(605)));
        assertThat(result.getCreditInfoResult().getBestOptionId(), is("abc456"));

        assertThat(result.getCreditOptions(), hasSize(2));
        assertThat(result.getCreditOptions().get(0).getId(), is("abc123"));
        assertThat(result.getCreditOptions().get(0).getBank(), is("Сбербанк"));
        assertThat(result.getCreditOptions().get(0).getTerm(), is(12));
        assertThat(result.getCreditOptions().get(0).getRate(), is(BigDecimal.valueOf(16.5)));
        assertThat(result.getCreditOptions().get(0).getInitialPaymentPercent(), is(BigDecimal.valueOf(20.5)));

        assertThat(result.getCreditOptions().get(1).getId(), is("abc456"));
        assertThat(result.getCreditOptions().get(1).getBank(), is("другойбанк"));
        assertThat(result.getCreditOptions().get(1).getTerm(), is(6));
        assertThat(result.getCreditOptions().get(1).getRate(), is(BigDecimal.valueOf(14.5)));
        assertThat(result.getCreditOptions().get(1).getInitialPaymentPercent(), is(BigDecimal.valueOf(20.5)));

        assertThat(result.getGlobalRestrictions(), notNullValue());
        assertThat(result.getGlobalRestrictions().getMinPrice(), is(BigDecimal.valueOf(3500)));

        Set<CreditTerm> creditInfoTerms = result.getCreditInfoResult().getTerms();
        assertThat(creditInfoTerms, notNullValue());
        assertThat(creditInfoTerms, equalTo(getExpectedCreditInfoTermsSet()));
    }

    @Test
    public void shouldParseCreditOptionTermsPlace() throws IOException {
        CreditInfoJsonParser parser = new CreditInfoJsonParser();
        CreditInfo result = parser.parse(CreditInfoJsonParserTest.class.getResourceAsStream
                ("/files/credit_info_with_credit_option_terms.json"));

        assertNotNull(result);
        assertThat(result.getCreditDenial(), nullValue());
        assertThat(result.getCreditInfoResult(), notNullValue());
        assertThat(result.getCreditInfoResult().getTermRange().getMin(), is(6));
        assertThat(result.getCreditInfoResult().getTermRange().getMax(), is(24));
        assertThat(result.getCreditInfoResult().getInitialPayment(), is(BigDecimal.valueOf(300)));
        assertThat(result.getCreditInfoResult().getMonthlyPayment(), is(BigDecimal.valueOf(605)));
        assertThat(result.getCreditInfoResult().getBestOptionId(), is("abc456"));

        assertThat(result.getCreditOptions(), hasSize(1));
        assertThat(result.getCreditOptions().get(0).getId(), is("abc123"));
        assertThat(result.getCreditOptions().get(0).getBank(), is("Тинькофф"));
        assertThat(result.getCreditOptions().get(0).getTerm(), is(24));
        assertThat(result.getCreditOptions().get(0).getRate(), is(BigDecimal.valueOf(49.9)));
        assertThat(result.getCreditOptions().get(0).getInitialPaymentPercent(), is(BigDecimal.valueOf(0)));
        assertThat(result.getCreditOptions().get(0).getTerms(), is(ImmutableSet.of(3, 6, 12, 24)));

        assertThat(result.getGlobalRestrictions(), notNullValue());
        assertThat(result.getGlobalRestrictions().getMinPrice(), is(BigDecimal.valueOf(3500)));
    }

    private Object getExpectedCreditInfoTermsSet() {
        CreditTerm firstCreditTerm = new CreditTerm();
        firstCreditTerm.setTerm(3);
        MonthlyPayment firstMonthlyPayment = new MonthlyPayment();
        firstMonthlyPayment.setCurrency("RUR");
        firstMonthlyPayment.setValue(new BigDecimal("3614"));
        firstCreditTerm.setMonthlyPayment(firstMonthlyPayment);

        CreditTerm secondCreditTerm = new CreditTerm();
        secondCreditTerm.setTerm(6);
        MonthlyPayment secondMonthlyPayment = new MonthlyPayment();
        secondMonthlyPayment.setCurrency("RUR");
        secondMonthlyPayment.setValue(new BigDecimal("1917"));
        secondCreditTerm.setMonthlyPayment(secondMonthlyPayment);

        CreditTerm thirdCreditTerm = new CreditTerm();
        thirdCreditTerm.setTerm(12);
        MonthlyPayment thirdMonthlyPayment = new MonthlyPayment();
        thirdMonthlyPayment.setCurrency("RUR");
        thirdMonthlyPayment.setValue(new BigDecimal("1075"));
        thirdCreditTerm.setMonthlyPayment(thirdMonthlyPayment);

        CreditTerm fourthCreditTerm = new CreditTerm();
        fourthCreditTerm.setTerm(24);
        MonthlyPayment fourthMonthlyPayment = new MonthlyPayment();
        fourthMonthlyPayment.setCurrency("RUR");
        fourthMonthlyPayment.setValue(new BigDecimal("667"));
        fourthCreditTerm.setMonthlyPayment(fourthMonthlyPayment);

        return ImmutableSet.of(firstCreditTerm, secondCreditTerm, thirdCreditTerm, fourthCreditTerm);
    }

    private List<CreditOffer> getExpectedCreditOffers() {
        List<CreditOffer> creditOffers = new ArrayList<>();
        creditOffers.add(createCreditOffer("offer-1", 1, "wareId-1", true));
        creditOffers.add(createCreditOffer("offer-2", 2, "wareId-2", false));
        CreditOffer creditOffer = createCreditOffer("offer-3", 3, "wareId-3", true);
        creditOffer.setYandexBnplInfo(null);
        creditOffers.add(creditOffer);
        return creditOffers;
    }

    private CreditOffer createCreditOffer(String entity, int hid, String wareId, boolean yandexBnplInfoEnabled) {
        CreditOffer creditOffer = new CreditOffer();
        creditOffer.setEntity(entity);
        creditOffer.setHid(hid);
        creditOffer.setWareId(wareId);
        creditOffer.setYandexBnplInfo(createYandexBnplInfo(yandexBnplInfoEnabled));
        return creditOffer;
    }

    private YandexBnplInfo createYandexBnplInfo(boolean yandexBnplInfoEnabled) {
        YandexBnplInfo yandexBnplInfo = new YandexBnplInfo();
        yandexBnplInfo.setEnabled(yandexBnplInfoEnabled);
        if (!yandexBnplInfoEnabled) {
            BnplDenial bnplDenial = new BnplDenial();
            bnplDenial.setReason(TOO_CHEAP);
            bnplDenial.setThreshold(new BigDecimal(1500));
            yandexBnplInfo.setBnplDenial(bnplDenial);
        }
        return yandexBnplInfo;
    }
}
