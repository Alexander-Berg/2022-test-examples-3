package ru.yandex.direct.core.entity.campaign.repository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheckDefaultImpl;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignForAccessCheckRepositoryAdapter;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllClientCampaignsDefaultAccessCheckRepositoryAdapter;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowableCampaignsDefaultAccessCheckRepositoryAdapter;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignSourceUtils.API_DEFAULT_CAMPAIGN_SOURCES;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignForAccessCheckDefaultRepositoryTest extends
        CampaignForAccessCheckRepositoryTestBase<CampaignForAccessCheckDefaultImpl> {
    @Override
    protected CampaignForAccessCheckRepositoryAdapter<CampaignForAccessCheckDefaultImpl>
    getAllClientCampaignsForAccessCheckRepositoryAdapter(ClientId clientId) {
        return new AllClientCampaignsDefaultAccessCheckRepositoryAdapter(clientId);
    }

    @Override
    protected CampaignForAccessCheckRepositoryAdapter<CampaignForAccessCheckDefaultImpl>
    getAllowableCampaignsForAccessCheckRepositoryAdapter(ClientId clientId, Set<CampaignType> campaignTypes) {
        return new AllowableCampaignsDefaultAccessCheckRepositoryAdapter(clientId, campaignTypes, API_DEFAULT_CAMPAIGN_SOURCES);
    }

    @Test
    public void invalidCampaignType_CampaignNotReturned() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long entityId = entitySupplier.apply(steps, campaignInfo);
        Set<CampaignType> allButTgo = new HashSet<>(CampaignTypeKinds.BASE);
        allButTgo.remove(CampaignType.TEXT);

        Map<Long, CampaignForAccessCheckDefaultImpl> campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getAllowableCampaignsForAccessCheckRepositoryAdapter(campaignInfo.getClientId(), allButTgo),
                        asList(entityId));

        assertThat(campaignsByIdMap.keySet().isEmpty(), is(true));
    }
}
