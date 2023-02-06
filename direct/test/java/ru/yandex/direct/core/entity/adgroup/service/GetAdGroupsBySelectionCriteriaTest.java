package ru.yandex.direct.core.entity.adgroup.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdGroupsBySelectionCriteriaTest {

    private static final long CIS = 166L;
    private static final long BELARUS = BY_REGION_ID;
    private static final long UKRAINE = UKRAINE_REGION_ID;

    private static final CompareStrategy COMMON_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("effectiveGeo"), newPath("restrictedGeo"));

    private int shard;

    private ClientInfo clientInfo;

    private Long otherClientCampaignId;
    private Long otherClientAdGroupId;

    private Long notSupportedCampaignId;
    private Long notSupportedAdGroupId;

    private Long textCampaignId;
    private Long textAdGroupId;

    private Long mobileAppCampaignId;
    private Long mobileAppAdGroupId;

    private Long dynamicCampaignTextId;
    private Long dynamicTextAdGroupId;

    private Long dynamicFeedCampaignId;
    private Long dynamicFeedAdGroupId;

    private Long smartCampaignId;
    private Long smartAdGroupId;

    private List<AdGroup> expectedAdGroups;
    private List<AdGroup> allExpectedAdGroups;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupService service;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Before
    public void setUp() {
        // Этот тест использует ограничения по AccessibleCampaignTypes, которых в ядре быть не должно по идее
        requestAccessibleCampaignTypes.setApi5();

        CampaignSteps campaignSteps = steps.campaignSteps();
        AdGroupSteps adGroupSteps = steps.adGroupSteps();
        BannerSteps bannerSteps = steps.bannerSteps();

        clientInfo = steps.clientSteps().createClient(new ClientInfo());

        shard = clientInfo.getShard();

        ClientInfo otherClientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(shard));

        // other client campaign
        CampaignInfo otherClientCampaign = campaignSteps.createActiveTextCampaign(otherClientInfo);
        otherClientCampaignId = otherClientCampaign.getCampaignId();
        TextAdGroup otherClietAdGroup = activeTextAdGroup(otherClientCampaignId);
        otherClientAdGroupId = adGroupSteps.createAdGroup(otherClietAdGroup, otherClientCampaign).getAdGroupId();

        // not supported campaign
        CampaignInfo cpmYndxFrontpageCampaign = campaignSteps.createActiveCpmYndxFrontpageCampaign(clientInfo);
        notSupportedCampaignId = cpmYndxFrontpageCampaign.getCampaignId();
        CpmYndxFrontpageAdGroup expectedCpmYndxFrontpageAdGroup = activeCpmYndxFrontpageAdGroup(notSupportedCampaignId);
        notSupportedAdGroupId =
                adGroupSteps.createAdGroup(expectedCpmYndxFrontpageAdGroup, cpmYndxFrontpageCampaign).getAdGroupId();

        expectedAdGroups = new ArrayList<>();
        allExpectedAdGroups = new ArrayList<>();

        // text
        CampaignInfo textCampaign = campaignSteps.createActiveTextCampaign(clientInfo);
        textCampaignId = textCampaign.getCampaignId();
        TextAdGroup expectedTextAdGroup =
                activeTextAdGroup(textCampaignId).withGeo(asList(CIS, -BELARUS));
        expectedAdGroups.add(expectedTextAdGroup);
        AdGroupInfo textAdGroupInfo = adGroupSteps.createAdGroup(expectedTextAdGroup, textCampaign);
        textAdGroupId = textAdGroupInfo.getAdGroupId();
        bannerSteps.createDefaultTextBannerWithMinusGeo(textAdGroupInfo, singletonList(UKRAINE));
        expectedTextAdGroup.setEffectiveGeo(asList(CIS, -BELARUS, -UKRAINE));
        expectedTextAdGroup.setRestrictedGeo(singletonList(UKRAINE));

        // mobile app
        CampaignInfo mobileAppCampaign = campaignSteps.createActiveMobileAppCampaign(clientInfo);
        mobileAppCampaignId = mobileAppCampaign.getCampaignId();
        MobileContentAdGroup expectedMobileAppAdGroup =
                createMobileAppAdGroup(mobileAppCampaignId, defaultMobileContent());
        expectedAdGroups.add(expectedMobileAppAdGroup);
        mobileAppAdGroupId = adGroupSteps.createAdGroup(expectedMobileAppAdGroup, mobileAppCampaign).getAdGroupId();

        // dynamic text
        CampaignInfo dynamicCampaign = campaignSteps.createActiveDynamicCampaign(clientInfo);
        dynamicCampaignTextId = dynamicCampaign.getCampaignId();
        DynamicTextAdGroup expectedDynamicTextAdGroup = activeDynamicTextAdGroup(dynamicCampaignTextId);
        expectedAdGroups.add(expectedDynamicTextAdGroup);
        dynamicTextAdGroupId = adGroupSteps.createAdGroup(expectedDynamicTextAdGroup, dynamicCampaign).getAdGroupId();

        // dynamic feed
        CampaignInfo dynamicCampaign1 = campaignSteps.createActiveDynamicCampaign(clientInfo);
        dynamicFeedCampaignId = dynamicCampaign1.getCampaignId();
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        DynamicFeedAdGroup expectedDynamicFeedAdGroup =
                activeDynamicFeedAdGroup(dynamicFeedCampaignId, feedId);
        expectedAdGroups.add(expectedDynamicFeedAdGroup);
        dynamicFeedAdGroupId = adGroupSteps.createAdGroup(expectedDynamicFeedAdGroup, dynamicCampaign).getAdGroupId();

        // smart
        CampaignInfo smartCampaign = campaignSteps.createActivePerformanceCampaign(clientInfo);
        smartCampaignId = smartCampaign.getCampaignId();
        PerformanceAdGroup expectedSmartAdGroup = activePerformanceAdGroup(smartCampaignId, feedId);
        expectedAdGroups.add(expectedSmartAdGroup);
        smartAdGroupId = adGroupSteps.createAdGroup(expectedSmartAdGroup, smartCampaign).getAdGroupId();

        allExpectedAdGroups.addAll(asList(otherClietAdGroup, expectedCpmYndxFrontpageAdGroup));
        allExpectedAdGroups.addAll(expectedAdGroups);
    }

    @Test
    public void noAdgroupsOnEmptyAfterFiltrationCampaignIds() {
        List<AdGroup> adGroups = service.getAdGroupsBySelectionCriteria(clientInfo.getUid(), clientInfo.getClientId(),
                new AdGroupsSelectionCriteria().withCampaignIds(otherClientCampaignId, notSupportedCampaignId),
                maxLimited(), false);
        assertThat("no adgroups found", adGroups, is(empty()));
    }

    @Test
    public void noAdgroupsOnEmptyAfterFiltrationAdGroupIds() {
        List<AdGroup> adGroups = service.getAdGroupsBySelectionCriteria(clientInfo.getUid(), clientInfo.getClientId(),
                new AdGroupsSelectionCriteria().withAdGroupIds(notSupportedAdGroupId, otherClientAdGroupId),
                maxLimited(), false);
        assertThat("no adgroups found", adGroups, is(empty()));
    }

    @Test
    public void getAdGroupsByCampaignIds() {
        List<AdGroup> adGroups = service.getAdGroupsBySelectionCriteria(clientInfo.getUid(), clientInfo.getClientId(),
                new AdGroupsSelectionCriteria()
                        .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignTextId,
                                dynamicFeedCampaignId, smartCampaignId,
                                otherClientCampaignId, notSupportedCampaignId),
                maxLimited(), true);

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(5));
        assertThat("данные извлеченных групп соответствуют ожидаемым", adGroups,
                contains(mapList(expectedAdGroups, expectedAdGroup -> beanDiffer(expectedAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY))));
    }

    @Test
    public void getAdGroupsByAdgroupIds() {
        List<AdGroup> adGroups = service.getAdGroupsBySelectionCriteria(clientInfo.getUid(), clientInfo.getClientId(),
                new AdGroupsSelectionCriteria()
                        .withAdGroupIds(textAdGroupId, mobileAppAdGroupId, dynamicTextAdGroupId, dynamicFeedAdGroupId,
                                smartAdGroupId, notSupportedAdGroupId, otherClientAdGroupId),
                maxLimited(), true);

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(5));
        assertThat("данные извлеченных групп соответствуют ожидаемым", adGroups,
                contains(mapList(expectedAdGroups, expectedAdGroup -> beanDiffer(expectedAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY))));
    }

    @Test
    public void getAdGroupsBySelectionCriteria_AllClientsByAdGroupIds() {
        List<AdGroup> adGroups = service.getAdGroupsBySelectionCriteria(new AdGroupsSelectionCriteria()
                        .withAdGroupIds(textAdGroupId, mobileAppAdGroupId, dynamicTextAdGroupId, dynamicFeedAdGroupId,
                                smartAdGroupId, notSupportedAdGroupId, otherClientAdGroupId),
                maxLimited(), true);

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(7));
        assertThat("данные извлеченных групп соответствуют ожидаемым", adGroups,
                contains(mapList(allExpectedAdGroups, expectedAdGroup -> beanDiffer(expectedAdGroup)
                        .useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY))));
    }

    @Test
    public void getAdGroupsBySelectionCriteria_AllClientsByCampaignIds() {
        List<AdGroup> adGroups = service.getAdGroupsBySelectionCriteria(new AdGroupsSelectionCriteria()
                        .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignTextId,
                                smartCampaignId, dynamicFeedCampaignId,
                                notSupportedCampaignId, otherClientCampaignId),
                maxLimited(), true);

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(7));
        assertThat("данные извлеченных групп соответствуют ожидаемым", adGroups,
                contains(mapList(allExpectedAdGroups, expectedAdGroup -> beanDiffer(expectedAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY))));
    }

    @Test
    public void getAdGroupsBySelectionCriteria_withoutEffectiveAndRestrictedGeo() {
        List<AdGroup> adGroups = service.getAdGroupsBySelectionCriteria(new AdGroupsSelectionCriteria()
                        .withAdGroupIds(textAdGroupId),
                maxLimited(), false);

        AdGroup textAdGroup = expectedAdGroups.get(0);
        List<AdGroup> expectedAdGroupsWithoutGeo = singletonList(
                new AdGroup()
                        .withId(textAdGroup.getId())
                        .withCampaignId(textAdGroup.getCampaignId())
                        .withType(textAdGroup.getType())
                        .withEffectiveGeo(null)
                        .withRestrictedGeo(null)
        );

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(1));
        assertThat("данные извлеченных групп соответствуют ожидаемым", adGroups.get(0),
                beanDiffer(expectedAdGroupsWithoutGeo.get(0)).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }
}
