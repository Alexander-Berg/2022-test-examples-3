package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateGetTest {

    private ClientInfo clientInfo;

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

    private Long contentPromotionCampaignVideoId;
    private Long contentPromotionAdGroupVideoId;

    private Long contentPromotionCampaignCollectionId;
    private Long contentPromotionAdGroupCollectionId;

    private Long contentPromotionCampaignServiceId;
    private Long contentPromotionAdGroupServiceId;

    private Long contentPromotionCampaignEdaId;
    private Long contentPromotionAdGroupEdaId;

    @Autowired
    private Steps steps;

    @Autowired
    private ApiAuthenticationSource apiAuthenticationSource;

    @Autowired
    private GetAdGroupsDelegate delegate;

    @Before
    public void prepare() {
        clientInfo = steps.clientSteps().createClient(new ClientInfo());

        CampaignSteps campaignSteps = steps.campaignSteps();
        AdGroupSteps adGroupSteps = steps.adGroupSteps();

        // text
        CampaignInfo textCampaign = campaignSteps.createActiveTextCampaign(clientInfo);
        textCampaignId = textCampaign.getCampaignId();
        textAdGroupId = adGroupSteps.createAdGroup(activeTextAdGroup(textCampaignId), textCampaign).getAdGroupId();

        // mobile app
        CampaignInfo mobileAppCampaign = campaignSteps.createActiveMobileAppCampaign(clientInfo);
        mobileAppCampaignId = mobileAppCampaign.getCampaignId();
        mobileAppAdGroupId = adGroupSteps
                .createAdGroup(createMobileAppAdGroup(mobileAppCampaignId, defaultMobileContent()), mobileAppCampaign)
                .getAdGroupId();

        // dynamic text
        CampaignInfo dynamicCampaign = campaignSteps.createActiveDynamicCampaign(clientInfo);
        dynamicCampaignTextId = dynamicCampaign.getCampaignId();
        dynamicTextAdGroupId =
                adGroupSteps.createAdGroup(activeDynamicTextAdGroup(dynamicCampaignTextId), dynamicCampaign)
                        .getAdGroupId();

        // dynamic feed
        CampaignInfo dynamicCampaign1 = campaignSteps.createActiveDynamicCampaign(clientInfo);
        dynamicFeedCampaignId = dynamicCampaign1.getCampaignId();
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        dynamicFeedAdGroupId = adGroupSteps
                .createAdGroup(activeDynamicFeedAdGroup(dynamicFeedCampaignId, feedInfo.getFeedId()), dynamicCampaign)
                .getAdGroupId();

        CampaignInfo smartCampaign = campaignSteps.createActivePerformanceCampaign(clientInfo);
        smartCampaignId = smartCampaign.getCampaignId();
        PerformanceAdGroup smartAdGroup = activePerformanceAdGroup(smartCampaignId, feedInfo.getFeedId());
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(smartAdGroup, smartCampaign);
        smartAdGroupId = adGroupInfo.getAdGroupId();

        var contentPromotionCampaignVideo = steps.contentPromotionCampaignSteps()
                .createDefaultCampaign(clientInfo);
        contentPromotionCampaignVideoId = contentPromotionCampaignVideo.getCampaignId();
        ContentPromotionAdGroup contentPromotionVideoAdGroup = fullContentPromotionAdGroup(
                contentPromotionCampaignVideoId, ContentPromotionAdgroupType.VIDEO);
        contentPromotionAdGroupVideoId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaignVideo, contentPromotionVideoAdGroup)
                .getAdGroupId();

        var contentPromotionCampaignCollection = steps.contentPromotionCampaignSteps()
                .createDefaultCampaign(clientInfo);
        contentPromotionCampaignCollectionId = contentPromotionCampaignCollection.getCampaignId();
        ContentPromotionAdGroup contentPromotionCollectionAdGroup = fullContentPromotionAdGroup(
                contentPromotionCampaignCollectionId, ContentPromotionAdgroupType.COLLECTION);
        contentPromotionAdGroupCollectionId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaignCollection, contentPromotionCollectionAdGroup)
                .getAdGroupId();

        var contentPromotionCampaignService = steps.contentPromotionCampaignSteps()
                .createDefaultCampaign(clientInfo);
        contentPromotionCampaignServiceId = contentPromotionCampaignService.getCampaignId();
        ContentPromotionAdGroup contentPromotionServiceAdGroup = fullContentPromotionAdGroup(
                contentPromotionCampaignServiceId, ContentPromotionAdgroupType.SERVICE);
        contentPromotionAdGroupServiceId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaignService, contentPromotionServiceAdGroup)
                .getAdGroupId();

        var contentPromotionCampaignEda = steps.contentPromotionCampaignSteps()
                .createDefaultCampaign(clientInfo);
        contentPromotionCampaignEdaId = contentPromotionCampaignEda.getCampaignId();
        ContentPromotionAdGroup contentPromotionEdaAdGroup = fullContentPromotionAdGroup(
                contentPromotionCampaignServiceId, ContentPromotionAdgroupType.EDA);
        contentPromotionAdGroupEdaId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaignEda, contentPromotionEdaAdGroup)
                .getAdGroupId();

        when(apiAuthenticationSource.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(apiAuthenticationSource.getChiefSubclient())
                .thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
    }

    @Test
    public void getAdgroupsByIdsWithSpecificType() {
        Set<AdGroupAnyFieldEnum> requestedFields = ImmutableSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID);
        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withAdGroupIds(textAdGroupId, mobileAppAdGroupId, dynamicTextAdGroupId, dynamicFeedAdGroupId,
                        smartAdGroupId)
                .withAdGroupTypes(AdGroupType.BASE);
        List<AdGroup> adGroups =
                delegate.get(new GenericGetRequest<>(requestedFields, selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(1));

        List<Long> adgroupIds = adGroups.stream().map(AdGroup::getId).collect(toList());
        assertThat(adgroupIds).contains(textAdGroupId);
    }

    @Test
    public void getAdgroupsByCampaignIdsWithAllTypes() {
        Set<AdGroupAnyFieldEnum> requestedFields = ImmutableSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID);
        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignTextId, dynamicFeedCampaignId,
                        smartCampaignId);
        List<AdGroup> adGroups =
                delegate.get(new GenericGetRequest<>(requestedFields, selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(5));

        List<Long> adgroupIds = adGroups.stream().map(AdGroup::getId).collect(toList());
        assertThat(adgroupIds).contains(textAdGroupId, mobileAppAdGroupId, dynamicTextAdGroupId, dynamicFeedAdGroupId,
                smartAdGroupId);
    }

    @Test
    public void getAdgroupsByCampaignIdsWithContentPromotionVideoType() {
        Set<AdGroupAnyFieldEnum> requestedFields = ImmutableSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID,
                AdGroupAnyFieldEnum.ADGROUP_CONTENT_PROMOTION_TYPE);
        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignTextId, dynamicFeedCampaignId,
                        smartCampaignId, contentPromotionCampaignCollectionId, contentPromotionCampaignVideoId,
                        contentPromotionCampaignServiceId, contentPromotionCampaignEdaId)
                .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.VIDEO));
        List<AdGroup> adGroups =
                delegate.get(new GenericGetRequest<>(requestedFields, selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(1));

        List<Long> adgroupIds = adGroups.stream().map(AdGroup::getId).collect(toList());
        List<ContentPromotionAdgroupType> contentPromotionAdGroupTypes = StreamEx.of(adGroups)
                .select(ContentPromotionAdGroup.class)
                .map(ContentPromotionAdGroup::getContentPromotionType)
                .toList();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(adgroupIds).contains(contentPromotionAdGroupVideoId);
        sa.assertThat(contentPromotionAdGroupTypes).contains(ContentPromotionAdgroupType.VIDEO);
        sa.assertAll();
    }

    @Test
    public void getAdgroupsByCampaignIdsWithContentPromotionCollectionType() {
        Set<AdGroupAnyFieldEnum> requestedFields = ImmutableSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID,
                AdGroupAnyFieldEnum.ADGROUP_CONTENT_PROMOTION_TYPE);
        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignTextId, dynamicFeedCampaignId,
                        smartCampaignId, contentPromotionCampaignCollectionId, contentPromotionCampaignVideoId,
                        contentPromotionCampaignServiceId, contentPromotionCampaignEdaId)
                .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.COLLECTION));
        List<AdGroup> adGroups =
                delegate.get(new GenericGetRequest<>(requestedFields, selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(1));

        List<Long> adgroupIds = adGroups.stream().map(AdGroup::getId).collect(toList());
        List<ContentPromotionAdgroupType> contentPromotionAdGroupTypes = StreamEx.of(adGroups)
                .select(ContentPromotionAdGroup.class)
                .map(ContentPromotionAdGroup::getContentPromotionType)
                .toList();

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(adgroupIds).contains(contentPromotionAdGroupCollectionId);
        sa.assertThat(contentPromotionAdGroupTypes).contains(ContentPromotionAdgroupType.COLLECTION);
        sa.assertAll();
    }

    @Test
    public void getAdgroupsByCampaignIdsWithContentPromotionServiceType() {
        Set<AdGroupAnyFieldEnum> requestedFields = ImmutableSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID,
                AdGroupAnyFieldEnum.ADGROUP_CONTENT_PROMOTION_TYPE);
        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignTextId, dynamicFeedCampaignId,
                        smartCampaignId, contentPromotionCampaignCollectionId, contentPromotionCampaignVideoId,
                        contentPromotionCampaignServiceId, contentPromotionCampaignEdaId)
                .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.SERVICE));
        List<AdGroup> adGroups =
                delegate.get(new GenericGetRequest<>(requestedFields, selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(1));

        List<Long> adgroupIds = adGroups.stream().map(AdGroup::getId).collect(toList());
        List<ContentPromotionAdgroupType> contentPromotionAdGroupTypes = StreamEx.of(adGroups)
                .select(ContentPromotionAdGroup.class)
                .map(ContentPromotionAdGroup::getContentPromotionType)
                .toList();

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(adgroupIds).contains(contentPromotionAdGroupServiceId);
        sa.assertThat(contentPromotionAdGroupTypes).contains(ContentPromotionAdgroupType.SERVICE);
        sa.assertAll();
    }

    @Test
    public void getAdgroupsByCampaignIdsWithContentPromotionEdaType() {
        Set<AdGroupAnyFieldEnum> requestedFields = ImmutableSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID,
                AdGroupAnyFieldEnum.ADGROUP_CONTENT_PROMOTION_TYPE);
        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignTextId, dynamicFeedCampaignId,
                        smartCampaignId, contentPromotionCampaignCollectionId, contentPromotionCampaignVideoId,
                        contentPromotionCampaignServiceId, contentPromotionCampaignEdaId)
                .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.EDA));
        List<AdGroup> adGroups =
                delegate.get(new GenericGetRequest<>(requestedFields, selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(1));

        List<Long> adgroupIds = adGroups.stream().map(AdGroup::getId).collect(toList());
        ContentPromotionAdgroupType contentPromotionAdGroupType = StreamEx.of(adGroups)
                .select(ContentPromotionAdGroup.class)
                .filter(t -> t.getId().equals(contentPromotionAdGroupEdaId))
                .findFirst()
                .map(ContentPromotionAdGroup::getContentPromotionType)
                .orElse(null);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(adgroupIds).contains(contentPromotionAdGroupEdaId);
        sa.assertThat(contentPromotionAdGroupType).isEqualTo(ContentPromotionAdgroupType.EDA);
        sa.assertAll();
    }

    @Test
    public void getAdGroupsWithNegativeKeywordSets() {
        MinusKeywordsPackInfo libraryMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(clientInfo);
        AdGroupInfo adGroupWithNegativeKeywordsSets = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack.getMinusKeywordPackId())),
                clientInfo);

        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria()
                .withAdGroupIds(textAdGroupId, adGroupWithNegativeKeywordsSets.getAdGroupId())
                .withNegativeKeywordSharedSetIds(libraryMinusKeywordsPack.getMinusKeywordPackId());
        //noinspection RedundantTypeArguments не билдится без явного типа
        List<AdGroup> adGroups = delegate.get(new GenericGetRequest<>(Collections.<AdGroupAnyFieldEnum>emptySet(),
                selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число групп", adGroups.size(), is(1));

        AdGroup adGroup = adGroups.get(0);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(adGroup.getId()).isEqualTo(adGroupWithNegativeKeywordsSets.getAdGroupId());
        sa.assertThat(adGroup.getLibraryMinusKeywordsIds()).
                contains(libraryMinusKeywordsPack.getMinusKeywordPackId());
        sa.assertAll();
    }

}
