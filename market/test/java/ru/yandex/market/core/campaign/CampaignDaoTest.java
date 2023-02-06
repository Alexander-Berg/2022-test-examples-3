package ru.yandex.market.core.campaign;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.partner.model.CompletePartnerInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем функциональность {@link CampaignDao}.
 */
public class CampaignDaoTest extends FunctionalTest {
    @Autowired
    private CampaignDao campaignDao;

    @Test
    @DbUnitDataSet(before = "../business/BusinessDaoTest.before.csv")
    void checkGetPartnersByBusinessCampaigns() {
        List<Long> allBusinessCampaignIdSet = List.of(1L, 2L, 3L, 4L, 10L);
        assertThat(Set.of(1L, 2L, 4L)).isEqualTo(
                campaignDao.getPartnersByBusinessCampaigns(allBusinessCampaignIdSet, CampaignType.SHOP).keySet());
        assertThat(Set.of(6L)).isEqualTo(
                campaignDao.getPartnersByBusinessCampaigns(allBusinessCampaignIdSet, CampaignType.SUPPLIER).keySet());
        assertThat(Set.of(1L, 2L, 4L, 6L)).isEqualTo(
                campaignDao.getPartnersByBusinessCampaigns(allBusinessCampaignIdSet, null).keySet());

        assertThat(campaignDao.getPartnersByBusinessCampaigns(List.of(1L), CampaignType.SUPPLIER)).isEmpty();
        assertThat(campaignDao.getPartnersByBusinessCampaigns(List.of(), null)).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "../business/BusinessDaoTest.before.csv")
    void checkGetCompletePartnersInfo() {
        List<CompletePartnerInfo> completePartnersInfo = campaignDao.getCompletePartnersInfo(List.of(1L, 2L, 14L));
        assertThat(completePartnersInfo).hasSize(3);
    }

}
