package ru.yandex.direct.grid.processing.service.group;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilter;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddMobileContentAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRetargetingItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdatePerformanceAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdatePerformanceAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.uac.UacTestDataKt.STORE_URL;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupNetworkTargeting.WI_FI;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupMutationServiceSearchRetargetingsTest {

    @Autowired
    Steps steps;
    @Autowired
    AdGroupMutationService mutationService;
    @Autowired
    BidModifierService bidModifierService;
    @Autowired
    CampaignRepository campaignRepository;

    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);
    private static final List<Integer> REGION_IDS = Collections
            .singletonList(Long.valueOf(MOSCOW_REGION_ID).intValue());

    private ClientInfo clientInfo;
    private User user;
    private int shard;
    private Long operatorUid;
    private Long retargetingConditionId1, retargetingConditionId2;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.RUSSIA_REGION_ID));
        user = clientInfo.getChiefUserInfo().getUser();

        operatorUid = user.getUid();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.SEARCH_RETARGETING_ENABLED, true);
        shard = clientInfo.getShard();
        TestAuthHelper.setDirectAuthentication(user);
    }

    @Test
    public void addTextAdGroup() {
        CampaignInfo campaignInfo = createCampaignOfGivenType(
                TestCampaigns.activeTextCampaign(clientInfo.getClientId(), operatorUid));
        var gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(List.of(
                        createDefaultAddTextAdGroupItem(campaignInfo)
                                .withSearchRetargetings(createNewSearchRetargetingItem(campaignInfo))
                        ));
        GdAddAdGroupPayload payload = mutationService
                .addTextAdGroups(clientInfo.getClientId(), operatorUid, operatorUid, gdAddTextAdGroup);

        SoftAssertions soft = new SoftAssertions();
        checkAddResponseAndActualData(soft, payload);
        soft.assertAll();
        steps.campaignSteps().deleteCampaign(shard, campaignInfo.getUid());
    }

    @Test
    public void addTextAdGroup_emptySearchRetargeting() {
        CampaignInfo campaignInfo = createCampaignOfGivenType(
                TestCampaigns.activeTextCampaign(clientInfo.getClientId(), operatorUid));
        var gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(List.of(
                        createDefaultAddTextAdGroupItem(campaignInfo)
                                .withSearchRetargetings(emptyList())
                ));
        GdAddAdGroupPayload payload = mutationService
                .addTextAdGroups(clientInfo.getClientId(), operatorUid, operatorUid, gdAddTextAdGroup);
        SoftAssertions soft = new SoftAssertions();
        checkAddResponseAndEmptyModifiers(soft, payload);
        steps.campaignSteps().deleteCampaign(shard, campaignInfo.getUid());
    }

    @Test
    public void updateTextAdGroup() {
       var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
       var gdUpdateTextAddGroup = new GdUpdateTextAdGroup()
               .withUpdateItems(List.of(
                       createDefaultUpdateTextAdGroupItem(adGroupInfo)
                               .withSearchRetargetings(createSearchRetargetingItemForUpdate(adGroupInfo))
               ));

       GdUpdateAdGroupPayload payload = mutationService
               .updateTextAdGroup(
                       UidAndClientId.of(operatorUid, clientInfo.getClientId()), operatorUid, gdUpdateTextAddGroup);

        SoftAssertions soft = new SoftAssertions();
        checkUpdateResponseAndActualData(soft, payload);
        soft.assertAll();
        steps.campaignSteps().deleteCampaign(shard, adGroupInfo.getUid());
    }

    @Test
    public void updateTextAdGroup_emptySearchRetargeting() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        var gdUpdateTextAddGroup = new GdUpdateTextAdGroup()
                .withUpdateItems(List.of(
                        createDefaultUpdateTextAdGroupItem(adGroupInfo)
                                .withSearchRetargetings(emptyList())
                ));

        GdUpdateAdGroupPayload payload = mutationService
                .updateTextAdGroup(
                        UidAndClientId.of(operatorUid, clientInfo.getClientId()), operatorUid, gdUpdateTextAddGroup);

        SoftAssertions soft = new SoftAssertions();
        checkUpdateResponseAndEmptyModifiers(soft, payload);
        soft.assertAll();
        steps.campaignSteps().deleteCampaign(shard, adGroupInfo.getUid());
    }

    @Test
    public void addMobileContentAdGroup() {
        CampaignInfo campaignInfo = createCampaignOfGivenType(
                TestCampaigns.activeMobileContentCampaign(clientInfo.getClientId(), operatorUid));
        MobileAppInfo appInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);
        campaignRepository
                .setMobileAppIds(clientInfo.getShard(), Map.of(campaignInfo.getCampaignId(), appInfo.getMobileAppId()));

        var gdAddMobileContentAdGroup = new GdAddMobileContentAdGroup()
                .withAddItems(List.of(new GdAddMobileContentAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withAdGroupMinusKeywords(emptyList())
                        .withRegionIds(REGION_IDS)
                        .withCurrentMinimalOsVersion("1.0")
                        .withBidModifiers(new GdUpdateBidModifiers())
                        .withDeviceTypeTargeting(Set.of(PHONE))
                        .withNetworkTargeting(Set.of(WI_FI))
                        .withInterests(emptyList())
                        .withKeywords(emptyList())
                        .withRetargetings(emptyList())
                        .withRelevanceMatch(new GdUpdateAdGroupRelevanceMatchItem().withIsActive(true))
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withSearchRetargetings(createNewSearchRetargetingItem(campaignInfo)))
                );
        GdAddAdGroupPayload payload = mutationService
                .addMobileContentAdGroups(user, operatorUid, gdAddMobileContentAdGroup);

        SoftAssertions soft = new SoftAssertions();
        checkAddResponseAndActualData(soft, payload);
        soft.assertAll();
        steps.campaignSteps().deleteCampaign(shard, campaignInfo.getUid());
    }


    @Test
    public void updateMobileContentAdGroup() {
        var adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        var adGroup = adGroupInfo.getAdGroup();

        var gdUpfateMobileContentAdGroup = new GdUpdateMobileContentAdGroup()
                .withUpdateItems(List.of(new GdUpdateMobileContentAdGroupItem()
                        .withAdGroupName(adGroup.getName())
                        .withAdGroupId(adGroupInfo.getAdGroupId())
                        .withAdGroupMinusKeywords(adGroup.getMinusKeywords())
                        .withRegionIds(mapList(adGroup.getGeo(), Long::intValue))
                        .withCurrentMinimalOsVersion("1.0")
                        .withDeviceTypeTargeting(Set.of(PHONE))
                        .withNetworkTargeting(Set.of(WI_FI))
                        .withInterests(emptyList())
                        .withKeywords(emptyList())
                        .withRetargetings(emptyList())
                        .withRelevanceMatch(new GdUpdateAdGroupRelevanceMatchItem().withIsActive(true))
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withSearchRetargetings(createSearchRetargetingItemForUpdate(adGroupInfo)))
                );
        GdUpdateAdGroupPayload payload = mutationService
                .updateMobileContentAdGroup(clientInfo.getClientId(), operatorUid, gdUpfateMobileContentAdGroup);

        SoftAssertions soft = new SoftAssertions();
        checkUpdateResponseAndActualData(soft, payload);
        soft.assertAll();
        steps.campaignSteps().deleteCampaign(shard, adGroupInfo.getUid());
    }

    @Test
    public void addPerformanceAddGroup() {
        var adgroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        //сейчас новые корректировки(чем и является ретаргетинг на поиске) заводятся через updateSmartAdGroups
        var gdUpdatePerformanceAddGroup = new GdUpdatePerformanceAdGroup()
                .withUpdateItems(List.of(new GdUpdatePerformanceAdGroupItem()
                        .withId(adgroupInfo.getAdGroupId())
                        .withName(AD_GROUP_NAME)
                        .withRegionIds(REGION_IDS)
                        .withMinusKeywords(emptyList())
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withBidModifiers(new GdUpdateBidModifiers())
                        .withPageGroupTags(emptyList())
                        .withTargetTags(emptyList())
                        .withSearchRetargetings(createNewSearchRetargetingItem(adgroupInfo.getCampaignInfo())))
                );
        GdUpdateAdGroupPayload payload = mutationService
                .updatePerformanceAdGroups(clientInfo.getClientId(), operatorUid, gdUpdatePerformanceAddGroup);

        SoftAssertions soft = new SoftAssertions();
        checkUpdateResponseAndActualData(soft, payload);
        steps.campaignSteps().deleteCampaign(shard, adgroupInfo.getUid());
        soft.assertAll();
    }

    private void checkAddResponseAndActualData(SoftAssertions soft, GdAddAdGroupPayload payload) {
        validateResponseSuccessful(payload);
        assertThat(payload.getAddedAdGroupItems()).hasSize(1);

        var addedItem = payload.getAddedAdGroupItems().get(0);
        checkBidModifiersData(soft, addedItem.getAdGroupId());
    }

    private void checkUpdateResponseAndActualData(SoftAssertions soft, GdUpdateAdGroupPayload payload) {
        validateResponseSuccessful(payload);
        assertThat(payload.getUpdatedAdGroupItems()).hasSize(1);

        var updatedItem = payload.getUpdatedAdGroupItems().get(0);
        checkBidModifiersData(soft, updatedItem.getAdGroupId());
    }

    private void checkAddResponseAndEmptyModifiers(SoftAssertions soft, GdAddAdGroupPayload payload) {
        validateResponseSuccessful(payload);
        assertThat(payload.getAddedAdGroupItems()).hasSize(1);

        var adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        checkEmptyBidModifiersData(soft, adGroupId);
    }

    private void checkUpdateResponseAndEmptyModifiers(SoftAssertions soft, GdUpdateAdGroupPayload payload) {
        validateResponseSuccessful(payload);
        assertThat(payload.getUpdatedAdGroupItems()).hasSize(1);

        var adGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        checkEmptyBidModifiersData(soft, adGroupId);
    }

    private void checkBidModifiersData(SoftAssertions soft, long adGroupId) {
        List<BidModifier> modifiers = getActualBidModifiers(adGroupId);
        soft.assertThat(modifiers).hasSize(1);

        List<AbstractBidModifierRetargetingAdjustment> adjustments = ((BidModifierRetargetingFilter) modifiers.get(0))
                .getRetargetingAdjustments();
        soft.assertThat(adjustments).is(matchedBy(allOf(hasSize(2),
                containsInAnyOrder(
                        allOf(
                                hasProperty("retargetingConditionId", equalTo(retargetingConditionId1)),
                                hasProperty("percent", equalTo(0)),
                                hasProperty("accessible", equalTo(true))
                        ),
                        allOf(
                                hasProperty("retargetingConditionId", equalTo(retargetingConditionId2)),
                                hasProperty("percent", equalTo(0)),
                                hasProperty("accessible", equalTo(true))
                        )
                )))
        );
    }

    private void checkEmptyBidModifiersData(SoftAssertions soft, long adGroupId) {
        List<BidModifier> modifiers = getActualBidModifiers(adGroupId);
        soft.assertThat(modifiers).hasSize(0);
    }

    private CampaignInfo createCampaignOfGivenType(Campaign campaign) {
        return steps.campaignSteps().createCampaign(campaign, clientInfo);
    }

    private GdAddTextAdGroupItem createDefaultAddTextAdGroupItem(CampaignInfo campaignInfo) {
        return new GdAddTextAdGroupItem()
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupMinusKeywords(emptyList())
                .withRegionIds(Collections.singletonList(Long.valueOf(MOSCOW_REGION_ID).intValue()))
                .withBidModifiers(new GdUpdateBidModifiers())
                .withLibraryMinusKeywordsIds(emptyList());
    }

    private GdUpdateTextAdGroupItem createDefaultUpdateTextAdGroupItem(AdGroupInfo adGroupInfo) {
        var adGroup = adGroupInfo.getAdGroup();
        return new GdUpdateTextAdGroupItem()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withAdGroupName(adGroup.getName())
                .withAdGroupMinusKeywords(adGroup.getMinusKeywords())
                .withLibraryMinusKeywordsIds(adGroup.getLibraryMinusKeywordsIds())
                .withRegionIds(mapList(adGroup.getGeo(), Long::intValue))
                .withUseBidModifiers(true);
    }

    private List<GdUpdateAdGroupRetargetingItem> createNewSearchRetargetingItem(CampaignInfo campaignInfo) {
        retargetingConditionId1 = steps.retargetingSteps().createDefaultRetargeting(campaignInfo).getRetConditionId();
        retargetingConditionId2 = steps.retargetingSteps().createDefaultRetargeting(campaignInfo).getRetConditionId();
        return List.of(
                new GdUpdateAdGroupRetargetingItem()
                        .withRetCondId(retargetingConditionId1),
                new GdUpdateAdGroupRetargetingItem()
                        .withRetCondId(retargetingConditionId2)
        );
    }

    //добавляем одно условие и удаляем другое
    private List<GdUpdateAdGroupRetargetingItem> createSearchRetargetingItemForUpdate(AdGroupInfo adGroupInfo) {
        retargetingConditionId1 = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo).getRetConditionId();
        long retargetingConditionId3 = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo).getRetConditionId();
        retargetingConditionId2 = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo).getRetConditionId();
        AdGroupBidModifierInfo bidModifierInfo = steps.bidModifierSteps()
                .createAdGroupBidModifierRetargetingFilterWithRetCondIds(
                        adGroupInfo,
                        List.of(retargetingConditionId1, retargetingConditionId3));
        List<AbstractBidModifierRetargetingAdjustment> adjustments =
                ((BidModifierRetargetingFilter) bidModifierInfo.getBidModifier())
                        .getRetargetingAdjustments();
        long retargetingId1 = filterList(adjustments, adj -> Objects.equals(adj.getRetargetingConditionId(),
                retargetingConditionId1))
                .get(0).getId();

        return List.of(
                new GdUpdateAdGroupRetargetingItem()
                        .withRetCondId(retargetingConditionId1)
                        .withId(retargetingId1),
                new GdUpdateAdGroupRetargetingItem()
                        .withRetCondId(retargetingConditionId2)
        );
    }

    private List<BidModifier> getActualBidModifiers(long adGroupId) {
        return bidModifierService
                .getByAdGroupIds(clientInfo.getShard(), Set.of(adGroupId),
                        Set.of(BidModifierType.RETARGETING_FILTER), Set.of(BidModifierLevel.ADGROUP));
    }
}
