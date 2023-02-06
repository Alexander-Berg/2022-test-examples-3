package ru.yandex.autotests.market.partner.api.campaigns.invoice;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.InvoiceRequestData;
import ru.yandex.autotests.market.partner.api.steps.InvoiceSteps;
import ru.yandex.qatools.allure.annotations.Stories;

@Feature("Invoice")
@Stories("POST /invoice")
@Aqua.Test(title = "Выставление счета")
public class InvoiceTest {

    private InvoiceSteps tester = new InvoiceSteps();

    @Test
    public void testAgencyNormal() {
        tester.checkAgencyNormal(InvoiceRequestData.AGENCY_CAMPAIGN_ID, InvoiceRequestData.AGENCY_UID,
                InvoiceRequestData.AGENCY_CLIENT_ID);
    }

    @Test
    public void testShopNormal() {
        tester.checkShopNormal(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testShopCredit() {
        tester.checkShopCredit(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testShopOverdraft() {
        tester.checkShopOverdraft(InvoiceRequestData.SHOP_NO_CONTRACT_CAMPAIGN_ID,
                InvoiceRequestData.SHOP_NO_CONTRACT_UID);
    }

    @Test
    public void testAgencyCredit() {
        tester.checkAgencyCredit(InvoiceRequestData.AGENCY_CAMPAIGN_ID, InvoiceRequestData.AGENCY_UID,
                InvoiceRequestData.AGENCY_CLIENT_ID);
    }

    @Test
    public void testCreditAndOverdraft() {
        tester.checkCreditAndOverdraft(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testWithoutMandatoryParams() {
        tester.checkWithoutMandatoryParams(InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testShopCreditWithoutContract() {
        tester.checkShopCreditWithoutContract(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testAgencyCreditDouble() {
        tester.checkAgencyCreditDouble(InvoiceRequestData.AGENCY_CAMPAIGN_ID, InvoiceRequestData.AGENCY_UID,
                InvoiceRequestData.AGENCY_CLIENT_ID);
    }
}
