package ru.yandex.direct.web.entity.adgroup.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Arrays.asList;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupGeoTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_GlobalClientAndAdGroupWithRussiaMinusCrimeaGeoToRussiaGeo_NoErrors() {
        clientInfo =
                steps.clientSteps().createClient(TestClients.defaultClient().withCountryRegionId(GLOBAL_REGION_ID));
        setAuthData();

        campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroup =
                steps.adGroupSteps().createAdGroup(TestGroups.activeTextAdGroup(campaignInfo.getCampaignId())
                        .withGeo(asList(RUSSIA_REGION_ID, -CRIMEA_REGION_ID)), campaignInfo);

        WebTextAdGroup webTextAdGroup = new WebTextAdGroup()
                .withId(adGroup.getAdGroupId())
                .withGeo(RUSSIA_REGION_ID + "")
                .withName(adGroup.getAdGroup().getName())
                .withCampaignId(campaignInfo.getCampaignId());

        updateAndCheckResult(webTextAdGroup);
    }
}
