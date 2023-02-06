package ru.yandex.market.core.currency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

@DbUnitDataSet(before = "FinanceCurrencyConverterServiceTest.before.csv")
public class FinanceCurrencyConverterServiceTest extends FunctionalTest {

    @Autowired
    FinanceCurrencyConverterService financeCurrencyConverterService;

    @Test
    void getCampaignCurrencyTest() {
        long cpcCampaign = 200001;
        long supplierCampaign = 200002;
        long dbsCampaign = 200003;
        long abstractCampaign = 200004;
        Assertions.assertEquals(Currency.UE, financeCurrencyConverterService.getCampaignCurrency(cpcCampaign));
        Assertions.assertEquals(Currency.RUR, financeCurrencyConverterService.getCampaignCurrency(supplierCampaign));
        Assertions.assertEquals(Currency.RUR, financeCurrencyConverterService.getCampaignCurrency(dbsCampaign));
        Assertions.assertEquals(Currency.UE, financeCurrencyConverterService.getCampaignCurrency(abstractCampaign));
    }
}
