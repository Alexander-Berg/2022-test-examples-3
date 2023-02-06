package ru.yandex.market.billing.marketing;

import java.time.LocalDate;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ParametersAreNonnullByDefault
class PartnerMarketingFixedBillingServiceTest extends FunctionalTest {

    @Autowired
    PartnerMarketingFixedBillingService partnerMarketingFixedBillingService;

    @Test
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingFixedBillingServiceTest.common.csv",
                    "PartnerMarketingFixedBillingServiceTest.before.csv"
            },
            after = "PartnerMarketingFixedBillingServiceTest.after.csv"
    )
    void processUniqueCampaignsTest() {
        partnerMarketingFixedBillingService.process(LocalDate.parse("2021-05-16"));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingFixedBillingServiceTest.common.csv",
                    "PartnerMarketingFixedBillingServiceTest.reimported.before.csv"
            },
            after = "PartnerMarketingFixedBillingServiceTest.reimported.after.csv"
    )
    void processReimportedCampaignsTest() {
        partnerMarketingFixedBillingService.process(LocalDate.parse("2021-05-18"));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingFixedBillingServiceTest.common.csv",
                    "PartnerMarketingFixedBillingServiceTest.withIgnored.before.csv"
            },
            after = "PartnerMarketingFixedBillingServiceTest.withIgnored.after.csv"
    )
    void processCampaignsWithIgnoredTest() {
        partnerMarketingFixedBillingService.process(LocalDate.parse("2021-05-16"));
    }
}
