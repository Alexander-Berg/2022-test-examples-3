package ru.yandex.direct.core.entity.adgroup.service.complex.suboperation.common;

import java.time.LocalDateTime;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.service.ComplexBidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile.MOBILE_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo.PERFORMANCE_TGO_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier.DESKTOP_MODIFIER;
import static ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier.MOBILE_MODIFIER;
import static ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier.PERFORMANCE_TGO_MODIFIER;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.deviceBidModifiersAllZeros;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.oneTypeUsedTwiceInComplexModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createComplexBidModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierRetargeting;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierVideo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientVideoModifier;
import static ru.yandex.direct.dbschema.ppc.tables.HierarchicalMultipliers.HIERARCHICAL_MULTIPLIERS;
import static ru.yandex.direct.dbschema.ppc.tables.RetargetingMultiplierValues.RETARGETING_MULTIPLIER_VALUES;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SetBidModifiersSubOperationTest {
    @Autowired
    protected Steps steps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private ComplexBidModifierService complexBidModifierService;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    DslContextProvider dslContextProvider;

    private AdGroupInfo textAdGroup;
    private ClientId clientId;
    private long clientUid;
    private int shard;
    private long adGroupId;
    private long campaignId;
    private long retCondId;

    @Before
    public void before() {
        textAdGroup = adGroupSteps.createActiveTextAdGroup();
        clientId = textAdGroup.getClientId();
        clientUid = textAdGroup.getUid();
        shard = textAdGroup.getShard();
        adGroupId = textAdGroup.getAdGroupId();
        campaignId = textAdGroup.getCampaignId();

        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(textAdGroup.getClientInfo());
        retCondId = retCondition.getRetConditionId();
    }

    @Test
    @Description("Если позвать операцию с пустыми списками, ничего не должно сломаться")
    public void testEmptyInput() {
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                emptyList());
        subOperation.setCampaignTypesBeforePrepare(emptyMap());
        subOperation.setAdGroupWithTypesBeforePrepare(emptyMap());
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(emptyMap(), emptyMap(), emptySet());
        subOperation.apply();
    }

    @Test
    @Description("Если передать универсальные корректировки с повторами, должна сработать валидация")
    public void testSameTypeUsedTwiceValidation() {
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(new ComplexBidModifier().withExpressionModifiers(
                        asList(
                                new BidModifierTraffic().withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER),
                                new BidModifierTraffic().withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)))));
        subOperation.setCampaignTypesBeforePrepare(emptyMap());
        subOperation.setAdGroupWithTypesBeforePrepare(emptyMap());
        ValidationResult<List<ComplexBidModifier>, Defect> result = subOperation.prepare();
        assertThat(result, hasDefectWithDefinition(
                validationError(path(index(0)), oneTypeUsedTwiceInComplexModifier())));
    }

    @Test
    @Description("Валидация должна выполняться, результаты должны быть разложены корректно")
    public void testBasicValidate() {
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(2500, null, null, null, 100, null, false)));
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        ValidationResult<List<ComplexBidModifier>, Defect> result = subOperation.prepare();

        Path errPath1 = path(index(0), field(MOBILE_MODIFIER.name()), field(MOBILE_ADJUSTMENT.name()),
                field(BidModifierMobileAdjustment.PERCENT.name()));
        assertThat(result, hasDefectWithDefinition(
                validationError(errPath1,
                        new Defect<>(
                                NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX,
                                new NumberDefectParams().withMax(1300)))));

        Path errPath2 = path(index(0), field(PERFORMANCE_TGO_MODIFIER.name()),
                field(PERFORMANCE_TGO_ADJUSTMENT.name()));
        assertThat(result, hasDefectWithDefinition(
                validationError(errPath2,
                        BidModifiersDefects.notSupportedMultiplier())));
    }

    @Test
    public void deviceModifiersCanNotBeZeroSimultaneously() {
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(0, null, null, null, null, 0, false)));
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        ValidationResult<List<ComplexBidModifier>, Defect> result = subOperation.prepare();

        assertThat(result, hasDefectWithDefinition(
                validationError(path(index(0), field(MOBILE_MODIFIER.name())),
                        deviceBidModifiersAllZeros())));
        assertThat(result, hasDefectWithDefinition(
                validationError(path(index(0), field(DESKTOP_MODIFIER.name())),
                        deviceBidModifiersAllZeros())));
    }

    @Test
    public void deviceModifiersCanBeZeroSimultaneouslyIfOsTypeSpecified() {
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(0, null, null, null, null, 0, true)));
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        ValidationResult<List<ComplexBidModifier>, Defect> result = subOperation.prepare();
        assertThat(result.flattenErrors(), empty());
    }

    @Test
    @Description("Заменяем уже имеющиеся корректировки на группе")
    public void testBasicApply() {
        bidModifierService.add(getBidModifiersForServiceAdd(adGroupId, 100, 120, 110, 120), clientId, clientUid);

        ComplexBidModifier complexBidModifier = createComplexBidModifier(150, retCondId, 30, true, null, null, false);
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(complexBidModifier)
        );
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(
                ImmutableMap.of(0, adGroupId),
                ImmutableMap.of(adGroupId, campaignId),
                singleton(adGroupId)
        );
        subOperation.apply();

        // Получаем корректировки на группе
        List<BidModifier> bidModifiers =
                bidModifierService.getByAdGroupIds(clientId, singleton(adGroupId), emptySet(),
                        ALL_TYPES, ALL_LEVELS, clientUid);

        // Мобильная должна быть заменена, видео и демография должны быть удалены, ретаргетинговая -- добавлена
        assertThat(bidModifiers, hasSize(2));
        assertThat(bidModifiers, hasItem(matchesMobileModifier(campaignId, adGroupId, 150)));
        assertThat(bidModifiers, hasItem(matchesRetargetingModifier(campaignId, adGroupId, retCondId, true, 30)));
    }

    @Test
    @Description("Изменяем enabled с true на false в демографической корректировке")
    public void testChangeEnabled() {
        bidModifierService.add(getBidModifiersForServiceAdd(adGroupId, 100, 120, 110, 120), clientId, clientUid);

        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withDemographyModifier(createEmptyClientDemographicsModifier()
                        .withEnabled(false)
                        .withDemographicsAdjustments(createDefaultDemographicsAdjustments()));

        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(complexBidModifier)
        );
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(
                ImmutableMap.of(0, adGroupId),
                ImmutableMap.of(adGroupId, campaignId),
                singleton(adGroupId)
        );
        subOperation.apply();

        // Получаем корректировки на группе
        List<BidModifier> bidModifiers =
                bidModifierService.getByAdGroupIds(clientId, singleton(adGroupId), emptySet(),
                        ALL_TYPES, ALL_LEVELS, clientUid);

        // Должна остаться только демографическая корректировка с enabled=false
        assertThat(bidModifiers, hasSize(1));
        assertThat(bidModifiers, hasItem(allOf(
                hasProperty("campaignId", equalTo(campaignId)),
                hasProperty("adGroupId", equalTo(adGroupId)),
                hasProperty("type", equalTo(BidModifierType.DEMOGRAPHY_MULTIPLIER)),
                hasProperty("enabled", equalTo(false)),
                hasProperty("demographicsAdjustments",
                        contains(allOf(
                                hasProperty("age", equalTo(createDefaultDemographicsAdjustment().getAge())),
                                hasProperty("gender", equalTo(createDefaultDemographicsAdjustment().getGender())),
                                hasProperty("percent",
                                        equalTo(createDefaultDemographicsAdjustment().getPercent()))))))));
    }

    @Test
    @Description("Для созданных двух групп с корректировками зовём операцию, в которой для первой группы"
            + " набор корректировок заменяется, а для второй вообще не указан. Ожидается, что набор первой группы"
            + " будет заменён, а набор второй -- удалён из базы")
    public void testApplyWithDeletion() {
        AdGroupInfo adGroup2 = adGroupSteps.createActiveTextAdGroup(textAdGroup.getClientInfo());
        Long adGroupId2 = adGroup2.getAdGroupId();

        bidModifierService.add(getBidModifiersForServiceAdd(adGroupId, 110, 120, 130, 140), clientId, clientUid);
        bidModifierService.add(getBidModifiersForServiceAdd(adGroupId2, 210, 220, 230, 240), clientId, clientUid);

        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(150, retCondId, 160, false, null, null, false))
        );
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(
                ImmutableMap.of(0, this.adGroupId),
                ImmutableMap.of(this.adGroupId, campaignId, adGroupId2, adGroup2.getCampaignId()),
                ImmutableSet.of(this.adGroupId, adGroupId2)  // Со второй группы всё должно быть удалено
        );
        subOperation.apply();

        // Получаем корректировки на группах
        List<BidModifier> bidModifiers =
                bidModifierService.getByAdGroupIds(clientId, ImmutableSet.of(this.adGroupId, adGroupId2),
                        emptySet(), ALL_TYPES, ALL_LEVELS, clientUid);

        // Должны остаться только указанные на входе операции корректировки
        assertThat(bidModifiers, hasSize(2));
        assertThat(bidModifiers, hasItem(matchesMobileModifier(campaignId, adGroupId, 150)));
        assertThat(bidModifiers, hasItem(matchesRetargetingModifier(campaignId, adGroupId, retCondId, false, 160)));
    }

    @Test
    @Description("Для созданной группы со статусом statusBsSynced='Yes' зовём операцию"
            + " установки корректировок. Ожидается, что statusBsSynced будет сброшен")
    public void testApplyResetsBsSyncedStatus() {
        AdGroupInfo adGroup = adGroupSteps.createActiveTextAdGroup(textAdGroup.getClientInfo());
        Long adGroupId = adGroup.getAdGroupId();

        AdGroup adGroupBefore = adGroupService.getAdGroups(clientId, singleton(adGroupId)).get(0);
        assumeThat(adGroupBefore, hasProperty("statusBsSynced", equalTo(StatusBsSynced.YES)));

        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(150, retCondId, 160, false, null, null, false))
        );
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(
                ImmutableMap.of(0, adGroupId),
                ImmutableMap.of(adGroupId, adGroup.getCampaignId()),
                ImmutableSet.of(adGroupId)
        );
        subOperation.apply();

        AdGroup adGroupAfter = adGroupService.getAdGroups(clientId, singleton(adGroupId)).get(0);
        assertThat(adGroupAfter, hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO)));
    }

    @Test
    @Description("Для созданной группы со статусом statusBsSynced='Yes' зовём операцию"
            + " установки корректировок, которая ничего не меняет. Ожидается, что statusBsSynced останется 'Yes'")
    public void testApplyDoesntResetBsSyncedStatusIfNothingChanged() {
        AdGroupInfo adGroup = adGroupSteps.createActiveTextAdGroup(textAdGroup.getClientInfo());
        Long adGroupId = adGroup.getAdGroupId();

        // Несколько корректировок разных типов (single value и multi value)
        BidModifierMobile bidModifierMobile = createDefaultBidModifierMobile(null).withAdGroupId(adGroupId);
        BidModifierVideo bidModifierVideo = createDefaultBidModifierVideo(null).withAdGroupId(adGroupId);
        BidModifierRetargeting bidModifierRetargeting =
                createDefaultBidModifierRetargeting(null, adGroupId, retCondId);
        bidModifierService.add(
                asList(bidModifierMobile, bidModifierRetargeting, bidModifierVideo),
                clientId, clientUid);

        AdGroup adGroupBefore = adGroupService.getAdGroups(clientId, singleton(adGroupId)).get(0);
        assumeThat(adGroupBefore, hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO)));

        // Возвращаем statusBsSynced в 'Yes' искусственно
        adGroupSteps.setAdGroupProperty(
                new AdGroupInfo().withCampaignInfo(adGroup.getCampaignInfo())
                        .withAdGroup(adGroupBefore), AdGroup.STATUS_BS_SYNCED, StatusBsSynced.YES);
        adGroupBefore = adGroupService.getAdGroups(clientId, singleton(adGroupId)).get(0);
        assumeThat(adGroupBefore, hasProperty("statusBsSynced", equalTo(StatusBsSynced.YES)));

        // Выполняем операцию
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(
                        bidModifierMobile.getMobileAdjustment().getPercent(),
                        bidModifierRetargeting.getRetargetingAdjustments().get(0).getRetargetingConditionId(),
                        bidModifierRetargeting.getRetargetingAdjustments().get(0).getPercent(),
                        bidModifierRetargeting.getEnabled(), null,
                        null, false,
                        bidModifierVideo.getVideoAdjustment().getPercent()))
        );
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(
                ImmutableMap.of(0, adGroupId),
                ImmutableMap.of(adGroupId, adGroup.getCampaignId()),
                ImmutableSet.of(adGroupId)
        );
        subOperation.apply();

        AdGroup adGroupAfter = adGroupService.getAdGroups(clientId, singleton(adGroupId)).get(0);
        assertThat(adGroupAfter, hasProperty("statusBsSynced", equalTo(StatusBsSynced.YES)));
    }

    @Test
    @Description("Для созданной группы зовём операцию установки корректировок, которая меняет корректировки."
            + " Ожидается, что lastChange будет изменён.")
    public void testApplyUpdatesLastChange() {
        AdGroupInfo adGroup = adGroupSteps.createActiveTextAdGroup(textAdGroup.getClientInfo());
        Long adGroupId = adGroup.getAdGroupId();

        // Корректировки разных типов (single value и multi value)
        BidModifierVideo bidModifierVideo = createDefaultBidModifierVideo(null).withAdGroupId(adGroupId);
        BidModifierRetargeting bidModifierRetargeting =
                createDefaultBidModifierRetargeting(null, adGroupId, retCondId);
        bidModifierService.add(asList(bidModifierRetargeting, bidModifierVideo), clientId, clientUid);

        // Возвращаем last_change назад во времени
        LocalDateTime oldLastChage = LocalDateTime.now().minusDays(7).withNano(0);
        dslContextProvider.ppc(shard)
                .update(HIERARCHICAL_MULTIPLIERS)
                .set(HIERARCHICAL_MULTIPLIERS.LAST_CHANGE, oldLastChage)
                .where(HIERARCHICAL_MULTIPLIERS.PID.eq(adGroupId))
                .and(HIERARCHICAL_MULTIPLIERS.CID.eq(adGroup.getCampaignId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(RETARGETING_MULTIPLIER_VALUES)
                .set(RETARGETING_MULTIPLIER_VALUES.LAST_CHANGE, oldLastChage)
                .where(RETARGETING_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID.eq(bidModifierRetargeting.getId()))
                .execute();

        // Выполняем операцию
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(
                        null,
                        bidModifierRetargeting.getRetargetingAdjustments().get(0).getRetargetingConditionId(),
                        bidModifierRetargeting.getRetargetingAdjustments().get(0).getPercent() + 50,  // Изменяем
                        bidModifierRetargeting.getEnabled(), null,
                        null, false,
                        bidModifierVideo.getVideoAdjustment().getPercent() + 50))  // Изменяем
        );
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(
                ImmutableMap.of(0, adGroupId),
                ImmutableMap.of(adGroupId, adGroup.getCampaignId()),
                ImmutableSet.of(adGroupId)
        );
        subOperation.apply();

        List<BidModifier> gotModifiers = bidModifierService
                .getByAdGroupIds(clientId, singleton(adGroupId), emptySet(), ALL_TYPES, ALL_LEVELS, clientUid);

        assertThat(gotModifiers, hasSize(2));
        assertThat(gotModifiers, hasItem(allOf(
                hasProperty("type", equalTo(BidModifierType.RETARGETING_MULTIPLIER)),
                hasProperty("lastChange", not(equalTo(oldLastChage))),
                hasProperty("retargetingAdjustments",
                        contains(hasProperty("lastChange", not(equalTo(oldLastChage))))))));
        assertThat(gotModifiers, hasItem(allOf(
                hasProperty("type", equalTo(BidModifierType.VIDEO_MULTIPLIER)),
                hasProperty("lastChange", not(equalTo(oldLastChage))))));
    }

    @Test
    @Description("Для созданной группы зовём операцию установки корректировок, которая ничего не меняет."
            + " Ожидается, что lastChange не будет изменён.")
    public void testApplyDoesntUpdateLastChangeIfNoChanges() {
        AdGroupInfo adGroup = adGroupSteps.createActiveTextAdGroup(textAdGroup.getClientInfo());
        Long adGroupId = adGroup.getAdGroupId();

        // Корректировки разных типов (single value и multi value)
        BidModifierVideo bidModifierVideo = createDefaultBidModifierVideo(null).withAdGroupId(adGroupId);
        BidModifierRetargeting bidModifierRetargeting =
                createDefaultBidModifierRetargeting(null, adGroupId, retCondId);
        bidModifierService.add(asList(bidModifierRetargeting, bidModifierVideo), clientId, clientUid);

        // Возвращаем last_change назад во времени
        LocalDateTime oldLastChage = LocalDateTime.now().minusDays(7).withNano(0);
        dslContextProvider.ppc(shard)
                .update(HIERARCHICAL_MULTIPLIERS)
                .set(HIERARCHICAL_MULTIPLIERS.LAST_CHANGE, oldLastChage)
                .where(HIERARCHICAL_MULTIPLIERS.PID.eq(adGroupId))
                .and(HIERARCHICAL_MULTIPLIERS.CID.eq(adGroup.getCampaignId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(RETARGETING_MULTIPLIER_VALUES)
                .set(RETARGETING_MULTIPLIER_VALUES.LAST_CHANGE, oldLastChage)
                .where(RETARGETING_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID.eq(bidModifierRetargeting.getId()))
                .execute();

        // Выполняем операцию
        SetBidModifiersSubOperation subOperation = new SetBidModifiersSubOperation(
                clientId, clientUid, shard, bidModifierService, complexBidModifierService,
                singletonList(createComplexBidModifier(
                        null,
                        bidModifierRetargeting.getRetargetingAdjustments().get(0).getRetargetingConditionId(),
                        bidModifierRetargeting.getRetargetingAdjustments().get(0).getPercent(),
                        bidModifierRetargeting.getEnabled(), null,
                        null, false,
                        bidModifierVideo.getVideoAdjustment().getPercent()))
        );
        subOperation.setCampaignTypesBeforePrepare(ImmutableMap.of(0, CampaignType.TEXT));
        subOperation.setAdGroupWithTypesBeforePrepare(ImmutableMap.of(0, new TextAdGroup().withType(AdGroupType.BASE)));
        subOperation.prepare();
        subOperation.setAdGroupsInfoBeforeApply(
                ImmutableMap.of(0, adGroupId),
                ImmutableMap.of(adGroupId, adGroup.getCampaignId()),
                ImmutableSet.of(adGroupId)
        );
        subOperation.apply();

        List<BidModifier> gotModifiers = bidModifierService
                .getByAdGroupIds(clientId, singleton(adGroupId), emptySet(), ALL_TYPES, ALL_LEVELS, clientUid);

        assertThat(gotModifiers, hasSize(2));
        assertThat(gotModifiers, hasItem(allOf(
                hasProperty("type", equalTo(BidModifierType.RETARGETING_MULTIPLIER)),
                hasProperty("lastChange", equalTo(oldLastChage)),
                hasProperty("retargetingAdjustments",
                        contains(hasProperty("lastChange", equalTo(oldLastChage)))))));
        assertThat(gotModifiers, hasItem(allOf(
                hasProperty("type", equalTo(BidModifierType.VIDEO_MULTIPLIER)),
                hasProperty("lastChange", equalTo(oldLastChage)))));
    }

    private Matcher<BidModifier> matchesMobileModifier(long campaignId, long adGroupId, int pct) {
        return allOf(
                hasProperty("campaignId", equalTo(campaignId)),
                hasProperty("adGroupId", equalTo(adGroupId)),
                hasProperty("type", equalTo(BidModifierType.MOBILE_MULTIPLIER)),
                hasProperty("mobileAdjustment", hasProperty("percent", equalTo(pct))));
    }

    private Matcher<BidModifier> matchesRetargetingModifier(long campaignId, long adGroupId, long retCondId,
                                                            boolean enabled, int pct) {
        return allOf(
                hasProperty("campaignId", equalTo(campaignId)),
                hasProperty("adGroupId", equalTo(adGroupId)),
                hasProperty("type", equalTo(BidModifierType.RETARGETING_MULTIPLIER)),
                hasProperty("enabled", equalTo(enabled)),
                hasProperty("retargetingAdjustments",
                        contains(allOf(
                                hasProperty("retargetingConditionId", equalTo(retCondId)),
                                hasProperty("percent", equalTo(pct))))));
    }

    public static List<BidModifier> getBidModifiersForServiceAdd(long adGroupId, int mobilePct, int videoPct,
                                                                 int demographyPct1, int demographyPct2) {
        BidModifierDemographicsAdjustment adjustment1 = new BidModifierDemographicsAdjustment()
                .withAge(AgeType._25_34).withGender(GenderType.FEMALE).withPercent(demographyPct1);
        BidModifierDemographicsAdjustment adjustment2 = new BidModifierDemographicsAdjustment()
                .withAge(AgeType._18_24).withGender(GenderType.FEMALE).withPercent(demographyPct2);

        return asList(
                createEmptyClientMobileModifier()
                        .withAdGroupId(adGroupId)
                        .withEnabled(true)
                        .withMobileAdjustment(new BidModifierMobileAdjustment().withPercent(mobilePct)),
                createEmptyClientDemographicsModifier()
                        .withAdGroupId(adGroupId)
                        .withEnabled(true)
                        .withDemographicsAdjustments(asList(adjustment1, adjustment2)),
                createEmptyClientVideoModifier()
                        .withAdGroupId(adGroupId)
                        .withEnabled(true)
                        .withVideoAdjustment(new BidModifierVideoAdjustment().withPercent(videoPct)));
    }
}
