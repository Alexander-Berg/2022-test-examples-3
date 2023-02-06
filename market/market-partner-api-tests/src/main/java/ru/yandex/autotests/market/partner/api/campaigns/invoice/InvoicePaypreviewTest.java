package ru.yandex.autotests.market.partner.api.campaigns.invoice;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.InvoiceRequestData;
import ru.yandex.autotests.market.partner.api.steps.InvoicePaypreviewSteps;
import ru.yandex.qatools.allure.annotations.Stories;

@Feature("Invoice paypreview")
@Stories("POST /campaigns/{campaignId}/invoice/paypreview")
@Aqua.Test(title = "Предвыставление счета")
public class InvoicePaypreviewTest {

    private InvoicePaypreviewSteps tester = new InvoicePaypreviewSteps();

    @Test
    public void testEmptyAmount() {
        tester.checkEmptyAmount(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testAmountLess10() {
        tester.checkAmountLess10(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testEmptyCampaign() {
        tester.checkEmptyCampaign(InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testSubclient() {
        tester.checkSubclient(InvoiceRequestData.SUB_CAMPAIGN_ID, InvoiceRequestData.SUB_UID);
    }

    @Test
    public void testAgencyAmount10() {
        tester.checkAgencyAmount10(InvoiceRequestData.AGENCY_CAMPAIGN_ID, InvoiceRequestData.AGENCY_UID);
    }

    @Test
    public void testShopAmountHundredth() {
        tester.checkShopAmountHundredth(InvoiceRequestData.SHOP_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

    @Test
    public void testShopNotModeration() {
        tester.checkShopNotModeration(InvoiceRequestData.NOT_MODERATION_CAMPAIGN_ID,
                InvoiceRequestData.NOT_MODERATION_UID);
    }

    @Test
    public void testFakeCampaign() {
        tester.checkFakeCampaign(InvoiceRequestData.AGENCY_CAMPAIGN_ID, InvoiceRequestData.SHOP_UID);
    }

}
