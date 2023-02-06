package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.List;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.repository.typesupport.InternalAdGroupSupport.DEFAULT_LEVEL;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupRepositoryAddTest {

    private static final CompareStrategy STRATEGY_ADGOUP = DefaultCompareStrategies.allFields()
            .forFields(newPath(AdGroup.LAST_CHANGE.name())).useMatcher(approximatelyNow());

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository repository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private int shard;
    private ClientId clientId;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        steps.placementSteps().clearPlacements();
        clientInfo = steps.clientSteps().createClient(new ClientInfo());
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
    }

    @Test
    public void addTextAdGroup() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        TextAdGroup adGroup = activeTextAdGroup(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, adGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
    }

    @Test
    public void addMobileContent() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        MobileContentAdGroup adGroup = activeMobileAppAdGroup(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, adGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertThat(((MobileContentAdGroup) adGroups.get(0)).getStoreUrl(), equalTo(adGroup.getStoreUrl()));
    }

    @Test
    public void addDynamicText() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        DynamicTextAdGroup adGroup = activeDynamicTextAdGroup(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, adGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertThat(((DynamicTextAdGroup) adGroups.get(0)).getDomainUrl(), equalTo(adGroup.getDomainUrl()));
    }

    @Test
    public void addDynamicFeed() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        DynamicFeedAdGroup adGroup = activeDynamicFeedAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, adGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertThat(((DynamicFeedAdGroup) adGroups.get(0)).getFeedId(), equalTo(feedInfo.getFeedId()));
    }

    @Test
    public void addCpmOutdoor() {
        OutdoorPlacement placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        CpmOutdoorAdGroup cpmOutdoorAdGroup = activeCpmOutdoorAdGroup(campaignInfo.getCampaignId(), placement);
        List<Long> adGroupIds = createAdGroups(shard, clientId, cpmOutdoorAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(cpmOutdoorAdGroup, adGroups.get(0));
    }

    @Test
    public void addCpmGeoproduct() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        CpmGeoproductAdGroup cpmGeoproductAdGroup = activeCpmGeoproductAdGroup(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, cpmGeoproductAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(cpmGeoproductAdGroup, adGroups.get(0));
    }

    @Test
    public void addCpmIndoor() {
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        CpmIndoorAdGroup cpmIndoorAdGroup = activeCpmIndoorAdGroup(campaignInfo.getCampaignId(), placement);
        List<Long> adGroupIds = createAdGroups(shard, clientId, cpmIndoorAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(cpmIndoorAdGroup, adGroups.get(0));
    }

    @Test
    public void addCpmYndxFrontpage() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        CpmYndxFrontpageAdGroup cpmYndxFrontpageAdGroup = activeCpmYndxFrontpageAdGroup(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, cpmYndxFrontpageAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(cpmYndxFrontpageAdGroup, adGroups.get(0));
    }

    @Test
    public void addContentPromotionVideoAdGroup_SaveAllFields_SuccessTest() {
        var campaignInfo = steps.contentPromotionCampaignSteps()
                .createDefaultCampaign(clientInfo);
        var contentVideoAdGroup =
                fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                        .withCampaignId(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, contentVideoAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(contentVideoAdGroup, adGroups.get(0));
    }

    @Test
    public void addContentPromotionAdGroup_SaveAllFields_SuccessTest() {
        var campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        ContentPromotionAdGroup contentPromotionAdGroup =
                fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION)
                        .withCampaignId(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, contentPromotionAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(contentPromotionAdGroup, adGroups.get(0));
    }

    @Test
    public void addInternalFree() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign(clientInfo);
        InternalAdGroup internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId(), 0L);
        List<Long> adGroupIds = createAdGroups(shard, clientId, internalAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(internalAdGroup, adGroups.get(0));
    }

    @Test
    public void addInternalFree_withoutRf() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign(clientInfo);
        InternalAdGroup internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId(), null)
                .withRf(null)
                .withRfReset(null);
        List<Long> adGroupIds = createAdGroups(shard, clientId, internalAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        internalAdGroup.setLevel(DEFAULT_LEVEL);
        assertAdGroup(internalAdGroup, adGroups.get(0));
    }

    @Test
    public void addInternalDistrib() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        InternalAdGroup internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, internalAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        assertAdGroup(internalAdGroup, adGroups.get(0));
    }

    @Test
    public void addInternalAutobudget() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign(clientInfo);
        InternalAdGroup internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId(), null);
        List<Long> adGroupIds = createAdGroups(shard, clientId, internalAdGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
        internalAdGroup.setLevel(DEFAULT_LEVEL);
        assertAdGroup(internalAdGroup, adGroups.get(0));
    }

    @Test
    public void addMcBannerAdGroup() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMcBannerCampaign(clientInfo);
        McBannerAdGroup adGroup = activeMcBannerAdGroup(campaignInfo.getCampaignId());
        List<Long> adGroupIds = createAdGroups(shard, clientId, adGroup);

        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);

        assertThat(adGroups, hasSize(1));
    }

    @Test
    public void addAdGroups_successPerformanceAdGroupAllFields() {
        //Подготваливаем данные
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        Long campaignId = steps.campaignSteps().createActivePerformanceCampaign(clientInfo).getCampaignId();
        PerformanceAdGroup saving = defaultPerformanceAdGroup(campaignId, feedId);
        Long savedId = createAdGroups(shard, clientId, saving).get(0);

        //Ожидаемый результат
        PerformanceAdGroup defaultAdGroup = defaultPerformanceAdGroup(campaignId, feedId);

        //Актуальные данные из базы
        AdGroup actual = repository.getAdGroups(clientInfo.getShard(), singletonList(savedId)).get(0);

        //Сверяем ожидания и реальность
        AssertionsForClassTypes.assertThat(actual)
                .is(matchedBy(beanDiffer(defaultAdGroup)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private List<Long> createAdGroups(int shard, ClientId clientId, AdGroup... adGroups) {
        return repository.addAdGroups(dslContextProvider.ppc(shard).configuration(), clientId, asList(adGroups));
    }

    private void assertAdGroup(AdGroup expectedAdGroup, AdGroup actualAdGroup) {
        assertThat("Состояние группы не соответствует ожидаемому",
                actualAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(STRATEGY_ADGOUP));
    }

}
