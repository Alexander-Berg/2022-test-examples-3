package ru.yandex.direct.grid.processing.service.client;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientTopRegionInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientTopRegions;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.utils.ListUtils;

import static java.util.List.of;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientDataServiceTopRegionsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ClientDataService clientDataService;

    private ClientInfo clientInfo;
    private GdClientInfo gdClientInfo;
    private GdClientTopRegions request;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();

        gdClientInfo = new GdClientInfo()
                .withId(clientInfo.getClientId().asLong())
                .withCountryRegionId(clientInfo.getClient().getCountryRegionId());

        var gdLimitOffset = new GdLimitOffset()
                .withOffset(0)
                .withLimit(Integer.MAX_VALUE);
        request = new GdClientTopRegions()
                .withLimitOffset(gdLimitOffset);
    }

    @Test
    public void getTopRegions_OneGroupWithGeo() {
        List<Long> adGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);

        Campaign campaign = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        AdGroup firstAdGroup = defaultTextAdGroup(campaignId).withGeo(adGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, campaignInfo);

        List<GdClientTopRegionInfo> regionsTop = clientDataService.getAdGroupTopRegions(gdClientInfo, request);

        List<GdClientTopRegionInfo> expected = of(new GdClientTopRegionInfo()
                .withRegionIds(ListUtils.longToIntegerList(adGroupGeo))
                .withCount(1));
        Assert.assertThat(regionsTop, beanDiffer(expected));
    }

    @Test
    public void getTopRegions_OneGroupWithoutGeo() {
        Campaign campaign = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        AdGroup firstAdGroup = defaultTextAdGroup(campaignId).withGeo(null);
        steps.adGroupSteps().createAdGroup(firstAdGroup, campaignInfo);

        List<GdClientTopRegionInfo> regionsTop = clientDataService.getAdGroupTopRegions(gdClientInfo, request);

        List<GdClientTopRegionInfo> expected = List.of();
        Assert.assertThat(regionsTop, beanDiffer(expected));
    }

    @Test
    public void getTopRegions_TwoGroupWithSameGeo_InOneCampaign() {
        List<Long> adGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);

        Campaign campaign = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        AdGroup firstAdGroup = defaultTextAdGroup(campaignId).withGeo(adGroupGeo);
        AdGroup secondAdGroup = defaultTextAdGroup(campaignId).withGeo(adGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, campaignInfo);
        steps.adGroupSteps().createAdGroup(secondAdGroup, campaignInfo);

        List<GdClientTopRegionInfo> regionsTop = clientDataService.getAdGroupTopRegions(gdClientInfo, request);

        List<GdClientTopRegionInfo> expected = of(new GdClientTopRegionInfo()
                .withRegionIds(ListUtils.longToIntegerList(adGroupGeo))
                .withCount(2));
        Assert.assertThat(regionsTop, beanDiffer(expected));
    }

    @Test
    public void getTopRegions_TwoGroupWithSameGeo_InDifferentCampaigns() {
        List<Long> adGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);

        Campaign firstCampaign = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        Campaign secondCampaign = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        CampaignInfo firstCampaignInfo = steps.campaignSteps().createCampaign(firstCampaign, clientInfo);
        CampaignInfo secondCampaignInfo = steps.campaignSteps().createCampaign(secondCampaign, clientInfo);
        Long firstCampaignId = firstCampaignInfo.getCampaignId();
        Long secondCampaignId = secondCampaignInfo.getCampaignId();

        AdGroup firstAdGroup = defaultTextAdGroup(firstCampaignId).withGeo(adGroupGeo);
        AdGroup secondAdGroup = defaultTextAdGroup(secondCampaignId).withGeo(adGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, firstCampaignInfo);
        steps.adGroupSteps().createAdGroup(secondAdGroup, secondCampaignInfo);

        List<GdClientTopRegionInfo> regionsTop = clientDataService.getAdGroupTopRegions(gdClientInfo, request);

        List<GdClientTopRegionInfo> expected = of(new GdClientTopRegionInfo()
                .withRegionIds(ListUtils.longToIntegerList(adGroupGeo))
                .withCount(2));
        Assert.assertThat(regionsTop, beanDiffer(expected));
    }

    @Test
    public void getTopRegions_ThreeGroupsWithDifferentGeo_InDifferentCampaigns() {
        List<Long> adGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);
        List<Long> adGroupGeoAnother = List.of(Region.MOSCOW_REGION_ID);

        Campaign firstCampaign = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        Campaign secondCampaign = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        CampaignInfo firstCampaignInfo = steps.campaignSteps().createCampaign(firstCampaign, clientInfo);
        CampaignInfo secondCampaignInfo = steps.campaignSteps().createCampaign(secondCampaign, clientInfo);
        Long firstCampaignId = firstCampaignInfo.getCampaignId();
        Long secondCampaignId = secondCampaignInfo.getCampaignId();

        AdGroup firstAdGroup = defaultTextAdGroup(firstCampaignId).withGeo(adGroupGeo);
        AdGroup secondAdGroup = defaultTextAdGroup(secondCampaignId).withGeo(adGroupGeoAnother);
        AdGroup thirdAdGroup = defaultTextAdGroup(secondCampaignId).withGeo(adGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, firstCampaignInfo);
        steps.adGroupSteps().createAdGroup(secondAdGroup, firstCampaignInfo);
        steps.adGroupSteps().createAdGroup(thirdAdGroup, secondCampaignInfo);

        List<GdClientTopRegionInfo> regionsTop = clientDataService.getAdGroupTopRegions(gdClientInfo, request);

        List<GdClientTopRegionInfo> expected = of(
                new GdClientTopRegionInfo()
                        .withRegionIds(ListUtils.longToIntegerList(adGroupGeo))
                        .withCount(2),
                new GdClientTopRegionInfo()
                        .withRegionIds(ListUtils.longToIntegerList(adGroupGeoAnother))
                        .withCount(1));
        Assert.assertThat(regionsTop, beanDiffer(expected));
    }

    @Test
    public void getTopRegions_CheckFilteringByCampaignIds() {
        List<Long> firstCampaignAdGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);
        List<Long> secondCampaignAdGroupGeo = List.of(Region.MOSCOW_REGION_ID);

        CampaignInfo firstCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        CampaignInfo secondCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        Long firstCampaignId = firstCampaignInfo.getCampaignId();
        Long secondCampaignId = secondCampaignInfo.getCampaignId();

        AdGroup firstCampaignAdGroup = defaultTextAdGroup(firstCampaignId).withGeo(firstCampaignAdGroupGeo);
        AdGroup anotherFirstCampaignAdGroup = defaultTextAdGroup(firstCampaignId).withGeo(firstCampaignAdGroupGeo);
        AdGroup secondCampaignAdGroup = defaultTextAdGroup(secondCampaignId).withGeo(secondCampaignAdGroupGeo);
        steps.adGroupSteps().createAdGroup(firstCampaignAdGroup, firstCampaignInfo);
        steps.adGroupSteps().createAdGroup(anotherFirstCampaignAdGroup, firstCampaignInfo);
        steps.adGroupSteps().createAdGroup(secondCampaignAdGroup, secondCampaignInfo);

        request.setCampaignIds(List.of(firstCampaignId));
        List<GdClientTopRegionInfo> regionsTop = clientDataService.getAdGroupTopRegions(gdClientInfo, request);

        var expectedRegionsTop = List.of(new GdClientTopRegionInfo()
                .withRegionIds(ListUtils.longToIntegerList(firstCampaignAdGroupGeo))
                .withCount(2));
        Assert.assertThat(regionsTop, beanDiffer(expectedRegionsTop));

        // проверяем, что регионы всех групп учитываются если передать все кампании клиента
        request.setCampaignIds(List.of(firstCampaignId, secondCampaignId));
        regionsTop = clientDataService.getAdGroupTopRegions(gdClientInfo, request);

        expectedRegionsTop = List.of(expectedRegionsTop.get(0),
                new GdClientTopRegionInfo()
                        .withRegionIds(ListUtils.longToIntegerList(secondCampaignAdGroupGeo))
                        .withCount(1));
        Assert.assertThat(regionsTop, beanDiffer(expectedRegionsTop));
    }
}
