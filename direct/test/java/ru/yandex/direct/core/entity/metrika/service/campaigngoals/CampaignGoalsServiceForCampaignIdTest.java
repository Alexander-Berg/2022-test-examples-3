package ru.yandex.direct.core.entity.metrika.service.campaigngoals;

import java.util.Map;
import java.util.Set;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.metrika.container.CampaignTypeWithCounterIds;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignGoalsServiceForCampaignIdTest extends CampaignGoalsServiceTest {
    private static final long CAMPAIGN_ID = 11L;

    @Autowired
    private CampaignGoalsService campaignGoalsService;

    @Override
    Set<Long> getAvailableGoals(Long uid, ClientId clientId,
                                CampaignTypeWithCounterIds campaignTypeWithCounterIds,
                                RequestBasedMetrikaClientAdapter metrikaClientAdapter) {
        Map<Long, Set<Goal>> availableGoalsForCampaignId =
                campaignGoalsService.getAvailableGoalsForCampaignId(uid, clientId,
                        Map.of(CAMPAIGN_ID, campaignTypeWithCounterIds), metrikaClientAdapter);
        return listToSet(availableGoalsForCampaignId.get(CAMPAIGN_ID), Goal::getId);
    }
}
