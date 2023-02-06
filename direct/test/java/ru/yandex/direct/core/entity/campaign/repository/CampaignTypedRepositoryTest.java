package ru.yandex.direct.core.entity.campaign.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.USER_CAMPAIGNS_FAVORITE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignTypedRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public DslContextProvider dslContextProvider;

    private ClientInfo defaultClient;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void getDynamicCampaigns() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        CampaignInfo campaignInfo1 = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        CampaignInfo campaignInfo2 = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);

        List<? extends BaseCampaign> foundCampaigns = campaignTypedRepository
                .getTypedCampaigns(DEFAULT_SHARD,
                        List.of(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()));
        List<DynamicCampaign> campaigns = mapList(foundCampaigns, (campaign) -> (DynamicCampaign) campaign);
        List<Long> campaignIds = campaigns.stream().map(DynamicCampaign::getId).collect(Collectors.toList());

        assertThat(campaignIds).hasSize(2)
                .contains(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId());
    }

    @Test
    public void getSmartCampaigns() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        CampaignInfo campaignInfo1 = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        CampaignInfo campaignInfo2 = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        List<? extends BaseCampaign> foundCampaigns = campaignTypedRepository
                .getTypedCampaigns(DEFAULT_SHARD,
                        List.of(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()));
        List<SmartCampaign> campaigns = mapList(foundCampaigns, (campaign) -> (SmartCampaign) campaign);
        List<Long> campaignIds = campaigns.stream().map(SmartCampaign::getId).collect(Collectors.toList());

        assertThat(campaignIds).hasSize(2)
                .contains(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId());
    }

    @Test
    public void getMcBannerCampaigns() {
        var campaignInfo1 = steps.mcBannerCampaignSteps()
                .createDefaultCampaign(defaultClient);
        var campaignInfo2 = steps.mcBannerCampaignSteps()
                .createDefaultCampaign(defaultClient);

        List<? extends BaseCampaign> foundCampaigns = campaignTypedRepository
                .getTypedCampaigns(DEFAULT_SHARD, List.of(campaignInfo1.getId(), campaignInfo2.getId()));
        List<McBannerCampaign> campaigns = mapList(foundCampaigns, (campaign) -> (McBannerCampaign) campaign);
        List<Long> campaignIds = campaigns.stream()
                .map(McBannerCampaign::getId)
                .collect(Collectors.toList());

        assertThat(campaignIds).hasSize(2).contains(campaignInfo1.getId(), campaignInfo2.getId());
    }

    @Test
    public void getTypedCampaigns_getNotFavoriteCampaign() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        List<? extends BaseCampaign> foundCampaigns = campaignTypedRepository
                .getTypedCampaigns(DEFAULT_SHARD, singletonList(campaignInfo.getCampaignId()));
        List<TextCampaign> campaigns = mapList(foundCampaigns, (campaign) -> (TextCampaign) campaign);

        assertThat(campaigns).hasSize(1);
        assertThat(campaigns.get(0).getFavoriteForUids()).isNull();
    }

    @Test
    public void getTypedCampaigns_getFavoriteCampaign() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        addToUserCampaignFavorite(clientInfo.getUid(), campaignInfo.getCampaignId());

        List<? extends BaseCampaign> foundCampaigns = campaignTypedRepository
                .getTypedCampaigns(DEFAULT_SHARD, singletonList(campaignInfo.getCampaignId()));
        List<TextCampaign> campaigns = mapList(foundCampaigns, (campaign) -> (TextCampaign) campaign);

        assertThat(campaigns).hasSize(1);
        assertThat(campaigns.get(0).getFavoriteForUids()).containsExactly(clientInfo.getUid());
    }

    @Test
    public void getTypedCampaigns_getFavoriteCampaignOfTwoUsers() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        long userId1 = clientInfo.getUid();
        long userId2 = RandomUtils.nextLong();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        addToUserCampaignFavorite(userId1, campaignInfo.getCampaignId());
        addToUserCampaignFavorite(userId2, campaignInfo.getCampaignId());

        List<? extends BaseCampaign> foundCampaigns = campaignTypedRepository
                .getTypedCampaigns(DEFAULT_SHARD, singletonList(campaignInfo.getCampaignId()));
        List<TextCampaign> campaigns = mapList(foundCampaigns, (campaign) -> (TextCampaign) campaign);

        assertThat(campaigns).hasSize(1);
        assertThat(campaigns.get(0).getFavoriteForUids()).containsExactlyInAnyOrder(userId1, userId2);
    }

    @Test
    public void getTypedCampaigns_getFavoriteCampaignOfAnotherUser() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        long userId = RandomUtils.nextLong();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        addToUserCampaignFavorite(userId, campaignInfo.getCampaignId());

        List<? extends BaseCampaign> foundCampaigns = campaignTypedRepository
                .getTypedCampaigns(DEFAULT_SHARD, singletonList(campaignInfo.getCampaignId()));
        List<TextCampaign> campaigns = mapList(foundCampaigns, (campaign) -> (TextCampaign) campaign);

        assertThat(campaigns).hasSize(1);
        assertThat(campaigns.get(0).getFavoriteForUids()).containsExactly(userId);
    }

    @Test
    public void checkGetDynamicCampaign() {
        Campaign campaign =
                activeDynamicCampaign(defaultClient.getClientId(), defaultClient.getUid());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        DynamicCampaign dynamicCampaign = (DynamicCampaign) campaignTypedRepository
                .getTypedCampaigns(defaultClient.getShard(), singletonList(campaignInfo.getCampaignId())).get(0);
        DynamicCampaign expectedDynamicCampaign = getExpectedDynamicCampaign(campaign);
        assertThat(dynamicCampaign)
                .isEqualToIgnoringNullFields(expectedDynamicCampaign);
    }

    @Test
    public void checkGetSmartCampaign() {
        Campaign campaign =
                activePerformanceCampaign(defaultClient.getClientId(), defaultClient.getUid());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        SmartCampaign smartCampaign = (SmartCampaign) campaignTypedRepository
                .getTypedCampaigns(defaultClient.getShard(), List.of(campaignInfo.getCampaignId())).get(0);
        SmartCampaign expectedSmartCampaign = getExpectedSmartCampaign(campaign);
        assertThat(smartCampaign)
                .isEqualToIgnoringNullFields(expectedSmartCampaign);
    }

    @Test
    public void checkGetMcBannerCampaign() {
        var typedCampaignInfo = steps.mcBannerCampaignSteps()
                .createDefaultCampaign(defaultClient);
        McBannerCampaign mcBannerCampaign = (McBannerCampaign) campaignTypedRepository
                .getTypedCampaigns(defaultClient.getShard(), List.of(typedCampaignInfo.getId())).get(0);

        CompareStrategy compareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("dayBudgetLastChange"), newPath("timeTarget"), newPath("isVirtual"),
                        newPath("hasTurboApp"), newPath("requireFiltrationByDontShowDomains"), newPath("source"))
                .forFields(newPath("sumToPay")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("sum")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("sumSpent")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("sumLast")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("dayBudget")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("lastChange")).useMatcher(approximatelyNow())
                .forFields(newPath("createTime")).useMatcher(approximatelyNow())
                .forFields(newPath("metrikaCounters")).useMatcher(nullValue())
                .forFields(newPath("source")).useMatcher(equalTo(CampaignSource.DIRECT))
                .forFields(newPath("metatype")).useMatcher(equalTo(CampaignMetatype.DEFAULT_));

        assertThat(mcBannerCampaign)
                .is(matchedBy(beanDiffer(typedCampaignInfo.getTypedCampaign())
                        .useCompareStrategy(compareStrategy)));
    }

    @Test
    public void campaignAbsentInCampOptionsTest() {
        var campaign = steps.campaignSteps().createActiveTextCampaign(defaultClient);
        dslContextProvider.ppc(campaign.getShard())
                .deleteFrom(CAMP_OPTIONS)
                .where(CAMP_OPTIONS.CID.eq(campaign.getCampaignId()))
                .execute();
        var result = campaignTypedRepository.getTyped(campaign.getShard(), List.of(campaign.getCampaignId()));
        assertThat(result).isEmpty();

    }

    private DynamicCampaign getExpectedDynamicCampaign(Campaign campaign) {
        return new DynamicCampaign()
                .withId(campaign.getId())
                .withClientId(campaign.getClientId())
                .withWalletId(campaign.getWalletId())
                .withUid(campaign.getUid())
                .withName(campaign.getName())
                .withStatusEmpty(campaign.getStatusEmpty())
                .withStatusModerate(CampaignStatusModerate.valueOf(campaign.getStatusModerate().name()))
                .withAgencyId(campaign.getAgencyId())
                .withOrderId(campaign.getOrderId())
                .withEnableCompanyInfo(campaign.getEnableCompanyInfo())
                .withStatusShow(campaign.getStatusShow())
                .withStatusArchived(campaign.getArchived())
                .withStatusActive(campaign.getStatusActive())
                .withStatusBsSynced(CampaignStatusBsSynced.valueOf(campaign.getStatusBsSynced().name()))
                .withStatusPostModerate(CampaignStatusPostmoderate.valueOf(campaign.getStatusModerate().name()))
                .withStartDate(campaign.getStartTime())
                .withEmail(campaign.getEmail())
                .withSum(campaign.getBalanceInfo().getSum())
                .withSumLast(campaign.getBalanceInfo().getSumLast())
                .withSumSpent(campaign.getBalanceInfo().getSumSpent())
                .withTimeZoneId(campaign.getTimezoneId())
                .withAllowedPageIds(campaign.getAllowedPageIds())
                .withDefaultPermalinkId(campaign.getDefaultPermalink())
                .withDisabledSsp(campaign.getDisabledSsp())
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withMinusKeywords(campaign.getMinusKeywords());
    }

    private SmartCampaign getExpectedSmartCampaign(Campaign campaign) {
        return new SmartCampaign()
                .withId(campaign.getId())
                .withClientId(campaign.getClientId())
                .withWalletId(campaign.getWalletId())
                .withUid(campaign.getUid())
                .withName(campaign.getName())
                .withStatusEmpty(campaign.getStatusEmpty())
                .withStatusModerate(CampaignStatusModerate.valueOf(campaign.getStatusModerate().name()))
                .withAgencyId(campaign.getAgencyId())
                .withOrderId(campaign.getOrderId())
                .withEnableCompanyInfo(campaign.getEnableCompanyInfo())
                .withStatusShow(campaign.getStatusShow())
                .withStatusArchived(campaign.getArchived())
                .withStatusActive(campaign.getStatusActive())
                .withStatusBsSynced(CampaignStatusBsSynced.valueOf(campaign.getStatusBsSynced().name()))
                .withStatusPostModerate(CampaignStatusPostmoderate.valueOf(campaign.getStatusModerate().name()))
                .withStartDate(campaign.getStartTime())
                .withEmail(campaign.getEmail())
                .withSum(campaign.getBalanceInfo().getSum())
                .withSumLast(campaign.getBalanceInfo().getSumLast())
                .withSumSpent(campaign.getBalanceInfo().getSumSpent())
                .withTimeZoneId(campaign.getTimezoneId())
                .withDisabledSsp(campaign.getDisabledSsp())
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withMinusKeywords(campaign.getMinusKeywords());
    }

    private void addToUserCampaignFavorite(long uid, long cid) {
        dslContextProvider.ppc(DEFAULT_SHARD)
                .insertInto(USER_CAMPAIGNS_FAVORITE, USER_CAMPAIGNS_FAVORITE.UID, USER_CAMPAIGNS_FAVORITE.CID)
                .values(uid, cid)
                .execute();
    }
}
