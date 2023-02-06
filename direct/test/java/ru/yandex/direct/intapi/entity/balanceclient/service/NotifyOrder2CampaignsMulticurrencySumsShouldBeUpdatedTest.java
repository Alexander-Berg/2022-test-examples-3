package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignMulticurrencySums;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.campaignsMulticurrencySumsShouldBeUpdated;

/**
 * Тесты на метод NotifyOrderService.campaignsMulticurrencySumsShouldBeUpdated
 *
 * @see NotifyOrderService
 */
public class NotifyOrder2CampaignsMulticurrencySumsShouldBeUpdatedTest {

    private CampaignDataForNotifyOrder dbCampaignData;
    private CampaignMulticurrencySums campaignMulticurrencySums;

    @Before
    public void before() {
        dbCampaignData = new CampaignDataForNotifyOrder()
                .withCmsSum(RandomNumberUtils.nextPositiveBigDecimal())
                .withCmsChipsCost(RandomNumberUtils.nextPositiveBigDecimal())
                .withCmsChipsSpent(RandomNumberUtils.nextPositiveBigDecimal());

        campaignMulticurrencySums = new CampaignMulticurrencySums()
                .withSum(dbCampaignData.getCmsSum())
                .withChipsCost(dbCampaignData.getCmsChipsCost())
                .withChipsSpent(dbCampaignData.getCmsChipsSpent());
    }


    @Test
    public void isNotCampaignsMulticurrencySumsShouldBeUpdated_whenAllParamsAreEqual() {
        boolean result = campaignsMulticurrencySumsShouldBeUpdated(dbCampaignData, campaignMulticurrencySums);

        assertThat(result).isFalse();
    }

    @Test
    public void isCampaignsMulticurrencySumsShouldBeUpdated_whenSumNotEqual() {
        BigDecimal newSum = dbCampaignData.getCmsSum().add(RandomNumberUtils.nextPositiveBigDecimal());
        boolean result =
                campaignsMulticurrencySumsShouldBeUpdated(dbCampaignData, campaignMulticurrencySums.withSum(newSum));

        assertThat(result).isTrue();
    }

    @Test
    public void isCampaignsMulticurrencySumsShouldBeUpdated_whenChipsCostNotEqual() {
        BigDecimal newChipsCost = dbCampaignData.getCmsChipsCost().add(RandomNumberUtils.nextPositiveBigDecimal());
        boolean result = campaignsMulticurrencySumsShouldBeUpdated(dbCampaignData,
                campaignMulticurrencySums.withChipsCost(newChipsCost));

        assertThat(result).isTrue();
    }

    @Test
    public void isCampaignsMulticurrencySumsShouldBeUpdated_whenChipsSpentNotEqual() {
        BigDecimal newChipsSpent = dbCampaignData.getCmsChipsSpent().add(RandomNumberUtils.nextPositiveBigDecimal());
        boolean result = campaignsMulticurrencySumsShouldBeUpdated(dbCampaignData,
                campaignMulticurrencySums.withChipsSpent(newChipsSpent));

        assertThat(result).isTrue();
    }
}
