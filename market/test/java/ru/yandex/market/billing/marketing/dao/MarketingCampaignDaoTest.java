package ru.yandex.market.billing.marketing.dao;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.marketing.model.MarketingCampaign;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.marketing.MarketingCampaignType;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class MarketingCampaignDaoTest extends FunctionalTest {

    @Autowired
    MarketingCampaignDao marketingCampaignDao;

    @Test
    @DisplayName("Получение кампаний, импортированных в заданную дату")
    @DbUnitDataSet(before = "MarketingCampaignDaoTest.before.csv")
    void fetchCampaignsEndedAt() {
        var expectedCampaigns = List.of(
                MarketingCampaign.builder()
                        .setId(44)
                        .setType(MarketingCampaignType.NOTIFICATIONS_EMAIL_INDIVIDUAL)
                        .setPartnerId(13)
                        .setStartDate(LocalDate.parse("2021-06-01"))
                        .setEndDate(LocalDate.parse("2021-06-17"))
                        .setSum(300500)
                        .setAnaplanId(0)
                        .setCurrency("RUB")
                        .setNds(true)
                        .setImportDate(LocalDate.parse("2021-06-16"))
                        .build(),
                MarketingCampaign.builder()
                        .setId(45)
                        .setType(MarketingCampaignType.PROMO_BANNERS)
                        .setPartnerId(15)
                        .setStartDate(LocalDate.parse("2021-06-05"))
                        .setEndDate(LocalDate.parse("2021-06-18"))
                        .setSum(400500)
                        .setAnaplanId(0)
                        .setCurrency("RUB")
                        .setNds(true)
                        .setImportDate(LocalDate.parse("2021-06-16"))
                        .build()
        );

        assertThat(marketingCampaignDao.getCampaignsImportedAt(LocalDate.parse("2021-06-16")))
                .containsExactlyInAnyOrderElementsOf(expectedCampaigns);
    }
}
