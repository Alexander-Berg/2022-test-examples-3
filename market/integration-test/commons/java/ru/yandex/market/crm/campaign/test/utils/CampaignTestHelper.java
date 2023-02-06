package ru.yandex.market.crm.campaign.test.utils;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;

/**
 * @author apershukov
 */
@Component
public class CampaignTestHelper {

    private final CampaignDAO campaignDAO;

    public CampaignTestHelper(CampaignDAO campaignDAO) {
        this.campaignDAO = campaignDAO;
    }

    public Campaign prepareCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Test Campaign");
        return campaignDAO.insert(campaign);
    }
}
