package ru.yandex.autotests.market.partner.api.campaigns.invoice;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.InvoiceRequestData;
import ru.yandex.autotests.market.partner.api.steps.InvoicePrintSteps;
import ru.yandex.qatools.allure.annotations.Stories;

@Feature("Invoice print")
@Stories("GET /campaigns/{campaignId}/invoices/{invoiceId}")
@Aqua.Test(title = "Квитанция по счету")
public class InvoicePrintTest {

    private InvoicePrintSteps tester = new InvoicePrintSteps();

    @Test
    public void testShopNormal() {
        tester.checkShopNormal(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID,
                InvoiceRequestData.SHOP_CLIENT_ID);
    }

    @Test
    public void testAgencyNormal() {
        tester.checkAgencyNormal(InvoiceRequestData.AGENCY_CAMPAIGN_ID, InvoiceRequestData.AGENCY_UID,
                InvoiceRequestData.AGENCY_CLIENT_ID);
    }

    @Test
    public void testShopCredit() {
        tester.checkShopCredit(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID,
                InvoiceRequestData.SHOP_CLIENT_ID);
    }

    @Test
    public void testShopOverdraft() {
        tester.checkShopOverdraft(InvoiceRequestData.SHOP_NO_CONTRACT_CAMPAIGN_ID,
                InvoiceRequestData.SHOP_NO_CONTRACT_UID);
    }

    @Test
    public void testShopFake() {
        tester.checkShopFake(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID,
                InvoiceRequestData.SHOP_CLIENT_ID, InvoiceRequestData.AGENCY_UID);
    }

    @Test
    public void testWithoutMandatoryParams() {
        tester.checkWithoutMandatoryParams(InvoiceRequestData.AGENCY_UID);
    }

}
