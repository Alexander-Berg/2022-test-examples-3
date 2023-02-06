package ru.yandex.direct.grid.processing.service.client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignBidModifierInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.ALL_BID_MODIFIER_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class DeleteClientConversionCampaignModifiersTest {
    public static final LocalDateTime NOW = LocalDateTime.now();
    @Autowired
    ClientMutationService clientMutationService;

    @Autowired
    BidModifierService bidModifierService;

    @Autowired
    Steps steps;

    private UserInfo user;

    @Before
    public void before() {
        user = steps.userSteps().createDefaultUser();
    }

    @Test
    public void clientWithOneConversionCampaign_CleanAllBidModifiers() {
        TextCampaign textCampaign = getConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);
        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);

        clientMutationService.deleteClientConversionCampaignModifiers(user.getUid(), user.getClientId());

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).isEmpty();
    }

    @Test
    public void clientWithOneCpiCampaignWithNullGoalId_CleanAllBidModifiers() {
        TextCampaign textCampaign = getConversionCampaignWithBidModifier();
        DbStrategy strategy =
                (DbStrategy) averageCpiStrategy(null).withAutobudget(CampaignsAutobudget.YES);
        textCampaign.setStrategy(strategy);
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);
        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);

        clientMutationService.deleteClientConversionCampaignModifiers(user.getUid(), user.getClientId());

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).isEmpty();
    }

    @Test
    public void clientWithOneArchivedConversionCampaign_NotCleanAllBidModifiers() {
        TextCampaign textCampaign = getConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);

        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);
        steps.campaignSteps().archiveCampaign(campaign.getShard(), campaign.getId());

        clientMutationService.deleteClientConversionCampaignModifiers(user.getUid(), user.getClientId());

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).hasSize(2);
    }

    @Test
    public void clientWithOneNotConversionCampaign_NotCleanAllBidModifiers() {
        TextCampaign textCampaign = getNotConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);

        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);

        clientMutationService.deleteClientConversionCampaignModifiers(user.getUid(), user.getClientId());

        CampaignBidModifierInfo campaignBidModifiers =
                steps.bidModifierSteps().getCampaignBidModifiers(campaign.getShard(),
                campaign.getId(), ALL_BID_MODIFIER_TYPES);

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).hasSize(2);
    }

    @Test
    public void clientWithOneUacCampaign_NotCleanAllModifiers() {
        TextCampaign textCampaign = getConversionCampaignWithBidModifier().withSource(CampaignSource.UAC);
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);

        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);

        clientMutationService.deleteClientConversionCampaignModifiers(user.getUid(), user.getClientId());

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).hasSize(2);
    }

    private void addAdGroupBidModifier(TextCampaignInfo campaign) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaign);
        var demographicForAdGroup = createEmptyDemographicsModifier()
                .withDemographicsAdjustments(createDefaultDemographicsAdjustments())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        bidModifierService.add(List.of(demographicForAdGroup), user.getClientId(), user.getUid());
    }

    private void addCampaignBidModifier(TextCampaignInfo campaign) {
        var demographicForCampaign = createEmptyDemographicsModifier()
                .withDemographicsAdjustments(createDefaultDemographicsAdjustments())
                .withCampaignId(campaign.getId());
        bidModifierService.add(List.of(demographicForCampaign), user.getClientId(), user.getUid());
    }

    private TextCampaign getConversionCampaignWithBidModifier() {
        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(user.getClientInfo());

        DbStrategy strategy =
                (DbStrategy) defaultAverageCpaStrategy((long) RandomNumberUtils.nextPositiveInteger()).withAutobudget(CampaignsAutobudget.YES);
        textCampaign
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withStrategy(strategy);
        return textCampaign;
    }

    private TextCampaign getNotConversionCampaignWithBidModifier() {
        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(user.getClientInfo());

        DbStrategy strategy = manualSearchStrategy();
        textCampaign
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withStrategy(strategy);
        return textCampaign;
    }
}
