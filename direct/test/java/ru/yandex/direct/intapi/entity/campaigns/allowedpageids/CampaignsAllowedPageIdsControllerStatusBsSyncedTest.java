package ru.yandex.direct.intapi.entity.campaigns.allowedpageids;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
public class CampaignsAllowedPageIdsControllerStatusBsSyncedTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    CampaignAllowedPageIdsController controller;

    @Autowired
    public CampaignSteps campaignSteps;

    @Autowired
    public CampaignRepository campaignRepository;

    @Autowired
    public ShardHelper shardHelper;

    private Long campaignId;
    private LocalDateTime lastChangeNotNow;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);

        // Creating campaign
        campaignId = campaignSteps.createDefaultCampaign().getCampaignId();

        // settting initial page_ids
        HttpStatus httpStatus = controller.setCampaignAllowedPageIds(String.valueOf(campaignId), Arrays.asList("123"));
        assumeTrue("setting allowed pages request returns 200 OK",
                httpStatus.value() == 200 && httpStatus.getReasonPhrase().equals("OK"));

        // setting status bs_synced to yes and last change to something other that not now
        int shard = shardHelper.getShardByCampaignId(campaignId);
        Campaign campaignNew = campaignRepository.getCampaigns(shard, Collections.singletonList(campaignId)).get(0);
        lastChangeNotNow = campaignNew.getLastChange().minusHours(5);
        campaignRepository.setCampaignBsSynced(shard, campaignId, StatusBsSynced.YES, lastChangeNotNow);

        // Checking if initialization done
        Campaign campaignAfterUpdate = campaignRepository.getCampaigns(shard, Collections.singletonList(campaignId))
                .get(0);

        assumeThat("statusBsSynced is set to YES", campaignAfterUpdate.getStatusBsSynced(),
                Matchers.is(StatusBsSynced.YES));
        assumeThat("LastChange changed to minus 5 hours", campaignAfterUpdate.getLastChange(),
                Matchers.is(lastChangeNotNow));
    }

    @Test
    public void changingPageIdsFlushesStatusBsSynced() {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        controller.setCampaignAllowedPageIds(String.valueOf(campaignId), Arrays.asList("123", "456"));
        Campaign campaignAfterUpdate =
                campaignRepository.getCampaigns(shard, Collections.singletonList(campaignId)).get(0);
        assumeThat("LastChange preserved", campaignAfterUpdate.getLastChange(), Matchers.is(lastChangeNotNow));
        assertThat("statusBsSynced is set to NO after updating pageIds", campaignAfterUpdate.getStatusBsSynced(),
                Matchers.is(StatusBsSynced.NO));
    }
}
