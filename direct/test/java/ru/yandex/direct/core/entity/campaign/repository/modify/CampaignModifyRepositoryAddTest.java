package ru.yandex.direct.core.entity.campaign.repository.modify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.usercampaignsfavorite.repository.UserCampaignsFavoriteRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPerformanceNowOptimizingBy;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignWithSystemFieldsByCampaignType;
import static ru.yandex.direct.dbschema.ppc.Tables.USER_CAMPAIGNS_FAVORITE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignModifyRepositoryAddTest {
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    CampaignRepository campaignRepository;
    @Autowired
    UserCampaignsFavoriteRepository userCampaignsFavoriteRepository;
    @Autowired
    SspPlatformsRepository sspPlatformsRepository;
    @Autowired
    public Steps steps;
    @Autowired
    public DslContextProvider dslContextProvider;

    private ClientInfo defaultClientAndUser;
    private int shard;
    private long userId;

    @Before
    public void before() {
        defaultClientAndUser = steps.clientSteps().createDefaultClient();
        shard = defaultClientAndUser.getShard();
        userId = defaultClientAndUser.getUid();
    }

    @Test
    public void insert_textCampaigns() {
        TextCampaign textCampaignOne = (TextCampaign) getCampaign(CampaignType.TEXT);
        TextCampaign textCampaignTwo = (TextCampaign) getCampaign(CampaignType.TEXT);
        insert(List.of(textCampaignOne, textCampaignTwo));
    }

    @Test
    public void insert_dynamicCampaigns() {
        DynamicCampaign dynamicCampaignOne = (DynamicCampaign) getCampaign(CampaignType.DYNAMIC);
        DynamicCampaign dynamicCampaignTwo = (DynamicCampaign) getCampaign(CampaignType.DYNAMIC);
        insert(List.of(dynamicCampaignOne, dynamicCampaignTwo));
    }

    @Test
    public void insert_smartCampaigns() {
        SmartCampaign smartCampaignOne = (SmartCampaign) getCampaign(CampaignType.PERFORMANCE);
        SmartCampaign smartCampaignTwo = (SmartCampaign) getCampaign(CampaignType.PERFORMANCE);
        insert(List.of(smartCampaignOne, smartCampaignTwo));
    }

    @Test
    public void insert_mcBannerCampaigns() {
        McBannerCampaign mcBannerCampaignOne = (McBannerCampaign) getCampaign(CampaignType.MCBANNER);
        McBannerCampaign mcBannerCampaignTwo = (McBannerCampaign) getCampaign(CampaignType.MCBANNER);
        insert(List.of(mcBannerCampaignOne, mcBannerCampaignTwo));
    }

    @Test
    public void insert_mobileContentCampaigns() {
        Long mobileAppId = 5L;
        MobileContentCampaign mobileContentCampaignOne =
                ((MobileContentCampaign) getCampaign(CampaignType.MOBILE_CONTENT))
                        .withMobileAppId(mobileAppId);
        MobileContentCampaign mobileContentCampaignTwo =
                ((MobileContentCampaign) getCampaign(CampaignType.MOBILE_CONTENT))
                        .withMobileAppId(mobileAppId);
        insert(List.of(mobileContentCampaignOne, mobileContentCampaignTwo));
    }

    @Test
    public void insert_FavoriteCampaign() {
        TextCampaign campaign = ((TextCampaign) getCampaign(CampaignType.TEXT))
                .withFavoriteForUids(Set.of(userId));
        Long campaignId = insert(List.of(campaign)).get(0);

        List<Long> userIds = getUserCampaignFavoriteUidsByCampaignId(campaignId);
        assertThat(userIds)
                .hasSize(1)
                .contains(userId);
    }

    @Test
    public void insert_SmartCampaign_CheckThatAddingDataToCampaignsPerformanceTable() {
        SmartCampaign smartCampaign = (SmartCampaign) getCampaign(CampaignType.PERFORMANCE);
        Long campaignId = insert(List.of(smartCampaign)).get(0);

        CampaignsPerformanceNowOptimizingBy nowOptimizingBy =
                steps.campaignsPerformanceSteps().getNowOptimizingByCid(shard, campaignId);
        assertThat(nowOptimizingBy)
                .isEqualTo(CampaignsPerformanceNowOptimizingBy.CPC);
    }

    @Test
    public void insert_FavoriteCampaignForTwoUsers() {
        long userId2 = RandomUtils.nextLong();

        TextCampaign campaign = ((TextCampaign) getCampaign(CampaignType.TEXT))
                .withFavoriteForUids(Set.of(userId, userId2));
        Long campaignId = insert(List.of(campaign)).get(0);

        List<Long> userIds = getUserCampaignFavoriteUidsByCampaignId(campaignId);
        assertThat(userIds)
                .hasSize(2)
                .containsExactlyInAnyOrder(userId, userId2);
    }

    @Test
    public void insert_FavoriteCampaignsWithSameUser() {
        TextCampaign campaign1 = ((TextCampaign) getCampaign(CampaignType.TEXT))
                .withFavoriteForUids(Set.of(userId));
        TextCampaign campaign2 = ((TextCampaign) getCampaign(CampaignType.TEXT))
                .withFavoriteForUids(Set.of(userId));
        List<Long> campaignIds = insert(List.of(campaign1, campaign2));

        Map<Long, Set<Long>> favoriteCidsForUids =
                userCampaignsFavoriteRepository.getUserIdsByCampaignIds(dslContextProvider.ppc(shard), campaignIds);

        assertThat(favoriteCidsForUids)
                .hasSize(2)
                .containsEntry(campaignIds.get(0), Set.of(userId))
                .containsEntry(campaignIds.get(1), Set.of(userId));
    }

    @Test
    public void insert_FavoriteCampaignsWithDifferentUsers() {
        long userId2 = RandomUtils.nextLong();
        long userId3 = RandomUtils.nextLong();

        TextCampaign campaign1 = ((TextCampaign) getCampaign(CampaignType.TEXT))
                .withFavoriteForUids(Set.of(userId, userId2));
        TextCampaign campaign2 = ((TextCampaign) getCampaign(CampaignType.TEXT))
                .withFavoriteForUids(Set.of(userId3));
        List<Long> campaignIds = insert(List.of(campaign1, campaign2));

        Map<Long, Set<Long>> favoriteCidsForUids =
                userCampaignsFavoriteRepository.getUserIdsByCampaignIds(dslContextProvider.ppc(shard), campaignIds);
        assertThat(favoriteCidsForUids)
                .hasSize(2)
                .containsEntry(campaignIds.get(0), Set.of(userId, userId2))
                .containsEntry(campaignIds.get(1), Set.of(userId3));
    }

    private List<Long> insert(List<CommonCampaign> campaigns) {
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                RestrictedCampaignsAddOperationContainer.create(defaultClientAndUser.getShard(), defaultClientAndUser.getUid(),
                        defaultClientAndUser.getClientId(), defaultClientAndUser.getUid(),
                        defaultClientAndUser.getUid());
        List<Long> ids = campaignModifyRepository.addCampaigns(dslContextProvider.ppc(defaultClientAndUser.getShard()),
                addCampaignParametersContainer,
                campaigns);
        assertThat(ids).hasSize(campaigns.size());
        return ids;
    }

    private CommonCampaign getCampaign(CampaignType campaignType) {
        CommonCampaign campaign = defaultCampaignWithSystemFieldsByCampaignType(campaignType);
        return campaign.withTimeZoneId(130L)
                .withUid(defaultClientAndUser.getUid())
                .withClientId(defaultClientAndUser.getClientId().asLong());
    }

    private List<Long> getUserCampaignFavoriteUidsByCampaignId(long cid) {
        return dslContextProvider.ppc(defaultClientAndUser.getShard())
                .select(USER_CAMPAIGNS_FAVORITE.UID)
                .from(USER_CAMPAIGNS_FAVORITE)
                .where(USER_CAMPAIGNS_FAVORITE.CID.eq(cid))
                .fetch(USER_CAMPAIGNS_FAVORITE.UID);
    }
}
