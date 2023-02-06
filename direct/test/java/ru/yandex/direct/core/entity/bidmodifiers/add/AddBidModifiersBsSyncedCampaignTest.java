package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка времени последнего изменения и статуса синхронизации с БК при добавлении корректировки ставок")
public class AddBidModifiersBsSyncedCampaignTest {
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private BidModifierService bidModifierService;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public Supplier<BidModifier> bidModifier;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"withMobileAdjustment", (Supplier<BidModifier>) () -> createEmptyClientMobileModifier()
                        .withMobileAdjustment(createDefaultClientMobileAdjustment())},
                {"withDemographicsAdjustment", (Supplier<BidModifier>) () -> createEmptyClientDemographicsModifier()
                        .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments())},
                {"withRetargetingAdjustment", (Supplier<BidModifier>) () -> createEmptyClientRetargetingModifier()
                        .withRetargetingAdjustments(createDefaultClientRetargetingAdjustments(retCondId))},
        };
        return Arrays.asList(data);
    }

    private void setAdGroupLastChange(AdGroupInfo adGroup, LocalDateTime lastChange) {
        ModelChanges<AdGroup> changes = new ModelChanges<>(adGroup.getAdGroupId(), AdGroup.class);
        changes.process(lastChange, AdGroup.LAST_CHANGE);
        AppliedChanges<AdGroup> appliedChanges = changes.applyTo(new TextAdGroup().withId(adGroup.getAdGroupId()));
        adGroupRepository.updateAdGroups(adGroup.getShard(), adGroup.getClientId(), singleton(appliedChanges));

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(adGroup.getShard(), singleton(adGroup.getAdGroupId()));
        adGroup.getAdGroup().setLastChange(adGroups.get(0).getLastChange());
    }

    private Campaign getCampaignById(int shard, long campaignId) {
        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singleton(campaignId));
        return campaigns.get(0);
    }

    private AdGroup getAdGroupById(int shard, long adGroupId) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singleton(adGroupId));
        return adGroups.get(0);
    }

    private AdGroupInfo adGroupInfo;
    private CampaignInfo campaignInfo;
    private static long retCondId;

    private TestContextManager testContextManager;

    @Before
    public void before() throws Exception {
        // Manual Spring integration (because we're using Parametrized runner)
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);

        // Создаём кампанию+группу, устанавливаем им StatusBsSynced=YES и LastChange=now() минус 5 дней
        adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        CampaignInfo campaign = adGroupInfo.getCampaignInfo();
        LocalDateTime lastChange = LocalDateTime.now().minusDays(5);
        campaign.getCampaign().setLastChange(lastChange);
        campaignRepository.setCampaignBsSynced(campaign.getShard(), campaign.getCampaignId(),
                StatusBsSynced.YES, campaign.getCampaign().getLastChange());
        adGroupRepository.updateStatusBsSynced(
                adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId()), StatusBsSynced.YES);
        setAdGroupLastChange(adGroupInfo, lastChange);
        campaignInfo = adGroupInfo.getCampaignInfo();

        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaignInfo.getClientInfo());
        retCondId = retCondition.getRetConditionId();
    }

    @Test
    @Description("Добавляем корректировку в кампанию и следим за изменением StatusBsSynced кампании")
    public void campaignChangedCampaignStatusBsSyncedTest() {
        addBidModifiers(singletonList(bidModifier.get().withCampaignId(campaignInfo.getCampaignId())));
        Campaign campaignAfter = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());

        assertThat("У кампании должен сбрасываться StatusBsSynced",
                campaignAfter.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    @Description("Добавляем корректировку в кампанию и следим за тем, чтобы это не повлияло на StatusBsSynced группы")
    public void campaignChangedGroupStatusBsSyncedTest() {
        AdGroup adGroupBefore = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        addBidModifiers(singletonList(bidModifier.get().withCampaignId(campaignInfo.getCampaignId())));
        AdGroup adGroupAfter = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());

        assertThat("StatusBsSynced группы не должен измениться",
                adGroupAfter.getStatusBsSynced(), is(adGroupBefore.getStatusBsSynced()));
    }

    @Test
    @Description("Добавляем корректировку в кампанию и следим за изменением LastChange кампании")
    public void campaignChangedCampaignLastChangeTest() {
        Campaign campaignBefore = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());
        addBidModifiers(singletonList(bidModifier.get().withCampaignId(campaignInfo.getCampaignId())));
        Campaign campaignAfter = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());

        assertThat("LastChange кампании не должен измениться",
                campaignAfter.getLastChange(), is(campaignBefore.getLastChange()));
    }

    @Test
    @Description("Добавляем корректировку в кампанию и следим за тем, чтобы это не повлияло на LastChange группы")
    public void campaignChangedGroupLastChangeTest() {
        AdGroup adGroupBefore = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        addBidModifiers(singletonList(bidModifier.get().withCampaignId(campaignInfo.getCampaignId())));
        AdGroup adGroupAfter = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());

        assertThat("LastChange группы не должен измениться",
                adGroupAfter.getLastChange(), is(adGroupBefore.getLastChange()));
    }

    @Test
    @Description("Добавляем корректировку в группу и следим за изменением StatusBsSynced кампании")
    public void groupChangedCampaignStatusBsSyncedTest() {
        Campaign campaignBefore = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());
        addBidModifiers(singletonList(bidModifier.get().withAdGroupId(adGroupInfo.getAdGroupId())));
        Campaign campaignAfter = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());

        assertThat("У кампании StatusBsSynced не должен измениться",
                campaignAfter.getStatusBsSynced(), is(campaignBefore.getStatusBsSynced()));
    }

    @Test
    @Description("Добавляем корректировку в группу и следим за изменением StatusBsSynced группы")
    public void groupChangedGroupStatusBsSyncedTest() {
        addBidModifiers(singletonList(bidModifier.get().withAdGroupId(adGroupInfo.getAdGroupId())));
        AdGroup adGroupAfter = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());

        assertThat("У группы должен сбрасываться StatusBsSynced",
                adGroupAfter.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    @Description("Добавляем корректировку в группу и следим за тем, что это не повлияло на LastChange кампании")
    public void groupChangedCampaignLastChangeTest() {
        Campaign campaignBefore = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());
        addBidModifiers(singletonList(bidModifier.get().withAdGroupId(adGroupInfo.getAdGroupId())));
        Campaign campaignAfter = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());

        assertThat("LastChange кампании не должен измениться",
                campaignAfter.getLastChange(), is(campaignBefore.getLastChange()));
    }

    @Test
    @Description("Добавляем корректировку в группу и следим за тем, чтобы это не повлияло на LastChange группы")
    public void groupChangedGroupLastChangeTest() {
        AdGroup adGroupBefore = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        addBidModifiers(singletonList(bidModifier.get().withAdGroupId(adGroupInfo.getAdGroupId())));
        AdGroup adGroupAfter = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());

        assertThat("LastChange группы не должен измениться",
                adGroupAfter.getLastChange(), is(adGroupBefore.getLastChange()));
    }

    private void addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        bidModifierService.add(bidModifiers, campaignInfo.getClientId(), campaignInfo.getUid());
    }
}
