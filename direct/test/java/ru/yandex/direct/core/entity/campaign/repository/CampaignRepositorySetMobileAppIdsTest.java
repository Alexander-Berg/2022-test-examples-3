package ru.yandex.direct.core.entity.campaign.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.jooq.InsertValuesStep1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.CampaignsMobileContentRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_MOBILE_CONTENT;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignRepositorySetMobileAppIdsTest {
    private static final Random RANDOM = new Random();

    @Autowired
    private CampaignRepository repository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo clientInfo;
    private Integer shard;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Test
    public void oneCampaign_existingCampaignsMobileContentRecord() {
        CampaignInfo campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        List<Long> campaignIds = singletonList(campaign.getCampaignId());
        fillUpCampaignsMobileContent(campaignIds);

        Map<Long, Long> insertedCidToMobileAppId = ImmutableMap.of(campaign.getCampaignId(), RANDOM.nextLong());

        repository.setMobileAppIds(shard, insertedCidToMobileAppId);

        assertThat(getCidToMobileAppIdMap(campaignIds)).isEqualTo(insertedCidToMobileAppId);
    }

    @Test
    public void oneCampaign_missingCampaignsMobileContentRecord() {
        CampaignInfo campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        List<Long> campaignIds = singletonList(campaign.getCampaignId());
        cleanUpCampaignsMobileContent(campaignIds);

        Map<Long, Long> insertedCidToMobileAppId = ImmutableMap.of(campaign.getCampaignId(), RANDOM.nextLong());

        repository.setMobileAppIds(shard, insertedCidToMobileAppId);

        assertThat(getCidToMobileAppIdMap(campaignIds)).isEqualTo(insertedCidToMobileAppId);
    }

    @Test
    public void multipleCampaigns() {
        List<Long> campaignIdsWithCampaignContentCampaignsRecord = new ArrayList<>();
        List<Long> campaignIdsWithoutCampaignContentCampaignsRecord = new ArrayList<>();
        List<Long> allCampaignIds = new ArrayList<>();
        Map<Long, Long> insertedCidToMobileAppId = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            CampaignInfo campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
            Long campaignId = campaign.getCampaignId();
            campaignIdsWithCampaignContentCampaignsRecord.add(campaignId);
            allCampaignIds.add(campaignId);
            insertedCidToMobileAppId.put(campaignId, RANDOM.nextLong());
        }

        for (int i = 0; i < 5; i++) {
            CampaignInfo campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
            Long campaignId = campaign.getCampaignId();
            campaignIdsWithoutCampaignContentCampaignsRecord.add(campaignId);
            allCampaignIds.add(campaignId);
            insertedCidToMobileAppId.put(campaignId, RANDOM.nextLong());
        }

        fillUpCampaignsMobileContent(campaignIdsWithCampaignContentCampaignsRecord);
        cleanUpCampaignsMobileContent(campaignIdsWithoutCampaignContentCampaignsRecord);

        repository.setMobileAppIds(shard, insertedCidToMobileAppId);

        assertThat(getCidToMobileAppIdMap(allCampaignIds)).isEqualTo(insertedCidToMobileAppId);
    }

    private Map<Long, Long> getCidToMobileAppIdMap(List<Long> campaignIds) {
        return dslContextProvider.ppc(shard)
                .select(CAMPAIGNS_MOBILE_CONTENT.CID, CAMPAIGNS_MOBILE_CONTENT.MOBILE_APP_ID)
                .from(CAMPAIGNS_MOBILE_CONTENT)
                .where(CAMPAIGNS_MOBILE_CONTENT.CID.in(campaignIds))
                .fetchMap(CAMPAIGNS_MOBILE_CONTENT.CID, CAMPAIGNS_MOBILE_CONTENT.MOBILE_APP_ID);
    }

    private void cleanUpCampaignsMobileContent(List<Long> campaignIds) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CAMPAIGNS_MOBILE_CONTENT)
                .where(CAMPAIGNS_MOBILE_CONTENT.CID.in(campaignIds))
                .execute();
    }

    private void fillUpCampaignsMobileContent(List<Long> campaignIds) {
        InsertValuesStep1<CampaignsMobileContentRecord, Long> step = dslContextProvider.ppc(shard)
                .insertInto(CAMPAIGNS_MOBILE_CONTENT)
                .columns(CAMPAIGNS_MOBILE_CONTENT.CID);

        for (Long campaignId : campaignIds) {
            step = step.values(campaignId);
        }

        step.onDuplicateKeyIgnore().execute();
    }
}
