package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
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
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.StatusBsSynced.NO;
import static ru.yandex.direct.core.entity.StatusBsSynced.YES;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel.ADGROUP;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel.CAMPAIGN;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingModifier;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка у группы времени последнего изменения и статуса синхронизации с БК при отключении набора корректировок")
public class ToggleBidModifiersBsSyncedGroupTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

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

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public Supplier<BidModifierAdjustment> adjustmentsSupplier;

    @Parameterized.Parameter(2)
    public Function<BidModifierAdjustment, BidModifier> bidModifierFactory;

    @Parameterized.Parameter(3)
    public BidModifierLevel level;

    @Parameterized.Parameter(4)
    public StatusBsSynced expectedCampaignStatus;

    @Parameterized.Parameter(5)
    public StatusBsSynced expectedAdGroupStatus;

    private Campaign campaignBefore;
    private Campaign campaignAfter;
    private AdGroup adGroupBefore;
    private AdGroup adGroupAfter;

    private static long retCondId;

    @Parameterized.Parameters(name = "{0} expectedCampaignStatus={4} expectedAdGroupStatus={5}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Mobile to campaign",
                        (Supplier<BidModifierMobileAdjustment>) TestBidModifiers::createDefaultMobileAdjustment,
                        (Function<BidModifierMobileAdjustment, BidModifierMobile>)
                                (adjustment) -> createEmptyMobileModifier()
                                        .withMobileAdjustment(adjustment),
                        CAMPAIGN, NO, YES},
                {"Mobile to adgroup",
                        (Supplier<BidModifierMobileAdjustment>) TestBidModifiers::createDefaultMobileAdjustment,
                        (Function<BidModifierMobileAdjustment, BidModifierMobile>)
                                (adjustment) -> createEmptyMobileModifier()
                                        .withMobileAdjustment(adjustment),
                        ADGROUP, YES, NO},
                {"Demographics to campaign",
                        (Supplier<BidModifierDemographicsAdjustment>)
                                TestBidModifiers::createDefaultDemographicsAdjustment,
                        (Function<BidModifierDemographicsAdjustment, BidModifierDemographics>)
                                (adjustment) -> createEmptyDemographicsModifier()
                                        .withDemographicsAdjustments(singletonList(adjustment)),
                        CAMPAIGN, NO, YES},
                {"Demographics to adgroup",
                        (Supplier<BidModifierDemographicsAdjustment>)
                                TestBidModifiers::createDefaultDemographicsAdjustment,
                        (Function<BidModifierDemographicsAdjustment, BidModifierDemographics>)
                                (adjustment) -> createEmptyDemographicsModifier()
                                        .withDemographicsAdjustments(singletonList(adjustment)),
                        ADGROUP, YES, NO},
                {"Retargeting to campaign",
                        (Supplier<BidModifierRetargetingAdjustment>)
                                () -> TestBidModifiers.createDefaultRetargetingAdjustment(retCondId),
                        (Function<BidModifierRetargetingAdjustment, BidModifierRetargeting>)
                                (adjustment) -> createEmptyRetargetingModifier()
                                        .withRetargetingAdjustments(singletonList(adjustment)),
                        CAMPAIGN, NO, YES},
                {"Retargeting to adgroup",
                        (Supplier<BidModifierRetargetingAdjustment>)
                                () -> TestBidModifiers.createDefaultRetargetingAdjustment(retCondId),
                        (Function<BidModifierRetargetingAdjustment, BidModifierRetargeting>)
                                (adjustment) -> createEmptyRetargetingModifier()
                                        .withRetargetingAdjustments(singletonList(adjustment)),
                        ADGROUP, YES, NO},
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

    @Before
    public void before() throws Exception {
        // Создаём кампанию и группу
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();

        //Создаем условие ретаргетинга
        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaignInfo.getClientInfo());
        retCondId = retCondition.getRetConditionId();

        //Добавляем корректировку
        BidModifierAdjustment bidModifierAdjustment = adjustmentsSupplier.get();
        BidModifier bidModifier =
                bidModifierFactory.apply(bidModifierAdjustment)
                        .withCampaignId(level == CAMPAIGN ? campaignInfo.getCampaignId() : null)
                        .withAdGroupId(level == ADGROUP ? adGroupInfo.getAdGroupId() : null);

        bidModifierService.add(singletonList(bidModifier),
                campaignInfo.getClientId(), campaignInfo.getUid());

        //Устанавливаем кампании и группе StatusBsSynced=YES и LastChange=now() минус 5 дней
        LocalDateTime lastChange = LocalDateTime.now().minusDays(5);
        campaignInfo.getCampaign().setLastChange(lastChange);
        campaignRepository.setCampaignBsSynced(campaignInfo.getShard(), campaignInfo.getCampaignId(), YES,
                campaignInfo.getCampaign().getLastChange());
        adGroupRepository.updateStatusBsSynced(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId()), YES);
        setAdGroupLastChange(adGroupInfo, lastChange);

        //Получаем данные кампании и группы до изменения
        campaignBefore = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());
        adGroupBefore = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());

        //Выключаем корректировку
        List<UntypedBidModifier> items = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withCampaignId(level == CAMPAIGN ? campaignInfo.getCampaignId() : null)
                .withAdGroupId(level == ADGROUP ? adGroupInfo.getAdGroupId() : null)
                .withType(bidModifier.getType())
                .withEnabled(false));

        bidModifierService.toggle(items, campaignInfo.getClientId(), campaignInfo.getUid());

        //Получаем данные кампании и группы после изменения
        campaignAfter = getCampaignById(campaignInfo.getShard(), campaignInfo.getCampaignId());
        adGroupAfter = getAdGroupById(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
    }

    @Test
    public void campaignStatusBsSyncedTest() {
        assertEquals("StatusBsSynced у кампании", campaignAfter.getStatusBsSynced(), expectedCampaignStatus);
    }

    @Test
    public void adGroupStatusBsSyncedTest() {
        assertEquals("StatusBsSynced у группы", adGroupAfter.getStatusBsSynced(), expectedAdGroupStatus);
    }

    @Test
    public void campaignLastChangeTest() {
        assertEquals("LastChange кампании не должен измениться",
                campaignAfter.getLastChange(), campaignBefore.getLastChange());
    }

    @Test
    public void adGroupLastChangeTest() {
        assertEquals("LastChange группы не должен измениться",
                adGroupAfter.getLastChange(), adGroupBefore.getLastChange());
    }
}
