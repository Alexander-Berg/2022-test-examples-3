package ru.yandex.market.billing.marketing;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.model.ProductId;

@ParametersAreNonnullByDefault
class BilledMarketingCampaignDaoTest extends FunctionalTest {

    @Autowired
    BilledMarketingCampaignDao billedMarketingCampaignDao;

    @Test
    @DbUnitDataSet(
            before = "BilledMarketingCampaignDaoTest.before.csv",
            after = "BilledMarketingCampaignDaoTest.after.csv"
    )
    void persistBilledAmounts() {
        var billedCampaigns = List.of(
                // обиллили еще одну кампанию
                BilledMarketingCampaign.builder()
                        .setId(55)
                        .setPartnerId(15)
                        .setStartDate(LocalDate.parse("2021-06-01"))
                        .setEndDate(LocalDate.parse("2021-06-27"))
                        .setProduct(ProductId.MARKETING_PROMO_YANDEX_MARKET)
                        .setAmount(7777)
                        .setNds(true)
                        .setBillingDate(LocalDate.parse("2021-06-24"))
                        .setExportedToTlog(false)
                        .build()
        );

        billedMarketingCampaignDao.persistBilledAmounts(billedCampaigns);
    }
}
