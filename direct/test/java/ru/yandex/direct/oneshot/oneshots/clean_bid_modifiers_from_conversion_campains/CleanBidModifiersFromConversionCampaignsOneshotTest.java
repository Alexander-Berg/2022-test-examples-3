package ru.yandex.direct.oneshot.oneshots.clean_bid_modifiers_from_conversion_campains;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.ALL_BID_MODIFIER_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;

@OneshotTest
@RunWith(SpringRunner.class)
public class CleanBidModifiersFromConversionCampaignsOneshotTest {

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    ShardHelper shardHelper;

    @Autowired
    BidModifierService bidModifierService;

    @Autowired
    Steps steps;

    PpcPropertiesSupport ppcPropertiesSupport;

    private YtOperator operator;

    private CleanBidModifiersFromConversionCampaignsOneshot oneshot;
    private UserInfo user;

    @Before
    public void before() {
        var provider = mock(YtProvider.class);
        operator = mock(YtOperator.class);
        doReturn(operator).when(provider).getOperator(any());

        mockPpcPropertiesSupport();

        oneshot = new CleanBidModifiersFromConversionCampaignsOneshot(provider, campaignTypedRepository,
                shardHelper, bidModifierService, ppcPropertiesSupport);

        user = steps.userSteps().createDefaultUser();


    }

    private void mockPpcPropertiesSupport() {
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        PpcProperty<Long> relaxTime = mock(PpcProperty.class);
        doReturn(0L).when(relaxTime).getOrDefault(any());
        PpcProperty<Long> chunkSize = mock(PpcProperty.class);
        doReturn(10L).when(chunkSize).getOrDefault(any());
        doReturn(relaxTime).when(ppcPropertiesSupport).get(eq(PpcPropertyNames.CLEAN_CONVERSION_BID_MODIFIERS_RELAX_TIME));
        doReturn(chunkSize).when(ppcPropertiesSupport).get(eq(PpcPropertyNames.CLEAN_CONVERSION_BID_MODIFIERS_CHUNK_SIZE));
    }

    @Test
    public void executeOneshotForConversionCampaignWithBidModifiers_NotCleanAllBidModifiers() {
        TextCampaign textCampaign = getConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);
        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);

        var rowToReturn = mock(ClientsToCleanTableRow.class);
        doReturn(user.getClientId().asLong()).when(rowToReturn).convert();

        doAnswer(invocation -> {
            //noinspection unchecked
            ((Consumer<ClientsToCleanTableRow>) invocation.getArgument(1)).accept(rowToReturn);
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());

        oneshot.execute(new CleanParam(YtCluster.HAHN, "path", user.getUid()), new CleanState(0));

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).hasSize(2);
    }

    @Test
    public void executeOneshotForNotConversionCampaignWithBidModifiers_NotCleanAllBidModifiers() {
        TextCampaign textCampaign = getNotConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);
        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);

        var rowToReturn = mock(ClientsToCleanTableRow.class);
        doReturn(user.getClientId().asLong()).when(rowToReturn).convert();

        doAnswer(invocation -> {
            //noinspection unchecked
            ((Consumer<ClientsToCleanTableRow>) invocation.getArgument(1)).accept(rowToReturn);
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());

        oneshot.execute(new CleanParam(YtCluster.HAHN, "path", user.getUid()), new CleanState(0));

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).hasSize(2);
    }

    @Test
    public void executeOneshotForPayForConversionCampaignWithBidModifiers_CleanAllBidModifiers() {
        TextCampaign textCampaign = getPayForConversionConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);
        addCampaignBidModifier(campaign);
        addAdGroupBidModifier(campaign);

        var rowToReturn = mock(ClientsToCleanTableRow.class);
        doReturn(user.getClientId().asLong()).when(rowToReturn).convert();

        doAnswer(invocation -> {
            //noinspection unchecked
            ((Consumer<ClientsToCleanTableRow>) invocation.getArgument(1)).accept(rowToReturn);
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());

        oneshot.execute(new CleanParam(YtCluster.HAHN, "path", user.getUid()), new CleanState(0));

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        assertThat(actualBidModifiers).isEmpty();
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
                (DbStrategy) defaultAverageCpaStrategy((long) RandomNumberUtils.nextPositiveInteger())
                        .withAutobudget(CampaignsAutobudget.YES);

        strategy.getStrategyData().setPayForConversion(false);
        textCampaign
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withStrategy(strategy);
        return textCampaign;
    }

    private TextCampaign getPayForConversionConversionCampaignWithBidModifier() {
        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(user.getClientInfo());

        DbStrategy strategy =
                (DbStrategy) defaultAverageCpaStrategy((long) RandomNumberUtils.nextPositiveInteger())
                        .withAutobudget(CampaignsAutobudget.YES);

        strategy.getStrategyData().setPayForConversion(true);

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
