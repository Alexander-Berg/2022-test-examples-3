package ru.yandex.market.billing.export.balance;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.billing.export.balance.CampaignStatsMatcher.hasCampaignId;
import static ru.yandex.market.billing.export.balance.CampaignStatsMatcher.hasSum;
import static ru.yandex.market.billing.export.balance.CampaignStatsMatcher.hasTestShop;

@DbUnitDataSet(before = "CampaignStatsDaoTest.before.csv")
public class CampaignStatsDaoTest extends FunctionalTest {

    @Autowired
    private CampaignStatsDao campaignStatsDao;

    @DisplayName("Тест на получение информации по платежам кампаний")
    @Test
    void testGetPayments() {
        var payments = campaignStatsDao.getPayments(LocalDate.parse("2019-03-27"));
        assertThat(payments, hasSize(2));
        var item1 = payments.get(0).getCampaignId() == 101001 ? payments.get(0) : payments.get(1);
        var item2 = payments.get(0).getCampaignId() == 101001 ? payments.get(1) : payments.get(0);
        assertThat(item1, allOf(
                hasCampaignId(101001L),
                hasSum(200L),
                hasTestShop(false)
        ));
        assertThat(item2, allOf(
                hasCampaignId(101002L),
                hasSum(300L),
                hasTestShop(true)
        ));
    }

    @DisplayName("Тест на получение информации по тратам кампаний")
    @Test
    void testGetSpendings() {
        var payments = campaignStatsDao.getSpendings(LocalDate.parse("2019-03-27"));
        assertThat(payments, hasSize(2));
        var item1 = payments.get(0).getCampaignId() == 101001 ? payments.get(0) : payments.get(1);
        var item2 = payments.get(0).getCampaignId() == 101001 ? payments.get(1) : payments.get(0);
        assertThat(item1, allOf(
                hasCampaignId(101001L),
                hasSum(600L),
                hasTestShop(false)
        ));
        assertThat(item2, allOf(
                hasCampaignId(101002L),
                hasSum(900L),
                hasTestShop(true)
        ));
    }
}
